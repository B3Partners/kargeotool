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

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import nl.b3p.kar.hibernate.*;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.geotools.geometry.jts.WKTReader2;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.Stripersist;

/**
 * Stripes klasse welke de zoek functionaliteit regelt.
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
@StrictBinding
@UrlBinding("/action/search")
public class SearchActionBean implements ActionBean {

    private static final String GEOCODER_URL = "http://geodata.nationaalgeoregister.nl/geocoder/Geocoder?zoekterm=";
    private static final Log log = LogFactory.getLog(SearchActionBean.class);
    private ActionBeanContext context;
    @Validate
    private String term;
    @Validate(required = false) // niet noodzakelijk: extra filter voor buslijnen, maar hoeft niet ingevuld te zijn
    private String dataOwner;
    private static final String KV7NETWERK_JNDI_NAME = "java:comp/env/jdbc/kv7netwerk";

    private static final String EXACT_MATCH_PREFIX = ":";
    private static final String WILDCARD = "*";

    @Validate
    private String vehicleType;
    
    
    private final WKTReader2 wkt = new WKTReader2();

    /**
     *
     * @return De rseq
     * @throws Exception The error
     */
    public Resolution rseq() throws Exception {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {

            Session sess = (Session) em.getDelegate();
            Criteria criteria = sess.createCriteria(RoadsideEquipment.class);
            if(term != null){

                boolean validAddressSearch = false;
                if(term.equals("*MIJN_VRI*")){
                    validAddressSearch = true;
                    info.put("MIJN_VRI", true);
                }
                if(term.startsWith(EXACT_MATCH_PREFIX)) {
                    try {
                        int karAddress = Integer.parseInt(term.substring(EXACT_MATCH_PREFIX.length()));
                        criteria.add(Restrictions.eq("karAddress", karAddress));
                        validAddressSearch = true;
                    } catch(NumberFormatException e) {

                    }
                }
                if(!validAddressSearch) {
                    String[] terms = term.split(" ");
                    Conjunction con = Restrictions.conjunction();
                    for (String t : terms) {
                        Disjunction dis = getRseqDisjunction(t,false);
                        con.add(dis);
                    }
                    
                    Disjunction unsplitDisjunctionExact = getRseqDisjunction(term, true);
                    
                    String wholeTerm = term.replaceAll("\\"+WILDCARD, ""); // remove all the wildcards (possible between words)
                    Criterion descriptionIlike = Restrictions.ilike("description", wholeTerm, MatchMode.ANYWHERE);
                    
                    Disjunction or = Restrictions.or(unsplitDisjunctionExact, con,descriptionIlike);

                    criteria.add(or);
                }
            }
            List<RoadsideEquipment> l = criteria.list();
            JSONArray rseqs = new JSONArray();
            Envelope e = new Envelope();
            for (RoadsideEquipment roadsideEquipment : l) {
                if(getGebruiker().canRead(roadsideEquipment)) {
                    if(vehicleType == null || roadsideEquipment.hasSignalForVehicleType(vehicleType) ){
                        rseqs.put(roadsideEquipment.getRseqGeoJSON());
                        e.expandToInclude(roadsideEquipment.getLocation().getCoordinate());
                    }
                }
            }
            
            JSONObject bbox = new JSONObject();
            bbox.put("minx", e.getMinX());
            bbox.put("miny", e.getMinY());
            bbox.put("maxx", e.getMaxX());
            bbox.put("maxy", e.getMaxY());
            info.put("bbox", bbox);
            info.put("rseqs", rseqs);
            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("search rseq exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        StreamingResolution res =new StreamingResolution("application/json", new StringReader(info.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }
    
    private Disjunction getRseqDisjunction(String t, boolean exact){
        String oldTerm = t;
        MatchMode mm = MatchMode.EXACT;

        if (!exact && t.contains(WILDCARD)) {
            if (t.startsWith(WILDCARD)) {
                t = t.substring(WILDCARD.length());
                mm = MatchMode.END;
            }
            if (t.endsWith(WILDCARD)) {
                t = t.substring(0,t.indexOf(WILDCARD));
                mm = mm.equals(MatchMode.END) ? MatchMode.ANYWHERE : MatchMode.START;
            }
        }

        Disjunction dis = Restrictions.disjunction();
        dis.add(Restrictions.ilike("description", t, mm));
        dis.add(Restrictions.ilike("town", t, mm));

        try {
            int karAddress = Integer.parseInt(t);
            dis.add(Restrictions.eq("karAddress", karAddress));
        } catch (NumberFormatException e) {
        }

        dis.add(Restrictions.ilike("crossingCode", t, mm));
        return dis;
    }

    /**
     *
     * @return De gevonden wegen
     * @throws Exception The error
     */
    public Resolution road() throws Exception {
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {

            Session sess = (Session) em.getDelegate();
            String param;
            
            param = term.replaceAll("\\*", "%");
            
            Query q = sess.createSQLQuery("SELECT ref,name,st_astext(st_union(geometry)) FROM Road where ref ilike :ref group by ref,name");
            q.setParameter("ref", param);

            List<Object[]> l = (List<Object[]>) q.list();
            JSONArray roads = new JSONArray();
            GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 28992);
            WKTReader reader = new WKTReader(gf);
            for (Object[] road : l) {
                JSONObject jRoad = new JSONObject();
                jRoad.put("weg", road[0]);
                if (road[1] != null) {
                    jRoad.put("name", road[1]);
                }
                try {
                    Geometry g = reader.read((String) road[2]);
                    Envelope env = g.getEnvelopeInternal();
                    if (env != null) {
                        JSONObject jEnv = new JSONObject();
                        jEnv.put("minx", env.getMinX());
                        jEnv.put("miny", env.getMinY());
                        jEnv.put("maxx", env.getMaxX());
                        jEnv.put("maxy", env.getMaxY());
                        jRoad.put("envelope", jEnv);
                    }
                } catch (ParseException ex) {
                }
                roads.put(jRoad);
            }
            info.put("roads", roads);
            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("search road exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        StreamingResolution res =new StreamingResolution("application/json", new StringReader(info.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }

    /**
     * Doorzoekt de KV7 database. Gebruikt de parameter term voor publicnumber
     * en linename uit de line tabel. Alleen resultaten met een geometrie worden
     * teruggegeven.
     *
     * @return Resolution Resolution met daarin een JSONObject met de gevonden
     * buslijnen (bij succes) of een fout (bij falen).
     * @throws Exception De error
     *
     */
    public Resolution busline() throws Exception {

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);

        Connection c = getConnection();
        try {

            String schema = new QueryRunner().query(c, "select schema from data.netwerk where state = 'active' order by processed_date desc limit 1", new ScalarHandler<String>());

            JSONArray lines = new JSONArray();
            if(schema != null) {
                String sql = "select linepublicnumber,linename, min(st_xmin(the_geom)), min(st_ymin(the_geom)), max(st_xmax(the_geom)), "
                        + "max(st_ymax(the_geom)), dataownercode "
                        + "from " + schema + ".map_line "
                        + "where the_geom is not null and (linepublicnumber ilike ? or linename ilike ?)";

                if (dataOwner != null) {
                    sql += " and dataownercode = ?";
                }
                sql += " group by linepublicnumber,linename, dataownercode order by linename";
                ResultSetHandler<JSONArray> h = new ResultSetHandler<JSONArray>() {
                    public JSONArray handle(ResultSet rs) throws SQLException {
                        JSONArray lines = new JSONArray();
                        while (rs.next()) {
                            JSONObject line = new JSONObject();
                            try {
                                line.put("publicnumber", rs.getString(1));
                                line.put("name", rs.getString(2));
                                JSONObject jEnv = new JSONObject();
                                jEnv.put("minx", rs.getDouble(3));
                                jEnv.put("miny", rs.getDouble(4));
                                jEnv.put("maxx", rs.getDouble(5));
                                jEnv.put("maxy", rs.getDouble(6));
                                line.put("envelope", jEnv);
                                line.put("dataowner", rs.getString(7));
                                lines.put(line);

                            } catch (JSONException je) {
                                log.error("Kan geen buslijn ophalen: ", je);
                            }
                        }
                        return lines;
                    }
                };
                if (term == null) {
                    term = "";
                }
                JSONObject s = new JSONObject();
                s.put("schema", schema);
                JSONArray matchedLines;
                String param;
                param = term.replaceAll("\\*", "%");
                
                if (dataOwner != null) {
                    matchedLines = new QueryRunner().query(c, sql, h, param, param, dataOwner);
                } else {
                    matchedLines  = new QueryRunner().query(c, sql, h, param, param);
                }
                s.put("lines", matchedLines);
                if(matchedLines.length() != 0) {
                    lines.put(s);
                }
            }
            info.put("buslines", lines);
            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("Cannot execute query:", e);
        } finally {
            DbUtils.closeQuietly(c);
        }
        StreamingResolution res =new StreamingResolution("application/json", new StringReader(info.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }

    /**
     *
     * @return De gebruiker
     */
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

    /**
     *
     * @return Stripes Resolution geocode
     * @throws Exception De fout
     */
    public Resolution geocode() throws Exception {
        HttpSolrClient client = new HttpSolrClient.Builder("http://geodata.nationaalgeoregister.nl/locatieserver")
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();
        JSONObject result = new JSONObject();
        try {
            JSONArray respDocs = new JSONArray();
            SolrQuery query = new SolrQuery();
            // add asterisk to make it match partial queries (for autosuggest)
            term += "*";
    
            query.setQuery(term);
            query.setRequestHandler("/free");
            QueryResponse rsp = client.query(query);
            SolrDocumentList list = rsp.getResults();
            
            for (SolrDocument solrDocument : list) {
                JSONObject doc = solrDocumentToResult(solrDocument);
                if (doc != null) {
                    respDocs.put(doc);
                }
            }
            result.put("results", respDocs);
            result.put("numResults", list.size());
        } catch (SolrServerException ex) {
            log.error("Cannot search:",ex);
        }
        
        StreamingResolution res = new StreamingResolution("text/xml",  new StringReader(result.toString(4)));
        res.setCharacterEncoding("UTF-8");
        return res;
    }
    
    private JSONObject solrDocumentToResult(SolrDocument doc){
        JSONObject result = null;
        try {
            Map<String, Object> values = doc.getFieldValueMap();
            result = new JSONObject();
            for (String key : values.keySet()) {
                result.put(key, values.get(key));
            }
            String centroide = (String)doc.getFieldValue("centroide_rd");
            
            String geom = centroide;
            if(values.containsKey("geometrie_rd")){
                geom = (String) values.get("geometrie_rd");
            }
            Geometry g = wkt.read(geom);
            Envelope env = g.getEnvelopeInternal();
            
            if (centroide != null) {
                Map bbox = new HashMap();
                bbox.put("minx", env.getMinX());
                bbox.put("miny", env.getMinY());
                bbox.put("maxx", env.getMaxX());
                bbox.put("maxy", env.getMaxY());

                result.put("location", bbox);
            }
            result.put("label", values.get("weergavenaam"));
            
        } catch (JSONException ex) {
            log.error(ex);
        }catch( ParseException ex){
            log.error(ex);
            
        }
        return result;
    }
    // <editor-fold desc="Getters and Setters">

    /**
     *
     * @return De context
     */
    public ActionBeanContext getContext() {
        return context;
    }

    /**
     *
     * @param context context
     */
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    /**
     *
     * @return dataowner
     */
    public String getDataOwner() {
        return dataOwner;
    }

    /**
     * 
     * @param dataOwner dataowner
     */
    public void setDataOwner(String dataOwner) {
        this.dataOwner = dataOwner;
    }

    /**
     *
     * @return vehicletype
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
     * @return term
     */
    public String getTerm() {
        return term;
    }

    /**
     *
     * @param term term
     */
    public void setTerm(String term) {
        this.term = term;
    }

    private Connection getConnection() throws NamingException, SQLException {
        Context initCtx = new InitialContext();
        DataSource ds = (DataSource) initCtx.lookup(KV7NETWERK_JNDI_NAME);

        return ds.getConnection();
    }
    // </editor-fold>
}
