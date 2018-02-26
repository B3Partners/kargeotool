/**
 * KAR Geo Tool - applicatie voor het registreren van KAR meldpunten
 *
 * Copyright (C) 2009-2018 B3Partners B.V.
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
package nl.b3p.kar.stripes;

import com.vividsolutions.jts.geom.GeometryCollection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.tag.BeanFirstPopulationStrategy;
import net.sourceforge.stripes.validation.OneToManyTypeConverter;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrorHandler;
import net.sourceforge.stripes.validation.ValidationErrors;
import nl.b3p.commons.stripes.CustomPopulationStrategy;
import nl.b3p.incaa.IncaaExport;
import nl.b3p.kar.hibernate.ActivationPointSignal;
import nl.b3p.kar.hibernate.DataOwner;
import nl.b3p.kar.hibernate.Deelgebied;
import nl.b3p.kar.hibernate.Gebruiker;
import nl.b3p.kar.hibernate.Movement;
import nl.b3p.kar.hibernate.MovementActivationPoint;
import nl.b3p.kar.hibernate.RoadsideEquipment;
import nl.b3p.kar.hibernate.VehicleType;
import nl.b3p.kar.jaxb.KarNamespacePrefixMapper;
import nl.b3p.kar.jaxb.TmiPush;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.Type;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.EntityTypeConverter;
import org.stripesstuff.stripersist.Stripersist;

/**
 * Exporteren van KV9 gegevens.
 *
 * @author Matthijs Laan
 */
@UrlBinding("/action/export")
@StrictBinding
@CustomPopulationStrategy(BeanFirstPopulationStrategy.class)
public class ExportActionBean implements ActionBean, ValidationErrorHandler {

    private static final Log log = LogFactory.getLog(ExportActionBean.class);
    private static final String OVERVIEW = "/WEB-INF/jsp/export/overview.jsp";
    private ActionBeanContext context;

    @Validate(required = true, on = {"exportPtx", "exportXml"})
    private RoadsideEquipment rseq;

    @Validate(converter = OneToManyTypeConverter.class, on = "export")
    private List<Long> rseqs;
    private List<RoadsideEquipment> roadsideEquipmentList;
    private List<Deelgebied> deelgebieden = new ArrayList();

    @Validate(converter = EntityTypeConverter.class, on = {"rseqByDeelgebied"})
    private Deelgebied filter;

    @Validate(converter = OneToManyTypeConverter.class, on = "export")
    private List<DataOwner> dataowner;

    @Validate
    private Integer karAddress;

    @Validate(on = "export")
    private String exportType;

    @Validate
    private String filterType;

    @Validate(on = {"rseqByDeelgebied", "allRseqs"})
    private boolean onlyValid;

    @Validate(on = {"rseqByDeelgebied", "allRseqs"})
    private String vehicleType;

    @Validate
    private boolean onlyReady = true;

    private List<DataOwner> dataowners = new ArrayList<>();

    @Validate(converter = OneToManyTypeConverter.class, on = "adminExport")
    private List<DataOwner> dos = new ArrayList<>();

    @DefaultHandler
    public Resolution overview() {
        return new ForwardResolution(OVERVIEW);
    }

    public Resolution export() throws Exception {
        EntityManager em = Stripersist.getEntityManager();
        roadsideEquipmentList = new ArrayList();
        List<RoadsideEquipment> notReadyForExport = new ArrayList<>();
        if (rseqs == null) {
            this.context.getValidationErrors().add("Verkeerssystemen", new SimpleError(("Selecteer een of meerdere verkeerssystemen")));
            return new ForwardResolution(OVERVIEW);
        }
        for (Long id : rseqs) {
            RoadsideEquipment r = em.find(RoadsideEquipment.class, id);
            if (r.isReadyForExport() || !onlyReady) {
                r.setVehicleTypeToExport(vehicleType);
                roadsideEquipmentList.add(r);
            } else {
                notReadyForExport.add(r);
            }
        }
        if (!notReadyForExport.isEmpty()) {
            String message = "Kan niet exporteren omdat er een of meerdere verkeerssytemen zijn die niet klaar voor export zijn. Pas de selectie aan. De volgende zijn nog niet klaar: ";

            for (RoadsideEquipment r : notReadyForExport) {
                message += "<br/> " + r.getKarAddress() + " - " + r.getDescription() + ", ";
            }
            message = message.substring(0, message.length() - 2);
            this.context.getValidationErrors().add("export", new SimpleError((message)));
            return new ForwardResolution(OVERVIEW);
        } else if (exportType == null) {
            this.context.getValidationErrors().add("exportType", new SimpleError(("Selecteer een exporttype")));
            return new ForwardResolution(OVERVIEW);
        } else if (exportType.equals("incaa")) {
            return exportPtx();
        } else if (exportType.equals("kv9")) {
            return exportXml();
        } else if (exportType.equals("csvsimple")) {
            return exportCSVSimple();
        } else if (exportType.equals("csvextended")) {
            return exportCSVExtended();
        }else{
            this.context.getMessages().add(new SimpleError("Export Type is niet bekend", exportType));
            return new ForwardResolution(OVERVIEW);
        }
    }

