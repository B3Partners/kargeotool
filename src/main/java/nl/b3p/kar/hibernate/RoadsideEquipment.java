/**
 * KAR Geo Tool - applicatie voor het registreren van KAR meldpunten
 *
 * Copyright (C) 2009-2013 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package nl.b3p.kar.hibernate;

import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import nl.b3p.kar.jaxb.Namespace;
import nl.b3p.kar.jaxb.TmiDateAdapter;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.*;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import nl.b3p.geojson.GeoJSON;
import nl.b3p.kar.imp.KV9ValidationError;
import nl.b3p.kar.jaxb.XmlB3pRseq;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.Stripersist;

/**
 * Klasse voor het beschrijven van RoadsideEquipment zoals VRI's
 *
 * @author Matthijs Laan
 */
@Entity
@Table(name = "roadside_equipment")
@XmlRootElement(name="RSEQDEF")
@XmlType(name="RSEQDEFType",
        propOrder={
            "dataOwner",
            "karAddress",
            "type",
            "validFrom",
            "validUntil",
            "crossingCode",
            "town",
            "description",
            "karAttributes",
            "points",
            "movements",
            "delimiter",
            "extraXml"
        }
)
@XmlAccessorType(XmlAccessType.FIELD)
public class RoadsideEquipment implements Comparable<RoadsideEquipment> {

    private static final int RIJKSDRIEHOEKSTELSEL = 28992;
    /**
     * Waarde voor de functie van het verkeerssysteem welke een verkeersregel-
     * installatie (VRI) aanduidt.
     */
    public static final String TYPE_CROSSING = "CROSSING";

    /**
     * Waarde voor de functie van het verkeerssysteem, duidt een bewaking aan
     * die een alarmsignaal geeft bij het naderen van een OV of hulpdienst
     * voertuig.
     */
    public static final String TYPE_GUARD = "GUARD";

    /**
     * Waarde voor de functie van het verkeerssysteem om een afsluiting aan te
     * duiden, een wegafsluitend object dat bij het naderen van een OV of
     * hulpdienst voertuig tijdelijk verdwijnt (in de weg verzinkt), en na
     * passage van het voertuig weer verschijnt.
     */
    public static final String TYPE_BAR = "BAR";

    /**
     * Automatisch gegenereerde unieke sleutel volgens een sequence. Niet zichtbaar
     * in Kv9 XML export.
     */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @XmlTransient
    private Long id;

    @ManyToOne(optional=false)
    @JoinColumn(name = "data_owner")
    @XmlElement(name="dataownercode")
    @XmlJavaTypeAdapter(DataOwner.class)
    private DataOwner dataOwner;

    @XmlTransient
    private Point location;

    /**
     * Het KAR adres (SID) van het verkeerssysteem. Verplicht voor Kv9.
     */
    @XmlElement(name="karaddress")
    @Column(name="kar_address")
    private Integer karAddress;

    /**
     * Datum vanaf wanneer het verkeerssysteem actief is (inclusief). Verplicht
     * voor Kv9.
     */
    @Temporal(TemporalType.DATE)
    @XmlElement(name="validfrom")
    @XmlJavaTypeAdapter(TmiDateAdapter.class)
    @Column(name="valid_from")
    private Date validFrom;

    /**
     * Datum tot aan wanneer het verkeersysteem actief is (exclusief).
     */
    @Temporal(TemporalType.DATE)
    @XmlElement(name="validuntil")
    @XmlJavaTypeAdapter(TmiDateAdapter.class)
    @Column(name="valid_until")
    private Date validUntil;

    /**
     * De functie van het verkeerssysteem, zie de TYPE_ constanten in
     * RoadsideEquipment.
     */
    @XmlElement(name="rseqtype")
    private String type;
 
   /**
     * Identificeert het kruispunt volgens codering domein DataOwner
     * (wegbeheerder). Verplicht voor Kv9.
     */
    @XmlElement(name="crossingcode")
    @Column(name="crossing_code")
    private String crossingCode;

    /**
     * De plaats waar het verkeerssysteem staat. Verplicht voor Kv9.
     */
    @Column(length=50)
    @XmlElement(name="town")
    private String town;

    /**
     * Omschrijving van het verkeerssysteem.
     */
    @Column(length=255)
    @XmlElement(name="description")
    private String description;

    /**
     * Voor service en command types te versturen KAR attributen.
     */
    @ElementCollection
    @JoinTable(name = "roadside_equipment_kar_attributes",
            joinColumns={
                @JoinColumn(name="roadside_equipment")
            })
    @OrderColumn(name="list_index")
    @XmlElement(name="KARATTRIBUTES")
    private List<KarAttributes> karAttributes = new ArrayList<KarAttributes>();

    @OneToMany(cascade=CascadeType.ALL, mappedBy="roadsideEquipment", orphanRemoval=true)
    @Sort(type=SortType.NATURAL)
    @XmlElement(name="MOVEMENT")
    private SortedSet<Movement> movements = new TreeSet<Movement>();

