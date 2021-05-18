/* global style, selectstyle, tempstyle, surroundStyle, Ext, Measure, Proj4js, editor */

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
/**
 * Class dat als 'wrapper' functioneert om eenvoudig OpenLayers aan te sturen.
 **/
Ext.define("ol", {
    mixins: {
        observable: 'Ext.util.Observable'
    },

    editor : null,
    map : null,
    panel : null,
    vectorLayer : null,
    rseqVectorLayer: null,
    snapLayer:null,
    surroundingPointsLayer:null,
    otherVehicleTypeLayer:null,
    snap:null,

    geojson_format : null,
    gfi : null,
    dragFeature : null,
    point : null,
    line : null,

    /**
     * Het OpenLayers.Control dat geactiveerd wordt om een locatie aan te klikken
     * waar Street View geopend moet worden.
     */
    streetViewClickControl: null,

    identifyButton : null,
    overview : null,
    activeFeature : null,
    selectCtrl : null,
    highlight:null,
    measureTool:null,
    standaloneMeasure:null,

    markerLayer:null,

    cacheControl:null,
    constructor : function(editor){
        this.mixins.observable.constructor.call(this);
        this.editor = editor;

        //this.addEvents('measureChanged');
        this.editor.on('activeRseqUpdated', this.updateVectorLayer, this);
        this.editor.on('vehicleTypeChanged', this.updateVectorLayer, this);
        this.editor.on('selectedObjectChanged', this.toggleDragfeature, this);
        this.editor.on('otherVehicleTypeChanged', this.updateOtherVehicleType, this);
    },
    /**
     *Maak een map
     */
    createMap : function(domId){
        var maxBounds = new OpenLayers.Bounds(12000,304000,280000,620000);
        this.panel = new OpenLayers.Control.Panel({
            allowDepress:true
        });
        //opties voor openlayers map.
        var opt = {
            projection: new OpenLayers.Projection("EPSG:28992"),
            maxExtent: maxBounds,
            srs: 'epsg:28992',
            allOverlays: true,
            resolutions: [3440.64,1720.32,860.16,430.08,215.04,107.52,53.76,26.88,13.44,6.72,3.36,1.68,0.84,0.42,0.21,0.105,0.0525],
            theme: OpenLayers._getScriptLocation()+'theme/b3p/style.css',
            units : 'm',
            controls : [this.panel]
        };
        //maak openlayers map
        this.map = new OpenLayers.Map(domId,opt);
        //maak vector layers.
        this.vectorLayer = new OpenLayers.Layer.Vector("Points", {
            styleMap: new OpenLayers.StyleMap( {
                "default": style,
                "select": selectstyle,
                "temporary" : tempstyle
            })
        }
        );
        var clusterStrategy = new OpenLayers.Strategy.ResolutionCluster({
            maxResolution: 0.06,
            threshold:2
          });
        this.rseqVectorLayer = new OpenLayers.Layer.Vector("RseqSelect", {
            strategies: [clusterStrategy],
            styleMap: new OpenLayers.StyleMap( {
                "default": style,
                "select": selectstyle,
                "temporary" : tempstyle
            })
        }
        );
        this.snapLayer = new OpenLayers.Layer.Vector("SnapLayer", {
            styleMap: new OpenLayers.StyleMap( {
                "default": snap,
                "select": snap,
                "temporary" : snap
            })
        }
        );
        this.surroundingPointsLayer = new OpenLayers.Layer.Vector("surroundingPointsLayer",{
            styleMap: new OpenLayers.StyleMap( {
                "default": surroundStyle
            })
        });
        this.otherVehicleTypeLayer = new OpenLayers.Layer.Vector("otherVehicleTypeLayer",{
            styleMap: new OpenLayers.StyleMap( {
                "default": otherVehicleStyle
            })
        });

        this.markerLayer = new OpenLayers.Layer.Markers( "Markers" );
        this.map.addLayer(this.markerLayer);

        this.geojson_format = new OpenLayers.Format.GeoJSON();
        this.map.addLayer(this.vectorLayer);
        this.map.addLayer(this.rseqVectorLayer);
        this.map.addLayer(this.snapLayer);
        this.map.addLayer(this.surroundingPointsLayer);
        this.map.addLayer(this.otherVehicleTypeLayer);
        this.createControls(domId);

        OpenLayers.IMAGE_RELOAD_ATTEMPTS = 2;
    },
    /**
     * Private method which adds all the controls
     */
    createControls : function (domId){
        var nav = new OpenLayers.Control.Navigation();
        this.map.addControl(nav);

        this.map.addControl( new OpenLayers.Control.MousePosition({
            numDigits: 2
        }));
        this.map.addControl(new OpenLayers.Control.PanZoomBar());
        var me = this;
        var options = new Object();
        options["persist"]=true;
        options["callbacks"]={
            modify: function (evt){
                if (evt.parent){
                    var bestLengthTokens=this.getBestLength(evt.parent);
                    me.editor.olc.fireEvent('measureChanged', bestLengthTokens[0].toFixed(0), bestLengthTokens[1]);
                }
            }
        };
        options["handlerOptions"]={
            style :{
                strokeColor : "",
                strokeOpacity: 0
            }
        };
        options["displaySystemUnits"] = new Object();
        options["displaySystemUnits"].metric=["m"];
        //voeg meet tool toe
        this.measureTool= new OpenLayers.Control.Measure( OpenLayers.Handler.Path, options);

        this.measureTool.events.register('measure',this.measureTool,function(){
            var measureValueDiv=document.getElementById("olControlMeasureValue");
            if (measureValueDiv){
                measureValueDiv.style.display="none";
            }
            this.cancel();
        });
        this.measureTool.events.register('deactivate',this.measureTool,function(){
            var measureValueDiv=document.getElementById("olControlMeasureValue");
            if (measureValueDiv){
                measureValueDiv.style.display="none";
            }
        });

        this.map.addControl(this.measureTool);

        this.standaloneMeasure = Ext.create(Measure,{
            map: this.map,
            panel: this.panel
        });
        this.standaloneMeasure.on('measurechanged',function(){
            this.editor.changeCurrentEditAction("MEASURE_STANDALONE");
        }, this);


        //voeg 'teken punt' tool toe.
        this.point =  new OpenLayers.Control.DrawFeature(this.vectorLayer, OpenLayers.Handler.Point, {
            displayClass: 'olControlDrawFeaturePoint',
            featureAdded: function(feature ) {
                me.drawFeature(feature);
            }
        });
        //voeg 'teken line' tool toe.
        this.line = new OpenLayers.Control.DrawFeature(this.vectorLayer, OpenLayers.Handler.Path, {
            displayClass: 'olControlDrawFeaturePath',
            callbacks:{
                modify : function(evt){
                    me.featureModified(evt);
                }
            }
        });
        this.line.events.register('featureadded', me, me.drawFeature);
        //voeg 'versleep feature' tool toe.
        this.dragFeature= new OpenLayers.Control.DragFeature(this.vectorLayer,{
            onComplete : me.dragComplete,
            featureCallbacks:{
                over: function(feature){
                    if(editor.selectedObject && feature.data.id == editor.selectedObject.getId() && editor.selectedObject.$className == feature.data.className){
                        this.overFeature(feature);
                    }
                }
            }
        });

        this.dragFeature.handlers['drag'].stopDown = false;
        this.dragFeature.handlers['drag'].stopUp = false;
        this.dragFeature.handlers['drag'].stopClick = false;
        this.dragFeature.handlers['feature'].stopDown = false;
        this.dragFeature.handlers['feature'].stopUp = false;
        this.dragFeature.handlers['feature'].stopClick = false;

        // streetViewClickControl

        var StreetViewClick = OpenLayers.Class(OpenLayers.Control, {
            button: null,
            defaultHandlerOptions: {
                'single': true,
                'double': false,
                'pixelTolerance': 0,
                'stopSingle': false,
                'stopDouble': false
            },

            initialize: function(options) {
                this.handlerOptions = OpenLayers.Util.extend({}, this.defaultHandlerOptions);
                OpenLayers.Control.prototype.initialize.apply(this, arguments);
                this.handler = new OpenLayers.Handler.Click(
                    this, {
                        'click': this.clicked
                    }, this.handlerOptions
                    );
                this.button = new OpenLayers.Control( {
                    displayClass: "olControlStreetView",
                    type: OpenLayers.Control.TYPE_TOOL,
                    title: "StreetView (klik op kaart voor positie, opent nieuw venster)"
                });
                this.button.events.register("activate",this,function(){
                    this.activate();
                });
                this.button.events.register("deactivate",this,function(){
                    this.deactivate();
                });
                options.panel.addControls(this.button);
            },
            clicked: function(e) {
                var lonlat = this.map.getLonLatFromPixel(e.xy);

                this.deactivate();
                this.button.deactivate();

                var dest = new Proj4js.Proj("EPSG:4236");
                var source = new Proj4js.Proj("EPSG:28992");
                var point = new Proj4js.Point(lonlat.lon, lonlat.lat);
                Proj4js.transform(source, dest, point);

                window.open("http://maps.google.nl/maps?q=" + point.y + "," + point.x + "&z=16&layer=c&cbll=" + point.y + "," + point.x + "&cbp=12,0,,0,0", "_blank");
            }
        });
        this.streetViewClickControl = new StreetViewClick({
            panel: this.panel,
            displayClass: 'olStreetViewClick'
        });
        this.map.addControl(this.streetViewClickControl);

        this.map.addControl(this.point);
        this.map.addControl(this.line);
        this.map.addControl(this.dragFeature);
        //maak en voeg achtergrond kaartlaag toe.
        const ovmLayer = new OpenLayers.Layer.WMTS({
            name: 'BRTOverviewLayer',
            url: 'https://geodata.nationaalgeoregister.nl/tiles/service/wmts/',
            matrixSet: 'EPSG:28992',
            layer: 'brtachtergrondkaart',
            format: 'image/png',
            style: 'default',
            serverResolutions: [3440.64,1720.32,860.16,430.08,215.04,107.52,53.76],
            maxExtent: new OpenLayers.Bounds(-285401.920, 22598.080, 595401.920, 903401.920),
            projection: new OpenLayers.Projection("EPSG:28992"),
        });
        var maxBounds = new OpenLayers.Bounds(12000,304000,280000,620000);
        //Maak een overview kaart.
        this.overview = new OpenLayers.Control.OverviewMap({
            layers: [ovmLayer],
            mapOptions: {
                projection: new OpenLayers.Projection("EPSG:28992"),
                maxExtent: maxBounds,
                srs: 'epsg:28992',
                resolutions: [3440.64,1720.32,860.16,430.08,215.04,107.52,53.76,26.88],
                theme: OpenLayers._getScriptLocation()+'theme/b3p/style.css',
                units : 'm'
            }
        });
        this.map.addControl(this.overview);
        //defineer de click afhandeling.
        var oClick = new OpenLayers.Control.Click({
            rightclick: function (evt){
                var f = editor.olc.getFeatureFromEvent(evt);
                var x = evt.clientX;
                var y = evt.clientY;
                if(f && f.cluster ){

                }else if(f && f.layer.name === "RseqSelect"){
                    editor.loadRseqInfo({
                        rseq: f.data.id
                    },function(){
                        editor.contextMenu.show(x,y);
                    });
                }else if(f && f.layer.name === "Points"){
                    editor.contextMenu.show(x,y);
                }else{
                    editor.contextMenu.show(x,y,true);
                }
                return false;
            },
            click: function (evt){
                editor.setSelectedObject(null);
                editor.olc.selectCtrl.unselectAll();
                editor.contextMenu.deactivateContextMenu();
            },
            includeXY:true
        });

        this.map.addControl(oClick);
        oClick.activate();

        this.highlight = new OpenLayers.Control.SelectFeature([this.vectorLayer, this.rseqVectorLayer], {
            highlightOnly: true,
            renderIntent: "temporary",
            hover:true
        });

        this.map.addControl(this.highlight);
        this.highlight.activate();
        this.selectCtrl = new OpenLayers.Control.SelectFeature([this.vectorLayer, this.rseqVectorLayer],{
            clickout: true,
            onSelect : function (feature){
                if(feature && feature.cluster && feature.layer.name === "RseqSelect"){
                    var cluster = feature.cluster;
                    var bounds = new OpenLayers.Bounds();
                    for(var i = 0 ; i< cluster.length; i++){
                        var point = cluster[i];
                        bounds.extend(point.geometry);
                    }
                    editor.olc.map.zoomToExtent(bounds);
                }else if(feature && feature.layer.name === "RseqSelect"){
                    editor.loadRseqInfo({
                        rseq: feature.data.id
                    });
                }else{
                    editor.setSelectedObject(feature);
                }
            },
            onUnselect : function(feature){
                if(feature && feature.layer.name === "Points"){
                    editor.setSelectedObject(null);
                }
            }
        });

        this.map.addControl(this.selectCtrl);
        this.selectCtrl.activate();

        this.snap = new OpenLayers.Control.Snapping({
            layer: this.vectorLayer,
            targets: [{
                layer: this.snapLayer,
                tolerance: 30
            }],
            greedy: true
        });
        this.map.addControl(this.snap);

        this.cacheControl = Ext.create("nl.b3p.kar.Cache",{
            olc:this,
            maxResolution: 50
        });
    },
    /**
     * Update de vector layer met de roadside equipment in de editor
     */
    updateVectorLayer : function(){
        if(!this.editor.activeRseq) {
            return;
        }
        this.removeAllFeatures();
        var activeRseq = this.editor.activeRseq;
        this.addFeatures(activeRseq);
        var selected = this.editor.selectedObject;
        if(selected){
            this.selectFeature(selected.getId(), selected.$className);
        }
        this.updateOtherVehicleType();
    },
    
    updateOtherVehicleType: function(){
        var vehicleType = this.editor.getOppositeVehicleType();
        this.otherVehicleTypeLayer.removeAllFeatures();
        
        var other= Ext.getCmp("showOtherVehicleType").getValue();
        var rseq = this.editor.activeRseq;
        if(other && rseq){
            var points = rseq.toGeoJSON(false,true,vehicleType);
            this.otherVehicleTypeLayer.addFeatures(this.geojson_format.read(points));
        }
    },
    /**
     * Selecteert een feature.
     * @param id id van de gewenste feature
     * @param className van de gewenste feature.
     */
    selectFeature : function(id,className){
        var olFeature = null;
        if(className==="RSEQ"){
            olFeature = this.vectorLayer.getFeaturesByAttribute("className",className)[0];
        }else{
            // Haal alle features op voor het id: dit kunnen punten en een rseq zijn
            var all =this.vectorLayer.getFeaturesByAttribute("id",id);
            for(var i = 0 ; i < all.length ;i++){
                var f = all[i];
                if(f.data.className === "Point"){
                    // Eerste zal altijd de goede zijn vanwege serial id in db
                    olFeature = f;
                    break;
                }
            }
        }

        if(olFeature && (this.vectorLayer.selectedFeatures.length===0||this.vectorLayer.selectedFeatures[0].data.id !== id)){
            this.selectCtrl.unselectAll();
            this.selectCtrl.select(olFeature);
        }
    },
    /**
     * Toggle(aan/uit) de sleep feature functionaliteit/tool
     */
    toggleDragfeature : function (feature){
        if(feature){
            this.dragFeature.activate();
        }else{
            this.dragFeature.deactivate();
        }
    },
    /**
     * Event dat aangeroepen wordt zodra er featureinfo is gevonden (Bijvoorbeeld doormiddel van een klik)
     * @param evt het event eigenschappen
     */
    raiseOnDataEvent : function(evt){
        var stub = new Object();
        var walapparatuur = new Array();
        walapparatuur[0] = {
            id: "424"
        };

        stub.walapparatuur = walapparatuur;
        onIdentifyData("map_kar_layer",stub);
        this.identifyButton.deactivate();
    },
    /**
     * Add a layer. Assumed is that everything is in epsg:28992, units in meters and the maxextent is The Netherlands
     * @param type The type of the layer [WMS/TMS]
     * @param name The name of the layer
     * @param url The url to the service
     * @param layers The layers of the service which must be retrieved
     * @param visible Indicates whether or not the layer must be visible from start
     * @param extension Optional parameter to indicate the extension (type)
     */
    addLayer : function (type,name, url, layers,visible,extension,opacity,maxResolution,maxExtent,noSingleTile){
        var layer;
        if(type === 'WMS'){
            layer = new OpenLayers.Layer.WMS(name,url,{
                'layers':layers,
                'transparent': true
            },{
                singleTile: !noSingleTile,
                ratio: 1,
                isBaseLayer: false,
                transitionEffect: 'resize',
                opacity: opacity,
                maxResolution: maxResolution,
                maxExtent: maxExtent
            });
        }else if (type === "TMS" ){
            if(!extension){
                extension = 'png';
            }
            layer = new OpenLayers.Layer.TMS(name, url,{
                layername:layers,
                type: extension,
                isBaseLayer:false,
                serverResolutions: [3440.64,1720.32,860.16,430.08,215.04,107.52,53.76,26.88,13.44,6.72,3.36,1.68,0.84,0.42,0.21],
                tileOrigin:new OpenLayers.LonLat(-285401.920000,22598.080000),
                opacity: opacity
            });
        } else if (type === 'WMTS') {
            layer = new OpenLayers.Layer.WMTS({
                name,
                url,
                matrixSet: layers.split('/')[0],
                layer: layers.split('/')[1],
                format: extension,
                style: "default",
                serverResolutions: [3440.64,1720.32,860.16,430.08,215.04,107.52,53.76,26.88,13.44,6.72,3.36,1.68,0.84,0.42,0.21],
                maxExtent: new OpenLayers.Bounds(-285401.920, 22598.080, 595401.920, 903401.920),
                opacity: opacity
            });
        }else{
        //console.log("Type " + type + " not known.");
        }
        if(layer){
            layer.setVisibility(visible);
            this.map.addLayer(layer);
            this.map.setLayerIndex(this.snapLayer, this.map.getLayerIndex(layer)+1);
            this.map.setLayerIndex(this.vectorLayer, this.map.getLayerIndex(layer)+2);
            this.map.setLayerIndex(this.rseqVectorLayer, this.map.getLayerIndex(layer)+3);
            this.map.setLayerIndex(this.surroundingPointsLayer, this.map.getLayerIndex(layer)+4);
            this.map.setLayerIndex(this.otherVehicleTypeLayer, this.map.getLayerIndex(layer)+5);
            this.map.setLayerIndex(this.markerLayer, this.map.getLayerIndex(layer)+6);
        }
    },
    /**
     * Kijk of een layer zichtbaar is\
     * @param name de naam van de layer.
     * @return true/false zichtbaarheid van de layer
     */
    isLayerVisible : function (name){
        var lyrs = this.map.getLayersByName(name);
        if(lyrs && lyrs.length > 0){
            return lyrs[0].visibility;
        }
        return false;
    },
    /**
     *Verander de zichtbaarheid van een layer
     *@param name de naam van de gewenste layer
     *@param vis true/false zichtbaar/niet zichtbaar
     */
    setLayerVisible : function (name,vis){
        var lyrs = this.map.getLayersByName(name);
        if(lyrs && lyrs.length > 0){
            var layer = lyrs[0];
            layer.setVisibility(vis);
        }
    },
    /**
     * Zoom het kaart beeld naar de gegeven extent
     * @param minx minimale x
     * @param miny minimale y
     * @param maxx maximale x
     * @param maxy maximale y
     */
    zoomToExtent : function (minx,miny,maxx,maxy){
        this.map.zoomToExtent([minx,miny,maxx,maxy]);
    },
    /**
     * Update het kaartbeeld door alle layers opnieuw te tekenen/op te halen
     */
    update : function (){
        for ( var i = 0 ; i< this.map.layers.length ;i++ ){
            var layer = this.map.layers[i];
            layer.redraw(true);
        }
    },
    /**
     * Voeg nieuw sld toe aan de kaarten.
     * @param publiclinenumber nummer van de buslijnen die gefilterd moeten worden
     * @param name De naam van de layer
     */
    addFilterToKargis : function (publiclinenumber, name){
        var buslijnen = this.map.getLayersByName(name)[0];
        buslijnen.maxResolution =4000;
        buslijnen.mergeNewParams({
            cql_filter : "linepublicnumber = " + publiclinenumber
        });
    },
    /**
     * Haal de sld's van de layers 'buslijnen'
     * @param layer De laag waarvan het SLD gehaald moet worden
     */
    removeFilterFromKargis : function (layer){
        var buslijnen = this.map.getLayersByName(layer)[0];

        buslijnen.maxResolution =13;
        buslijnen.mergeNewParams({
            cql_filter:null
        });

    },
    //All the vectorlayer functions
    /**
     * Teken het punt meegegeven punt
     * @param wkt het punt als WKT (Well Known Text). Als deze parameter niet wordt
     * meegegeven dan wordt het tekenen gestart en kan de gebruiker zelf tekenen
     */
    drawPoint : function(wkt){
        if(wkt){
            var olFeature = new OpenLayers.Geometry.Point(wkt[0],wkt[1]);
            geometryDrawUpdate(olFeature.toString());
            this.point.drawFeature(olFeature);
        }else{
            this.point.activate();
        }
        this.dragFeature.activate();
    },
    /**
     * Teken de meegegeven lijn
     * @param wkt de lijn als WKT (Well Known Text). Als deze parameter niet wordt
     * meegegeven dan wordt het tekenen gestart en kan de gebruiker zelf tekenen
     */
    drawLine : function(wkt){
        if(wkt){
            var olFeature = new OpenLayers.Geometry.fromWKT(wkt);
            this.line.drawFeature(olFeature);
        }else{
            this.line.activate();
        }
        this.dragFeature.activate();
    },
    /**
     * Teken een lijn vanaf een bepaald punt
     * @param x de x coordinaat waar begonnen moet worden
     * @param y de y coordinaat waar begonnen moet worden
     */
    drawLineFromPoint : function (x,y){
        var lonlat = new OpenLayers.LonLat (x,y);
        var pixel = this.map.getPixelFromLonLat(lonlat);
        this.measureTool.activate();
        this.line.activate();
        this.line.handler.createFeature(pixel);
        this.line.handler.insertXY(x,y);
        this.measureTool.handler.createFeature(pixel);
        this.measureTool.handler.insertXY(x,y);
    },
    featureModified : function (evt){
        var geom = null;
        if (evt.parent){
            geom = evt.parent;
        }
        if(evt.feature){
            geom = evt.feature.geometry;
        }
        if(geom){
            var bestlengthtokens = this.measureTool.getBestLength(geom);
            this.getMeasureTooltip().showMouseTooltip(evt, bestlengthtokens[0].toFixed(0), bestlengthtokens[1]);
        }
    },
    getMeasureTooltip: function() {
        if(!this.measureTooltip) {
            this.measureTooltip = Ext.create('MeasureTooltip', this.measureTool);
        }
        return this.measureTooltip;
    },
    /**
     * Verwijder alle features die getekend zijn op de vectorlayer     *
     */
    removeAllFeatures : function(){
        this.vectorLayer.removeAllFeatures();
        this.dragFeature.deactivate();
    },
    /**
     * Verwijder alle features die getekend zijn als road side equipment.
     */
    removeAllRseqs: function(){
        this.rseqVectorLayer.removeAllFeatures();
    },
    /**
     * Event dat aangeroepen wordt als de gebruiker klaar is met het verslepen
     * van een feature
     * @param feature de feature die verplaatst is.
     */
    dragComplete : function (feature){
        var x = feature.geometry.x;
        var y = feature.geometry.y;
        editor.changeGeom(feature.data.className, feature.data.id, x,y);
    },
    /**
     * Teken een feature
     * @param object.feature de feature die getekend moet worden
     */
    drawFeature : function (object){
        var feature =object.feature;
        var lastPoint = feature.geometry.components[feature.geometry.components.length-1];
        this.point.deactivate();
        this.line.deactivate();
        this.measureTool.deactivate();
        this.editor.pointFinished(lastPoint);
        this.highlight.activate();
        this.measureTooltip.hideMouseTooltip();
    // TODO fire event geometry updated
    },
    /**
     * Activeer een feature op de kaart.
     * @param feature de feature die geactiveerd moet worden
     */
    setActiveFeature : function (feature){
        this.activeFeature = feature;
    },
    /**
     * Voeg 1 of meer features toe. Dit is een GeoJSON representatie van een RSEQ, bestaande dus uit een VRI, en 0 of meerdere activationpoints.
     * @param rseq een object met feature(s) in GeoJSON formaat
     */
    addFeatures : function(rseq){
        var filtered = rseq.toGeoJSON(false, false,this.editor.getCurrentVehicleType());
        
        this.vectorLayer.addFeatures(this.geojson_format.read(filtered));
    },
    /**
     * Voeg 1 of meer road side equipment features toe
     * @param rseqs rseqs die toegevoegd moeten worden in GeoJSON formaat.
     */
    addRseqs : function(rseqs){
        this.cacheControl.initTree(rseqs);
    },

    addRseqGeoJson : function(rseqs){
        this.rseqVectorLayer.addFeatures(this.geojson_format.read(rseqs));
    },
    /**
     * Haal de feature op uit de twee bestaande vectorlagen.
     * @param e Het event om de feature uit te halen
     */
    getFeatureFromEvent : function (e){
        var f = editor.olc.vectorLayer.getFeatureFromEvent(e);
        if(f){
            return f;
        }
        var rseq = editor.olc.rseqVectorLayer.getFeatureFromEvent(e);
        if(rseq){
            return rseq;
        }
        return rseq;
    },
    /**
     * Update de grote van de kaart.
     */
    resizeMap : function(){
        this.map.updateSize();
    },

    addMarker : function(x,y){
        var lonlat = null;
        if(y){
            lonlat = new OpenLayers.LonLat(x, y);
        }else{
            lonlat = x;
        }
        var marker = new OpenLayers.Marker(lonlat);
        this.clearMarkers();
        this.markerLayer.addMarker(marker);
    },
    clearMarkers : function(){
        this.markerLayer.clearMarkers();
    },
    isMeasuring: function(){
        return this.measureTool.active || this.standaloneMeasure.button.active;
    }
});
/**
 * Create a Click controller
 * @param options
 * @param options.handlerOptions options passed to the OpenLayers.Handler.Click
 * @param options.click the function that is called on a single click (optional)
 * @param options.dblclick the function that is called on a dubble click (optional)
 */
OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control,{
    defaultHandlerOptions: {
        'single': true,
        'double': false,
        'stopSingle': false,
        'stopDouble': false
    },
    handleRightClicks:true,
    /**
     * @constructor
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
        if (options.rightclick){
            this.onRightclick=options.rightclick;
        }
        this.handler = new OpenLayers.Handler.Click(
            this, {
                'click': this.onClick,
                'dblclick': this.onDblclick,
                'rightclick' : this.onRightclick
            }, this.handlerOptions
            );
    },
    /**
     * functie dat wordt aangeroepen zodra er wordt geklikt. Functie moet overschreven worden
     * in object dat deze classe implementeerd.
     */
    onClick: function(evt) {
    },
    /**
     * functie dat wordt aangeroepen zodra er dubbel wordt geklikt. Functie moet overschreven worden
     * in object dat deze classe implementeerd.
     */
    onDblclick: function(evt) {
    },
    /**
     * functie dat wordt aangeroepen zodra er met de rechter muis wordt geklikt.
     * Functie moet overschreven worden in object dat deze classe implementeerd.
     */
    onRightclick : function (evt){
    }
});

/* Copyright (c) 2006-2012 by OpenLayers Contributors (see authors.txt for
 * full list of contributors). Published under the 2-clause BSD license.
 * See license.txt in the OpenLayers distribution or repository for the
 * full text of the license. */

