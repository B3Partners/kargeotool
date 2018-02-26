/* global karTheme, editor, Ext */

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
/*
 * Hieronder volgen een aantal styles die worden gebruikt om features op de vectorlayers
 * van OpenLayers te tekenen.
 */

var styleContext = {
    opposite:function (feature){
        var show = false;
        if(feature.renderIntent === "default"){
            if(editor.olc.map.getResolution() < 2){
                show = true;
            }
        } else if(feature.renderIntent === "select"){
            show = true;
        } else{
            // Temporary
            if(editor.olc.vectorLayer.selectedFeatures !== undefined && editor.olc.vectorLayer.selectedFeatures.length > 0 && editor.olc.vectorLayer.selectedFeatures[0].id === feature.id){
                show = true;
            }
        }
        
        if(show){
            return feature.attributes.label + " (" + editor.getOppositeVehicleType() + ")";
        }else{
            return "";
        }
    },
    label:function (feature){
        var show = false;
        if(feature.renderIntent === "default"){
            if(editor.olc.map.getResolution() < 4){
                show = true;
            }
        } else if(feature.renderIntent === "select"){
            show = true;
        } else{
            // Temporary
            if(editor.olc.vectorLayer.selectedFeatures !== undefined && editor.olc.vectorLayer.selectedFeatures.length > 0 && editor.olc.vectorLayer.selectedFeatures[0].id === feature.id){
                show = true;
            }
        }
        
        if(show){
            return feature.attributes.label + " (" + editor.getCurrentVehicleType() + ")";
        }else{
            return "";
        }
    },
    getLabel:function (feature){
        var show = false;
        if(feature.renderIntent == "default"){
            if(editor.olc.map.getResolution() < 1){
                show = true;
            }
        } else if(feature.renderIntent == "select"){
            show = true;
        } else{
            // Temporary
            if(editor.olc.vectorLayer.selectedFeatures != undefined && editor.olc.vectorLayer.selectedFeatures.length > 0 && editor.olc.vectorLayer.selectedFeatures[0].id == feature.id){
                show = true;
            }
        }
        
        if(show){
            return feature.attributes.description;
        }else{
            return "";
        }
    },
    getSVG: function(feature){
      
        var validationerrors = feature.attributes.validationErrors;
        var isReadyToExport = feature.attributes.readyForExport;
        var fillcolor = "51,51,51";
        if(validationerrors > 0){
            fillcolor = "251,51,51";
        }else if(isReadyToExport){
            fillcolor = "51,251,51";            
        }else{
            fillcolor = "251,251,51";            
        }
        
        var memo = feature.attributes.memo;
        var paperclip = '';
        if(memo && memo !== ""){
            paperclip = '<g class="paperclip" transform="matrix(0.0148522,-0.0158228,0.0158228,0.0148522,-1.80218,12.0506)"><path d="M486.4,1024C359.357,1024 256,920.643 256,793.6L256,179.2C256,80.389 336.389,0 435.2,0C534.011,0 614.4,80.389 614.4,179.2L614.4,742.4C614.4,812.979 556.979,870.4 486.4,870.4C415.821,870.4 358.4,812.979 358.4,742.4L358.4,435.2C358.4,421.062 369.862,409.6 384,409.6C398.138,409.6 409.6,421.062 409.6,435.2L409.6,742.4C409.6,784.749 444.053,819.2 486.4,819.2C528.749,819.2 563.2,784.749 563.2,742.4L563.2,179.2C563.2,108.621 505.779,51.2 435.2,51.2C364.621,51.2 307.2,108.621 307.2,179.2L307.2,793.6C307.2,892.411 387.589,972.8 486.4,972.8C585.211,972.8 665.6,892.411 665.6,793.6L665.6,435.2C665.6,421.062 677.061,409.6 691.2,409.6C705.339,409.6 716.8,421.062 716.8,435.2L716.8,793.6C716.8,920.643 613.443,1024 486.4,1024Z" style="fill-rule:nonzero;stroke:black;stroke-width:23.04px;"/></g>';
        }
        
        var stroke = 'stroke-width="1" stroke="black"';
        if(feature.renderIntent === "select"){
            stroke = 'stroke-width="2" stroke="blue"';
        }
        var type = feature.attributes.type;
        var svg;
        if(type === "GUARD"){ //wri
            if(feature.renderIntent === "select"){
                stroke = 'stroke-width="1" stroke="blue"';
            }else{
                stroke = '';
            }
            svg = 'data:image/svg+xml;utf8,<svg viewBox="0 0 40 40" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" xml:space="preserve"><g ' + stroke + '><path d="M34.4,36L4.6,36L20,3L34.4,36ZM30,33L20,10L10,33L30,33Z" style="fill:rgb(' + fillcolor + ');"/><g transform="matrix(1.93626,0,0,1.93626,-21.8404,-13.6321)"><path d="M22.033,16.419L21.828,20.222C21.824,20.351 21.777,20.457 21.688,20.541C21.598,20.625 21.484,20.667 21.348,20.667C21.215,20.667 21.104,20.625 21.014,20.541C20.924,20.457 20.875,20.351 20.867,20.222L20.668,16.419L20.668,16.361C20.668,16.134 20.729,15.955 20.85,15.825C20.971,15.694 21.137,15.628 21.348,15.628C21.563,15.628 21.731,15.694 21.852,15.825C21.973,15.955 22.033,16.134 22.033,16.361L22.033,16.419ZM20.258,22.167C20.258,21.894 20.358,21.671 20.56,21.499C20.761,21.327 21.026,21.242 21.351,21.242C21.679,21.242 21.921,21.327 22.122,21.499C22.323,21.671 22.443,21.894 22.443,22.167C22.443,22.445 22.343,22.67 22.142,22.844C21.94,23.018 21.676,23.105 21.348,23.105C21.023,23.105 20.761,23.017 20.56,22.841C20.358,22.665 20.258,22.441 20.258,22.167Z" style="fill:rgb(51,51,51);fill-rule:nonzero;"/></g></g>' + paperclip +'</svg>';
        }else if (type === "CROSSING"){ // vri
            svg = 'data:image/svg+xml;utf8,<svg viewBox="0 0 40 40" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" xml:space="preserve"><g ' + stroke + ' transform="matrix(1,0,0,1,9.711,1.5)" class="vri"><path d="M15.289,36.5L5.289,36.5C2.529,36.5 0.289,34.196 0.289,31.357L0.289,5.643C0.289,2.835 2.482,0.548 5.289,0.5L15.289,0.5C18.095,0.548 20.289,2.835 20.289,5.643L20.289,31.357C20.289,34.196 18.048,36.5 15.289,36.5ZM10.5,25C8.016,25 6,27.016 6,29.5C6,31.984 8.016,34 10.5,34C12.984,34 15,31.984 15,29.5C15,27.016 12.984,25 10.5,25ZM10.5,14C8.016,14 6,16.016 6,18.5C6,20.984 8.016,23 10.5,23C12.984,23 15,20.984 15,18.5C15,16.016 12.984,14 10.5,14ZM10.5,3C8.016,3 6,5.016 6,7.5C6,9.984 8.016,12 10.5,12C12.984,12 15,9.984 15,7.5C15,5.016 12.984,3 10.5,3Z" style="fill:rgb(' + fillcolor +');fill-rule:nonzero;"/></g>' + paperclip +'</svg>';
        }else if(type === "BAR"){ // bar
            svg = 'data:image/svg+xml;utf8,<svg viewBox="0 0 40 40" version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" xml:space="preserve"><g ' + stroke + ' class="afsluitingssysteem"><rect x="9" y="25" width="18" height="9" style="fill:rgb(' + fillcolor + ');"/><g transform="matrix(0.00123417,2.22222,-0.94712,0.000526008,31.7072,-28.1185)"><path d="M18.51,18.18L18.51,24L14,14.5L18.51,5L18.51,10.82L23,10.82L23,18.18L18.51,18.18Z" style="fill:rgb(' + fillcolor + ');"/></g></g>' + paperclip +'</svg>';
        }
        return svg;
    }
};

