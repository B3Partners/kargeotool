/**
 * KAR Geo Tool - applicatie voor het registreren van KAR meldpunten
 *
 * Copyright (C) 2009-2014 B3Partners B.V.
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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Meine Toonen
 */
@Entity
@Table(name="inform_message")
public class InformMessage {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional=false)
    @JoinColumn(name="vervoerder")
    private Gebruiker vervoerder;

    @ManyToOne(optional=false)
    @JoinColumn(name="afzender")
    private Gebruiker afzender;

    @ManyToOne(optional=false)
    @JoinColumn(name="rseq")
    private RoadsideEquipment rseq;

    @Column(name="mail_sent")
    private boolean mailSent = false;

    @Column(name="mail_processed")
    private boolean mailProcessed = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="created_at")
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="sent_at")
    private Date sentAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="processed_at")
    private Date processedAt;


    // <editor-fold defaultstate="collapsed" desc="Getters and setters">
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Gebruiker getVervoerder() {
        return vervoerder;
    }

    public void setVervoerder(Gebruiker vervoerder) {
        this.vervoerder = vervoerder;
    }

    public Gebruiker getAfzender() {
        return afzender;
    }

    public void setAfzender(Gebruiker afzender) {
        this.afzender = afzender;
    }

    public RoadsideEquipment getRseq() {
        return rseq;
    }

    public void setRseq(RoadsideEquipment rseq) {
        this.rseq = rseq;
    }

    public boolean isMailSent() {
        return mailSent;
    }

    public void setMailSent(boolean mailSent) {
        this.mailSent = mailSent;
    }

    public boolean isMailProcessed() {
        return mailProcessed;
    }

    public void setMailProcessed(boolean mailProcessed) {
        this.mailProcessed = mailProcessed;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    // </editor-fold>

}
