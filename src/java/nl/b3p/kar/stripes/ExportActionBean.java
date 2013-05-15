/**
 * Geo-OV - applicatie voor het registreren van KAR meldpunten
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
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.tag.BeanFirstPopulationStrategy;
import net.sourceforge.stripes.validation.OneToManyTypeConverter;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationErrorHandler;
import net.sourceforge.stripes.validation.ValidationErrors;
import nl.b3p.commons.stripes.CustomPopulationStrategy;
import nl.b3p.incaa.IncaaExport;
import nl.b3p.kar.hibernate.DataOwner;
import nl.b3p.kar.hibernate.Deelgebied;
import nl.b3p.kar.hibernate.Gebruiker;
import nl.b3p.kar.hibernate.RoadsideEquipment;
import nl.b3p.kar.jaxb.KarNamespacePrefixMapper;
import nl.b3p.kar.jaxb.TmiPush;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.Type;
import org.hibernatespatial.GeometryUserType;
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
public class ExportActionBean implements ActionBean,ValidationErrorHandler{

    private static final Log log = LogFactory.getLog(ExportActionBean.class);
    private static final String OVERVIEW = "/WEB-INF/jsp/export/overview.jsp";
    private static final String NIEUW_DEELGEBIED = "/WEB-INF/jsp/export/deelgebied.jsp";
    private final int SRID = 28992;
    private ActionBeanContext context;
    
    @Validate(required = true, on = {"exportPtx", "exportXml"})
    private RoadsideEquipment rseq;
    
    
    @Validate(converter = OneToManyTypeConverter.class,on = "export")
    private List<Long> rseqs;
    
    private List<RoadsideEquipment> roadsideEquipmentList;
    
    private List<Deelgebied> deelgebieden = new ArrayList();
    
    @Validate(converter = EntityTypeConverter.class,on ={"saveDeelgebied"} )
    @ValidateNestedProperties({
        @Validate(field = "name")
    })
    private Deelgebied deelgebied;
    
    
    @Validate(converter = EntityTypeConverter.class,on ={"bewerkDeelgebied", "removeDeelgebied", "rseqByDeelgebied"} )
    private Deelgebied filter;
    
    @Validate(on = "saveDeelgebied", required = true)
    private String geom;
    
    @Validate(on = "export")
    private String exportType;

    @DefaultHandler
    public Resolution overview() {
        return new ForwardResolution(OVERVIEW);
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void lists() throws Exception{
        EntityManager em = Stripersist.getEntityManager();
        deelgebieden= em.createQuery("from Deelgebied where gebruiker = :geb order by id").setParameter( "geb", getGebruiker()).getResultList();
    }
    
    public Resolution export() throws Exception {
        EntityManager em = Stripersist.getEntityManager();
        roadsideEquipmentList = new ArrayList();
        for (Long id : rseqs) {
            RoadsideEquipment r = em.find(RoadsideEquipment.class, id);
            
            roadsideEquipmentList.add(r);
        }
        if(exportType == null){
            this.context.getValidationErrors().add("exportType", new SimpleError(("Selecteer een exporttype")));
            return new ForwardResolution(OVERVIEW);
        }else if (exportType.equals("incaa")) {
            return exportPtx();
        } else if (exportType.equals("kv9")) {
            return exportXml();
        } else {
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
        if(rseq != null){
            roadsideEquipmentList = Arrays.asList(new RoadsideEquipment[]{rseq});
        }
        TmiPush push = new TmiPush("B3P", roadsideEquipmentList);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        m.marshal(push, bos);
        
        Date now = new Date();
        DateFormat sdf = new SimpleDateFormat("yyyyMMDDHHmmss");
        String filename = "geo-ov_kv9_" + sdf.format(now);
        return new StreamingResolution("text/xml", new ByteArrayInputStream(bos.toByteArray()))
                .setAttachment(true)
                .setFilename( filename + ".xml")
                .setLength(bos.size());
    }
    
    public Resolution exportPtx() throws Exception {

        IncaaExport exporter = new IncaaExport();
        
        File f = null;
        if(rseq != null){
            f = exporter.convert(rseq);
        }else if( roadsideEquipmentList != null){
            f = exporter.convert(roadsideEquipmentList);
        }
        if(f != null){
            FileInputStream fis = new FileInputStream(f);

            String filename = "HLPXXXXX";
            Date now = new Date();
            DateFormat sdf = new SimpleDateFormat("yyyyMMDDHHmmss");
            filename += sdf.format(now);
            return new StreamingResolution("text/ptx", fis)
                    .setAttachment(true)
                    .setFilename(filename+ ".ptx");
        }else{
            throw new Exception("Could not find roadsideequipments");
        }
    }
    
    @DontBind
    @DontValidate
    public Resolution maakDeelgebied() {
        deelgebied = new Deelgebied();
        return new ForwardResolution(NIEUW_DEELGEBIED);
    }
    
    public Resolution bewerkDeelgebied(){
        deelgebied = filter;
        return new ForwardResolution(NIEUW_DEELGEBIED);
    }
    
    public Resolution removeDeelgebied() throws Exception{
        EntityManager em = Stripersist.getEntityManager();
        deelgebied = filter;
        em.remove(deelgebied);
        em.getTransaction().commit();
        lists();
        return new ForwardResolution(OVERVIEW);
    }
    
    public Resolution saveDeelgebied() throws Exception {
        EntityManager em = Stripersist.getEntityManager();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), SRID);
        WKTReader reader = new WKTReader(gf);
        try {
            Polygon geometrie = (Polygon) reader.read(geom);
            deelgebied.setGeom(geometrie);
            deelgebied.setGebruiker(getGebruiker());
            em.persist(deelgebied);
            em.getTransaction().commit();
            filter = deelgebied;
        } catch (ParseException ex) {
            log.error(ex);
        }
        lists();
        return new ForwardResolution(OVERVIEW);
    }
    
    public Resolution rseqByDeelgebied() throws JSONException{
        EntityManager em = Stripersist.getEntityManager();

        JSONObject info = new JSONObject();
        info.put("success", Boolean.FALSE);
        try {

            Session sess = (Session) em.getDelegate();
            
            Polygon deelgebiedPoly = filter.getGeom();
            
            Query q = sess.createQuery("from RoadsideEquipment where intersects(location, ?) = true");
            Type geometryType = GeometryUserType.TYPE;
            q.setParameter(0, deelgebiedPoly, geometryType);
            List<RoadsideEquipment> rseqs = (List<RoadsideEquipment>)q.list();
            
            Gebruiker g = getGebruiker();
            Set<DataOwner> rights =  g.getEditableDataOwners();
            
            boolean isBeheerder = g.isBeheerder();
            
            JSONArray rseqArray = new JSONArray();
            for (RoadsideEquipment rseqObj : rseqs) {
                if(isBeheerder || rights.contains(rseqObj.getDataOwner())){
                    JSONObject jRseq = new JSONObject();
                    jRseq.put("id", rseqObj.getId());
                    jRseq.put("naam", rseqObj.getDescription());
                    jRseq.put("dataowner", rseqObj.getDataOwner().getOmschrijving());
                    String type = rseqObj.getType();
                    if(type.equalsIgnoreCase("CROSSING")){
                        jRseq.put("type", "VRI");
                    }else if(type.equalsIgnoreCase("BAR")){
                        jRseq.put("type", "Afsluitingssysteem");
                    }else if(type.equalsIgnoreCase("GUARD")){
                        jRseq.put("type", "Waarschuwingssyteem");
                    }
                    rseqArray.put(jRseq);
                }
            }
          
            info.put("rseqs", rseqArray);
            info.put("success", Boolean.TRUE);
        } catch (Exception e) {
            log.error("search rseq exception", e);
            info.put("error", ExceptionUtils.getMessage(e));
        }
        return new StreamingResolution("application/json", new StringReader(info.toString(4)));
    }

    public Resolution handleValidationErrors(ValidationErrors errors) throws Exception {
        int a= 0;
        return null;
    }

    // <editor-fold desc="Getters and Setters">
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
    
    public ActionBeanContext getContext() {
        return context;
    }

    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    public List<Deelgebied> getDeelgebieden() {
        return deelgebieden;
    }

    public void setDeelgebieden(List<Deelgebied> deelgebieden) {
        this.deelgebieden = deelgebieden;
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

  
    public RoadsideEquipment getRseq() {
        return rseq;
    }

    public void setRseq(RoadsideEquipment rseq) {
        this.rseq = rseq;
    }
    // </editor-fold>

}
