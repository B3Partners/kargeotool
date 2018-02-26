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

import com.vividsolutions.jts.geom.GeometryCollection;
import javax.persistence.Basic;
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
public class Deelgebied {
    
    public Deelgebied(){
        
    }
    
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;
    
    private String name;
    
    @Basic(optional=false)
    private GeometryCollection geom;
    
    
    @ManyToOne(optional=false) 
    @JoinColumn(name="gebruiker")
    private Gebruiker gebruiker;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gebruiker getGebruiker() {
        return gebruiker;
    }

    public void setGebruiker(Gebruiker gebruiker) {
        this.gebruiker = gebruiker;
    }

    public GeometryCollection getGeom() {
        return geom;
    }

    public void setGeom(GeometryCollection geom) {
        this.geom = geom;
    }
}