// <editor-fold desc="Standaardstyle">
var style = new OpenLayers.Style(
{
    graphicWidth: 28,
    graphicHeight: 28,
    graphicYOffset: -14, // shift graphic up 28 pixels
    labelYOffset: -29,
    labelOutlineColor:"#ffffff",
    labelOutlineWidth:2,
    label: "${label}" // label will be foo attribute value
},
{
    context: styleContext,
    rules: [
    // VRI/GUARD/BAR 
    new OpenLayers.Rule({
        filter: new OpenLayers.Filter.Function({
            evaluate: function (attributes){
                return Ext.Array.contains(['GUARD', 'BAR','CROSSING'], attributes.type);
            }
        }),
        symbolizer: {
            externalGraphic: "${getSVG}",
            graphicYOffset: -40,
            labelYOffset: 0,
            label: "${getLabel}"
        }
    }),
    // Cluster 
    new OpenLayers.Rule({
        filter: new OpenLayers.Filter.Function({
            evaluate: function (attributes){
                return attributes.count ;
            }
        }),
        symbolizer: {
            externalGraphic: karTheme.cluster,
            graphicYOffset: -40,
            label: "${count}",
            labelYOffset: 32,
            labelXOffset: 0,
            fontColor: "#000",
            labelOutlineColor:'#F0F000'
        }
    }),
    new OpenLayers.Rule({
        // a rule contains an optional filter
        elseFilter: true,
        // if a feature matches the above filter, use this symbolizer
        symbolizer: {
            externalGraphic: karTheme.punt,
            label: "",
            strokeColor: "#99BCE8",
            strokeLinecap: "butt",
            strokeDashstyle: "longdash"
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_1"
        }),
        symbolizer: {
            externalGraphic: karTheme.inmeldPunt
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_2"
        }),
        symbolizer: {
            externalGraphic: karTheme.uitmeldPunt
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_3"
        }),
        symbolizer: {
            externalGraphic: karTheme.voorinmeldPunt
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"END"
        }),
        symbolizer: {
            externalGraphic: karTheme.eindPunt,
            graphicYOffset: -25,
            graphicXOffset: -5
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"BEGIN"
        }),
        symbolizer: {
            externalGraphic: karTheme.startPunt,
            graphicYOffset: -25,
            graphicXOffset: -5
        }
    })
    ]
}
);
// </editor-fold>

