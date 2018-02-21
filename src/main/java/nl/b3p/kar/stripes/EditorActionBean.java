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
package nl.b3p.kar.stripes;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;
import java.io.StringReader;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.OneToManyTypeConverter;
import net.sourceforge.stripes.validation.Validate;
import nl.b3p.geojson.GeoJSON;
import nl.b3p.kar.hibernate.*;
import nl.b3p.kar.imp.KV9ValidationError;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.Type;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.Stripersist;

/**
 * Stripes klasse welke de edit functionaliteit regelt.
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
@StrictBinding
@UrlBinding("/action/editor")
public class EditorActionBean implements ActionBean {

    private static final Log log = LogFactory.getLog(EditorActionBean.class);
    private static final String JSP = "/WEB-INF/jsp/viewer/editor.jsp";
    private static final int RIJKSDRIEHOEKSTELSEL = 28992;
    private ActionBeanContext context;
    @Validate
    private RoadsideEquipment rseq;
    @Validate
    private String json;
    private JSONArray vehicleTypesJSON;
    private JSONArray dataOwnersJSON;
    private JSONArray dataOwnersVervoerders;
    private JSONObject ovInfoJSON;
    
    private JSONArray dataownersForUser;

    @Validate
    private String extent;

    @Validate(converter = OneToManyTypeConverter.class)
    private List<Gebruiker> usersToInform;

    public Gebruiker getGebruiker() {
        final String attribute = this.getClass().getName() + "_GEBRUIKER";
        Gebruiker g = (Gebruiker)getContext().getRequest().getAttribute(attribute);
        if(g != null) {
            return g;
        }
        Gebruiker principal = (Gebruiker) context.getRequest().getUserPrincipal();
        g = Stripersist.getEntityManager().find(Gebruiker.class, principal.getId());
        getContext().getRequest().setAttribute(attribute, g);
        return g;
    }

    /**
     * Stripes methode waarmee de view van het edit proces wordt voorbereid.
     *
     * @return Stripes Resolution view
     * @throws Exception Wordt gegooid als er iets mis gaat.
     */
    @DefaultHandler
    public Resolution view() throws Exception {

        if("true".equals(getContext().getServletContext().getInitParameter("debug_editoractionbean_view"))) {
            log.info("view() start, setting root log level to DEBUG");
            LogManager.getRootLogger().setLevel(Level.DEBUG);
        }

        EntityManager em = Stripersist.getEntityManager();

        log.debug("view(): vehicleTypesJSON");
        vehicleTypesJSON = new JSONArray();
        for (VehicleType vt : (List<VehicleType>) em.createQuery("from VehicleType order by nummer").getResultList()) {
            JSONObject jvt = new JSONObject();
            jvt.put("nummer", vt.getNummer());
            jvt.put("groep", vt.getGroep());
            jvt.put("omschrijving", vt.getOmschrijving());
            vehicleTypesJSON.put(jvt);
        }

        log.debug("view(): dataOwnersJSON");
        dataOwnersJSON = new JSONArray();
        dataOwnersVervoerders = new JSONArray();
        
        Collection<DataOwner> dataOwners;
        Gebruiker g = getGebruiker();
        if(g.isBeheerder()) {
            dataOwners = (List<DataOwner>) em.createQuery("from DataOwner order by omschrijving").getResultList();
        } else {
            // TODO: geen sortering op omschrijving
            dataOwners = g.getEditableDataOwners();
        }

        for (DataOwner dao : dataOwners) {
            JSONObject jdao = new JSONObject();
            jdao.put("code", dao.getCode());
            jdao.put("id", dao.getId());
            jdao.put("classificatie", dao.getClassificatie());
            jdao.put("companyNumber", dao.getCompanyNumber());
            jdao.put("omschrijving", dao.getOmschrijving());
            dataOwnersJSON.put(jdao);
            if(dao.getClassificatie().equals(DataOwner.CLASSIFICATIE_VERVOERDER)){
                dataOwnersVervoerders.put(jdao);
            }
        }
        
        Set<DataOwner> da = g.getDataOwnerRights().keySet();
        dataownersForUser = new JSONArray(da);

        log.debug("view(): ovInfoJSON");
        ovInfoJSON = makeOvInfoJSON();

        log.debug("view forwarding to JSP, setting root log level to INFO");
        LogManager.getRootLogger().setLevel(Level.INFO);

        return new ForwardResolution(JSP);
    }

    private JSONObject makeOvInfoJSON() {
        JSONObject ovInfo = new JSONObject();

        Connection conn = null;
        try {
            log.debug("  makeOvInfoJSON(): lookup kv7netwerk datasource");
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource)initCtx.lookup("java:comp/env/jdbc/kv7netwerk");
            log.debug("  makeOvInfoJSON(): open connection");
            conn = ds.getConnection();

            log.debug("  makeOvInfoJSON(): get netwerk");
            Map<String,Object> netwerk = new QueryRunner().query(conn, "select id::varchar,schema,processed_date::varchar from data.netwerk where state = 'active' order by processed_date desc limit 1", new MapHandler());

            if(netwerk != null) {
                for(Map.Entry<String,Object> entry: netwerk.entrySet()) {
                    ovInfo.put(entry.getKey(), (String)entry.getValue());
                }
            }
        } catch(Exception e) {
            log.error("Kan geen ov info ophalen: ", e);
        } finally {
            log.debug("  makeOvInfoJSON(): close connection");
            DbUtils.closeQuietly(conn);
        }
        return ovInfo;
    }

    /**
     * Stripes methode waarmee de huidge roadside equipement wordt opgehaald.
     *
     * @return Stripes Resolution rseqJSON
     * @throws Exception  Wordt gegooid als er iets mis gaat.
     */
    public Resolution rseqJSON() throws Exception {
        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {
            if (rseq == null) {
                throw new IllegalArgumentException("rseq parameter ontbreekt voor rseqJSON(), probeer het opnieuw");
            }
            if(!getGebruiker().canRead(rseq)) {
                info.put("error", "De gebruiker is niet gemachtigd om dit verkeerssysteem te bewerken of lezen.");
            } else {
                info.put("roadsideEquipment", rseq.getJSON());
                info.put("editable", getGebruiker().canEdit(rseq));

                info.put("success", Boolean.TRUE);
            }
        } catch (Exception e) {
            log.error("rseqJSON exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        StreamingResolution res = new StreamingResolution("application/json", new StringReader(info.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }

    /**
     * Stripes methode waarmee alle roadside equipement wordt opgehaald.
     *
     * @return Stripes Resolution allRseqJSON
     * @throws Exception Wordt gegooid als er iets mis gaat.
     */
    public Resolution allRseqJSON() throws Exception {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {
            List<RoadsideEquipment> rseqList;

            if(getGebruiker().isBeheerder() || getGebruiker().isVervoerder()) {
                rseqList = (List<RoadsideEquipment>) em.createQuery("from RoadsideEquipment").getResultList();
            } else {
                Set<DataOwner> dos = getGebruiker().getReadableDataOwners();
                if (dos.isEmpty()) {
                    rseqList = Collections.EMPTY_LIST;
                } else {
                    // Geen test op GebruikerVRIRights.readable nodig: als record
                    // aanwezig is is editable of readable altijd true
                    // Zie Gebruiker.canRead() comments
                    // En editable betekent ook readable
                    List<RoadsideEquipment> allowedRseqs = em.createQuery(
                            "select distinct(gv.roadsideEquipment) from GebruikerVRIRights gv "
                            + "where gebruiker = :geb")
                            .setParameter("geb", getGebruiker())
                            .getResultList();


                    rseqList = (List<RoadsideEquipment>) em.createQuery(
                            "from RoadsideEquipment "
                            + "where dataOwner in (:dos)")
                            .setParameter("dos", dos)
                            .getResultList();
                    rseqList.addAll(allowedRseqs);
                }
            }
            Gebruiker g =getGebruiker();
            JSONArray rseqs = new JSONArray();
            for (RoadsideEquipment r : rseqList) {
                JSONObject rseqJson = r.getRseqGeoJSON();
                rseqJson.put("editable", g.canEdit(r));
                rseqs.put(rseqJson);
            }
            info.put("rseqs", rseqs);

            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("allRseqJSON exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        
        StreamingResolution res = new StreamingResolution("application/json", new StringReader(info.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }

    public Resolution roads() throws Exception {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {

            Point p = rseq.getLocation();
            Polygon buffer = (Polygon) p.buffer(500, 1);
            buffer.setSRID(RIJKSDRIEHOEKSTELSEL);
            Session session = (Session) em.getDelegate();

            Query q = session.createQuery("from Road where intersects(geometry, ?) = true");
            q.setMaxResults(100);
        //    Type geometryType = GeometryUserType.TYPE;
            q.setParameter(0, buffer);
            List<Road> roads = (List<Road>)q.list();

            JSONArray rs = new JSONArray();
            for (Road r : roads) {
                rs.put(r.getGeoJSON());
            }
            info.put("roads", rs);

            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("roads exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }


    public Resolution surroundingPoints() throws Exception {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {

            GeometryFactory gf = new GeometryFactory(new PrecisionModel(), RIJKSDRIEHOEKSTELSEL);
            WKTReader reader= new WKTReader(gf);
            Polygon p = (Polygon)reader.read(extent);
          //  p.setSRID(RIJKSDRIEHOEKSTELSEL);
            Session session = (Session) em.getDelegate();

            Collection<DataOwner> dos = getGebruiker().getReadableDataOwners();
            List<ActivationPoint> points;
            if(getGebruiker().isBeheerder() || getGebruiker().isVervoerder()) {
                points = session.createQuery(
                        "from ActivationPoint "
                      + "where intersects(location, :pos) = true "
                      + "and roadsideEquipment <> :this")
                        .setParameter("pos", p)
                        .setParameter("this", rseq)
                        .list();
            } else if(dos.isEmpty()) {
                points = Collections.EMPTY_LIST;
            } else {
                points = session.createQuery(
                        "from ActivationPoint "
                      + "where roadsideEquipment.dataOwner in (:dos) "
                      + "and intersects(location, :pos) = true and roadsideEquipment <> :this")
                        .setParameterList("dos", dos)
                        .setParameter("pos", p)
                        .setParameter("this", rseq)
                        .list();

                // TODO: include GebruikerVRIRights RSEQ's
            }

            JSONArray rs = new JSONArray();
            for (ActivationPoint r : points) {

                rs.put(r.getGeoJSON());
            }
            info.put("points", rs);

            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("surroundingPoints exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }

    /**
     * Bepaalt of een JSON object nieuw gemaakt client-side. De door JavaScript
     * client-side gestuurde JSON om een RoadsideEquipment op te slaan kan nieuw
     * gemaakte objecten bevatten die nog geen persistente JPA entities zijn,
     * deze hebben een door ExtJS bepaald id dat begint met "ext-gen".
     *
     * @param j het JSON object
     * @return of het JSON object client-side nieuw gemaakt is
     */
    private static boolean isNew(JSONObject j) {
        String id = j.optString("id");
        return id != null && id.startsWith("ext-");
    }

    /**
     * Ajax handler om een RSEQ te verwijderen.
     *
     * @return gelukt ja/nee
     */
    public Resolution removeRseq () throws JSONException{
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {
            Gebruiker g = getGebruiker();
            if(!g.canEdit(rseq)) {
                throw new IllegalStateException(
                        String.format("Gebruiker \"%s\" heeft geen schrijfrechten op VRI %d, \"%s\", data owner code %s (\"%s\")",
                        g.getUsername(),
                        rseq.getKarAddress(),
                        rseq.getDescription(),
                        rseq.getDataOwner().getCode(),
                        rseq.getDescription()));
            }

            Set<Long> mIds = new HashSet();
            for(Movement m : rseq.getMovements()) {
                m.getPoints().clear();
                mIds.add(m.getId());
            }
            em.flush();
            if (!mIds.isEmpty()) {
                em.createNativeQuery("delete from movement_activation_point where movement in (:m)")
                        .setParameter("m", mIds)
                        .executeUpdate();
            }
            rseq.getMovements().clear();
            rseq.getPoints().clear();
            em.flush();
            em.createNativeQuery("delete from activation_point where roadside_equipment = :r")
                    .setParameter("r", rseq.getId())
                    .executeUpdate();
            em.createQuery("delete from GebruikerVRIRights where roadsideEquipment = :rseq")
                    .setParameter("rseq", rseq)
                    .executeUpdate();
            em.createQuery("delete from InformMessage where rseq = :rseq")
                    .setParameter("rseq", rseq)
                    .executeUpdate();
            em.flush();
            em.remove(rseq);
            em.flush();
            em.getTransaction().commit();
            info.put("success", Boolean.TRUE);
        }catch(Exception e){
            log.error("Removing of rseq failed.",e);
            info.put("error",ExceptionUtils.getMessage(e));
        }
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }

    /**
     * Ajax handler welke verwerkt welke vervoerders geïnformeerd moeten worden over het opgegeven RoadsideEquipment
     * @return gelukt ja/nee
     */

    public Resolution informCarriers() throws JSONException{
        EntityManager em = Stripersist.getEntityManager();
        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        for (Gebruiker vervoerder : usersToInform) {
            InformMessage msg = new InformMessage();
            msg.setCreatedAt(new Date());
            msg.setAfzender(getGebruiker());
            msg.setVervoerder(vervoerder);
            msg.setRseq(rseq);
            purgePreviousMessage(msg);
            em.persist(msg);
        }
        rseq.setReadyForExport(true);
        em.persist(rseq);

        em.getTransaction().commit();
        info.put("success", Boolean.TRUE);
        
        StreamingResolution res =new StreamingResolution("application/json", new StringReader(info.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }

    private void purgePreviousMessage(InformMessage current){
        EntityManager em = Stripersist.getEntityManager();
        em.createQuery("DELETE FROM InformMessage where vervoerder = :vervoerder and afzender = :afzender and mailSent = false "
                + "and rseq = :rseq")
                .setParameter("vervoerder", current.getVervoerder())
                .setParameter("afzender", current.getAfzender())
                .setParameter("rseq", current.getRseq()).executeUpdate();
    }

    /**
     * Ajax handler om een RoadsideEquipment die in de json parameter is
     * meegegeven op te slaan.
     * @return Opgeslagen rseq
     * @throws java.lang.Exception Wordt gegooid als er iets mis gaat.
     */
    public Resolution saveOrUpdateRseq() throws Exception {
         if("true".equals(getContext().getServletContext().getInitParameter("debug_editoractionbean_saveorupdate"))) {
            log.info("saveOrUpdateRseq() start, setting root log level to DEBUG");
            LogManager.getRootLogger().setLevel(Level.DEBUG);
        }
        log.debug("saveOrUpdateRseq: original json: " + json);

        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {
            JSONObject jrseq = new JSONObject(json);

            Gebruiker g = getGebruiker();

            if (isNew(jrseq)) {
                rseq = new RoadsideEquipment();
            } else {
                rseq = em.find(RoadsideEquipment.class, jrseq.getLong("id"));

                if(rseq == null) {
                    throw new IllegalStateException("Kan verkeerssysteem niet vinden, verwijderd door andere gebruiker?");
                }
            }

            rseq.setLocation(GeoJSON.toPoint(jrseq.getJSONObject("location")));
            rseq.setDescription(jrseq.optString("description"));
            rseq.setTown(jrseq.optString("town"));
            rseq.setType(jrseq.getString("type"));
            rseq.setReadyForExport(jrseq.getBoolean("readyForExport"));
            rseq.setKarAddress(jrseq.getInt("karAddress"));
            rseq.setCrossingCode(jrseq.optString("crossingCode"));
            rseq.setDataOwner(em.find(DataOwner.class, jrseq.getInt("dataOwner")));
            if(rseq.getDataOwner() == null) {
                throw new IllegalArgumentException("Data owner is verplicht");
            }
            if(!g.canEdit(rseq)) {
                throw new IllegalStateException(
                        String.format("Gebruiker \"%s\" heeft geen schrijfrechten op VRI %d, \"%s\", data owner code %s",
                        g.getUsername(),
                        rseq.getKarAddress(),
                        rseq.getDescription(),
                        rseq.getDataOwner().getCode()));
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            rseq.setValidFrom(jrseq.has("validFrom") ? sdf.parse(jrseq.getString("validFrom")) : null);
            rseq.setValidUntil(jrseq.has("validUntil") &&  !jrseq.getString("validUntil").isEmpty() ? sdf.parse(jrseq.getString("validUntil")) : null);
            rseq.setMemo(jrseq.has("memo") ? jrseq.getString("memo") : null);

            rseq.getKarAttributes().clear();
            JSONObject attributes = jrseq.getJSONObject("attributes");
            for(Iterator it = attributes.keys(); it.hasNext();) {
                String serviceType = (String)it.next();
                JSONArray perCommandType = attributes.getJSONArray(serviceType);

                KarAttributes ka = new KarAttributes(
                        serviceType,
                        ActivationPointSignal.COMMAND_INMELDPUNT,
                        perCommandType.getJSONArray(0));
                if(ka.getUsedAttributesMask() != 0) {
                    rseq.getKarAttributes().add(ka);
                }
                ka = new KarAttributes(
                        serviceType,
                        ActivationPointSignal.COMMAND_UITMELDPUNT,
                        perCommandType.getJSONArray(1));
                if(ka.getUsedAttributesMask() != 0) {
                    rseq.getKarAttributes().add(ka);
                }
                ka = new KarAttributes(
                        serviceType,
                        ActivationPointSignal.COMMAND_VOORINMELDPUNT,
                        perCommandType.getJSONArray(2));
                if(ka.getUsedAttributesMask() != 0) {
                    rseq.getKarAttributes().add(ka);
                }
            }

            if (rseq.getId() == null) {
                em.persist(rseq);
            }

            JSONArray jpts = jrseq.getJSONArray("points");
            Integer highestPointNumber = 0;
            Map<String, ActivationPoint> pointsByJSONId = new HashMap();
            for (int i = 0; i < jpts.length(); i++) {
                JSONObject jpt = jpts.getJSONObject(i);

                ActivationPoint p = null;
                if (isNew(jpt)) {
                    p = new ActivationPoint();
                    p.setRoadsideEquipment(rseq);
                } else {
                    for (ActivationPoint ap2 : rseq.getPoints()) {
                        if (ap2.getId().equals(jpt.getLong("id"))) {
                            p = ap2;
                            break;
                        }
                    }
                }
                pointsByJSONId.put(jpt.get("id").toString(), p);
                p.setNummer(jpt.has("nummer") ? jpt.getInt("nummer") : null);
                p.setLabel(jpt.optString("label"));
                p.setLocation(GeoJSON.toPoint(jpt.getJSONObject("geometry")));
                if (p.getNummer() != null) {
                    highestPointNumber = Math.max(p.getNummer(), highestPointNumber);
                }
            }

            for (ActivationPoint ap : pointsByJSONId.values()) {
                if (ap.getNummer() == null) {
                    ap.setNummer(++highestPointNumber);
                }
                if (ap.getId() == null) {
                    rseq.getPoints().add(ap);
                    em.persist(ap);
                }

            }

            // XXX delete niet goed
            rseq.getPoints().retainAll(pointsByJSONId.values());

            Set<Long> mIds = new HashSet();
            for (Movement m : rseq.getMovements()) {
                m.getPoints().clear();
                mIds.add(m.getId());
            }
            em.flush();
            if (!mIds.isEmpty()) {
                em.createNativeQuery("delete from movement_activation_point where movement in (:m)")
                        .setParameter("m", mIds)
                        .executeUpdate();
            }
            rseq.getMovements().clear();
            em.flush();

            boolean incorrectJSON = false;
            JSONArray jmvmts = jrseq.getJSONArray("movements");
            for (int i = 0; i < jmvmts.length(); i++) {
                JSONObject jmvmt = jmvmts.getJSONObject(i);
                Movement m = new Movement();
                m.setRoadsideEquipment(rseq);
                m.setNummer(jmvmt.has("nummer") ? jmvmt.getInt("nummer") : null);

                JSONArray jmaps = jmvmt.getJSONArray("maps");
                for (int j = 0; j < jmaps.length(); j++) {
                    JSONObject jmap = jmaps.getJSONObject(j);
                    MovementActivationPoint map = new MovementActivationPoint();
                    map.setMovement(m);
                    map.setBeginEndOrActivation(jmap.getString("beginEndOrActivation"));
                    if(!pointsByJSONId.containsKey(jmap.get("pointId").toString())){
                        incorrectJSON = true;
                        log.error("Foutief kruispunt geprobeerd op te slaan. Getracht werd om pointID " + jmap.get("pointId").toString() + " op te halen bij movement.  JSON: " +json);
                        continue;
                    }
                    map.setPoint(pointsByJSONId.get(jmap.get("pointId").toString()));

                    if (MovementActivationPoint.ACTIVATION.equals(map.getBeginEndOrActivation())) {
                        ActivationPointSignal signal = new ActivationPointSignal();
                        map.setSignal(signal);
                        String s = jmap.optString("distanceTillStopLine", "");
                        signal.setDistanceTillStopLine(!"".equals(s) ? new Integer(s) : null);
                        signal.setKarCommandType(jmap.getInt("commandType"));
                        s = jmap.optString("signalGroupNumber");
                        signal.setSignalGroupNumber(!"".equals(s) ? new Integer(s) : null);
                        s = jmap.optString("virtualLocalLoopNumber");
                        signal.setVirtualLocalLoopNumber(!"".equals(s) ? new Integer(s) : null);
                        signal.setTriggerType(jmap.getString("triggerType"));
                        JSONArray vtids = jmap.optJSONArray("vehicleTypes");
                        if (vtids != null) {
                            for (int k = 0; k < vtids.length(); k++) {
                                signal.getVehicleTypes().add(em.find(VehicleType.class, vtids.getInt(k)));
                            }
                        }
                        JSONArray dir = jmap.optJSONArray("direction");
                        if(dir != null){
                            String direction = dir.join(",");
                            signal.setDirection(direction);
                        }
                    }
                    m.getPoints().add(map);
                }
                if (!m.getPoints().isEmpty()) {
                    m.setNummer(rseq.getMovements().size() + 1);
                    em.persist(m);
                    rseq.getMovements().add(m);
                }
            }

            rseq.setVehicleType(rseq.determineType());

            em.persist(rseq);
            em.getTransaction().commit();

            // Detach omdat bij afkappen rseq gewijzigd wordt
            em.detach(rseq);
            int validationErrors = rseq.validateKV9(new ArrayList<KV9ValidationError>());

            em.createQuery("update RoadsideEquipment set validationErrors = :res where id = :id")
                    .setParameter("res", validationErrors)
                    .setParameter("id", rseq.getId())
                    .executeUpdate();

            em.getTransaction().commit();

            // Rechtencontrole is al gedaan aan het begin van deze methode, als
            // gebruiker rseq niet zou kunnen bewerken kunnen we hier niet komen
            info.put("editable", /* g.canEdit(rseq) */ true);

            info.put("roadsideEquipment", rseq.getJSON());
            info.getJSONObject("roadsideEquipment").put("validationErrors", validationErrors);
            info.put("success", Boolean.TRUE);
            if (incorrectJSON) {
                info.put("extraMessage", "Bij het opslaan is iets mis gegaan: mogelijk zijn niet alle punten opgeslagen. Controleer het kruispunt en informeer de beheerder.");
            }
        } catch (Exception e) {
            log.error("saveOrUpdateRseq exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        
        StreamingResolution res =new StreamingResolution("application/json", new StringReader(info.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }

    public Resolution getValidationErrors() throws JSONException {
        JSONObject j = new JSONObject();
        JSONArray e = new JSONArray();
        j.put("errors", e);
        List<KV9ValidationError> kvErrors = new ArrayList();
        rseq.validateKV9(kvErrors);
        for(KV9ValidationError kvError: kvErrors) {
            e.put(kvError.toJSONObject());
        }
        
        StreamingResolution res =new StreamingResolution("application/json", new StringReader(j.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }


    public Resolution listCarriers() throws JSONException{
        List<Gebruiker> gebruikers = Stripersist.getEntityManager().createQuery("from Gebruiker where email is not null order by id").getResultList();
        JSONObject info = new JSONObject();
        info.put( "success", Boolean.FALSE );
        JSONArray users = new JSONArray();
        for (Gebruiker geb : gebruikers) {
            if(geb.isVervoerder()){
                users.put(geb.toJSON());
            }
        }

        info.put("carriers", users);
        info.put( "success", Boolean.TRUE );
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }

    // <editor-fold desc="Getters and Setters">
    public ActionBeanContext getContext() {
        return context;
    }

    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    /**
     *
     * @return rseq rseq 
     */
    public RoadsideEquipment getRseq() {
        return rseq;
    }

    /**
     *
     * @param rseq rseq
     */
    public void setRseq(RoadsideEquipment rseq) {
        this.rseq = rseq;
    }

    /**
     *
     * @return dataOwnersJSON dataOwnersJSON
     */
    public JSONArray getDataOwnersJSON() {
        return dataOwnersJSON;
    }

    /**
     *
     * @param dataOwnersJSON dataOwnersJSON
     */
    public void setDataOwnersJSON(JSONArray dataOwnersJSON) {
        this.dataOwnersJSON = dataOwnersJSON;
    }

    /**
     *
     * @return vehicleTypesJSON
     */
    public JSONArray getVehicleTypesJSON() {
        return vehicleTypesJSON;
    }

    /**
     *
     * @param vehicleTypesJSON vehicleTypesJSON
     */
    public void setVehicleTypesJSON(JSONArray vehicleTypesJSON) {
        this.vehicleTypesJSON = vehicleTypesJSON;
    }

    /**
     *
     * @return json
     */
    public String getJson() {
        return json;
    }

    /**
     *
     * @param json json
     */
    public void setJson(String json) {
        this.json = json;
    }

    public String getExtent() {
        return extent;
    }

    public void setExtent(String extent) {
        this.extent = extent;
    }

    public JSONObject getOvInfoJSON() {
        return ovInfoJSON;
    }

    public void setOvInfoJSON(JSONObject ovInfoJSON) {
        this.ovInfoJSON = ovInfoJSON;
    }

    public List<Gebruiker> getUsersToInform() {
        return usersToInform;
    }

    public void setUsersToInform(List<Gebruiker> usersToInform) {
        this.usersToInform = usersToInform;
    }
    
    public JSONArray getDataownersForUser() {
        return dataownersForUser;
    }

    public void setDataownersForUser(JSONArray dataownersForUser) {
        this.dataownersForUser = dataownersForUser;
    }
    
    public JSONArray getDataOwnersVervoerders() {
        return dataOwnersVervoerders;
    }

    public void setDataOwnersVervoerders(JSONArray dataOwnersVervoerders) {
        this.dataOwnersVervoerders = dataOwnersVervoerders;
    }
    // </editor-fold>

}
