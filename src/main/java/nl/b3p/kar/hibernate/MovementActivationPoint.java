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

import java.util.List;
import java.util.Map;
import javax.persistence.*;
import org.apache.commons.beanutils.BeanUtils;

/**
 * Entity voor de punten van een Movement.
 * 
 * @author Matthijs Laan
 */
@Entity
@Table(name="movement_activation_point")
public class MovementActivationPoint {

    /**
     * Waarde om aan te geven dat dit punt van een movement een beginpunt is. 
     * Een enkel beginpunt is optioneel, maar hoeft te worden vastgelegd indien
     * er geen (voor)inmeldpunten zijn.
     */
    public static final String BEGIN = "BEGIN";
    
    /**
     * Waarde om aan te geven dat dit punt van een movement een eindpunt is.
     * Een enkel eindpunt is verplicht.
     */
    public static final String END = "END";
    
    /**
     * Waarde om aan te geven dat dit punt van een movement een meldpunt is, wat
     * een voorinmeldpunt, inmeldpunt of uitmeldpunt kan zijn.
     * Er kunnen meerdere meldpunten per movement zijn, voor een geldige voorrang
     * is er een inmeldpunt en een uitmeldpunt voor dezelfde signaalgroep.
     */
    public static final String ACTIVATION = "ACTIVATION";
    
    /**
     * Automatisch gegenereerde unieke sleutel volgens een sequence. 
     * Niet zichtbaar in Kv9 XML export.
     */        
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Foreign-key backreference.
     */
    @ManyToOne
    @JoinColumn(name="movement",updatable=false,nullable=false)
    private Movement movement;

    /**
     * Locatie van het punt van dit movement; kan ook een punt zijn dat in een
     * andere movement wordt gebruikt.
     */
    @ManyToOne
    @Basic(optional=false)
    @JoinColumn(name="point",nullable=false)
    private ActivationPoint point;
    
    /**
     * Geeft aan of dit een beginpunt, eindpunt of meldpunt is.
     */
    @Column(name = "begin_end_or_activation")
    private String beginEndOrActivation;
    
    /**
     * Verplicht indien dit punt een meldpunt is, bevat de o.a. signaalgroep.
     */
    @OneToOne(cascade= CascadeType.ALL)
    @JoinColumn(name="signal")
    private ActivationPointSignal signal;


    public String determineVehicleType(){
        String typeMap = null;
        if(this.getSignal() == null){
            return null;
        }
        List<VehicleType> types = this.getSignal().getVehicleTypes();
        for (VehicleType vt : types) {
            if (typeMap == null) {
                typeMap = vt.getGroep();
            } else if (!typeMap.equals(vt.getGroep())) {
                typeMap = VehicleType.VEHICLE_TYPE_GEMIXT;
            }
        }
        return typeMap;
    }
    
    public MovementActivationPoint deepCopy(RoadsideEquipment rseq, Movement m,EntityManager em) throws Exception{
        MovementActivationPoint copy = (MovementActivationPoint)BeanUtils.cloneBean(this);
        copy.setId(null);
        copy.setMovement(m);
        if (signal != null) {
            copy.setSignal(signal.deepCopy(em));
        }
        ActivationPoint ap = point.deepCopy(rseq);
        em.persist(ap);
        copy.setPoint(ap);
        return copy;
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
     * @param id id
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     *
     * @return movement
     */
    public Movement getMovement() {
        return movement;
    }
    
    /**
     *
     * @param movement movement
     */
    public void setMovement(Movement movement) {
        this.movement = movement;
    }
    
    /**
     *
     * @return point
     */
    public ActivationPoint getPoint() {
        return point;
    }
    
    /**
     *
     * @param point point
     */
    public void setPoint(ActivationPoint point) {
        this.point = point;
    }
    
    /**
     *
     * @return beginEndOrActivation
     */
    public String getBeginEndOrActivation() {
        return beginEndOrActivation;
    }
    
    /**
     *
     * @param beginEndOrActivation beginEndOrActivation
     */
    public void setBeginEndOrActivation(String beginEndOrActivation) {
        this.beginEndOrActivation = beginEndOrActivation;
    }
    
    /**
     *
     * @return signal
     */
    public ActivationPointSignal getSignal() {
        return signal;
    }
    
    /**
     *
     * @param signal signal
     */
    public void setSignal(ActivationPointSignal signal) {
        this.signal = signal;
    }
    //</editor-fold>
}