    public Resolution exportXml() throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(TmiPush.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new KarNamespacePrefixMapper());
        m.setProperty("jaxb.formatted.output", Boolean.TRUE);

        /* TODO subscriberId per dataOwner of in gebruikersprofiel instellen/vragen oid */
        if (rseq != null) {
            roadsideEquipmentList = Arrays.asList(new RoadsideEquipment[]{rseq});
        }
        TmiPush push = new TmiPush("B3P", roadsideEquipmentList);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        m.marshal(push, bos);

        Date now = new Date();
        DateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String prefix = "geo-ov_";
        if (roadsideEquipmentList.size() == 1) {
            prefix = "" + roadsideEquipmentList.get(0).getKarAddress();
        }
        String filename = prefix + "_kv9_" + sdf.format(now);
        return new StreamingResolution("text/xml", new ByteArrayInputStream(bos.toByteArray()))
                .setAttachment(true)
                .setFilename(filename + ".xml")
                .setLength(bos.size());
    }

    public Resolution exportPtx() throws Exception {
        IncaaExport exporter = new IncaaExport();

        File f = null;
        if (rseq != null) {
            f = exporter.convert(rseq);
        } else if (roadsideEquipmentList != null) {
            f = exporter.convert(roadsideEquipmentList);
        }
        if (f != null) {
            FileInputStream fis = new FileInputStream(f);

            String filename = "HLPXXXXX";
            Date now = new Date();
            DateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            filename += sdf.format(now);
            return new StreamingResolution("text/plain", fis)
                    .setAttachment(true)
                    .setFilename(filename + ".ptx")
                    .setLength(f.length());
        } else {
            throw new Exception("Could not find roadsideequipments");
        }
    }

    public Resolution exportCSVSimple() {
        try {
            Collections.sort(roadsideEquipmentList);
            DateFormat stomdateformat = new SimpleDateFormat("dd-MM-yyyy");
            File f = File.createTempFile("tmp", "csvsimple.csv");
            FileWriter fw = new FileWriter(f);
            PrintWriter out = new PrintWriter(fw);
            try (CSVPrinter csv = new CSVPrinter(out, CSVFormat.DEFAULT)) {
                csv.printRecord(new Object[]{"Soort verkeerssysteem", "Beheerder", "Beheerdersaanduiding", "Plaats", "Locatie", "Geldig vanaf", "Geldig tot", "KAR-adres",
                    "RD-X", "RD-Y", "Bevat OV-punten", "Bevat HD-punten", "KV9-validatie", "Gereed voor export"});
                for (RoadsideEquipment r : roadsideEquipmentList) {
                    String hasHD = r.getVehicleType().equalsIgnoreCase("Gemixt") || r.getVehicleType().equalsIgnoreCase("Hulpdiensten") ? "Ja" : "Nee";
                    String hasOV = r.getVehicleType().equalsIgnoreCase("Gemixt") || r.getVehicleType().equalsIgnoreCase("OV") ? "Ja" : "Nee";
                    String type;
                    switch (r.getType()) {
                        case RoadsideEquipment.TYPE_BAR:
                            type = "Afsluitsysteem";
                            break;
                        case RoadsideEquipment.TYPE_CROSSING:
                            type = "VRI";
                            break;
                        case RoadsideEquipment.TYPE_GUARD:
                            type = "Bewakingssysteem";
                            break;
                        default:
                            type = "VRI";
                            break;
                    }
                    Object[] values = {type, r.getDataOwner().getOmschrijving(), r.getCrossingCode(), r.getTown(), r.getDescription(),
                        r.getValidFrom() != null ? stomdateformat.format(r.getValidFrom()) : "", r.getValidUntil() != null ? stomdateformat.format(r.getValidUntil()) : "",
                        r.getKarAddress(), r.getLocation().getCoordinate().x, r.getLocation().getCoordinate().y, hasOV, hasHD, r.getValidationErrors() > 0 ? "Bevat fouten" : "OK",
                        r.isReadyForExport() ? "Ja" : "Nee"};
                    csv.printRecord(values);
                }

                csv.flush();
            }

            return fileResolution(f);
        } catch (IOException ex) {
            log.error("Cannot read/write csv file", ex);
            return new ErrorResolution(500, "Exporteerproblemen. Raadpleeg CROW-NDOV.");
        }
    }

    public Resolution exportCSVExtended() {
        try {
            DateFormat stomdateformat = new SimpleDateFormat("dd-MM-yyyy");
            File f = File.createTempFile("tmp", "csvsimple.csv");
            FileWriter fw = new FileWriter(f);
            PrintWriter out = new PrintWriter(fw);
            CSVPrinter csv = new CSVPrinter(out, CSVFormat.DEFAULT);

            Collections.sort(roadsideEquipmentList);
            csv.printRecord(new Object[]{"Soort verkeerssysteem", "Beheerder", "Beheerdersaanduiding", "Plaats", "Locatie", "Geldig vanaf", "Geldig tot",
                "KAR-adres", "Signaalgroep", "Richtingen", "Beweging", "Volgnummer beweging", "KAR-punt", "Type melding", "Triggertype", "RD-X", "RD-Y",
                "Afstand", "Bus", "Tram", "CVV", "Taxi", "HOV", "Politie", "Brandweer", "Ambulance", "Politie niet in uniform", "Marechaussee",
                "Virtual local loop-number",});
            for (RoadsideEquipment r : roadsideEquipmentList) {
                String type;
                switch (r.getType()) {
                    case RoadsideEquipment.TYPE_BAR:
                        type = "Afsluitsysteem";
                        break;
                    case RoadsideEquipment.TYPE_CROSSING:
                        type = "VRI";
                        break;
                    case RoadsideEquipment.TYPE_GUARD:
                        type = "Bewakingssysteem";
                        break;
                    default:
                        type = "VRI";
                        break;
                }
                SortedSet<Movement> mset = r.getMovements();
                List<Movement> ms = new ArrayList<>(mset);
                Collections.sort(ms);
                for (Movement m : ms) {
                    List<MovementActivationPoint> maps = m.getPoints();
                    String movementLabel = getLabel(m);
                    for (MovementActivationPoint map : maps) {
                        String triggerType = "";
                        if (map.getSignal() != null) {
                            String tt = map.getSignal().getTriggerType();
                            switch (tt) {
                                case ActivationPointSignal.TRIGGER_FORCED:
                                    triggerType = "automatisch";
                                    break;
                                case ActivationPointSignal.TRIGGER_MANUAL:
                                    triggerType = "handmatig";
                                    break;
                                case ActivationPointSignal.TRIGGER_STANDARD:
                                    triggerType = "standaard";
                                    break;
                                default:
                                    triggerType = "";
                                    break;
                            }
                        }
                        
                        Map<String, String> vhts = getVehicleTypes(map);
                        Object[] values = {
                            type, r.getDataOwner().getOmschrijving(), r.getCrossingCode(), r.getTown(), r.getDescription(), 
                            r.getValidFrom() != null ? stomdateformat.format(r.getValidFrom()) : "",
                            r.getValidUntil() != null ? stomdateformat.format(r.getValidUntil()) : "", r.getKarAddress(), 
                            map.getSignal() != null ? map.getSignal().getSignalGroupNumber() : "", 
                            getDirectionLabel(map), 
                            movementLabel, 
                            m.getNummer(),
                            map.getPoint().getLabel(),
                            map.getBeginEndOrActivation().equals("END") ? "eind" :  map.getBeginEndOrActivation().equals("BEGIN") ? "begin": map.getSignal().getKarCommandType() == 1 ? "in" :  map.getSignal().getKarCommandType() == 2 ? "uit" : "voor",
                            triggerType,
                            map.getPoint().getLocation().getX(),
                            map.getPoint().getLocation().getY(),
                            map.getSignal() != null ?  map.getSignal().getDistanceTillStopLine(): "", 
                            vhts.getOrDefault("Bus", ""),
                            vhts.getOrDefault("Tram", ""),
                            vhts.getOrDefault("CVV", ""),
                            vhts.getOrDefault("Taxi", ""),
                            vhts.getOrDefault("Hoogwaardig Openbaar Vervoer (HOV) bus", ""),
                            vhts.getOrDefault("Politie", ""),
                            vhts.getOrDefault("Brandweer", ""),
                            vhts.getOrDefault("Ambulance", ""),
                            vhts.getOrDefault("Politie niet in uniform", ""),
                            vhts.getOrDefault("Marechaussee", ""),
                            map.getSignal() != null ? map.getSignal().getVirtualLocalLoopNumber() : ""};
                        csv.printRecord(values);
                    }
                }
            }

            csv.flush();
            csv.close();
            return fileResolution(f);
        } catch (IOException ex) {
            log.error("Cannot read/write csv file", ex);
            return new ErrorResolution(500, "Exporteerproblemen. Raadpleeg CROW-NDOV.");
        }
    }
    
    private Map<String, String> getVehicleTypes(MovementActivationPoint map){
        Map<String, String> mapVhs = new HashMap<>();
        if (map.getSignal() != null) {
            List<VehicleType> vts = map.getSignal().getVehicleTypes();
            for (VehicleType vt : vts) {
                mapVhs.put(vt.getOmschrijving(), "x");
            }
        }
        return mapVhs;
    }
    
    private String getDirectionLabel(MovementActivationPoint map){
        String label = "";
        if(map.getSignal() != null){
            String direction = map.getSignal().getDirection();
            if(direction == null || direction.isEmpty()){
                return label;
            }
            String[] dirs = direction.split(",");
            for (String dir : dirs) {
                switch(dir){
                    case "1":
                        label += "la-";
                        break;
                    case "2":
                        label += "ra-";
                        break;
                    case "3":
                        label += "rd-";
                        break;
                }
                
            }
            label = label.substring(0, label.length() - 1);
        }
        return label;
    }
    
    private String getLabel(Movement m){
        List<MovementActivationPoint> maps = m.getPoints();
        String l ="";
        if(maps.size() > 0){
            l = maps.get(0).getPoint().getLabel();
            if(maps.size() > 1){
                l += " - " + maps.get(maps.size()-1).getPoint().getLabel();
            }
        }
        return l;
    }
    
    private Resolution fileResolution(File f) throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(f);

        String filename = "KAR_Geo_Tool_CSV_";
        Date now = new Date();
        DateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        filename += sdf.format(now);
        return new StreamingResolution("text/plain", fis)
                .setAttachment(true)
                .setFilename(filename + ".csv")
                .setLength(f.length());

    }

    public Resolution adminExport() {
        EntityManager em = Stripersist.getEntityManager();
        if (context.getRequest().isUserInRole("beheerder")) {
            String[] headers = {"dataowner", "totalvri", "vriwithouterrors", "vriwithouterrorsready"};
            List<List<String>> values = getExportValues(dos, em);
            Resolution response;
            if (exportType == null) {
                return new ErrorResolution(405, "Verkeerde exporttype meegegeven.");
            } else if (exportType.endsWith("JSON")) {
                response = exportAdminJSON(values, headers);
            } else if (exportType.endsWith("CSV")) {
                try {
                    response = exportAdminCSV(values, headers);
                } catch (IOException ex) {
                    log.error("Cannot read/write csv file", ex);
                    response = null;
                }
            } else {
                return new ErrorResolution(405, "Verkeerde exporttype meegegeven.");
            }
            return response;
        } else {
            return new ErrorResolution(403, "Alleen beheerders mogen deze export maken.");
        }
    }

    public static List<List<String>> getExportValues(List<DataOwner> dos, EntityManager em) {
        List<List<String>> values = new ArrayList<>();
        for (DataOwner dataOwner : dos) {
            List<String> row = new ArrayList<>();
            row.add(dataOwner.getOmschrijving());
            List<RoadsideEquipment> rseqs = em.createQuery("FROM RoadsideEquipment Where dataOwner = :do", RoadsideEquipment.class).setParameter("do", dataOwner).getResultList();
            row.add("" + rseqs.size());
            Integer numberNoErrors = 0, numberReady = 0;
            for (RoadsideEquipment r : rseqs) {
                if (r.getValidationErrors() == 0) {
                    numberNoErrors++;
                    if (r.isReadyForExport()) {
                        numberReady++;
                    }
                }
            }
            row.add("" + numberNoErrors);
            row.add("" + numberReady);
            values.add(row);
        }
        return values;
    }

    private Resolution exportAdminJSON(List<List<String>> values, String[] headers) {
        JSONObject response = new JSONObject();
        response.put("success", Boolean.FALSE);
        JSONArray data = new JSONArray();
        for (List<String> row : values) {
            JSONObject j = new JSONObject();
            for (int i = 0; i < row.size(); i++) {
                String value = row.get(i);
                String key = headers[i];
                j.put(key, value);
            }
            data.put(j);
        }
        response.put("items", data);
        response.put("success", Boolean.TRUE);
        return new StreamingResolution("application/json", new StringReader(response.toString(4)));
    }

    private Resolution exportAdminCSV(List<List<String>> values, Object[] headers) throws IOException {
        final File f = getAdminExport(values);
        return new StreamingResolution("text/plain") {

            @Override
            public void stream(HttpServletResponse response) throws Exception {
                FileInputStream fis = new FileInputStream(f);
                OutputStream out = response.getOutputStream();
                try {
                    IOUtils.copy(fis, out);
                    out.flush();
                } finally {
                    fis.close();
                    out.close();
                    f.delete();
                }
            }
        }.setAttachment(true).setFilename("export.csv");
    }

    public static File getAdminExport(List<List<String>> values) throws IOException {
        File f = File.createTempFile("tmp", "adminExport");
        FileWriter fw = new FileWriter(f);
        PrintWriter out = new PrintWriter(fw);
        CSVPrinter csv = new CSVPrinter(out, CSVFormat.DEFAULT);
        csv.printRecord(new Object[]{"Beheerder", "Totaal VRI\'s", "VRI's zonder KV9-fouten", "VRI's zonder KV9-fouten en gereed voor export"});
        csv.printRecords(values);
        csv.flush();
        csv.close();
        return f;
    }

    // <editor-fold desc="RSEQ resolutions" defaultstate="collapsed">
    public Resolution rseqByDeelgebied() throws JSONException {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {

            Session sess = (Session) em.getDelegate();

            GeometryCollection deelgebiedPoly = filter.getGeom();

            String query = "from RoadsideEquipment where validation_errors = 0 AND intersects(location, ?) = true";
            if (onlyReady) {
                query += " and readyForExport = true";
            }
            Query q = sess.createQuery(query);
            q.setParameter(0, deelgebiedPoly);
            List<RoadsideEquipment> rseqList = (List<RoadsideEquipment>) q.list();

            JSONArray rseqArray = makeRseqArray(rseqList);

            info.put("rseqs", rseqArray);
            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("search rseq exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }

    public Resolution rseqByKarAddress() throws JSONException {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {
            String query = "from RoadsideEquipment where validation_errors = 0 AND karAddress = :karAddress";
            if (onlyReady) {
                query += " and readyForExport = true";
            }
            List<RoadsideEquipment> r = em.createQuery(query).setParameter("karAddress", karAddress).getResultList();

            JSONArray rseqArray = makeRseqArray(r);
            info.put("rseqs", rseqArray);
            info.put("success", Boolean.TRUE);
        } catch (JSONException e) {
            log.error("search rseq exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }

    public Resolution rseqByDataowner() throws JSONException {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {
            String query = "from RoadsideEquipment where validation_errors = 0 AND dataOwner in :dataowner";
            if (onlyReady) {
                query += " and readyForExport = true";
            }
            List<RoadsideEquipment> rseqs = em.createQuery(query).setParameter("dataowner", dataowner).getResultList();

            JSONArray rseqArray = makeRseqArray(rseqs);

            info.put("rseqs", rseqArray);
            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("search rseq exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }

    public Resolution allRseqs() throws JSONException {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {
            String query = "from RoadsideEquipment where validation_errors = 0";
            if (onlyReady) {
                query += " and readyForExport = true";

            }
            List<RoadsideEquipment> rseqList = em.createQuery(query, RoadsideEquipment.class).getResultList();

            JSONArray rseqArray = makeRseqArray(rseqList);

            info.put("rseqs", rseqArray);
            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("search rseq exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }
    // </editor-fold>

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void lists() throws Exception {
        EntityManager em = Stripersist.getEntityManager();
        deelgebieden = em.createQuery("from Deelgebied where gebruiker = :geb order by id").setParameter("geb", getGebruiker()).getResultList();
        dataowners = em.createQuery("from DataOwner order by omschrijving").getResultList();
    }

    private JSONArray makeRseqArray(List<RoadsideEquipment> rseqs) throws JSONException {
        JSONArray rseqArray = new JSONArray();
        for (RoadsideEquipment rseqObj : rseqs) {
            if (getGebruiker().canRead(rseqObj)) {
                if (onlyValid && !rseqObj.isValid()) {
                    continue;
                }
                
                if(vehicleType == null || vehicleType.equals(VehicleType.VEHICLE_TYPE_GEMIXT) || rseqObj.getVehicleType().equalsIgnoreCase(vehicleType)){
                
                JSONObject jRseq = new JSONObject();
                    jRseq.put("id", rseqObj.getId());
                    jRseq.put("naam", rseqObj.getDescription());
                    jRseq.put("karAddress", rseqObj.getKarAddress());
                    jRseq.put("dataowner", rseqObj.getDataOwner().getOmschrijving());
                    String type = rseqObj.getType();
                    if (type.equalsIgnoreCase("CROSSING")) {
                        jRseq.put("type", "VRI");
                    } else if (type.equalsIgnoreCase("BAR")) {
                        jRseq.put("type", "Afsluitingssysteem");
                    } else if (type.equalsIgnoreCase("GUARD")) {
                        jRseq.put("type", "Waarschuwingssyteem");
                    }
                    rseqArray.put(jRseq);
                }
            }
        }
        return rseqArray;
    }

    @Override
    public Resolution handleValidationErrors(ValidationErrors errors) throws Exception {
        return null;
    }

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Gebruiker getGebruiker() {
        final String attribute = this.getClass().getName() + "_GEBRUIKER";
        Gebruiker g = (Gebruiker) getContext().getRequest().getAttribute(attribute);
        if (g != null) {
            return g;
        }
        Gebruiker principal = (Gebruiker) context.getRequest().getUserPrincipal();
        g = Stripersist.getEntityManager().find(Gebruiker.class, principal.getId());
        getContext().getRequest().setAttribute(attribute, g);
        return g;
    }

    @Override
    public ActionBeanContext getContext() {
        return context;
    }

    @Override
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    public List<Deelgebied> getDeelgebieden() {
        return deelgebieden;
    }

    public void setDeelgebieden(List<Deelgebied> deelgebieden) {
        this.deelgebieden = deelgebieden;
    }

    public String getExportType() {
        return exportType;
    }

    public void setExportType(String exportType) {
        this.exportType = exportType;
    }

    public List<Long> getRseqs() {
        return rseqs;
    }

    public void setRseqs(List<Long> rseqs) {
        this.rseqs = rseqs;
    }

    public Deelgebied getFilter() {
        return filter;
    }

    public void setFilter(Deelgebied filter) {
        this.filter = filter;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public boolean isOnlyValid() {
        return onlyValid;
    }

    public void setOnlyValid(boolean onlyValid) {
        this.onlyValid = onlyValid;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public RoadsideEquipment getRseq() {
        return rseq;
    }

    public void setRseq(RoadsideEquipment rseq) {
        this.rseq = rseq;
    }

    public List<DataOwner> getDataowners() {
        return dataowners;
    }

    public void setDataowners(List<DataOwner> dataowners) {
        this.dataowners = dataowners;
    }

    public List<DataOwner> getDataowner() {
        return dataowner;
    }

    public void setDataowner(List<DataOwner> dataowner) {
        this.dataowner = dataowner;
    }

    public boolean isOnlyReady() {
        return onlyReady;
    }

    public void setOnlyReady(boolean onlyReady) {
        this.onlyReady = onlyReady;
    }

    public List<DataOwner> getDos() {
        return dos;
    }

    public void setDos(List<DataOwner> dos) {
        this.dos = dos;
    }

    public Integer getKarAddress() {
        return karAddress;
    }

    public void setKarAddress(Integer karAddress) {
        this.karAddress = karAddress;
    }
    // </editor-fold>
}
