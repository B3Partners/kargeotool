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

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
@Entity
public class GebruikerVRIRights  implements Serializable{
    
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name="gebruiker")
    private Gebruiker gebruiker;
    
    @ManyToOne
    @JoinColumn(name="roadside_equipment")
    private RoadsideEquipment roadsideEquipment;
    
    private boolean readable;
    
    private boolean editable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Gebruiker getGebruiker() {
        return gebruiker;
    }

    public void setGebruiker(Gebruiker gebruiker) {
        this.gebruiker = gebruiker;
    }

    public RoadsideEquipment getRoadsideEquipment() {
        return roadsideEquipment;
    }

    public void setRoadsideEquipment(RoadsideEquipment roadsideEquipment) {
        this.roadsideEquipment = roadsideEquipment;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
    
    
}
