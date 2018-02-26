/**
 * KAR Geo Tool - applicatie voor het registreren van KAR meldpunten
 *
 * Copyright (C) 2009-2013 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.incaa;

import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.validation.SimpleError;
import nl.b3p.kar.hibernate.ActivationPoint;
import nl.b3p.kar.hibernate.ActivationPointSignal;
import nl.b3p.kar.hibernate.DataOwner;
import nl.b3p.kar.hibernate.Gebruiker;
import nl.b3p.kar.hibernate.KarAttributes;
import nl.b3p.kar.hibernate.Movement;
import nl.b3p.kar.hibernate.MovementActivationPoint;
import nl.b3p.kar.hibernate.RoadsideEquipment;
import nl.b3p.kar.hibernate.VehicleType;
import nl.b3p.kar.imp.KV9ValidationError;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.Stripersist;

/**
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
public class IncaaImport {

    public IncaaImport() {
    }

    private static final int RIJKSDRIEHOEKSTELSEL = 28992;

    // Intermediate format: Map<karrAddres, Map<signalGroupNumber, Movement>
    private Map<Integer, Map<Integer, Movement>> movements;
    // Opzoeklijstje obv karaddress
    private Map<Integer, RoadsideEquipment> rseqs;
    // Hou de Points per rseq bij, om later de positie van de rseq te genereren (centroïde)
    private Map<Integer, List<Point>> rseqLocations;


    /**
     * Importeer een reader van een .ptx bestand.
     * @param in Reader van een INCAA .ptx bestand.
     * @param g Gebruiker om het voor te importeren
     * @param context Context om meldingen terug te geven naar de gebruiker over rseqs die niet geimporteerd konden worden
     * @return De lijst van geïmporteerde roadside equipments
     * @throws Exception Wordt gegooid als het parsen/opslaan mis gaat.
     */
    public List<RoadsideEquipment> importPtx(Reader in, Gebruiker g, ActionBeanContext context) throws Exception {

        JSONObject profile = new JSONObject(g.getProfile());
        JSONObject defaultKarAttributes = profile.getJSONObject("defaultKarAttributes");
        // (Her)initialiseer de intermediate formats.
        movements = new HashMap();
        rseqs = new HashMap();
        rseqLocations = new HashMap();

        // Definieer een parser en parse de ptx file
        CSVFormat format = CSVFormat.newFormat('\t');
        format.withQuote('"');
        format.withCommentMarker('#');
        Iterable<CSVRecord> parser = format.parse(in);
        //Iterable<CSVRecord> parser = CSVFormat.newBuilder().withCommentStart('#').withDelimiter('\t').withQuoteChar('"').parse(in);

        List<Message> messages = new ArrayList<Message>();
        for (CSVRecord csvRecord : parser) {
            try{
                parseRecord(csvRecord, messages);
            }catch (IllegalArgumentException e){
                context.getValidationErrors().add("Kruispunt mislukt", new SimpleError(e.getMessage()));
            }
        }

        // Sla de boel op
        EntityManager em = Stripersist.getEntityManager();
        List<RoadsideEquipment> savedRseqs = new ArrayList();
        // ??? XXX is rseqs niet altijd empty?
        for (Integer karAddress : rseqs.keySet()) {
            RoadsideEquipment rseq = rseqs.get(karAddress);
            if(g.canEdit(rseq)){
                // Maak van alle punten binnen een rseq een centroïde en gebruik dat als locatie van de rseq zelf
                List<Point> points = rseqLocations.get(rseq.getKarAddress());
                GeometryFactory gf = new GeometryFactory(new PrecisionModel(), RIJKSDRIEHOEKSTELSEL);
                GeometryCollection gc = new GeometryCollection(points.toArray(new Point[points.size()]), gf);
                rseq.setLocation(gc.getCentroid());

                for (ActivationPoint point : rseq.getPoints()) {
                    em.persist(point);
                }
                rseq.setVehicleType(rseq.determineType());
                int validationErrors = rseq.validateKV9(new ArrayList<KV9ValidationError>());
                rseq.setValidationErrors(validationErrors);
                getKarAttributes(defaultKarAttributes, rseq);
                em.persist(rseq);
                savedRseqs.add(rseq);
                messages.add(new SimpleMessage("Kruispunt met KAR-adres " + rseq.getKarAddress() + " is geïmporteerd"));
            }
        }
        context.getMessages().add(new SimpleMessage(("Er zijn " + rseqs.size() + " verkeerssystemen succesvol geïmporteerd.")));
        context.getMessages().addAll(messages);
        em.getTransaction().commit();
        return savedRseqs;
    }

    private void parseRecord(CSVRecord record,List<Message> messages) throws IllegalArgumentException{
        ActivationPoint ap = new ActivationPoint();
        EntityManager em = Stripersist.getEntityManager();

        // Haal de data uit het csv record
        String beheercode = record.get(0);
        String x = record.get(1);
        String y = record.get(2);
        Integer karAddress = Integer.parseInt(record.get(3));
        Integer signalGroupNumber = Integer.parseInt(record.get(4));
        Integer distanceTillStopline = Integer.parseInt(record.get(5));
        Integer timeTillStopline = Integer.parseInt(record.get(6));
        Integer commandType = Integer.parseInt(record.get(7));

        Integer triggerType = 0;
        try {
            triggerType = Integer.parseInt(record.get(8));// Niet zeker. Mogelijke vertalingen: 0,1: forced, 2,3,4,5: manual
        } catch (IndexOutOfBoundsException ex) {
            // Niet nodig, optioneel veld
        }

        if(triggerType < 0 ||triggerType > 255){
            throw new IllegalArgumentException("Triggertype not recognized: " + triggerType);
        }
        

        // Alleen vehicletypes behorende bij de groep hulpdiensten: ptx is voor hulpdiensten
        List<VehicleType> vehicleTypes = em.createQuery("from VehicleType where groep = :h order by nummer")
                .setParameter("h", VehicleType.VEHICLE_TYPE_HULPDIENSTEN)
                .getResultList();

        // Maak een ActivationPointSignal obv de gegevens uit de ptx
        ActivationPointSignal aps = new ActivationPointSignal();
        aps.setDistanceTillStopLine(distanceTillStopline);
        aps.setSignalGroupNumber(signalGroupNumber);
        aps.setKarCommandType(commandType);
        aps.setTriggerType("" + triggerType);
        aps.setVehicleTypes(vehicleTypes);

        // Haal objecten op om aan elkaar te linken
        DataOwner dataOwner = getDataOwner(beheercode);
        Movement movement = getMovement(karAddress, signalGroupNumber, dataOwner, messages);
        RoadsideEquipment rseq = movement.getRoadsideEquipment();

        Set<ActivationPoint> ps = rseq.getPoints();
        int maxNr = 0;
        for(ActivationPoint p: ps) {
            maxNr = Math.max(maxNr, p.getNummer());
        }
        ap.setNummer(maxNr + 1);
        ps.add(ap);

        // Maak geometrie
        int xInt = Integer.parseInt(x);
        int yInt = Integer.parseInt(y);
        ap.setX(xInt);
        ap.setY(yInt);

        addPoint(karAddress, ap.getLocation());
        ap.setRoadsideEquipment(rseq);

        em.persist(ap);
        // Maak MovementActivationPoint
        MovementActivationPoint map = new MovementActivationPoint();
        map.setPoint(ap);
        map.setMovement(movement);
        map.setBeginEndOrActivation(MovementActivationPoint.ACTIVATION); // geen begin/eindpunt in INCAA
        map.setSignal(aps);
        em.persist(map);

        movement.getPoints().add(map);
    }

    /**
     * Haal de movement op behorende bij het karAdress en signalGroupNumber. Binnen een .ptx gaan we ervan uit dat een karAdres 1 rseq
     * vertegenwoordigd en een punten gegroepeerd op signalGroupNumber een movement vormen.
     * Mocht deze movement nog niet bestaan, dan wordt hij aangemaakt.
     * @param karAddress KarAddress dat binnen dit .ptx bestand een rseq vertegenwoordigd
     * @param signalGroupNumber Signaalgroepnummer dat binnen de rseq van @karAddress een movement identificeert
     * @return De movement binnen een rseq met karAddress @karAddres en met signaalgroepnummer @signalGroupNumber
     */
    private Movement getMovement(Integer karAddress, Integer signalGroupNumber, DataOwner dataOwner,List<Message> messages) throws IllegalArgumentException{
        Movement mvmnt = null;
        RoadsideEquipment rseq = getRseq(karAddress,dataOwner, messages);

        EntityManager em = Stripersist.getEntityManager();
        if (!movements.containsKey(karAddress)) {
            Map<Integer, Movement> signalGroupMovement = new HashMap<Integer, Movement>();

            mvmnt = new Movement();
            mvmnt.setRoadsideEquipment(rseq);
            mvmnt.setNummer(rseq.getMovements().size() + 1);
            em.persist(mvmnt);
            signalGroupMovement.put(signalGroupNumber, mvmnt);

            rseq.getMovements().add(mvmnt);
            movements.put(karAddress, signalGroupMovement);
        } else {
            Map<Integer, Movement> signalGroupMovement = movements.get(karAddress);
            if (!signalGroupMovement.containsKey(signalGroupNumber)) {
                mvmnt = new Movement();
                mvmnt.setRoadsideEquipment(rseq);
                mvmnt.setNummer(rseq.getMovements().size() + 1);
                signalGroupMovement.put(signalGroupNumber, mvmnt);
                em.persist(mvmnt);

                rseq.getMovements().add(mvmnt);
                movements.put(karAddress, signalGroupMovement);
            }
        }

        mvmnt = movements.get(karAddress).get(signalGroupNumber);
        return mvmnt;
    }

    /**
     *
     * @param karAddres Haal de rseq op dat aangeduid wordt met een karAddress
     * @return
     */
    private RoadsideEquipment getRseq(Integer karAddres, DataOwner dataOwner, List<Message> messages) throws IllegalArgumentException{
        RoadsideEquipment rseq = null;

        if (!rseqs.containsKey(karAddres)) {
            EntityManager em = Stripersist.getEntityManager();

            List<RoadsideEquipment> rseqsList = em.createQuery("FROM RoadsideEquipment WHERE dataOwner = :do and karAddress = :karAddress", RoadsideEquipment.class).setParameter("do", dataOwner).setParameter("karAddress", karAddres).getResultList();
            if (rseqsList.size() > 0) {
                if (rseqsList.size() == 1) {
                    rseq = rseqsList.get(0);

                    if (rseq.getVehicleType() == null || rseq.getVehicleType().equals(VehicleType.VEHICLE_TYPE_OV)) {
                        messages.add(new SimpleMessage("Kruispunt met KAR-adres " + karAddres + " bestaat al, maar heeft nog geen hulpdienstpunten. Hulpdienstpunten worden toegevoegd."));
                    }else{
                        throw new IllegalArgumentException("Kruispunt met KAR-adres " + karAddres + " heeft al hulpdienst punten");
                    }
                } else {
                    throw new IllegalArgumentException("Meerdere kruispunten gevonden voor KAR-adres " + karAddres + " en wegbeheerder " + dataOwner.getOmschrijving());
                }
            }

            if (rseq == null) {
                rseq = new RoadsideEquipment();
                rseq.setKarAddress(karAddres);
                rseq.setDataOwner(dataOwner);
                rseq.setType(RoadsideEquipment.TYPE_CROSSING);
                rseq.setValidFrom(new Date());
                em.persist(rseq);
            }
            rseqs.put(karAddres, rseq);
        }
        rseq = rseqs.get(karAddres);
        return rseq;
    }

    private DataOwner getDataOwner(String code) {
        try{
            EntityManager em = Stripersist.getEntityManager();
            DataOwner dataOwner = (DataOwner) em.createQuery("from DataOwner where code = :code ").setParameter("code", code).getSingleResult();
            return dataOwner;
        }catch(NoResultException ex){
            throw new NoResultException("Kan databeheerder niet ophalen voor code: "+ code);
        }
    }

    /**
     * Voeg een punt toe aan een lijstje voor een rseq met kar adres @karAddress. Voor latere verwerking tot centroïde voor de locatie van het rseq..
     * @param karAddress
     * @param p
     */
    private void addPoint(Integer karAddress, Point p) {
        if (!rseqLocations.containsKey(karAddress)) {
            rseqLocations.put(karAddress, new ArrayList<Point>());
        }
        rseqLocations.get(karAddress).add(p);
    }

    private void getKarAttributes(JSONObject attributes, RoadsideEquipment rseq) throws JSONException{

        for (Iterator it = attributes.keys(); it.hasNext();) {
            String serviceType = (String) it.next();
            JSONArray perCommandType = attributes.getJSONArray(serviceType);

            KarAttributes ka = new KarAttributes(
                    serviceType,
                    ActivationPointSignal.COMMAND_INMELDPUNT,
                    perCommandType.getJSONArray(0));
            if (ka.getUsedAttributesMask() != 0) {
                rseq.getKarAttributes().add(ka);
            }
            ka = new KarAttributes(
                    serviceType,
                    ActivationPointSignal.COMMAND_UITMELDPUNT,
                    perCommandType.getJSONArray(1));
            if (ka.getUsedAttributesMask() != 0) {
                rseq.getKarAttributes().add(ka);
            }
            ka = new KarAttributes(
                    serviceType,
                    ActivationPointSignal.COMMAND_VOORINMELDPUNT,
                    perCommandType.getJSONArray(2));
            if (ka.getUsedAttributesMask() != 0) {
                rseq.getKarAttributes().add(ka);
            }
        }
    }
}
