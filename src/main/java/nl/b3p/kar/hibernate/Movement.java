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
package nl.b3p.kar.hibernate;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import nl.b3p.kar.jaxb.XmlActivationPoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static nl.b3p.kar.hibernate.MovementActivationPoint.*;
import nl.b3p.kar.jaxb.XmlActivation;
import nl.b3p.kar.jaxb.XmlActivationPointSignal;
import org.apache.commons.beanutils.BeanUtils;
import org.stripesstuff.stripersist.Stripersist;


/**
 * Entity voor persisteren van een Movement zoals bedoeld in BISON koppelvlak 9.
 *
 * @author Matthijs Laan
 */
@Entity
@XmlType(name="MOVEMENTType", 
        propOrder={
            "nummer",
            "begin",
            "activations",
            "end"
        }
)
@XmlAccessorType(XmlAccessType.FIELD)

public class Movement implements Comparable<Movement> {

    /**
     * Automatisch gegenereerde unieke sleutel volgens een sequence. Niet
     * zichtbaar in Kv9 XML export.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @XmlTransient
    private Long id;
    
    /**
     * Roadside equipment waarbij deze movement hoort.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "roadside_equipment")
    @XmlTransient
    private RoadsideEquipment roadsideEquipment;
    
    /**
     * Volgnummer van movement binnen het verkeerssysteem.
     */
    @XmlElement(name="movementnumber")
    private Integer nummer;
    