// <editor-fold desc="Selectstyle">
var selectstyle = new OpenLayers.Style(
// the first argument is a base symbolizer
// all other symbolizers in rules will extend this one
{
    graphicWidth: 37,
    graphicHeight: 39,
    graphicYOffset: -21, 
    labelYOffset: -30,
    labelOutlineColor:"#ffffff",
    labelOutlineWidth:2,
    label: "${label}" 
},
// the second argument will include all rules
{
    context: styleContext,
    rules: [
     new OpenLayers.Rule({
        filter: new OpenLayers.Filter.Function({
            evaluate: function (attributes){
                return Ext.Array.contains(['GUARD', 'BAR','CROSSING'], attributes.type);
            }
        }),
        symbolizer: {
            externalGraphic: "${getSVG}",
            graphicYOffset: -40,
            labelYOffset: 0,
            label: "${getLabel}"
        }
    }),
    
    // Cluster 
    new OpenLayers.Rule({
        filter: new OpenLayers.Filter.Function({
            evaluate: function (attributes){
                return attributes.count ;
            }
        }),
        symbolizer: {
            externalGraphic: karTheme.cluster,
            graphicYOffset: -40,
            label: "${count}",
            labelYOffset: 32,
            labelXOffset: 0,
            fontColor: "#000",
            labelOutlineColor:'#fff'
        }
    }),
    new OpenLayers.Rule({
        // a rule contains an optional filter
        elseFilter: true,
        // if a feature matches the above filter, use this symbolizer
        symbolizer: {
            externalGraphic: karTheme.punt_selected,
            label: "",
            strokeColor: "#99BCE8",
            strokeLinecap: "butt",
            strokeDashstyle: "longdash"
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_1"
        }),
        symbolizer: {
            externalGraphic: karTheme.inmeldPunt_selected
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_2"
        }),
        symbolizer: {
            externalGraphic: karTheme.uitmeldPunt_selected
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_3"
        }),
        symbolizer: {
            externalGraphic: karTheme.voorinmeldPunt_selected
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"END"
        }),
        symbolizer: {
            externalGraphic: karTheme.eindPunt_selected,
            graphicYOffset: -33,
            graphicXOffset: -6
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"BEGIN"
        }),
        symbolizer: {
            externalGraphic: karTheme.startPunt_selected,
            graphicYOffset: -33,
            graphicXOffset: -6
        }
    })
    ]
}
);
// </editor-fold>