    @OneToMany(orphanRemoval=true,cascade=CascadeType.ALL, mappedBy="roadsideEquipment")
    @XmlElement(name="ACTIVATIONPOINT")
    @Sort(type=SortType.NATURAL)
    private SortedSet<ActivationPoint> points = new TreeSet<ActivationPoint>();

    @Column(length=4096)
    @XmlTransient
    private String memo;

    @XmlTransient
    @Column(name="validation_errors")
    private Integer validationErrors;

    @Transient
    @XmlElement(namespace=Namespace.NS_BISON_TMI8_KV9_CORE)
    private String delimiter = "";

    @XmlTransient
    @Column(name="vehicle_type")
    private String vehicleType;

    @XmlTransient
    @Column(name="ready_for_export")
    private boolean readyForExport;
    
    @XmlTransient
    @Transient
    private String vehicleTypeToExport;

    //<editor-fold defaultstate="collapsed" desc="getters en setters">

    public String getVehicleTypeToExport() {
        return vehicleTypeToExport;
    }

    public void setVehicleTypeToExport(String vehicleTypeToExport) {
        this.vehicleTypeToExport = vehicleTypeToExport;
    }

    /**
     *
     * @return id id
     */
    public Long getId() {
        return id;
    }

    /**
     *
     * @param id id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     *
     * @return dataOwner
     */
    public DataOwner getDataOwner() {
        return dataOwner;
    }

    /**
     *
     * @param dataOwner dataOwner
     */
    public void setDataOwner(DataOwner dataOwner) {
        this.dataOwner = dataOwner;
    }

    /**
     *
     * @return karAddress
     */
    public Integer getKarAddress() {
        return karAddress;
    }

    /**
     *
     * @param karAddress karAddress
     */
    public void setKarAddress(Integer karAddress) {
        this.karAddress = karAddress;
    }

    /**
     *
     * @return validFrom
     */
    public Date getValidFrom() {
        return validFrom;
    }

    /**
     *
     * @param validFrom validFrom
     */
    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    /**
     *
     * @return validUntil
     */
    public Date getValidUntil() {
        return validUntil;
    }

    /**
     *
     * @param validUntil validUntil
     */
    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    /**
     *
     * @return memo
     */
    public String getMemo() {
        return memo;
    }

    /**
     *
     * @param memo memo
     */
    public void setMemo(String memo) {
        this.memo = memo;
    }

    /**
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return crossingCode
     */
    public String getCrossingCode() {
        return crossingCode;
    }

    /**
     *
     * @param crossingCode crossingCode
     */
    public void setCrossingCode(String crossingCode) {
        this.crossingCode = crossingCode;
    }

    /**
     *
     * @return town
     */
    public String getTown() {
        return town;
    }

    /**
     *
     * @param town town
     */
    public void setTown(String town) {
        this.town = town;
    }

    /**
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return location
     */
    public Point getLocation() {
        return location;
    }

    /**
     *
     * @param location location
     */
    public void setLocation(Point location) {
        this.location = location;
    }

    /**
     *
     * @return karAttributes
     */
    public List<KarAttributes> getKarAttributes() {
        return karAttributes;
    }

    /**
     *
     * @param karAttributes karAttributes
     */
    public void setKarAttributes(List<KarAttributes> karAttributes) {
        this.karAttributes = karAttributes;
    }

    /**
     *
     * @return movements
     */
    public SortedSet<Movement> getMovements() {
        return movements;
    }

    /**
     *
     * @param movements movements
     */
    public void setMovements(SortedSet<Movement> movements) {
        this.movements = movements;
    }

    /**
     *
     * @return points
     */
    public SortedSet<ActivationPoint> getPoints() {
        return points;
    }

    /**
     *
     * @param points points
     */
    public void setPoints(SortedSet<ActivationPoint> points) {
        this.points = points;
    }

    /**
     *
     * @return validationErrors
     */
    public Integer getValidationErrors() {
        return validationErrors;
    }

