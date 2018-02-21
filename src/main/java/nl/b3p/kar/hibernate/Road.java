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

import com.vividsolutions.jts.geom.LineString;
import javax.persistence.*;
import nl.b3p.geojson.GeoJSON;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Klasse voor het beschrijven van Wegen (mainroads en minorroads uit OSM), voor het mogelijk maken van snapping
 *
 * @author Meine Toonen
 */
@Entity
public class Road {

    private Long id;
    @Id
    private Long osm_id;
    private String name;
    private String type;
    private int tunnel;
    private int bridge;
    private int oneway;
    private String ref;
    private int z_order;
   // @org.hibernate.annotations.Type(type = "org.hibernatespatial.GeometryUserType")
    private LineString geometry;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOsm_id() {
        return osm_id;
    }

    public void setOsm_id(Long osm_id) {
        this.osm_id = osm_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTunnel() {
        return tunnel;
    }

    public void setTunnel(int tunnel) {
        this.tunnel = tunnel;
    }

    public int getBridge() {
        return bridge;
    }

    public void setBridge(int bridge) {
        this.bridge = bridge;
    }

    public int getOneway() {
        return oneway;
    }

    public void setOneway(int oneway) {
        this.oneway = oneway;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public int getZ_order() {
        return z_order;
    }

    public void setZ_order(int z_order) {
        this.z_order = z_order;
    }

    public LineString getGeometry() {
        return geometry;
    }

    public void setGeometry(LineString geometry) {
        this.geometry = geometry;
    }

    public JSONObject getGeoJSON() throws JSONException {
        JSONObject gj = new JSONObject();
        gj.put("type", "Feature");
        gj.put("geometry", GeoJSON.toGeoJSON(geometry));
        JSONObject p = new JSONObject();
        p.put("halo", "nee");
        gj.put("properties", p);
        return gj;
    }
}
