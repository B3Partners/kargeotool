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
import javax.persistence.*;

/**
 * Klasse voor beschrijving van de rechten van gebruikers inzake DataOwners
 * 
 * @author Chris
 */
@Entity
@Table(name = "gebruiker_data_owner_rights")
public class GebruikerDataOwnerRights implements Serializable {
    @Id
    @ManyToOne
    @JoinColumn(name = "gebruiker")
    private Gebruiker gebruiker;
    
    @Id
    @ManyToOne
    @JoinColumn(name = "data_owner")
    private DataOwner dataOwner;
    
    private boolean editable;
    private boolean readable;
    
    //<editor-fold defaultstate="collapsed" desc="getters en setters">
    /**
     *
     * @return gebruiker
     */
    public Gebruiker getGebruiker() {
        return gebruiker;
    }
    
    /**
     *
     * @param gebruiker gebruiker
     */
    public void setGebruiker(Gebruiker gebruiker) {
        this.gebruiker = gebruiker;
    }
    
    /**
     *
     * @return dataOwner
     */
    public DataOwner getDataOwner() {
        return dataOwner;
    }
    
    /**
     *
     * @param dataOwner dataOwner
     */
    public void setDataOwner(DataOwner dataOwner) {
        this.dataOwner = dataOwner;
    }
    
    /**
     *
     * @return mag er geedit worden?
     */
    public boolean isEditable() {
        return editable;
    }
    
    /**
     *
     * @param editable editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }
    
    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }
    //</editor-fold>

}
