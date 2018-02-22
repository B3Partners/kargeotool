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

import java.util.Date;
import javax.persistence.*;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.commons.beanutils.BeanUtils;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * De voor een RoadsideEquipment voor een bepaald service type en command type
 * te versturen attributen in het KAR bericht.
 * 
 * @author Matthijs Laan
 */
@Embeddable
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="KARATTRIBUTESType", propOrder={
    "serviceType",
    "commandType",
    "usedAttributesString"
})
public class KarAttributes {

    /**
     * Waarde voor KAR service type voor openbaar vervoer (public transport).
     */
    public static final String SERVICE_PT = "PT";
    
    /**
     * Waarde voor KAR service type voor hulpdiensten (emergency services).
     */
    public static final String SERVICE_ES = "ES";
    
    /**
     * Waarde voor KAR service type voor andersoortig vervoer, bijvoorbeeld taxi
     * (other transport).
     */
    public static final String SERVICE_OT = "OT";
    
    /**
     * Service type waarvoor de attributen gelden; zie SERVICE_ constanten in
     * deze class.
     */
    @Basic(optional=false)
    @XmlElement(name="karservicetype")
    @Column(name="service_type")
    private String serviceType;
    
    /**
     * Command type waarvoor de attributen gelden; zie COMMAND_ constanten 
     * in ActivationPointSignal.
     */
    @Basic(optional=false)
    @XmlElement(name="karcommandtype")
    @Column(name="command_type")
    private Integer commandType;
    
    public static class KarAttributesTypeAdapter extends XmlAdapter<String, Integer> {
        @Override
        public Integer unmarshal(String s) throws Exception {
            Integer num = 0;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(s.length() -i -1);
                num += c == '1' ? 1 << i : 0;
            }
            return num;
        }

        @Override
        public String marshal(Integer i) throws Exception {
            char[] bitstring = new char[24];
            for(int idx = 0; idx < bitstring.length; idx++) {
                bitstring[bitstring.length-idx-1] = (i.intValue() & (1 << idx)) != 0 ? '1' : '0';
            }
            return new String(bitstring);
        }
    }

    @XmlElement(name="karusedattributes")
    @Transient
    //@XmlJavaTypeAdapter(KarAttributesTypeAdapter.class)
    private String usedAttributesString;
    
    /**
     * Bitmask voor de te vullen attributen in het KAR bericht. LSB duidt 
     * attribuut 1 aan en de MSB (van een 24-bit waarde) attribuut 24. Verplicht
     * in Kv9.
     */    
    @Basic(optional=false)
    @XmlTransient
    @Column(name="used_attributes_mask")
    private Integer usedAttributesMask;
    
    public void beforeMarshal(Marshaller marshaller) throws Exception {
        usedAttributesString = new KarAttributesTypeAdapter().marshal(usedAttributesMask);
    }
    
    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) throws Exception {
        usedAttributesMask = new KarAttributesTypeAdapter().unmarshal(usedAttributesString);
    }
    
    public KarAttributes() {
    }

    public KarAttributes(String serviceType, int commandType, JSONArray ja) throws JSONException {
        this.serviceType = serviceType;
        this.commandType = commandType;
        this.usedAttributesMask = 0;
        for(int i = 0; i < ja.length(); i++) {
            this.usedAttributesMask = this.usedAttributesMask | (ja.getBoolean(i) ? 1 << i : 0);
        }
    }

    public KarAttributes deepCopy()throws Exception{
        KarAttributes copy = (KarAttributes) BeanUtils.cloneBean(this);
        return copy;
    }
    //<editor-fold defaultstate="collapsed" desc="getters en setters">
    /**
     *
     * @return serviceType
     */
    public String getServiceType() {
        return serviceType;
    }
    
    /**
     *
     * @param serviceType serviceType
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
    
    /**
     *
     * @return commandType
     */
    public Integer getCommandType() {
        return commandType;
    }
    
    /**
     *
     * @param commandType commandType
     */
    public void setCommandType(Integer commandType) {
        this.commandType = commandType;
    }
    
    public String getUsedAttributesString() {
        return usedAttributesString;
    }

    public void setUsedAttributesString(String usedAttributesString) {
        this.usedAttributesString = usedAttributesString;
    }

    public Integer getUsedAttributesMask() {
        return usedAttributesMask;
    }

    public void setUsedAttributesMask(Integer usedAttributesMask) {
        this.usedAttributesMask = usedAttributesMask;
    }
    //</editor-fold>
}
