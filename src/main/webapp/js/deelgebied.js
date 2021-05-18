/* global Ext, mapfilePath, deelgebiedActionBeanUrl */

/**
 * KAR Geo Tool - applicatie voor het registreren van KAR meldpunten
 *
 * Copyright (C) 2018 B3Partners B.V.
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
var draw =null;
var map = null;
var vector = null;
var click = null;


OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control,{
    defaultHandlerOptions: {
        'single': true,
        'double': false,
        'stopSingle': false,
        'stopDouble': false
    },
    /**
     *
     * @param {Object} options having:
     *        options.handlerOptions: options passed to the OpenLayers.Handler.Click,
     *        options.click the function that is called on a single click (optional),
     *        options.dblclick the function that is called on a dubble click (optional)
     * @returns {OpenLayers.Control.Click}
     */
    initialize: function(options) {
        this.handlerOptions = OpenLayers.Util.extend(
            {}, this.defaultHandlerOptions
        );
        Ext.apply(this.handlerOptions,options.handlerOptions);
        OpenLayers.Control.prototype.initialize.apply(
            this, arguments
        );
        if (options.click){
            this.onClick=options.click;
        }
        if (options.dblclick){
            this.onDblclick=options.dblclick;
        }
        this.handler = new OpenLayers.Handler.Click(
            this, {
                'click': this.onClick,
                'dblclick': this.onDblclick
            }, this.handlerOptions
        );
    },
    onClick: function(evt) {
    },
    onDblclick: function(evt) {
    }
});

function loadMap (){
    const brt = new OpenLayers.Layer.WMTS({
        name: 'BRT',
        url: 'https://geodata.nationaalgeoregister.nl/tiles/service/wmts/',
        matrixSet: 'EPSG:28992',
        layer: 'brtachtergrondkaart',
        format: 'image/png',
        style: 'default',
        serverResolutions: [3440.64,1720.32,860.16,430.08,215.04,107.52,53.76,26.88,13.44,6.72,3.36,1.68,0.84,0.42,0.21],
        maxExtent: new OpenLayers.Bounds(-285401.920, 22598.080, 595401.920, 903401.920),
    });

    var gemlayer = new OpenLayers.Layer.WMS("gemeente",mapfilePath,{
        'layers':"gemeentes",
        'transparent': true
    },{
        singleTile: false,
        ratio: 1,
        isBaseLayer: false,
        transitionEffect: 'resize',
        opacity: 0.5/*,
        maxResolution: maxResolution,*/
       , maxExtent : new OpenLayers.Bounds(-285401.920000,22598.080000,595401.920000,903401.920000)
    });

    vector = new OpenLayers.Layer.Vector("deelgebied");
    map = new OpenLayers.Map({
        resolutions : [860.16,430.08,215.04,107.52,53.76,26.88,13.44,6.72,3.36,1.68,0.84,0.42,0.21,0.105,0.0525],
        units : 'm',
        div : "map",
        layers : [
            brt,
            gemlayer,
            vector
        ]
    });
    draw = new OpenLayers.Control.DrawFeature(vector,OpenLayers.Handler.Polygon,{
        displayClass : 'olControlDrawFeaturePoint',
        featureAdded : featureAdded
    });

    map.addControl(draw);
    
    click = new OpenLayers.Control.Click({
        click: mapClick
    });
    map.addControl(click);
    click.activate();
    
    if(!map.getCenter()){
        map.zoomToMaxExtent();
    }
    if(wkt){
        var feat = new OpenLayers.Geometry.fromWKT(wkt);
        draw.drawFeature(feat);
    }
}

var features = {};
function drawArea (){
    draw.activate();
}

function resetArea (){
    vector.removeAllFeatures();
    features = {};
}

function featureAdded (feature){
    var id = feature.id;
    features[id] = feature;
    var col = new OpenLayers.Geometry.Collection();
    for (var property in features) {
        if (features.hasOwnProperty(property)) {
            var g = features[property].geometry;
            col.addComponent(g);
        }
    }

    
    Ext.get("geom").dom.value = col.toString();
    draw.deactivate();
}
var geomtogemid = {};
function mapClick(e){
    var lonlat = map.getLonLatFromViewPortPx(e.xy);
    var y = lonlat.lat;
    var x = lonlat.lon;
     Ext.Ajax.request({
        url : deelgebiedActionBeanUrl,
        method : 'GET',
        timeout: 120000,
        params : {
            x: parseInt(x),
            y: parseInt(y),
            gemeente: true
        },
        success : function (response){
            var msg = Ext.JSON.decode(response.responseText);
            if(msg.success){
                var gems = msg.gemeentes;
                for(var i = 0 ; i < gems.length ;i++){
                    var gem = gems[i];
                    var feat = new OpenLayers.Geometry.fromWKT(gem.geom);
                    var oldFeatureId = geomtogemid[gem.id];
                    if(!oldFeatureId){
                        geomtogemid[gem.id]= feat.id;
                        draw.drawFeature(feat);
                    }else{
                        var features = vector.features;
                        for(var i = 0 ; i < features.length ;i++){
                            var feat = features[i];
                            var gid = feat.geometry.id;
                            if(gid === oldFeatureId){
                                vector.removeFeatures([feat]);
                                delete geomtogemid[gem.id];
                            }
                        }
                    }
                }
            } else{
                
            }
        },
        failure : function (response){
        }
    });
    
}

Ext.onReady(function (){
    loadMap();
});