    /**
     * Geordende lijst met punten (begin, eind en meldpunten) voor deze
     * movement.
     */
    @ManyToMany(cascade = CascadeType.ALL) // Actually @OneToMany, workaround for HHH-1268    
    @JoinTable(name = "movement_points", inverseJoinColumns =
            @JoinColumn(name = "point"), joinColumns = @JoinColumn(name="movement"))
    @OrderColumn(name = "list_index")
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN) // cannot use orphanRemoval=true due to workaround
    @XmlTransient
    private List<MovementActivationPoint> points = new ArrayList();
    
    @Transient
    @XmlElement(name="BEGIN")
    private XmlActivationPoint begin;
    
    @Transient 
    @XmlElement(name="ACTIVATION")
    private List<XmlActivation> activations = new ArrayList();
    
    @Transient
    @XmlElement(name="END")
    private XmlActivationPoint end;
    
    @XmlTransient
    @Column(name="vehicle_type")
    private String vehicleType;
    
    public void beforeMarshal(Marshaller marshaller) {
        activations = new ArrayList();
        
        for(MovementActivationPoint map: points) {
            if(BEGIN.equals(map.getBeginEndOrActivation())) {
                begin = new XmlActivationPoint(map);
            } else if(END.equals(map.getBeginEndOrActivation())) {
                end = new XmlActivationPoint(map);
            } else { // ACTIVATION
                activations.add(new XmlActivation(map));
            }   
        }
    }

    public String determineVehicleType(){
        String typeMovement = null;
        for (MovementActivationPoint map : this.getPoints()) {
            if (map.getSignal() == null) {
                continue;
            }
            
            String tempType = map.determineVehicleType();
            if(typeMovement == null){
                typeMovement = tempType;
            }
            
            if(typeMovement != null && tempType != null && !typeMovement.equals(tempType)){
                typeMovement = VehicleType.VEHICLE_TYPE_GEMIXT;
            }
            
        }
        this.vehicleType = typeMovement;
        return typeMovement;
    }
    
    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        RoadsideEquipment rseq = (RoadsideEquipment)parent;
        this.roadsideEquipment = rseq;
        if(begin != null) {
            MovementActivationPoint map = new MovementActivationPoint();
            map.setMovement(this);
            map.setBeginEndOrActivation(MovementActivationPoint.BEGIN);
            map.setPoint(rseq.getPointByNumber(begin.getActivationpointnumber()));
            points.add(map);
        }
        
        for(XmlActivation xmlActivation: activations) {
            for(XmlActivationPointSignal xmlSignal: xmlActivation.getSignals()) {

                VehicleType vt = xmlSignal.getKarvehicletype() == null ? null : Stripersist.getEntityManager().find(VehicleType.class, xmlSignal.getKarvehicletype());
                
                // Indien zelfde punt nummer als eerdere activation, voeg 
                // vehicleType toe (negeer overige velden)
                MovementActivationPoint same = null;
                for(MovementActivationPoint map: points) {
                    if(map.getPoint() != null && map.getSignal() != null && map.getPoint().getNummer() == xmlSignal.getActivationpointnumber()) {
                        if(vt != null && !map.getSignal().getVehicleTypes().contains(vt)) {
                            map.getSignal().getVehicleTypes().add(vt);
                        }
                        same = map;
                        // eerdere activation met zelfde punt nummer gevonden,
                        // vehicleType toegevoegd - geen nieuwe ActivationPointSignal
                        // maken
                    }
                }
                
                if (same == null){
                    MovementActivationPoint map = new MovementActivationPoint();
                    map.setMovement(this);
                    map.setBeginEndOrActivation(MovementActivationPoint.ACTIVATION);
                    map.setPoint(rseq.getPointByNumber(xmlSignal.getActivationpointnumber()));
                    ActivationPointSignal signal = new ActivationPointSignal();

                    if(vt != null) {
                        signal.getVehicleTypes().add(vt);
                    }
                    signal.setKarCommandType(xmlSignal.getKarcommandtype());
                    signal.setTriggerType(xmlSignal.getTriggertype());
                    signal.setDistanceTillStopLine(xmlSignal.getDistancetillstopline());
                    signal.setSignalGroupNumber(xmlSignal.getSignalgroupnumber());
                    signal.setVirtualLocalLoopNumber(xmlSignal.getVirtuallocalloopnumber());
                    // TODO set direction uit b3pextra

                    map.setSignal(signal);
                    points.add(map);
                }
            }
        }
        
        if(end != null) {
            MovementActivationPoint map = new MovementActivationPoint();
            map.setMovement(this);
            map.setBeginEndOrActivation(MovementActivationPoint.END);
            map.setPoint(rseq.getPointByNumber(end.getActivationpointnumber()));
            points.add(map);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="getters en setters">
    /**
     *
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     *
     * @param  id id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     *
     * @return roadsideEquipment
     */
    public RoadsideEquipment getRoadsideEquipment() {
        return roadsideEquipment;
    }

    /**
     *
     * @param roadsideEquipment roadsideEquipment
     */
    public void setRoadsideEquipment(RoadsideEquipment roadsideEquipment) {
        this.roadsideEquipment = roadsideEquipment;
    }

    /**
     *
     * @return nummer
     */
    public Integer getNummer() {
        return nummer;
    }

    /**
     *
     * @param nummer nummer
     */
    public void setNummer(Integer nummer) {
        this.nummer = nummer;
    }

    /**
     *
     * @return points
     */
    public List<MovementActivationPoint> getPoints() {
        return points;
    }

    /**
     *
     * @param points points
     */
    public void setPoints(List<MovementActivationPoint> points) {
        this.points = points;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
    
    //</editor-fold>

    @Override
    public int compareTo(Movement t) {
        Movement rhs = t;
        if(vehicleType != null && !vehicleType.equals(t.getVehicleType())){
            return -1 * vehicleType.compareTo(t.getVehicleType());
        }
        if(nummer == null) {
            return rhs.nummer == null ? 0 : -1;
        }
        if(rhs.nummer == null) {
            return 1;
        }
        return nummer.compareTo(rhs.nummer);
    }

    public JSONObject getJSON() throws JSONException {
        JSONObject jm = new JSONObject();
        jm.put("id",getId());
        jm.put("nummer", getNummer());
        jm.put("vehicleType", vehicleType != null ? vehicleType : determineVehicleType());

        JSONArray maps = new JSONArray();
        jm.put("maps", maps);
        for (MovementActivationPoint map : getPoints()) {
            JSONObject jmap = new JSONObject();
            maps.put(jmap);
            jmap.put("id", map.getId());
            jmap.put("beginEndOrActivation", map.getBeginEndOrActivation());
            jmap.put("pointId", map.getPoint().getId());
            ActivationPointSignal signal = map.getSignal();
            if (signal != null) {
                jmap.put("distanceTillStopLine", signal.getDistanceTillStopLine());
                jmap.put("commandType", signal.getKarCommandType());
                jmap.put("signalGroupNumber", signal.getSignalGroupNumber());
                jmap.put("virtualLocalLoopNumber", signal.getVirtualLocalLoopNumber());
                jmap.put("triggerType", signal.getTriggerType());
                JSONArray jvt = new JSONArray();
                for (VehicleType vt : signal.getVehicleTypes()) {
                    jvt.put(vt.getNummer());
                }
                jmap.put("vehicleTypes", jvt);

                String direction = signal.getDirection();
                jmap.put("direction", direction);
            }
        }
        return jm;
    }
    
    public Movement deepCopy(RoadsideEquipment rseq, EntityManager em)throws Exception{
        Movement copy = (Movement)BeanUtils.cloneBean(this);
        copy.setId(null);
        copy.setRoadsideEquipment(rseq);
        copy.setPoints(null);
        em.persist(copy);
        
        List<MovementActivationPoint> copiedPoints = new ArrayList<>();
        for (MovementActivationPoint point : points) {
            MovementActivationPoint copyMap = point.deepCopy(rseq, copy, em);
            copiedPoints.add(copyMap);
            em.persist(copyMap);
        }
        copy.setPoints(copiedPoints);
        rseq.getMovements().add(copy);
        copy.setNummer(rseq.getMovements().size()+1);
        em.persist(copy);
        rseq.getMovements().add(copy);
        return copy;
        
    }
}