    /**
     *
     * @param validationErrors validationErrors
     */
    public void setValidationErrors(Integer validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     *
     * @return vehicleType
     */
    public String getVehicleType() {
        return vehicleType;
    }

    /**
     *
     * @param vehicleType vehicleType
     */
    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    /**
     *
     * @return readyForExport
     */
    public boolean isReadyForExport() {
        return readyForExport;
    }

    /**
     *
     * @param readyForExport readyForExport
     */
    public void setReadyForExport(boolean readyForExport) {
        this.readyForExport = readyForExport;
    }

    //</editor-fold>

    /**
     *
     * @param number number to retrieve point for
     * @return The activationpoint for number
     */

    public ActivationPoint getPointByNumber(int number) {
        for(ActivationPoint p: points) {
            if(p.getNummer() != null && p.getNummer() == number) {
                return p;
            }
        }
        return null;
    }

    /**
     *
     * @return boolean if it's valid
     */
    public boolean isValid(){
        Date now = new Date();
        if(validFrom.compareTo(now) <= 0 && (validUntil == null || validUntil.after(now))){
            return true;
        }else{
            return false;
        }
    }

    /**
     * Checkt of dit rseq signaalgroepen voor het opgegeven @param vehicleType heeft.
     * @param vehicleType Mogelijk waardes: OV, Hulpdiensten, Gemixt/null (geeft altijd true)
     * @return true/false
     */
    public boolean hasSignalForVehicleType(String vehicleType){
        if(vehicleType == null || vehicleType.equalsIgnoreCase("Gemixt")){
            return true;
        }
        for (Movement movement : movements) {
            for (MovementActivationPoint movementActivationPoint : movement.getPoints()) {
                if( movementActivationPoint.getBeginEndOrActivation().equalsIgnoreCase(MovementActivationPoint.ACTIVATION)){
                    for (VehicleType vehicleType1 : movementActivationPoint.getSignal().getVehicleTypes()) {
                        if(vehicleType1.getGroep().equalsIgnoreCase(vehicleType)){
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    /**
     * Methode converteert dit RoadsideEquipment object in een JSON object
     *
     * @return JSON object
     * @throws JSONException JSONException 
     */
    public JSONObject getRseqGeoJSON() throws JSONException {
        JSONObject gj = new JSONObject();
        gj.put("type","Feature");
        gj.put("geometry", GeoJSON.toGeoJSON(location));
        JSONObject p = new JSONObject();
        p.put("crossingCode", crossingCode);
        p.put("description", description);
        p.put("karAddress", karAddress);
        p.put("readyForExport", readyForExport);
        p.put("town", town);
        p.put("type", type);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        p.put("validFrom", sdf.format(validFrom));
        p.put("dataOwner", dataOwner.getCode());
        p.put("validUntil", validUntil == null ? null : sdf.format(validUntil));
        p.put("memo", memo);
        p.put("id",id);
        p.put("validationErrors", validationErrors);
        p.put("vehicleType", vehicleType);

        // geen kar attributes
        gj.put("properties", p);
        return gj;
    }

    /**
     * Methode bouwt het volledige JSON object op voor dit RoadsideEquipment
     * object.
     *
     * @return JSON Object
     * @throws JSONException JSONException
     */
    public JSONObject getJSON() throws JSONException {
        JSONObject j = new JSONObject();
        j.put("id", id);
        j.put("dataOwner", dataOwner.getId());
        j.put("crossingCode", crossingCode);
        j.put("description", description);
        j.put("karAddress", karAddress);
        j.put("town", town);
        j.put("type", type);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        j.put("validFrom", validFrom == null ? null : sdf.format(validFrom));
        j.put("validUntil", validUntil == null ? null : sdf.format(validUntil));
        j.put("location", GeoJSON.toGeoJSON(location));
        j.put("memo", memo);
        j.put("readyForExport", readyForExport);
        j.put("vehicleType", vehicleType);
        j.put("validationErrors", validationErrors);

        JSONObject jattrs = new JSONObject();
        j.put("attributes", jattrs);
        jattrs.put(KarAttributes.SERVICE_PT, new JSONArray("[ [], [], [] ]"));
        jattrs.put(KarAttributes.SERVICE_ES, new JSONArray("[ [], [], [] ]"));
        jattrs.put(KarAttributes.SERVICE_OT, new JSONArray("[ [], [], [] ]"));

        for(KarAttributes attribute: karAttributes) {
            JSONArray attributes = jattrs.getJSONArray(attribute.getServiceType()).getJSONArray(attribute.getCommandType()-1);

            if(attributes.length() != 0) {
                attributes = new JSONArray();
                jattrs.getJSONArray(attribute.getServiceType()).put(attribute.getCommandType()-1, attributes);
            }
            int mask = attribute.getUsedAttributesMask();
            for(int i = 0; i < 24; i++) {
                boolean set = (mask & (1 << i)) != 0;
                attributes.put(set);
            }
        }


        JSONArray jmvmts = new JSONArray();
        j.put("movements", jmvmts);
        for(Movement m: movements) {
            JSONObject jm = m.getJSON();
            jmvmts.put(jm);
        }

        Map<ActivationPoint,List<MovementActivationPoint>> mapsByAp2 = new HashMap();
        for(Movement m: movements) {
            for(MovementActivationPoint map: m.getPoints()) {
                List<MovementActivationPoint> maps = mapsByAp2.get(map.getPoint());
                if(maps == null) {
                    maps = new ArrayList();
                    mapsByAp2.put(map.getPoint(), maps);
                }
                maps.add(map);
            }
        }

        List<JSONObject> jpoints = new ArrayList();

        for(ActivationPoint ap2: points) {
            JSONObject pj = ap2.getGeoJSON();


            SortedSet<Integer> movementNumbers = new TreeSet();

            JSONArray mns = new JSONArray();
            List<MovementActivationPoint> maps = mapsByAp2.get(ap2);
            if(maps == null){
                continue;
            }
            for (MovementActivationPoint map : maps) {
                mns.put(map.getMovement().getNummer());
            }
            
            pj.put("movementNumbers", mns);

            jpoints.add(pj);
            Collections.sort(jpoints, new Comparator<JSONObject>() {
                public int compare(JSONObject lhs, JSONObject rhs) {
                    try {
                        return new Integer(lhs.getInt("nummer")).compareTo(rhs.getInt("nummer"));
                    } catch (JSONException ex) {
                        return 0;
                    }
                }
            });

            JSONArray jp = new JSONArray();
            for(JSONObject jo: jpoints) {
                jp.put(jo);
            }
            j.put("points", jp);

        }

        return j;
    }
  
    public void print(PrintWriter out){
        for (Movement movement : movements) {
            out.println("Beweging: " + movement.getNummer() + ": " + movement.getVehicleType());
            for (MovementActivationPoint point : movement.getPoints()) {
                out.print(point.getPoint().getLabel() + " ");
                if (point.getSignal() != null) {
                    switch(point.getSignal().getKarCommandType()){case 1: out.print("Inmeldpunt");break; case 2: out.print("Uitmeldpunt"); break; case 3: out.println("voorinmeldpunt");break;}
                    out.print(" : ");
                    out.print( point.getSignal().getSignalGroupNumber() + " :");
                    for (VehicleType vh : point.getSignal().getVehicleTypes()) {
                        out.print(vh.getOmschrijving() + ",");
                    }
                }else{
                    out.print(" eindpunt");
                }
                out.println();
            }
            out.println("----------------");
        }
        out.println();
        out.flush();
    }

    /**
     *
     * @param unmarshaller The unmarshaller
     * @param parent Parentnode
     */
    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        List<Point> ps = new ArrayList();
        for (ActivationPoint activationPoint : points) {
            if(activationPoint.getLocation() != null) {
                ps.add(activationPoint.getLocation());
            }
        }
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), RIJKSDRIEHOEKSTELSEL);
        GeometryCollection gc = new GeometryCollection(ps.toArray(new Point[ps.size()]), gf);
        this.location = gc.getCentroid();

    }
    
    public void beforeMarshal(Marshaller marshaller) {
        if (vehicleTypeToExport != null && !vehicleTypeToExport.equalsIgnoreCase("Gemixt")) {
            SortedSet<Movement> ms = new TreeSet<>();
            SortedSet<ActivationPoint> ps = new TreeSet<>();
            for (Movement movement : movements) {
                if (movement.determineVehicleType().equals(vehicleTypeToExport)) {
                    ms.add(movement);
                    for (MovementActivationPoint map : movement.getPoints()) {
                        ps.add(map.getPoint());
                    }
                }
            }
            movements = ms;
            points = ps;
        }
    }

    /**
     *
     * @param maxDistance Boundary
     * @return does it have a VRI in the vicinity
     */
    public boolean hasDuplicateKARAddressWithinDistance(int maxDistance) {
        return hasDuplicateKARAddressWithinDistance(maxDistance,false);
    }

    /**
     *
     * @param maxDistance Bounds
     * @param mustCheckForSameId yes/no
     * @return  does it have a VRI in the vicinity
     */
    public boolean hasDuplicateKARAddressWithinDistance(int maxDistance, boolean mustCheckForSameId){
        EntityManager em = Stripersist.getEntityManager();
        String query = "FROM RoadsideEquipment where karAddress = :karaddress and dataOwner = :dao";
        TypedQuery<RoadsideEquipment> q = null;
        if(mustCheckForSameId){
            query += " and id <> :id";
            q = em.createQuery(query, RoadsideEquipment.class).setParameter("karaddress", karAddress).setParameter("dao", dataOwner).setParameter("id", this.getId());
        }else{
            q = em.createQuery(query, RoadsideEquipment.class).setParameter("karaddress", karAddress).setParameter("dao", dataOwner);
        }

        List<RoadsideEquipment> rseqs = q.getResultList();
        for (RoadsideEquipment roadsideEquipment: rseqs) {
            double distance = roadsideEquipment.getLocation().distance(location);
            if(distance <= maxDistance){
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return default attributes
     */
    public static List<KarAttributes> getDefaultKarAttributes() {
        JSONArray ptBitmask = new JSONArray();
        JSONArray esBitmask = new JSONArray();
        JSONArray otBitmask = new JSONArray();
        Integer[] disabledDefaults = {0, 3, 4, 7, 8, 9, 11, 15, 16, 17, 19, 20, 21, 22, 23};
        List<Integer> disabled = Arrays.asList(disabledDefaults);
        List<Integer> disabledESOV = Arrays.asList(new Integer []{2,10});

        for (int i = 0; i < 24; i++) {
            if (disabled.contains(i)) {
                if(i == 11){
                    ptBitmask.put(true);// PT moet punctuality geven in kar bericht
                }else{
                    ptBitmask.put(false);
                }
                esBitmask.put(false);
                otBitmask.put(false);
            } else {
                if(disabledESOV.contains(i)){
                    esBitmask.put(true);
                    otBitmask.put(true);
                }
                ptBitmask.put(true);
            }

        }

        List<KarAttributes> karAttributes = new ArrayList();
        try {
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_PT, ActivationPointSignal.COMMAND_INMELDPUNT, ptBitmask));
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_OT, ActivationPointSignal.COMMAND_INMELDPUNT, ptBitmask));
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_ES, ActivationPointSignal.COMMAND_INMELDPUNT, ptBitmask));
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_PT, ActivationPointSignal.COMMAND_UITMELDPUNT, ptBitmask));
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_OT, ActivationPointSignal.COMMAND_UITMELDPUNT, ptBitmask));
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_ES, ActivationPointSignal.COMMAND_UITMELDPUNT, ptBitmask));
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_PT, ActivationPointSignal.COMMAND_VOORINMELDPUNT, ptBitmask));
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_OT, ActivationPointSignal.COMMAND_VOORINMELDPUNT, ptBitmask));
            karAttributes.add(new KarAttributes(KarAttributes.SERVICE_ES, ActivationPointSignal.COMMAND_VOORINMELDPUNT, ptBitmask));
        } catch(JSONException e) {
        }
        return karAttributes;
    }

    /**
     *
     * @return extra xml
     */
    @XmlElement(name="b3pextra")
    public XmlB3pRseq getExtraXml() {

        XmlB3pRseq extra = new XmlB3pRseq(this);

        if(extra.isEmpty()) {
            return null;
        } else {
            return extra;
        }
    }

    /**
     * Let op: kapt te lange waardes af / vervangt ongeldige waardes door defaults.
     *
     * @param errors The errors 
     * @return Aantal KV9 validatie meldingen.
     */
    public int validateKV9(List<KV9ValidationError> errors) {

        int errorsStartIndex = errors.size();

        String xmlContext = String.format("RSEQDEF[karaddress=%s]", karAddress == null ? "<leeg>" : karAddress.toString());
        String context = String.format("Verkeerssysteem (KAR adres %s)", karAddress == null ? "<leeg>" : karAddress.toString());

        String eXmlContext = xmlContext + "/dataownercode";
        String eContext = context + ", beheerder";
        if(dataOwner == null) {
            errors.add(new KV9ValidationError(true, "F104", eXmlContext, eContext, null, "Niet ingevuld"));
        } else if(!Stripersist.getEntityManager().contains(dataOwner)) {
            // Tijdens DataOwner.afterUnmarshal() niet in database gevonden
            errors.add(new KV9ValidationError(true, "F105", eXmlContext, eContext, dataOwner.getCode(), "Beheerder niet gevonden voor code"));
        }

        eXmlContext = xmlContext + "/karaddress";
        eContext = context + ", KAR adres";
        if(karAddress == null) {
            errors.add(new KV9ValidationError(true, "F106", eXmlContext, eContext, null, "Niet ingevuld"));
        } else {
            if(karAddress < 0 || karAddress > 65535) {
                errors.add(new KV9ValidationError(false, "F107", eXmlContext, eContext, karAddress + "", "Niet in bereik van 0 t/m 65535"));
            }
            if(getId() == null && hasDuplicateKARAddressWithinDistance(500)) {
                errors.add(new KV9ValidationError(true, "F108", eXmlContext, eContext, karAddress + "", "Dubbel binnen straal van 500 meter"));
            } else if(getId() != null && hasDuplicateKARAddressWithinDistance(500, true)) {
                errors.add(new KV9ValidationError(true, "F108", eXmlContext, eContext, karAddress + "", "Dubbel binnen straal van 500 meter"));
            }
        }

        eXmlContext = xmlContext + "/rseqtype";
        eContext = context + ", Soort verkeerssysteem";
        if(StringUtils.isBlank(type)) {
            errors.add(new KV9ValidationError(false, "F109", eXmlContext, eContext, null, "Niet ingevuld"));
            type = TYPE_CROSSING;
        } else if(!TYPE_CROSSING.equals(type) && !TYPE_BAR.equals(type) && !TYPE_GUARD.equals(type)) {
            errors.add(new KV9ValidationError(false, "F110", eXmlContext, eContext, type,
                    String.format("Ongeldig (niet '%s', '%s' of '%s')", TYPE_CROSSING, TYPE_GUARD, TYPE_BAR)));
            type = TYPE_CROSSING;
        }

        eXmlContext = xmlContext + "/validfrom";
        eContext = context + ", Geldig vanaf";
        if(validFrom == null) {
            errors.add(new KV9ValidationError(true, "F111", eXmlContext, eContext, null, "Niet ingevuld of ongeldig"));
        }

        eXmlContext = xmlContext + "/crossingcode";
        eContext = context + ", Beheerdersaanduiding";
        if(StringUtils.isBlank(crossingCode)) {
            errors.add(new KV9ValidationError(false, "F112", eXmlContext, eContext, null, "Niet ingevuld"));
        } else if(crossingCode.length() > 10) {
            errors.add(new KV9ValidationError(false, "F113", eXmlContext, eContext, crossingCode, "Langer dan 10 tekens"));
            crossingCode = crossingCode.substring(0, Math.min(crossingCode.length(), 255));
        }

        eXmlContext = xmlContext + "/town";
        eContext = context + ", Plaats";
        if(StringUtils.isBlank(town)) {
            errors.add(new KV9ValidationError(false, "F114", eXmlContext, eContext, null, "Niet ingevuld"));
        } else if(town.length() > 50) {
            errors.add(new KV9ValidationError(false, "F115", eXmlContext, eContext, town, "Langer dan 50 tekens"));
            town = town.substring(0, Math.min(town.length(), 50));
        }

        eXmlContext = xmlContext + "/description";
        eContext = context + ", Locatie";
        if(description != null && description.length() > 255) {
            errors.add(new KV9ValidationError(false, "F116", eXmlContext, eContext, description, "Langer dan 255 tekens"));
            description = description.substring(0, Math.min(description.length(), 255));
        }

        eXmlContext = xmlContext + "/KARATTRIBUTES";
        eContext = context + ", KAR attributen";
        if(karAttributes.isEmpty()) {
            errors.add(new KV9ValidationError(false, "F117", eXmlContext, eContext, null, "Afwezig"));
            setKarAttributes(getDefaultKarAttributes());
        } else {
            int pos = 0;
            for(KarAttributes ka: karAttributes) {
                pos++;
                String kaXmlContext = eXmlContext + "[" + pos + "]";
                String kaContext = eContext + " (#" + pos + ")";
                String st = ka.getServiceType();

                String ka2XmlContext = kaXmlContext + "/karservicetype";
                String ka2Context = kaContext + ", KAR service type";
                if(st == null) {
                    errors.add(new KV9ValidationError(true, "F118", ka2XmlContext, ka2Context, null, "Niet ingevuld"));
                } else if(!KarAttributes.SERVICE_ES.equals(st) && !KarAttributes.SERVICE_OT.contains(st) && !KarAttributes.SERVICE_PT.equals(st)) {
                    errors.add(new KV9ValidationError(true, "F119", ka2XmlContext, ka2Context, st, "Ongeldig (niet 'ES', 'PT' of 'OS')"));
                }

                Integer ct = ka.getCommandType();
                ka2XmlContext = kaXmlContext + "/karcommandtype";
                ka2Context = kaContext + ", KAR command type";
                if(ct == null) {
                    errors.add(new KV9ValidationError(true, "F120", ka2XmlContext, ka2Context, null, "Niet ingevuld"));
                } else if(!KarAttributes.SERVICE_ES.equals(st) && !KarAttributes.SERVICE_OT.contains(st) && !KarAttributes.SERVICE_PT.equals(st)) {
                    errors.add(new KV9ValidationError(true, "F121", ka2XmlContext, ka2Context, ct + "", "Ongeldig (niet 1, 2 of 3)"));
                }

                String ua = ka.getUsedAttributesString();
                if(ua == null && ka.getUsedAttributesMask() != null) {
                    try {
                        ka.beforeMarshal(null);
                    } catch (Exception ex) {
                    }
                    ua = ka.getUsedAttributesString();
                }
                ka2XmlContext = kaXmlContext + "/karusedattributes";
                ka2Context = kaContext + ", KAR gebruikte attributen";
                if(ua == null) {
                    errors.add(new KV9ValidationError(true, "F122", ka2XmlContext, ka2Context, null, "Niet ingevuld"));
                } else if(!ua.matches("[01]{24}")) {
                    errors.add(new KV9ValidationError(true, "F123", ka2XmlContext, ka2Context, ua, "Niet 24 tekens lang 1 of 0"));
                }

                // TODO: indien attributen niet compleet zijn voor alle command types / service types,
                // deze uit defaults halen
            }
        }

        eXmlContext = xmlContext + "/ACTIVATIONPOINT";
        if(points.isEmpty()) {
            errors.add(new KV9ValidationError(false, "F124", eXmlContext, context + ", punten", null, "Afwezig"));
            setKarAttributes(getDefaultKarAttributes());
        } else {
            // positie in XML niet beschikbaar, SortedSet op basis van nummer
            for(ActivationPoint ap: points) {
                String pXmlContext = eXmlContext + "[activationpointnumber=" + ap.getNummer() + "]";
                String pContext = context + ", punt nummer " + ap.getNummer();

                if(ap.getNummer() == null) {
                    errors.add(new KV9ValidationError(true, "F125", pXmlContext + "/activationpointnumber", pContext + ", nummer", null, "Niet ingevuld"));
                } else {
                    if(ap.getNummer() < 0 || ap.getNummer() > 9999) {
                        errors.add(new KV9ValidationError(false, "F126", pXmlContext + "/activationpointnumber", pContext + ", nummer", ap.getNummer() + "", "Ongeldig (niet 0 t/m 9999)"));
                    }

                    // F127 kan niet gecontroleerd worden vanwege SortedSet
                }

                if(ap.getLocation() == null || ap.getLocation().getX() == 0.0 || ap.getLocation().getY() == 0.0) {
                    errors.add(new KV9ValidationError(true, "F128", pXmlContext + ", rdx-coordinate, rdy-coordinate", pContext + ", coordinaten", null, "Afwezig/ongeldig"));
                }

            }
        }

        Map<ActivationPoint,String> apNummerType = new HashMap();

        eXmlContext = xmlContext + "/MOVEMENT";
        if(movements.isEmpty()) {
            errors.add(new KV9ValidationError(false, "F130", eXmlContext, context + ", bewegingen", null, "Afwezig"));
        } else {
            // positie in XML niet beschikbaar, SortedSet op basis van nummer
            for(Movement m: movements) {
                String mXmlContext = eXmlContext + "[movementnumber=" + m.getNummer() + "]";
                String mContext = context + ", beweging nummer " + m.getNummer();

                if(m.getNummer() == null) {
                    errors.add(new KV9ValidationError(true, "F131", mXmlContext + "/movementnumber", mContext + ", nummer", null, "Niet ingevuld"));
                } else {
                    if(m.getNummer() < 0 || m.getNummer() > 9999) {
                        errors.add(new KV9ValidationError(false, "F132", mXmlContext + "/movementnumber", mContext + ", nummer", m.getNummer() + "", "Ongeldig (niet 0 t/m 9999)"));
                    }

                    // F133 kan niet gecontroleerd worden vanwege SortedSet
                }

                String movementVehicleType = m.determineVehicleType();

                if(movementVehicleType == null) {
                    errors.add(new KV9ValidationError(true, "F146", mXmlContext + "//karvehicletype", mContext + ", voertuigtype", null, "Niet ingevuld"));
                    continue;
                }
                for(int i = 0; i < m.getPoints().size(); i++) {
                    MovementActivationPoint map = m.getPoints().get(i);
                    String mapXmlContext = mXmlContext + "/" + map.getBeginEndOrActivation();
                    String mapContext = mContext + ", punt op index " + i;

                    if(map.getPoint() == null) {
                        errors.add(new KV9ValidationError(true, "F134", mapXmlContext + "/activationpointnumber", mContext + ", nummer activationpoint", m.getNummer() + "", "Ongeldig"));
                    } else {
                        mapXmlContext = mapXmlContext + "[activationpointnumber=" + map.getPoint().getNummer() + "]";
                    }

                    String mapType = apNummerType.get(map.getPoint());
                    if(mapType == null) {
                        mapType = map.getBeginEndOrActivation();
                        apNummerType.put(map.getPoint(), mapType);
                    } else {
                        if(!map.getBeginEndOrActivation().equals(mapType)) {
                            errors.add(new KV9ValidationError(true, "F135", mapXmlContext, mapContext, map.getBeginEndOrActivation() + ", eerder " + mapType, "Punt al gebruikt voor ander soort punt (niet ondersteund)"));
                        }
                    }

                    if(i == m.getPoints().size() - 1 && !MovementActivationPoint.END.equals(mapType) && movementVehicleType.equals("OV")) {
                        errors.add(new KV9ValidationError(false, "F145", mXmlContext + "/END", mContext + ", eindpunt van beweging", null, "Afwezig"));
                    }

                    if(MovementActivationPoint.ACTIVATION.equals(mapType)) {
                        ActivationPointSignal s = map.getSignal();
                        String sXmlContext = mapXmlContext + "/ACTIVATIONPOINTSIGNAL";
                        String sContext = mapContext + ", signaalgegevens";
                        if(s == null) {
                            // Kan niet voorkomen, wordt overgeslagen in Movement.afterUnmarshal()
                            throw new IllegalStateException();
                        } else {
                            if(s.getVehicleTypes().isEmpty()) {
                                errors.add(new KV9ValidationError(true, "F136", sXmlContext + "/karvehicletype", sContext + ", voertuigtype", null, "Afwezig"));
                            }

                            if(s.getKarCommandType() == null) {
                                errors.add(new KV9ValidationError(true, "F137", sXmlContext + "/karcommandtype", sContext + ", soort melding", null, "Afwezig"));
                            } else {
                                if(s.getKarCommandType() < 1 || s.getKarCommandType() > 3) {
                                    errors.add(new KV9ValidationError(true, "F138", sXmlContext + "/karcommandtype", sContext + ", soort melding", s.getKarCommandType() + "", "Ongeldig (niet 1, 2 of 3)"));
                                }
                            }

                            if(s.getTriggerType() == null) {
                                errors.add(new KV9ValidationError(false, "F139", sXmlContext + "/triggertype", sContext + ", triggersoort van melding", null, "Afwezig"));
                                s.setTriggerType(ActivationPointSignal.TRIGGER_STANDARD);
                            } else {
                                if(m.getVehicleType().equals(VehicleType.VEHICLE_TYPE_HULPDIENSTEN)){
                                    String tt = s.getTriggerType();
                                    if( StringUtils.isNumeric(tt)){
                                        Integer t = Integer.parseInt(tt);
                                        if(t < 0 || t > 255){
                                            errors.add(new KV9ValidationError(false, "F140", sXmlContext + "/triggertype", sContext + ", triggersoort van melding", s.getTriggerType() + "", "Ongeldig voor hulpdiensten (< 0 of > 255)"));
                                            s.setTriggerType(ActivationPointSignal.TRIGGER_STANDARD);
                                        }
                                    } else {
                                        if (!ActivationPointSignal.TRIGGER_FORCED.equals(s.getTriggerType())
                                                && !ActivationPointSignal.TRIGGER_MANUAL.equals(s.getTriggerType())
                                                && !ActivationPointSignal.TRIGGER_STANDARD.equals(s.getTriggerType())) {
                                            errors.add(new KV9ValidationError(false, "F140", sXmlContext + "/triggertype", sContext + ", triggersoort van melding", s.getTriggerType() + "", "Ongeldig (niet 'STANDARD', 'FORCED' of 'MANUAL')"));
                                            s.setTriggerType(ActivationPointSignal.TRIGGER_STANDARD);
                                        }
                                    }
                                } else {
                                    if (!ActivationPointSignal.TRIGGER_FORCED.equals(s.getTriggerType())
                                            && !ActivationPointSignal.TRIGGER_MANUAL.equals(s.getTriggerType())
                                            && !ActivationPointSignal.TRIGGER_STANDARD.equals(s.getTriggerType())) {
                                        errors.add(new KV9ValidationError(false, "F140", sXmlContext + "/triggertype", sContext + ", triggersoort van melding", s.getTriggerType() + "", "Ongeldig (niet 'STANDARD', 'FORCED' of 'MANUAL')"));
                                        s.setTriggerType(ActivationPointSignal.TRIGGER_STANDARD);
                                    }
                                }
                            }

                            if(s.getDistanceTillStopLine() != null) {
                                if(s.getDistanceTillStopLine() < -99 || s.getDistanceTillStopLine() > 9999) {
                                    errors.add(new KV9ValidationError(false, "F141", sXmlContext + "/distancetillstopline", sContext + ", afstand tot stopstreep", s.getDistanceTillStopLine() + "", "Ongeldig (niet van -99 t/m 9999)"));
                                    s.setDistanceTillStopLine(null);
                                }
                            }

                            if(s.getVirtualLocalLoopNumber() == null && s.getSignalGroupNumber() == null) {
                                errors.add(new KV9ValidationError(false, "F142", sXmlContext + "/virtuallocalloopnumber, signalgroupnumber", sContext + ", virtuele lusnummer en signaalgroepnummer", null, "Beide afwezig (één van beide is verplicht)"));
                            }

                            if(s.getVirtualLocalLoopNumber() != null && (s.getVirtualLocalLoopNumber() < 0 || s.getVirtualLocalLoopNumber() > 127)) {
                                errors.add(new KV9ValidationError(false, "F143", sXmlContext + "/virtuallocalloopnumber", sContext + ", virtual local loop number", null, "Ongeldig (niet 0 t/m 127)"));
                            }

                            if(s.getSignalGroupNumber() != null && (s.getSignalGroupNumber() < 1 || s.getSignalGroupNumber() > 255)) {
                                errors.add(new KV9ValidationError(false, "F144", sXmlContext + "/signalgroupnumber", sContext + ", signalgroepnummer", s.getSignalGroupNumber() + "", "Ongeldig (niet 1 t/m 255)"));
                            }
                        }
                    }
                }
            }
        }

        return errors.size() - errorsStartIndex;
    }

    /**
     *
     * @return type of rseq 
     */
    public String determineType() {
        String typeRseq = null;
        for (Movement movement : movements) {
            String tempType = movement.determineVehicleType();
            if(typeRseq == null){
                typeRseq = tempType;
            }
            
            if(tempType != null && typeRseq != null && !typeRseq.equals(tempType)){
                typeRseq = VehicleType.VEHICLE_TYPE_GEMIXT;
            }
        }
        return typeRseq;
    }
    /*
    public RoadsideEquipment deepCopy(EntityManager em) throws Exception{
        RoadsideEquipment copy = (RoadsideEquipment) BeanUtils.cloneBean(this);
        
        copy.setId(null);
        copy.setMovements(null);
        copy.setPoints(null);
        copy.setKarAttributes(null);
        Point p = copy.getLocation();
        p.getCoordinate().x += 10;
        p.getCoordinate().y += 10;
        em.persist(copy);
        
        
        SortedSet<ActivationPoint> copiedPoints = new TreeSet<>();
        Map<ActivationPoint, ActivationPoint> oldToNew = new HashMap<>();
        for (ActivationPoint oldPoint : points) {
            ActivationPoint pointCopy = oldPoint.deepCopy(copy);
            oldToNew.put(oldPoint, pointCopy);
            em.persist(pointCopy);
            copiedPoints.add(pointCopy);
        }
        copy.setPoints(copiedPoints);
        
        
        SortedSet<Movement> copiedMovements = new TreeSet<>();
        for (Movement movement : movements) {
            copiedMovements.add(movement.deepCopy(copy, oldToNew, em));
        }
        copy.setMovements(copiedMovements);
        
        List<KarAttributes> attrs = new ArrayList<>();
        for (KarAttributes karAttribute : karAttributes) {
            attrs.add(karAttribute.deepCopy());
        }
        copy.setKarAttributes(attrs);
       
        return copy;
    }*/

    @Override
    public int compareTo(RoadsideEquipment o) {
        if(this.getId().equals(o.getId())){
            return 0;
        }
        if( (this.getDataOwner().getId().equals(o.getDataOwner().getId()) && this.getKarAddress().equals(o.getKarAddress()))){
            return 0;
        }
        
        if( this.getDataOwner().getId().equals(o.getDataOwner().getId())){
            
            return this.getKarAddress().compareTo(o.getKarAddress());
        }else{
            return this.getDataOwner().getOmschrijving().compareTo(o.getDataOwner().getOmschrijving());
        }
    }
}
