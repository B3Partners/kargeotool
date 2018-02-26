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

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Meine Toonen meinetoonen@b3partners.nl
 */

@Entity
public class Upload {
    
    public Upload(){
        
    }
    /*        featureBuilder.set("data_owner", dataowner.getId());
        featureBuilder.set("user_", gebruiker.getId());
        featureBuilder.set("filename", bestand.getFileName());
        featureBuilder.set("uploaddate", new Date());
        featureBuilder.set("rseq", rseq.getId());
        featureBuilder.set("description", description);
*/
    
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;
    
    private String filename;
    
    private String description;
    
    @ManyToOne(optional=false)
    @JoinColumn(name="rseq")
    private RoadsideEquipment rseq;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date uploaddate;
        
    @ManyToOne(optional=false) 
    @JoinColumn(name="data_owner")
    private DataOwner dataOwner;
   
    @ManyToOne(optional=false)
    @JoinColumn(name="user_")
    private Gebruiker user_;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoadsideEquipment getRseq() {
        return rseq;
    }

    public void setRseq(RoadsideEquipment rseq) {
        this.rseq = rseq;
    }

    public Date getUploaddate() {
        return uploaddate;
    }

    public void setUploaddate(Date uploaddate) {
        this.uploaddate = uploaddate;
    }

    public DataOwner getDataOwner() {
        return dataOwner;
    }

    public void setDataOwner(DataOwner dataOwner) {
        this.dataOwner = dataOwner;
    }

    public Gebruiker getUser_() {
        return user_;
    }

    public void setUser_(Gebruiker user_) {
        this.user_ = user_;
    }
    
}
