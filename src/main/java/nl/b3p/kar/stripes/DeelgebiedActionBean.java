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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.StrictBinding;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import nl.b3p.kar.hibernate.Deelgebied;
import nl.b3p.kar.hibernate.Gebruiker;
import nl.b3p.kar.hibernate.Gemeente;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.Type;
import org.json.JSONArray;
import org.json.JSONObject;
import org.stripesstuff.stripersist.EntityTypeConverter;
import org.stripesstuff.stripersist.Stripersist;

/**
 *
 * @author Meine Toonen
 */
@UrlBinding("/action/deelgebied")
@StrictBinding
public class DeelgebiedActionBean implements ActionBean{
  
    private static final Log log = LogFactory.getLog(DeelgebiedActionBean.class);
    private static final String NIEUW_DEELGEBIED = "/WEB-INF/jsp/export/deelgebied.jsp";
    private final int SRID = 28992;
    
    private ActionBeanContext context;
    
    @Validate(on = "saveDeelgebied", required = true)
    private String geom;

    @Validate(converter = EntityTypeConverter.class, on = {"saveDeelgebied"})
    @ValidateNestedProperties({
        @Validate(field = "name")
    })
    private Deelgebied deelgebied;

    @Validate(converter = EntityTypeConverter.class, on = {"bewerkDeelgebied"})
    private Deelgebied filter;
    
    @Validate(on="gemeente")
    private int x;
    
    @Validate(on="gemeente")
    private int y;
    
    @DefaultHandler
    public Resolution maakDeelgebied() {
        deelgebied = new Deelgebied();
        return new ForwardResolution(NIEUW_DEELGEBIED);
    }

    public Resolution bewerkDeelgebied() {
        deelgebied = filter;
        return new ForwardResolution(NIEUW_DEELGEBIED);
    }
    
    public Resolution saveDeelgebied() throws Exception {
        EntityManager em = Stripersist.getEntityManager();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), SRID);
        WKTReader reader = new WKTReader(gf);
        try {
            Geometry g = reader.read(geom);
            MultiPolygon geometrie;
            if(g instanceof MultiPolygon){
                geometrie = (MultiPolygon)g;
            }else if(g instanceof Polygon){
                
                Polygon p = (Polygon)g;
                Polygon[] ps = {p};
                geometrie = new MultiPolygon(ps, gf);
            }else if(g instanceof GeometryCollection){
                
                GeometryCollection p = (GeometryCollection)g;
                List<Polygon> ps = new ArrayList<>();
                for (int i = 0; i < p.getNumGeometries(); i++) {
                    Geometry geom = p.getGeometryN(i);
                    if(geom instanceof Polygon){
                        ps.add((Polygon)geom);
                    }else if( geom instanceof MultiPolygon){
                        MultiPolygon mp = (MultiPolygon)geom;
                        for (int j = 0; j < mp.getNumGeometries(); j++) {
                            Polygon pol = (Polygon) mp.getGeometryN(j);
                            ps.add(pol);
                        }
                    }
                    
                }
                geometrie  = new MultiPolygon(ps.toArray(new Polygon[0]), gf);
            }else{
                throw new IllegalArgumentException("Wrong geom type");
            }
            deelgebied.setGeom(geometrie);
            deelgebied.setGebruiker(getGebruiker());
            em.persist(deelgebied);
            em.getTransaction().commit();
            filter = deelgebied;
        } catch (ParseException ex) {
            log.error(ex);
        }
        return new ForwardResolution(ExportActionBean.class);
    }

    public Resolution removeDeelgebied() throws Exception {
        EntityManager em = Stripersist.getEntityManager();
        deelgebied = filter;
        em.remove(deelgebied);
        em.getTransaction().commit();
        return new ForwardResolution(ExportActionBean.class);
    }

    public Resolution gemeente(){
        JSONObject  response = new JSONObject();
        response.put("success", false);
        
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), SRID);
        Point p = gf.createPoint(new Coordinate(x,y));
        //Point  p = new Point(new Coordinate(x,y), new PrecisionModel(), SRID);
        String q = "from Gemeente where intersects(geom, ?) = true";
        
         //   Type geometryType = GeometryUserType.TYPE;
        EntityManager em = Stripersist.getEntityManager();
        Session s = (Session)em.getDelegate();
        Query query = s.createQuery(q).setParameter(0, p);
        List<Gemeente> res = query.list();
        JSONArray ar = new JSONArray();
        for (Gemeente g : res) {
            JSONObject obj = new JSONObject();
            obj.put("id", g.getId());
            obj.put("geom", g.getGeom().toText());
            ar.put(obj);
        }
        response.put("gemeentes", ar);
        response.put("success", true);
        return new StreamingResolution("application/json", response.toString());
    }

    // <editor-fold desc="Getters and setters" defaultstate="collapsed">
    @Override
    public ActionBeanContext getContext() {
        return context;
    }

    @Override
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }
    
    public Deelgebied getDeelgebied() {
        return deelgebied;
    }

    public void setDeelgebied(Deelgebied deelgebied) {
        this.deelgebied = deelgebied;
    }

    public String getGeom() {
        return geom;
    }

    public void setGeom(String geom) {
        this.geom = geom;
    }
    
    public Gebruiker getGebruiker() {
        Gebruiker principal = (Gebruiker) context.getRequest().getUserPrincipal();
        Gebruiker g = Stripersist.getEntityManager().find(Gebruiker.class, principal.getId());
        return g;
    }
        public Deelgebied getFilter() {
        return filter;
    }

    public void setFilter(Deelgebied filter) {
        this.filter = filter;
    }
        
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
    
    // </editor-fold>
}
