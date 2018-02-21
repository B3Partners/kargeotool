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
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import nl.b3p.kar.SecurityRealm;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.json.JSONException;
import org.json.JSONObject;
import org.stripesstuff.stripersist.Stripersist;

/**
 * Klasse voor definitie van een gebruiker
 *
 * @author Chris
 */
@Entity
public class Gebruiker implements Principal {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @Column(unique=true, nullable=false)
    private String username;
    private String passwordsalt;
    private String passwordhash;
    private String fullname;
    private String email;
    private String phone;
    private String position;

    /**
     * JSON string met profielinstellingen.
     */
    @Column(columnDefinition="text")
    private String profile;

    @ManyToMany
    @JoinTable(name = "gebruiker_roles", joinColumns=@JoinColumn(name="gebruiker"), inverseJoinColumns=@JoinColumn(name="role"))
    private Set<Role> roles = new HashSet();

    @OneToMany(mappedBy="gebruiker")
    @MapKeyJoinColumn(name="data_owner")
    @Sort(type=SortType.NATURAL)
    private SortedMap<DataOwner, GebruikerDataOwnerRights> dataOwnerRights = new TreeMap<DataOwner, GebruikerDataOwnerRights>();

    @OneToMany(mappedBy="gebruiker")
    private List<GebruikerVRIRights>  vriRights = new ArrayList();

