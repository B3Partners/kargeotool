/**
* Geo-OV - applicatie voor het registreren van KAR meldpunten
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
 * Forms om KAR objecten te editen in een popup window met ExtJS form.
 */

Ext.define("EditForms", {
    
    editor: null,
    
    rseqEditWindow: null,
    pointEditWindow: null,
    activationPointEditWindow: null,
    editCoords:null,
    
    constructor: function(editor) {
        this.editor = editor;
    },
    
    /**
     * Opent een popup waarmee de gebruiker een rseq kan wijzigen
     * @param newRseq (optioneel) de rseq die de init waardes invult in het formulier
     * als de newRseq niet wordt meegegeven wordt het geselecteerde object gebruikt 
     * @param okHanlder de functie die wordt aangeroepen nadat er op 'ok' is geklikt
     * De functie krijg als parameter de gewijzigde rseq mee.
     */
    editRseq: function(newRseq, okHandler) {
        var rseq = newRseq || editor.selectedObject;
        
        if(this.rseqEditWindow != null) {
            this.rseqEditWindow.destroy();
            this.rseqEditWindow = null;
        }   
        
        var type = {
            "": "nieuw verkeerssysteem",
            "CROSSING": "VRI",
            "GUARD": "bewakingssysteem nadering voertuig",
            "BAR": "afsluittingssysteem"
        };
        var me = this;
        var theType = rseq.type == "" ? "CROSSING" : rseq.type; // default voor nieuw
        me.rseqEditWindow = Ext.create('Ext.window.Window', {
            title: 'Bewerken ' + type[rseq.type] + (rseq.karAddress == null ? "" : " met KAR adres " + rseq.karAddress),
            height: 330,
            width: 450,
            modal: true,
            icon: rseq.type == "" ? karTheme.crossing : karTheme[rseq.type.toLowerCase()],
            layout: 'fit',
            items: {  
                xtype: 'form',
                bodyStyle: 'padding: 5px 5px 0',
                fieldDefaults: {
                    msgTarget: 'side',
                    labelWidth: 150
                },
                defaultType: 'textfield',
                defaults: {
                    anchor: '100%'
                },
                items: [{
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Soort verkeerssysteem',
                    defaultType: 'radiofield',
                    layout: 'vbox',               
                    items: [{
                        name: 'type',
                        inputValue: 'CROSSING',
                        checked: theType == 'CROSSING',
                        boxLabel: type['CROSSING']
                    },{
                        name: 'type',
                        inputValue: 'GUARD',
                        checked: theType == 'GUARD',
                        boxLabel: type['GUARD']
                    },{
                        name: 'type',
                        inputValue: 'BAR',
                        checked: theType == 'BAR',
                        boxLabel: type['BAR']
                    }]
                },{
                    xtype: 'combo',
                    fieldLabel: 'Beheerder',
                    name: 'dataOwner',
                    allowBlank: false,
                    blankText: 'Selecteer een optie',
                    displayField: 'omschrijving',
                    queryMode:'local',
                    typeAhead:true,
                    minChars:2,
                    valueField: 'code',
                    value: rseq.dataOwner,
                    store: Ext.create('Ext.data.Store', {
                        fields: ['code', 'classificatie', 'companyNumber', 'omschrijving'],
                        data: dataOwners
                    }),
                    listeners: {
                      buffer: 50,
                      change: function() {
                        var store = this.store;
                        //store.suspendEvents();
                        store.clearFilter();
                        //store.resumeEvents();
                        store.filter({
                            property: 'omschrijving',
                            anyMatch: true,
                            value   : this.getValue()
                        });
                      }
                    }
                },{
                    fieldLabel: 'Beheerdersaanduiding',
                    name: 'crossingCode',
                    value: rseq.crossingCode
                },{
                    xtype: 'numberfield',
                    fieldLabel: 'KAR adres',
                    name: 'karAddress',
                    allowBlank: false,
                    minValue: 0,
                    value: rseq.karAddress,
                    listeners: {
                        change: function(field, value) {
                            value = parseInt(value, 10);
                            field.setValue(value);
                        }
                    }                    
                },{
                    fieldLabel: 'Plaats',
                    name: 'town',
                    value: rseq.town
                },{
                    fieldLabel: 'Locatie',
                    name: 'description',
                    value: rseq.description
                },{
                    xtype: 'datefield',
                    format: 'Y-m-d',
                    fieldLabel: 'Geldig vanaf',
                    name: 'validFrom',
                    value: rseq.validFrom
                },{
                    xtype: 'datefield',
                    format: 'Y-m-d',
                    fieldLabel: 'Geldig tot',
                    name: 'validUntil',
                    value: rseq.validUntil
                }],
                buttons: [{
                    text: 'OK',
                    handler: function() {
                        var form = this.up('form').getForm();
                        if(!form.isValid()) {
                            Ext.Msg.alert('Ongeldige gegevens', 'Controleer aub de geldigheid van de ingevulde gegevens.')
                            return;
                        }

                        Ext.Object.merge(rseq, form.getValues());
                        if(rseq == me.editor.activeRseq) {
                            me.editor.fireEvent("activeRseqUpdated", rseq);
                        }
                        me.rseqEditWindow.destroy();
                        me.rseqEditWindow = null;
                        
                        if(okHandler) {
                            okHandler(rseq);
                        }
                    }
                },{
                    text: 'Annuleren',
                    handler: function() {
                        me.rseqEditWindow.destroy();
                        me.rseqEditWindow = null;
                    }
                }]
            }
        }).show();
    },
    
    /**
     * Opent een popup waarmee een non activation punt kan worden bewerkt.
     */
    editNonActivationPoint: function(newPoint, okHandler, cancelHandler) {
        var rseq = this.editor.activeRseq;
        var point = newPoint || this.editor.selectedObject;
        
        if(this.pointEditWindow != null) {
            this.pointEditWindow.destroy();
            this.pointEditWindow = null;
        }   
        
        var me = this;
        var label = point.getLabel() == null ? "" : point.getLabel();
        me.pointEditWindow = Ext.create('Ext.window.Window', {
            title: 'Bewerken ' + (point.getType() == null ? "ongebruikt punt " : (point.getType() == "BEGIN" ? "beginpunt " : "eindpunt ")) + label,
            height: 96,
            width: 250,
            modal: true,
            icon: point.getType() == null ? karTheme.punt : (point.getType() == 'BEGIN' ? karTheme.beginPunt :karTheme.eindPunt),
            layout: 'fit',
            items: {  
                xtype: 'form',
                bodyStyle: 'padding: 5px 5px 0',
                fieldDefaults: {
                    msgTarget: 'side',
                    labelWidth: 150
                },
                defaultType: 'textfield',
                defaults: {
                    anchor: '100%'
                },
                items: [{
                    fieldLabel: 'Label',
                    name: 'label',
                    value: point.label,
                    id: 'labelEdit'
                }],
                buttons: [{
                    text: 'OK',
                    handler: function() {
                        var form = this.up('form').getForm();
                        if(!form.isValid()) {
                            Ext.Msg.alert('Ongeldige gegevens', 'Controleer aub de geldigheid van de ingevulde gegevens.')
                            return;
                        }
                        
                        Ext.Object.merge(point, form.getValues());
                        if(rseq == me.editor.activeRseq) {
                            me.editor.fireEvent("activeRseqUpdated", rseq);
                        }
                        me.pointEditWindow.destroy();
                        me.pointEditWindow = null;
                        
                        if(okHandler) {
                            okHandler(point);
                        }                        
                    }
                },{
                    text: 'Annuleren',
                    handler: function() {
                        me.pointEditWindow.destroy();
                        me.pointEditWindow = null;
                        
                        if(cancelHandler) {
                            cancelHandler();
                        }                        
                    }
                }]
            }
        }).show();
        Ext.getCmp("labelEdit").selectText(0);
        Ext.getCmp("labelEdit").focus(false, 100);
    },
    
    /**
     * Opent een window waarmee een activation punt kan worden gewijzgd.
     */
    editActivationPoint: function(newUitmeldpunt, newMap, okHandler, cancelHandler) {
        var rseq = this.editor.activeRseq;
        var point = newUitmeldpunt || this.editor.selectedObject;
        
        if(this.activationPointEditWindow != null) {
            this.activationPointEditWindow.destroy();
            this.activationPointEditWindow = null;
        }   
        
        var me = this;
        var label = point.getLabel() == null ? "" : point.getLabel();
        var apType = point.getType().split("_")[1];
        var apName = (apType == "1" ? "inmeld" : (apType == "2" ? "uitmeld" : "voorinmeld")) + "Punt";
        
        var map = newMap;
        var movements = null;
        if(!map) {
            movements = rseq.findMovementsForPoint(point);

            if(movements.length == 0) {
                alert("Kan geen movements vinden voor activation point!");
                return;
            }
            map = movements[0].map;
        }
        
        var editingUitmeldpunt = map.commandType == 2;
        
        var signalItems = [{
            xtype: 'numberfield',
            fieldLabel: 'Afstand tot stopstreep',
            name: 'distanceTillStopLine',
            value: map.distanceTillStopLine
        },{
            xtype: 'combo',
            fieldLabel: 'Triggertype',
            name: 'triggerType',
            allowBlank: false,
            blankText: 'Selecteer een optie',
            editable: false,
            displayField: 'desc',
            valueField: 'value',
            value: map.triggerType,
            store: Ext.create('Ext.data.Store', {
                fields: ['value', 'desc'],
                data: [
                    {value: 'STANDARD', desc: 'Standaard: door vervoerder bepaald'},
                    {value: 'FORCED', desc: 'Automatisch: altijd melding'},
                    {value: 'MANUAL', desc: 'Handmatig: handmatig door chauffeur'}
                ]
            })   
        }];
        if(editingUitmeldpunt) {
            var ov = [];
            var hulpdienst = [];
            var data = {root:{
                text: 'Alle',
                id: 'root',
                iconCls: "noTreeIcon",
                expanded: true,
                checked: false,
                children :[ 
                    {
                        id: 'ov-node', 
                        text: 'OV', 
                        checked: false, 
                        iconCls: "noTreeIcon",
                        expanded:false,
                        leaf: false,
                        children: ov
                    }, 
                    {
                        id: 'hulpdienst-node', 
                        text: 'Hulpdiensten', 
                        checked: false, 
                        iconCls: "noTreeIcon",
                        expanded:false,
                        leaf: false,
                        children: hulpdienst
                    }
                ]
            }};
            var selectedVehicleTypes = [];
            Ext.Array.each(vehicleTypes, function(vt) {
                var selected = map.vehicleTypes.indexOf(vt.nummer) != -1;
                if(selected) {
                    selectedVehicleTypes.push(vt.nummer);
                }
                if(selected || vt.omschrijving.indexOf('Gereserveerd') == -1) {
                    var leaf = {
                            id: vt.nummer,
                            text: vt.omschrijving,
                            checked: false, 
                            iconCls: "noTreeIcon",
                            leaf: true};
                    if(vt.groep == "OV"){
                        ov.push(leaf);
                    }else{
                        hulpdienst.push(leaf);
                    }
                }
            });
            var vehicleTypesStore = Ext.create('Ext.data.TreeStore', data);

            signalItems = Ext.Array.merge(signalItems, [{
                xtype: 'numberfield',
                minValue: 0,
                listeners: {
                    change: function(field, value) {
                        value = parseInt(value, 10);
                        field.setValue(value);
                    }
                },
                fieldLabel: 'Signaalgroep',
                allowBlank: false,
                name: 'signalGroupNumber',
                value: map.signalGroupNumber
            },{
                xtype: 'numberfield',
                minValue: 0,
                listeners: {
                    change: function(field, value) {
                        value = parseInt(value, 10);
                        field.setValue(value);
                    }
                },                
                fieldLabel: 'Virtual local loop number',
                name: 'virtualLocalLoopNumber'
            },{
                xtype: 'treecombo',
                valueField: 'id',
                editable:false,
                value:  selectedVehicleTypes.join(","),
                fieldLabel: 'Voertuigtypes',
                treeWidth:290,
                treeHeight: 300,
                name: 'vehicleTypes',   
                store: vehicleTypesStore
            }]);
        }
                
        this.activationPointEditWindow = Ext.create('Ext.window.Window', {
            title: 'Bewerken ' + apName.toLowerCase() + " " + label,
            height: map.commandType == 2 ? 284 : 203,
            width: 490,
            modal: true,
            icon: karTheme[apName],
            layout: 'fit',
            items: {  
                xtype: 'form',
                bodyStyle: 'padding: 5px 5px 0',
                fieldDefaults: {
                    msgTarget: 'side',
                    labelWidth: 150
                },
                defaultType: 'textfield',
                defaults: {
                    anchor: '100%'
                },
                items: [{
                    xtype:'fieldset',
                    title: 'KAR signaal',
                    collapsible: false,
                    defaultType: 'textfield',
                    layout: 'anchor',
                    defaults: {
                        anchor: '100%'
                    },
                    items: signalItems
                },{
                    fieldLabel: 'Label',
                    name: 'label',
                    value: point.label,
                    maxLength: 4,
                    maxLengthText: "Maximale lengte is 4 karakters",
                    id: 'labelEdit'
                }],
                buttons: [{
                    text: 'OK',
                    handler: function() {
                        var form = this.up('form').getForm();
                        if(!form.isValid()) {
                            Ext.Msg.alert('Ongeldige gegevens', 'Controleer aub de geldigheid van de ingevulde gegevens.')
                            return;
                        }
                        
                        // merge label naar point
                        var formValues = form.getValues();
                        Ext.Object.merge(point, objectSubset(formValues, ["label"]));

                        // deze waardes naar alle maps van movements die dit
                        // pointId gebruiken
                        var pointSignalValues = objectSubset(formValues, ["distanceTillStopLine", "triggerType"]);
                        
                        // Alleen bij uitmeldpunt kunnen deze worden ingesteld
                        // maar moeten voor alle movements worden toegepast op
                        // alle signals
                        if(editingUitmeldpunt) {
                            allSignalValues = objectSubset(formValues, ["signalGroupNumber", "virtualLocalLoopNumber", "vehicleTypes"]);
                        }
                        
                        // nieuw punt, alleen naar map mergen
                        if(!movements) {
                            Ext.Object.merge(map, pointSignalValues);
                            if(editingUitmeldpunt) {
                                Ext.Object.merge(map, allSignalValues);
                            }
                        } else {
                            
                            // merge naar alle movements 
                            
                            Ext.Array.each(movements, function(mvmtAndMap) {
                                //console.log("movement nummer " + mvmtAndMap.movement.nummer);
                                if(mvmtAndMap.map) {
                                    //console.log("merging distanceTillStopLine and triggerType values to movement nummer " + mvmtAndMap.movement.nummer + " map pointId " + mvmtAndMap.map.pointId);
                                    Ext.Object.merge(mvmtAndMap.map, pointSignalValues);
                                }
                                if(editingUitmeldpunt) {
                                    // merge allSignalValues naar alle MovementActivationPoints
                                    // van movements die dit pointId gebruiken
                                    Ext.each(mvmtAndMap.movement.maps, function(theMap) {
                                        if(theMap.beginEndOrActivation == "ACTIVATION") {
                                            //console.log("merging signalGroupNumber, virtualLocalLoopNumber and vehicleType values point signal values to movement nummer " + mvmtAndMap.movement.nummer + " map pointId " + theMap.pointId);
                                            Ext.Object.merge(theMap, allSignalValues);
                                        }
                                    });
                                }
                            });
                        }
                        
                        if(rseq == me.editor.activeRseq) {
                            me.editor.fireEvent("activeRseqUpdated", rseq);
                        }
                        me.activationPointEditWindow.destroy();
                        me.activationPointEditWindow = null;
                        
                        if(okHandler) {
                            okHandler(point);
                        }
                    }
                },{
                    text: 'Annuleren',
                    handler: function() {
                        me.activationPointEditWindow.destroy();
                        me.activationPointEditWindow = null;
                        
                        if(cancelHandler) {
                            cancelHandler();
                        }
                    }
                }]
            }
        }).show();
        Ext.getCmp("labelEdit").selectText(0);
        Ext.getCmp("labelEdit").focus(false, 100);
    },
    
    editCoordinates : function (pointObject,okHandler,cancelHandler){
        var me = this;
        var coords = "";
        if(pointObject instanceof RSEQ){
            coords = pointObject.getLocation().coordinates;
        }else{
            coords = pointObject.getGeometry().coordinates;
        }
        var rdX = coords[0];
        var rdY = coords[1];
        var point = new Proj4js.Point(rdX, rdY);
        
        var wgs = new Proj4js.Proj("EPSG:4236");
        var rd = new Proj4js.Proj("EPSG:28992");
        Proj4js.transform(rd, wgs, point);            
        var wgsX = point.x;
        var wgsY = point.y;
        this.editCoords = Ext.create('Ext.window.Window', {
            title: 'Voer coördinaten',
            height: 240,
            width: 400,
            modal: true,
            icon: karTheme.gps,
            layout: 'fit',
            items: {  
                xtype: 'form',
                bodyStyle: 'padding: 5px 5px 0',
                fieldDefaults: {
                    msgTarget: 'side',
                    labelWidth: 150
                },
                defaultType: 'textfield',
                defaults: {
                    anchor: '100%'
                },
                items: [
                    {
                        xtype: 'fieldset',
                        defaultType: 'textfield',
                        text: 'Rijksdriehoek',
                        items:[
                            {
                                fieldLabel: 'RD x-coordinaat',
                                name: 'rdX',
                                value: rdX,
                                id: 'rdX'
                            },{
                                fieldLabel: 'RD y-coordinaat',
                                name: 'rdY',
                                value: rdY,
                                id: 'rdY'
                            }]
                    },
                    {
                        xtype: 'fieldset',
                        text: 'GPS',
                        defaultType: 'textfield',
                        items:[
                            {
                                fieldLabel: 'GPS x-coordinaat',
                                name: 'wgsX',
                                value: wgsX,
                                id: 'wgsX'
                            },{
                                fieldLabel: 'GPS y-coordinaat',
                                name: 'wgsY',
                                value: wgsY,
                                id: 'wgsY'
                            }]
                    }
                    ],
                buttons: [{
                    text: 'OK',
                    handler: function() {
                        var form = this.up('form').getForm();
                        if(!form.isValid()) {
                            Ext.Msg.alert('Ongeldige gegevens', 'Controleer aub de geldigheid van de ingevulde gegevens.');
                            return;
                        }
                       
                        var formValues= form.getValues();
                        if(rdX == formValues.rdX && rdY == formValues.rdY){
                            if(wgsX != formValues.wgsX && wgsY == formValues.wgsY){
                                // converteer nieuwe wgs naar rd en sla ze op
                                var point = new Proj4js.Point(formValues.wgsX, formValues.wgsY);
                                Proj4js.transform(wgs, rd, point);
                                rdX = point.x;
                                rdY = point.y;
                            }
                        }else{
                            rdX = formValues.rdX;
                            rdY = formValues.rdY;
                        }
                        
                        var coordObj = {coordinates:[parseFloat(rdX),parseFloat(rdY)], type: 'Point'};
                        if(pointObject instanceof RSEQ){
                            pointObject.setLocation(coordObj);
                        }else{
                            pointObject.setGeometry(coordObj);
                        }
                        
                        me.editor.fireEvent("activeRseqUpdated", me.editor.activeRseq);
                        me.editCoords.destroy();
                        me.editCoords = null;
                        
                        if(okHandler) {
                            okHandler(point);
                        }                        
                    }
                },{
                    text: 'Annuleren',
                    handler: function() {
                        me.editCoords.destroy();
                        me.editCoords = null;
                        
                        if(cancelHandler) {
                            cancelHandler();
                        }                        
                    }
                }]
            }
        }).show();
    }
    
    
});