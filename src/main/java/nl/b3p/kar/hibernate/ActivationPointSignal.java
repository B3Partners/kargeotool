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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import org.apache.commons.beanutils.BeanUtils;

/**
 * Entity voor een activation point in een beweging welke een KAR-signaal moet
 * sturen uit BISON koppelvlak 9.
 * 
 * @author Matthijs Laan
 */
@Entity
@Table(name="activation_point_signal")
public class ActivationPointSignal {
    /**
     * Waarde voor het KAR commandtype voor een inmeldpunt bij een movement.
     */
    public static final int COMMAND_INMELDPUNT = 1;
    
    /**
     * Waarde voor het KAR commandtype voor een uitmeldpunt bij een movement.
     */
    public static final int COMMAND_UITMELDPUNT = 2;
    
    /**
     * Waarde voor het KAR commandtype voor een voorinmeldpunt bij een movement.
     */
    public static final int COMMAND_VOORINMELDPUNT = 3;
    
    /**
     * Waarde voor het KAR triggertype welke betekent dat de vervoerder welke de
     * lijn exploiteert bepaalt of het bericht direct verstuurd wordt of dat er 
     * een aanvullende conditie geldt.
     */
    public static final String TRIGGER_STANDARD = "STANDARD";
    
    /**
     * Waarde voor het KAR triggertype welke betekent dat er altijd een 
     * automatische inmelding wordt geforceerd ongeacht het soort lijn en de 
     * eventuele aanwezigheid van een halte voor het verkeerssysteem.
     */
    public static final String TRIGGER_FORCED = "FORCED";
    
    /**
     * Waarde voor het KAR triggertype voor "altijd handmatig aanmelden", de
     * chauffeur bepaalt wanneer er aangemeld wordt.
     */
    public static final String TRIGGER_MANUAL = "MANUAL";

    /**
     * Automatisch gegenereerde unieke sleutel volgens een sequence. Niet zichtbaar
     * in Kv9 XML export.
     */        
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Soort melding, zie ActivationPointSignal.COMMAND_ constantes.
     */
    @Column(name="kar_command_type")
    private Integer karCommandType;
    
    /**
     * Voor welke voertuigtypes dit activation point signal geldt; in Kv9 XML
     * is er per vehicle type een ActivationPointSignal XML element. Indien voor
     * bepaalde vehicle types de waardes van triggerType, distanceTillStopLine
     * of signalGroupNumber of virtualLocalLoopNumber anders is dient een nieuwe
     * movement aangemaakt te worden.
     */
    @OneToMany
    @JoinTable(
        name="activation_point_signal_vehicle_types",
        joinColumns = @JoinColumn(name = "activation_point_signal"),
        inverseJoinColumns = @JoinColumn(name = "vehicle_types"))
    private List<VehicleType> vehicleTypes = new ArrayList();
    
    /**
     * Soort aanmelding, zie ActivationPointSignal.TRIGGER_ constantes.
     */
    @Column(name="trigger_type")
    private String triggerType;
    
    /**
     * Afstand tot aan de stopstreep in meters, waarde voorbij stopstreep zijn 
     * negatief.
     */
    @Column(name="distance_till_stop_line")
    private Integer distanceTillStopLine;
    
    /**
     * Signaalgroepnummer. Een signaalgroep kan gelden voor meerdere movements.
     * Een movement kan in een complexe situatie zoals het "Ei van Frans" 
     * (Edisonlaan in Apeldoorn) meerdere uitmeldpunten hebben voor andere 
     * signaalgroepen dan waarvoor werd ingemeld.
     */
    @Column(name="signal_group_number")
    private Integer signalGroupNumber;
    
    /**
     * Het virtuele lusnummer.
     */
    @Column(name="virtual_local_loop_number")
    private Integer virtualLocalLoopNumber;
    
    private String direction;

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
     * @param id setter
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     *
     * @return karCommandType
     */
    public Integer getKarCommandType() {
        return karCommandType;
    }
    
    /**
     *
     * @param karCommandType setter
     */
    public void setKarCommandType(Integer karCommandType) {
        this.karCommandType = karCommandType;
    }
    
    /**
     *
     * @return vehicleTypes
     */
    public List<VehicleType> getVehicleTypes() {
        return vehicleTypes;
    }
    
    /**
     *
     * @param vehicleTypes setter
     */
    public void setVehicleTypes(List<VehicleType> vehicleTypes) {
        this.vehicleTypes = vehicleTypes;
    }
    
    /**
     *
     * @return triggerType
     */
    public String getTriggerType() {
        return triggerType;
    }
    
    /**
     *
     * @param triggerType setter
     */
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }
    
    /**
     *
     * @return distanceTillStopLine
     */
    public Integer getDistanceTillStopLine() {
        return distanceTillStopLine;
    }
    
    /**
     *
     * @param distanceTillStopLine setter
     */
    public void setDistanceTillStopLine(Integer distanceTillStopLine) {
        this.distanceTillStopLine = distanceTillStopLine;
    }
    
    /**
     *
     * @return signalGroupNumber
     */
    public Integer getSignalGroupNumber() {
        return signalGroupNumber;
    }
    
    /**
     *
     * @param signalGroupNumber setter
     */
    public void setSignalGroupNumber(Integer signalGroupNumber) {
        this.signalGroupNumber = signalGroupNumber;
    }
    
    /**
     *
     * @return virtualLocalLoopNumber
     */
    public Integer getVirtualLocalLoopNumber() {
        return virtualLocalLoopNumber;
    }
    
    /**
     *
     * @param virtualLocalLoopNumber setter
     */
    public void setVirtualLocalLoopNumber(Integer virtualLocalLoopNumber) {
        this.virtualLocalLoopNumber = virtualLocalLoopNumber;
    }
    
    /**
     *
     * @return getter
     */
    public String getDirection() {
        return direction;
    }

    /**
     *
     * @param direction setter
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }
    //</editor-fold>

    public ActivationPointSignal deepCopy(EntityManager em) throws Exception{
        ActivationPointSignal copy = (ActivationPointSignal)BeanUtils.cloneBean(this);
        copy.setId(null);
        
        List<VehicleType> newTypes = new ArrayList<>();
        for (VehicleType vehicleType : vehicleTypes) {
            newTypes.add(em.find(VehicleType.class, vehicleType.getNummer()));
        }
        copy.setVehicleTypes(newTypes);
        return copy;
    }
}