    /**
     * Verandert het wachtwoord
     *
     * @param request nodig voor bepaling random salt
     * @param pw het wachtwoord
     * @throws NoSuchAlgorithmException The error
     * @throws UnsupportedEncodingException The error
     */
    public void changePassword(HttpServletRequest request, String pw) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String salt = SecurityRealm.generateHexSalt(request);
        String hash = SecurityRealm.getHexSha1(salt, pw);
        setPasswordsalt(salt);
        setPasswordhash(hash);
    }

    /**
     * Zoekt de huidige gebruiker
     *
     * @param request waarin gebruiker is te vinden
     * @return getter de gebruiker
     * @throws Exception The error
     */
    public static Gebruiker getNonTransientPrincipal(HttpServletRequest request) throws Exception {
        Gebruiker g = (Gebruiker)request.getUserPrincipal();
        if(g == null) {
            return null;
        }
        return Stripersist.getEntityManager().find(Gebruiker.class, g.getId());
    }

    /**
     * Haalt id van gebruiker op
     *
     * @return getter id
     */
    public Integer getId() {
        return id;
    }

    /**
     *
     * @param id setter
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Haalt gebruikersnaam op
     *
     * @return getter username
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     * @param username setter
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     *
     * @return getter passwordsalt
     */
    public String getPasswordsalt() {
        return passwordsalt;
    }

    /**
     *
     * @param passwordSalt setter
     */
    public void setPasswordsalt(String passwordSalt) {
        this.passwordsalt = passwordSalt;
    }

    /**
     *
     * @return getter passwordhash
     */
    public String getPasswordhash() {
        return passwordhash;
    }

    /**
     *
     * @param passwordhash setter
     */
    public void setPasswordhash(String passwordhash) {
        this.passwordhash = passwordhash;
    }

    /**
     * Principal implementatie
     */
    public String getName() {
        return getUsername();
    }

    /**
     *
     * @return getter fullname
     */
    public String getFullname() {
        return fullname;
    }

    /**
     *
     * @param fullname setter
     */
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    /**
     *
     * @return getter email
     */
    public String getEmail() {
        return email;
    }

    /**
     *
     * @param email setter
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     *
     * @return getter phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     *
     * @param phone setter
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     *
     * @return getter position
     */
    public String getPosition() {
        return position;
    }

    /**
     *
     * @param position setter
     */
    public void setPosition(String position) {
        this.position = position;
    }

    /**
     *
     * @return getter roles
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     *
     * @param roles setter
     */
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    /**
     *
     * @return getter profile
     */
    public String getProfile() {
        return profile;
    }

    /**
     *
     * @param profile setter
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     *
     * @return getter is gebruiker beheerder?
     */
    public boolean isBeheerder() {
        return isInRole(Role.BEHEERDER);
    }

    /**
     *
     * @return getter
     */
    public boolean isVervoerder(){
        return isInRole(Role.VERVOERDER);
    }

    /**
     *
     * @param roleName roleName
     * @return getter heeft gebruiker de gevraagde rol?
     */
    public boolean isInRole(String roleName) {
        for(Iterator it = getRoles().iterator(); it.hasNext();) {
            Role r = (Role)it.next();
            if(r.getRole().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return getter dataOwnerRights
     */
    public Map<DataOwner, GebruikerDataOwnerRights> getDataOwnerRights() {
        return dataOwnerRights;
    }

    /**
     *
     * @param dataOwnerRights setter
     */
    public void setDataOwnerRights(SortedMap<DataOwner, GebruikerDataOwnerRights> dataOwnerRights) {
        this.dataOwnerRights = dataOwnerRights;
    }

    /**
     *
     * @return getter
     */
    public List<GebruikerVRIRights> getVriRights() {
        return vriRights;
    }

    /**
     *
     * @param vriRights setter
     */
    public void setVriRights(List<GebruikerVRIRights> vriRights) {
        this.vriRights = vriRights;
    }

    /**
     * Lijst van van data owners die gebruiker met gebruiker role mag lezen (bij
     * beheerder/vervoerder worden NIET alle data owners teruggegeven). Een
     * dataOwnerRights record betekent altijd lezen of schrijven, nooit kan
     * editable en readable beide false zijn.
     * @return getter 
     */
    public Set<DataOwner> getReadableDataOwners() {
        return dataOwnerRights.keySet();
    }

    /**
     * Lijst van van data owners die gebruiker met gebruiker role mag bewerken (bij
     * beheerder role worden NIET alle data owners teruggegeven).
     * @return getter 
     */
    public Set<DataOwner> getEditableDataOwners() {
        HashSet<DataOwner> dataOwners = new HashSet<DataOwner>();
        for (Entry<DataOwner, GebruikerDataOwnerRights> entry : dataOwnerRights.entrySet()) {
            if(entry.getValue().isEditable()) {
                dataOwners.add(entry.getValue().getDataOwner());
            }
        }
        return dataOwners;
    }

    /**
     * Gebruiker kan RoadsideEquipment editen als:
     * - Gebruiker beheerder is, of
     * - Gebruiker DataOwner kan editen, of
     * - Gebruiker VRI kan editen.
     *
     * Vervoerders kunnen nooit editen, ook al staan in de database DataOwner/VRI
     * rechten (zou GUI niet mogelijk moeten maken).
     * @param rseq rseq
     * @return getter 
     */
    public boolean canEdit(RoadsideEquipment rseq) {
        if(isBeheerder()) {
            return true;
        }

        if(isVervoerder()) {
            return false;
        }

        DataOwner d = rseq.getDataOwner();
        if(d != null) {
            GebruikerDataOwnerRights r = dataOwnerRights.get(d);
            if(r != null && r.isEditable()) {
                return true;
            }
        }

        Long rseqId = rseq.getId();
        if(rseqId != null) {
            for(GebruikerVRIRights gebruikerVRIRights : vriRights) {
                if(gebruikerVRIRights.isEditable()) {
                    if(gebruikerVRIRights.getRoadsideEquipment().getId().equals(rseqId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Gebruiker kan RoadsideEquipment lezen als:
     * - Gebruiker is beheerder, of
     * - Gebruiker is vervoerder, of
     * - Gebruiker heeft GebruikerDataOwnerRights record voor DAO van RSEQ, of
     * - Gebruiker heeft GebruikerVRIRights record voor RSEQ.
     *
     * Een DataOwner recht of VRI recht bij een gebruiker betekent altijd mimimaal
     * leesrecht. Readable=false en editable=false bij rights records zouden niet
     * in db moeten kunnen staan. Hier wordt gebruik van gemaakt bij zoekacties.
     * @param rseq rseq
     * @return getter 
     */
    public boolean canRead(RoadsideEquipment rseq) {
        if(isBeheerder() || isVervoerder()) {
            return true;
        }

        DataOwner d = rseq.getDataOwner();
        if(d != null) {
            GebruikerDataOwnerRights r = dataOwnerRights.get(d);
            if(r != null) {
                // Geen test op isEditable() of isReadable() nodig, een van beide
                // is voldoende maar zouden nooit met allebei false in database
                // moeten staan
                return true;
            }
        }

        Long rseqId = rseq.getId();
        if(rseqId != null) {
            for(GebruikerVRIRights gebruikerVRIRights: vriRights) {
                // Geen test op isEditable() of isReadable() nodig, een van beide
                // is voldoende maar zouden nooit met allebei false in database
                // moeten staan
                if(gebruikerVRIRights.getRoadsideEquipment().getId().equals(rseqId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Zet de rechten mbt de DataOwner
     * @param dao het dataowner object
     * @param editable mag geedit worden
     * @param readable mag gelezen worden
     * @throws Exception The error
     */
    public void setDataOwnerRight(DataOwner dao, Boolean editable, Boolean readable) throws Exception {
        EntityManager em = Stripersist.getEntityManager();
        GebruikerDataOwnerRights dor = getDataOwnerRights().get(dao);
        if(dor == null) {
            dor = new GebruikerDataOwnerRights();
            dor.setDataOwner(dao);
            dor.setGebruiker(this);
            em.persist(dor);
            getDataOwnerRights().put(dao, dor);
        }
        if(editable != null) {
            dor.setEditable(editable);
        }
        if(readable != null) {
            dor.setReadable(readable);
        }
    }

    /**
     *
     * @return getter
     * @throws JSONException The error
     */
    public JSONObject toJSON() throws JSONException{
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("username", username);
        obj.put("fullname", fullname);
        obj.put("mail", email);
        return obj;
    }

    @Override
    public String toString(){
        return username + " (" +fullname + ")";
    }
}
