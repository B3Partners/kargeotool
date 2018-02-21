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

import javax.persistence.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.stripesstuff.stripersist.Stripersist;

/**
 * Entity voor een data eigenaar uit BISON koppelvlak 9.
 * 
 * @author Matthijs Laan
 */
@Entity
@Table(name = "data_owner")
public class DataOwner extends XmlAdapter<String, DataOwner> implements Comparable {

    /**
     * definitie voor vervoerder
     */
    public static final String CLASSIFICATIE_VERVOERDER = "Vervoerder";
    /**
     * definitie voor integrator
     */
    public static final String CLASSIFICATIE_INTEGRATOR = "Integrator";
    /**
     * definitie voor infra beheerder
     */
    public static final String CLASSIFICATIE_INFRA_BEHEERDER = "Infra Beheerder";

    public static DataOwner findByCode(String code) {
        return (DataOwner)Stripersist.getEntityManager().createQuery("from DataOwner where code = :code").setParameter("code", code).getSingleResult();
    }
    
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;
    
    private String code;
    
    @Column(name = "company_number")
    private Integer companyNumber;
    
    private String omschrijving;
    
    private String classificatie;

    //<editor-fold defaultstate="collapsed" desc="getters en setters">
    /**
     *
     * @return getter
     */
    public String getCode() {
        return code;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    /**
     *
     * @param code code
     */
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     *
     * @return companyNumber
     */
    public Integer getCompanyNumber() {
        return companyNumber;
    }
    
    /**
     *
     * @param companyNumber setter
     */
    public void setCompanyNumber(Integer companyNumber) {
        this.companyNumber = companyNumber;
    }
    
    /**
     *
     * @return omschrijving
     */
    public String getOmschrijving() {
        return omschrijving;
    }
    
    /**
     *
     * @param omschrijving setter
     */
    public void setOmschrijving(String omschrijving) {
        this.omschrijving = omschrijving;
    }
    
    /**
     * 
     * @return classificatie
     */
    public String getClassificatie() {
        return classificatie;
    }
    
    /**
     *
     * @param classificatie setter
     */
    public void setClassificatie(String classificatie) {
        this.classificatie = classificatie;
    }
    //</editor-fold>
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.code != null ? this.code.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataOwner other = (DataOwner) obj;
        if ((this.code == null) ? (other.code != null) : !this.code.equals(other.code)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Object other) {
        return code.compareTo(((DataOwner)other).getCode());
    }

    @Override
    public DataOwner unmarshal(String code) throws Exception {
        DataOwner d = (DataOwner)Stripersist.getEntityManager().createQuery("FROM DataOwner where code = :code",DataOwner.class).setParameter("code", code).getSingleResult();
        if(d == null) {
            d = new DataOwner();
            d.setCode(code);
        }
        return d;
    }

    @Override
    public String marshal(DataOwner dao) throws Exception {
        return dao.getCode();
    }
}