/**
 * @requires OpenLayers/Strategy.js
 */

/**
 * Class: OpenLayers.Strategy.Cluster
 * Strategy for vector feature clustering.
 *
 * Inherits from:
 *  - <OpenLayers.Strategy>
 */
OpenLayers.Strategy.ResolutionCluster = OpenLayers.Class(OpenLayers.Strategy, {

    /**
     * APIProperty: distance
     * {Integer} Pixel distance between features that should be considered a
     *     single cluster.  Default is 20 pixels.
     */
    distance: 50,

    /**
     * APIProperty: threshold
     * {Integer} Optional threshold below which original features will be
     *     added to the layer instead of clusters.  For example, a threshold
     *     of 3 would mean that any time there are 2 or fewer features in
     *     a cluster, those features will be added directly to the layer instead
     *     of a cluster representing those features.  Default is null (which is
     *     equivalent to 1 - meaning that clusters may contain just one feature).
     */
    threshold: null,

    /**
     * Property: features
     * {Array(<OpenLayers.Feature.Vector>)} Cached features.
     */
    features: null,

    /**
     * Property: clusters
     * {Array(<OpenLayers.Feature.Vector>)} Calculated clusters.
     */
    clusters: null,

    /**
     * Property: clustering
     * {Boolean} The strategy is currently clustering features.
     */
    clustering: false,

    /**
     * Property: resolution
     * {Float} The resolution (map units per pixel) of the current cluster set.
     */
    resolution: null,

     /**
     * Property: maxResolution
     * {Float} The resolution (map units per pixel) when clustering should be turned off
     */
    maxResolution: null,

    /**
     * Constructor: OpenLayers.Strategy.Cluster
     * Create a new clustering strategy.
     *
     * Parameters:
     * options - {Object} Optional object whose properties will be set on the
     *     instance.
     */

    /**
     * APIMethod: activate
     * Activate the strategy.  Register any listeners, do appropriate setup.
     *
     * Returns:
     * {Boolean} The strategy was successfully activated.
     */
    activate: function() {
        var activated = OpenLayers.Strategy.prototype.activate.call(this);
        if(activated) {
            this.layer.events.on({
                "beforefeaturesadded": this.cacheFeatures,
                "moveend": this.cluster,
                scope: this
            });
        }
        return activated;
    },

    /**
     * APIMethod: deactivate
     * Deactivate the strategy.  Unregister any listeners, do appropriate
     *     tear-down.
     *
     * Returns:
     * {Boolean} The strategy was successfully deactivated.
     */
    deactivate: function() {
        var deactivated = OpenLayers.Strategy.prototype.deactivate.call(this);
        if(deactivated) {
            this.clearCache();
            this.layer.events.un({
                "beforefeaturesadded": this.cacheFeatures,
                "moveend": this.cluster,
                scope: this
            });
        }
        return deactivated;
    },

    /**
     * Method: cacheFeatures
     * Cache features before they are added to the layer.
     *
     * Parameters:
     * event - {Object} The event that this was listening for.  This will come
     *     with a batch of features to be clustered.
     *
     * Returns:
     * {Boolean} False to stop features from being added to the layer.
     */
    cacheFeatures: function(event) {
        var propagate = true;
        if(!this.clustering) {
            this.clearCache();
            this.features = event.features;
            this.cluster();
            propagate = false;
        }
        return propagate;
    },

    /**
     * Method: clearCache
     * Clear out the cached features.
     */
    clearCache: function() {
        this.features = null;
    },

    /**
     * Method: cluster
     * Cluster features based on some threshold distance.
     *
     * Parameters:
     * event - {Object} The event received when cluster is called as a
     *     result of a moveend event.
     */
    cluster: function(event) {
        if((!event || event.zoomChanged) && this.features) {
            var resolution = this.layer.map.getResolution();
            if(resolution !== this.resolution || !this.clustersExist()) {
                this.resolution = resolution;
                var clusters = [];
                var feature, clustered, cluster;
                for(var i=0; i<this.features.length; ++i) {
                    feature = this.features[i];
                    if(feature && feature.geometry) {
                        clustered = false;
                        for(var j=clusters.length-1; j>=0; --j) {
                            cluster = clusters[j];
                            if(this.shouldCluster(cluster, feature)) {
                                this.addToCluster(cluster, feature);
                                clustered = true;
                                break;
                            }
                        }
                        if(!clustered) {
                            clusters.push(this.createCluster(this.features[i]));
                        }
                    }
                }
                this.layer.removeAllFeatures();
                if(clusters.length > 0) {
                    if(this.threshold > 1) {
                        var clone = clusters.slice();
                        clusters = [];
                        var candidate;
                        for(var i=0, len=clone.length; i<len; ++i) {
                            candidate = clone[i];
                            if(candidate.attributes.count < this.threshold) {
                                Array.prototype.push.apply(clusters, candidate.cluster);
                            } else {
                                clusters.push(candidate);
                            }
                        }
                    }
                    this.clustering = true;
                    // A legitimate feature addition could occur during this
                    // addFeatures call.  For clustering to behave well, features
                    // should be removed from a layer before requesting a new batch.
                    this.layer.addFeatures(clusters);
                    this.clustering = false;
                }
                this.clusters = clusters;
            }
        }
    },

    /**
     * Method: clustersExist
     * Determine whether calculated clusters are already on the layer.
     *
     * Returns:
     * {Boolean} The calculated clusters are already on the layer.
     */
    clustersExist: function() {
        var exist = false;
        if(this.clusters && this.clusters.length > 0 &&
           this.clusters.length === this.layer.features.length) {
            exist = true;
            for(var i=0; i<this.clusters.length; ++i) {
                if(this.clusters[i] !== this.layer.features[i]) {
                    exist = false;
                    break;
                }
            }
        }
        return exist;
    },

    /**
     * Method: shouldCluster
     * Determine whether to include a feature in a given cluster.
     *
     * Parameters:
     * cluster - {<OpenLayers.Feature.Vector>} A cluster.
     * feature - {<OpenLayers.Feature.Vector>} A feature.
     *
     * Returns:
     * {Boolean} The feature should be included in the cluster.
     */
    shouldCluster: function(cluster, feature) {
        var currentRes = this.layer.map.getResolution();
        if(this.maxResolution > currentRes){
            return false;
        }
        var cc = cluster.geometry.getBounds().getCenterLonLat();
        var fc = feature.geometry.getBounds().getCenterLonLat();
        var distance = (
            Math.sqrt(
                Math.pow((cc.lon - fc.lon), 2) + Math.pow((cc.lat - fc.lat), 2)
            ) / this.resolution
        );
        return (distance <= this.distance);
    },

    /**
     * Method: addToCluster
     * Add a feature to a cluster.
     *
     * Parameters:
     * @param cluster - {<OpenLayers.Feature.Vector>} A cluster.
     * @param feature - {<OpenLayers.Feature.Vector>} A feature.
     */
    addToCluster: function(cluster, feature) {
       cluster.cluster.push(feature);
        cluster.attributes.count += 1;
    },

    /**
     * Method: createCluster
     * Given a feature, create a cluster.
     *
     * Parameters:
     * feature - {<OpenLayers.Feature.Vector>}
     *
     * Returns:
     * @param  feature {<OpenLayers.Feature.Vector>} A cluster.
     */
    createCluster: function(feature) {
        var center = feature.geometry.getBounds().getCenterLonLat();
        var cluster = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(center.lon, center.lat),
            {count: 1}
        );
        cluster.cluster = [feature];
        return cluster;
    },

    CLASS_NAME: "OpenLayers.Strategy.ResolutionCluster"
});