// <editor-fold desc="Temporary style">
var tempstyle = new OpenLayers.Style(
// the first argument is a base symbolizer
// all other symbolizers in rules will extend this one
{
    graphicWidth: 37,
    graphicHeight: 39,
    graphicYOffset: -21, 
    labelOutlineColor:"#ffffff",
    labelOutlineWidth:2,
    labelYOffset: -30,
    label: "${label}" 
},
// the second argument will include all rules
{
    context: styleContext,
    rules: [
    
    new OpenLayers.Rule({
        filter: new OpenLayers.Filter.Function({
            evaluate: function (attributes){
                return Ext.Array.contains(['GUARD', 'BAR','CROSSING'], attributes.type);
            }
        }),
        symbolizer: {
            externalGraphic: "${getSVG}",
            graphicYOffset: -40,
            labelYOffset: 0,
            label: "${getLabel}"
        }
    }),
    // Cluster 
    new OpenLayers.Rule({
        filter: new OpenLayers.Filter.Function({
            evaluate: function (attributes){
                return attributes.count ;
            }
        }),
        symbolizer: {
            externalGraphic: karTheme.cluster,
            graphicYOffset: -40,
            label: "${count}",
            labelYOffset: 32,
            labelXOffset: 0,
            fontColor: "#000",
            labelOutlineColor:'#fff'
        }
    }),
    new OpenLayers.Rule({
        elseFilter: true,
        symbolizer: {
            externalGraphic: karTheme.point,
            label: "",
            strokeColor: "#99BCE8",
            strokeLinecap: "butt",
            strokeDashstyle: "longdash"
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_1"
        }),
        symbolizer: {
            externalGraphic: karTheme.signInPoint
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_2"
        }),
        symbolizer: {
            externalGraphic: karTheme.signOutPoint
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_3"
        }),
        symbolizer: {
            externalGraphic: karTheme.preSignInPoint
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"END"
        }),
        symbolizer: {
            externalGraphic: karTheme.endPoint,
            graphicYOffset: -33,
            graphicXOffset: -6
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"BEGIN"
        }),
        symbolizer: {
            externalGraphic: karTheme.startPoint,
            graphicYOffset: -33,
            graphicXOffset: -6
        }
    })
    ]
}
);
// </editor-fold>


// <editor-fold desc="surroundstyle">
var surroundStyle = new OpenLayers.Style(
{
    graphicWidth: 28,
    graphicHeight: 28,
    graphicYOffset: -14, // shift graphic up 28 pixels
    labelYOffset: -15,
    labelOutlineColor:"#ffffff",
    labelOutlineWidth:2,
    graphicOpacity: 0.5
},
{
    context: styleContext,
    rules: [
   
    new OpenLayers.Rule({
        // a rule contains an optional filter
        elseFilter: true,
        // if a feature matches the above filter, use this symbolizer
        symbolizer: {
            externalGraphic: karTheme.punt,
            label: "",
            strokeColor: "#99BCE8",
            strokeLinecap: "butt",
            strokeDashstyle: "longdash"
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_1"
        }),
        symbolizer: {
            externalGraphic: karTheme.inmeldPunt
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_2"
        }),
        symbolizer: {
            externalGraphic: karTheme.uitmeldPunt
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"ACTIVATION_3"
        }),
        symbolizer: {
            externalGraphic: karTheme.voorinmeldPunt
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"END"
        }),
        symbolizer: {
            externalGraphic: karTheme.eindPunt,
            graphicYOffset: -25,
            graphicXOffset: -5
        }
    }),
    new OpenLayers.Rule({   
        filter: new OpenLayers.Filter.Comparison({
            type: OpenLayers.Filter.Comparison.EQUAL_TO,
            property: "type",
            value:"BEGIN"
        }),
        symbolizer: {
            externalGraphic: karTheme.startPunt,
            graphicYOffset: -25,
            graphicXOffset: -5
        }
    })
    ]
}
);
// </editor-fold>

var otherVehicleStyle = surroundStyle.clone();

otherVehicleStyle.setDefaultStyle({
    graphicWidth: 28,
    graphicHeight: 28,
    graphicYOffset: -14,
    labelYOffset: -15,
    graphicOpacity: 0.5,
    fontOpacity: 0.5,
    labelOutlineColor :"#ffffff",
    labelOutlineWidth :2,
    label : "${opposite}"
});

var snap = new OpenLayers.Style(
// the first argument is a base symbolizer
// all other symbolizers in rules will extend this one
{
    strokeLinecap: "butt",
    strokeDashstyle: "dash",
    strokeColor: "#99BCE8",
    stokeOpacity: 1.0,
    strokeWidth: 2
});