Ext.define("nl.b3p.kar.Cache", {
    rt:null,
    pause:false,
    config:{
        olc:null,
        maxResolution:null
    },
    constructor: function(config) {
        this.initConfig(config);
        this.rt = RTree();
        this.config.olc.map.events.register ("moveend",this, this.update);
    },
    initTree: function(features) {
        if (this.rt.getTree().nodes.length > 0) {
            // Purge old data from RTree, prevents double points in vectorlayers
            this.rt = RTree();
        }
        this.rt.geoJSON(features);
        this.update();
    },
    insertIntoTree : function (feature){
        var geojson = feature.toGeoJSON(true);
        this.rt.geoJSON(geojson);
    },
    removeFromTree : function (feature){
        var geoJSON = feature.toGeoJSON(true);
        var rect = {
            x: geoJSON.geometry.coordinates[0],
            y: geoJSON.geometry.coordinates[1],
            w: 0,
            h: 0
        };
        var b = this.rt.remove(rect);
        var a = 0;
    },
    update: function(object,bbox) {
        if(!bbox){
            bbox = this.config.olc.map.getExtent().toArray();
        }
        var left = [bbox[0],bbox[1]];
        var right = [bbox[2],bbox[3]];
        var features =  this.rt.bbox(left,right);
        var currentResolution = this.config.olc.map.getResolution();
        this.config.olc.removeAllRseqs();
        if(!this.pause && this.config.maxResolution > currentResolution){
            this.insertIntoVectorlayer(features);
        }
    },
    insertIntoVectorlayer:function(features){
        var featureCollection = {
            type: "FeatureCollection",
            features: features
        };
        this.config.olc.addRseqGeoJson(featureCollection);
    }
});


/**
 * Polyfill for the Array.isArray function
 * todo: Test on IE7 and IE8
 */
Array.isArray || (Array.isArray = function ( a ) {
    return'' + a !== a && {}.toString.call( a ) == '[object Array]'
});
