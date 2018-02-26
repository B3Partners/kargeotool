/* global Ext, dataOwners, profile, karTheme, RSEQ, Proj4js, editor, vehicleTypes, KarAttributesEditWindow, exportActionBeanUrl */

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
 * Forms om KAR objecten te editen in een popup window met ExtJS form.
 */

Ext.define("EditForms", {

    editor: null,

    rseqEditWindow: null,
    karAttributesEditWindow: null,
    pointEditWindow: null,
    activationPointEditWindow: null,
    editCoords:null,
    editDirectionWindow:null,
    carriersWindow:null,
    adminExport:null,

    rseqType: {
        "": "nieuw verkeerssysteem",
        "CROSSING": "VRI",
        "GUARD": "bewakingssysteem nadering voertuig",
        "BAR": "afsluitingssysteem"
    },

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

        if(this.rseqEditWindow !== null) {
            this.rseqEditWindow.destroy();
            this.rseqEditWindow = null;
        }

        var okFunction = function (form) {
            var form = Ext.getCmp ("rseqForm").getForm();

            if(!form.isValid()) {
                Ext.Msg.alert('Ongeldige gegevens', 'Controleer aub de geldigheid van de ingevulde gegevens.');
                return;
            }

            rseq.setConfig(form.getValues());
            if(rseq == me.editor.activeRseq) {
                me.editor.fireEvent("activeRseqUpdated", rseq);
            }
            me.rseqEditWindow.destroy();
            me.rseqEditWindow = null;

            if(okHandler) {
                okHandler(rseq);
            }
        };

        var me = this;
        var theType = rseq.getType() === "" ? "CROSSING" : rseq.getType(); // default voor nieuw
        me.rseqEditWindow = Ext.create('Ext.window.Window', {
            title: 'Bewerken ' + me.rseqType[rseq.getType()] + (rseq.config.karAddress === null ? "" : " met KAR adres " + rseq.config.karAddress),
            height: 440,
            width: 450,
            modal: true,
            icon: rseq.getType() === "" ? karTheme.crossing : karTheme[rseq.getType().toLowerCase()],
            layout: 'fit',
            listeners: {
                afterRender: function(thisForm, options){
                    this.keyNav = Ext.create('Ext.util.KeyNav', this.el, {
                        enter: okFunction,
                        scope: this
                    });
                    var fields = this.query("field");
                    var button = Ext.getCmp("rseqOkButton");
                    var f = function(){
                        button.setDisabled( false );
                    };
                    Ext.each(fields, function(field){
                        field.addListener("change", f);
                    });
                }
            },
            items: {
                id: 'rseqForm',
                disabled: !rseq.getEditable(),
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
                        checked: theType === 'CROSSING',
                        boxLabel: me.rseqType['CROSSING']
                    },{
                        name: 'type',
                        inputValue: 'GUARD',
                        checked: theType === 'GUARD',
                        boxLabel: me.rseqType['GUARD']
                    },{
                        name: 'type',
                        inputValue: 'BAR',
                        checked: theType === 'BAR',
                        boxLabel: me.rseqType['BAR']
                    }]
                },{
                    xtype: 'combo',
                    fieldLabel: 'Beheerder',
                    validator:function(value){
                        var list =this.store.proxy.data;
                        var found = false;
                        for (var i = 0 ; i < list.length ;i++){
                            var entry = list[i];
                            if(entry.omschrijving === value){
                                found = true;
                                break;
                            }
                        }
                        if(found){
                            return true;
                        }else{
                            return "Waarde " + value + " is geen bestaande beheerder.";
                        }
                    },
                    name: 'dataOwner',
                    allowBlank: false,
                    blankText: 'Selecteer een optie',
                    displayField: 'omschrijving',
                    queryMode:'local',
                    typeAhead:true,
                    anyMatch: true,
                    minChars:2,
                    valueField: 'id',
                    value: rseq.getDataOwner(),
                    store: Ext.create('Ext.data.Store', {
                        fields: [ 'id','code', 'classificatie', 'companyNumber', 'omschrijving'],
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
                    allowBlank:false,
                    name: 'crossingCode',
                    value: rseq.getCrossingCode()
                },{
                    xtype: 'numberfield',
                    fieldLabel: 'KAR adres',
                    name: 'karAddress',
                    allowBlank: false,
                    minValue: 0,
                    value: rseq.getKarAddress(),
                    listeners: {
                        change: function(field, value) {
                            value = parseInt(value, 10);
                            field.setValue(value);
                        }
                    }
                },{
                    fieldLabel: 'Plaats',
                    name: 'town',
                    allowBlank: false,
                    value: rseq.getTown()
                },{
                    fieldLabel: 'Locatie',
                    name: 'description',
                    value: rseq.getDescription()
                },{
                    xtype: 'datefield',
                    format: 'Y-m-d',
                    fieldLabel: 'Geldig vanaf',
                    name: 'validFrom',
                    value: rseq.getValidFrom()
                },{
                    xtype: 'datefield',
                    format: 'Y-m-d',
                    fieldLabel: 'Geldig tot',
                    name: 'validUntil',
                    value: rseq.getValidUntil()
                },{
                    xtype: 'checkbox',
                    fieldLabel: 'Gereed voor export',
                    name: 'readyForExport',
                    id: 'readyForExport',
                    inputValue: true,
                    value: rseq.getReadyForExport(),
                    checked: rseq.getReadyForExport(),
                    listeners:{
                        change:{
                            scope: this,
                            fn: function (form, value) {
                                rseq.readyIsSet = value;
                            }
                        }
                    }
                },{
                    xtype: 'panel',
                    border: false,
                    items: [{
                        xtype: 'button',
                        name: 'karAttributes',
                        width: 100,
                        text: 'KAR attributen...',
                        handler: function() {
                            me.editKarAttributes(rseq);
                        }
                    },{
                        xtype: 'button',
                        name: 'messagesOverview',
                        width: 100,
                        disabled: Ext.isString(rseq.getId()),
                        text: 'Export informatie',
                        handler: function() {
                            Ext.create("MessagesOverview").show(rseq.getId());
                        }
                    }]
                }],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {
                            xtype: 'container',
                            style: {
                                fontWeight: 'bold'
                            },
                            flex: 1,
                            html: 'Voertuigtype: ' + this.editor.getCurrentVehicleType()
                        },
                        {
                            text: 'OK',
                            handler: okFunction,
                            id:"rseqOkButton",
                            disabled: true
                        },
                        {
                            text: 'Annuleren',
                            handler: function() {
                                me.rseqEditWindow.destroy();
                                me.rseqEditWindow = null;
                            }
                        }
                    ]
                }]
            }
        }).show();
    },

    /**
     * KAR attributen edit form voor een rseq.
     */
    editKarAttributes: function(rseq) {
        if(this.karAttributesEditWindow !== null) {
            this.karAttributesEditWindow.destroy();
            this.karAttributesEditWindow = null;
        }

        this.karAttributesEditWindow = Ext.create(KarAttributesEditWindow,
            "Bewerken KAR attributen voor " + this.rseqType[rseq.getType()] +
                (rseq.config.karAddress === null ? "" : " met KAR adres " + rseq.config.karAddress),
            "In dit scherm kan worden aangegeven welke KAR attributen in KAR " +
                "berichten die aan dit verkeerssysteem worden verzonden moeten " +
                "worden gevuld. Dit geldt voor alle soorten berichten " +
                "(voorinmeldpunt, inmeldpunt en uitmeldpunt).",
            rseq.config.attributes,
            function(atts) {
                rseq.config.attributes = atts;
            }
        );

        this.karAttributesEditWindow.show();
    },

    /**
     * Opent een popup waarmee een non activation punt kan worden bewerkt.
     */
    editNonActivationPoint: function(newPoint, okHandler, cancelHandler) {
        var rseq = this.editor.activeRseq;
        var point = newPoint || this.editor.selectedObject;

        if(this.pointEditWindow !== null) {
            this.pointEditWindow.destroy();
            this.pointEditWindow = null;
        }
        var okFunction =function() {
            var form = Ext.getCmp("nonActivationForm").getForm();
            if(!form.isValid()) {
                Ext.Msg.alert('Ongeldige gegevens', 'Controleer aub de geldigheid van de ingevulde gegevens.');
                return;
            }

            point.setConfig(form.getValues());
            if(rseq == me.editor.activeRseq) {
                me.editor.fireEvent("activeRseqUpdated", rseq);
            }
            me.pointEditWindow.destroy();
            me.pointEditWindow = null;

            if(okHandler) {
                okHandler(point);
            }
            me.editor.activeMovement = null;
        };
        var me = this;
        var label = point.getLabel() == null ? "" : point.getLabel();
        me.pointEditWindow = Ext.create('Ext.window.Window', {
            title: 'Bewerken ' + (point.getType() === null ? "ongebruikt punt " : (point.getType() === "BEGIN" ? "beginpunt " : "eindpunt ")) + label,
            height: 120,
            width: 360,
            modal: true,
            icon: point.getType() == null ? karTheme.punt : (point.getType() == 'BEGIN' ? karTheme.beginPunt :karTheme.eindPunt),
            layout: 'fit',
            listeners: {
                afterRender: function(thisForm, options){
                    this.keyNav = Ext.create('Ext.util.KeyNav', this.el, {
                        enter: okFunction,
                        scope: this
                    });
                    var fields = this.query("field");
                    var button = Ext.getCmp("nonActivationPointOkButton");
                    var f = function(){
                        button.setDisabled( false );
                    };
                    Ext.each(fields, function(field){
                        field.addListener("change", f);
                    });
                },
                close:function(){
                    editor.activeMovement = null;
                }
            },
            items: {
                xtype: 'form',
                id:'nonActivationForm',
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
                    value: point.config.label,
                    id: 'labelEdit'
                }],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {
                            xtype: 'container',
                            style: {
                                fontWeight: 'bold'
                            },
                            flex: 1,
                            html: 'Voertuigtype: ' + this.editor.getCurrentVehicleType()
                        },
                        {
                            text: 'OK',
                            id: "nonActivationPointOkButton",
                            disabled: true,
                            handler: okFunction
                        },
                        {
                            text: 'Annuleren',
                            handler: function() {
                                me.pointEditWindow.destroy();
                                me.pointEditWindow = null;

                                if(cancelHandler) {
                                    cancelHandler();
                                }
                                me.editor.activeMovement = null;
                            }
                        }
                    ]
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

        if(this.activationPointEditWindow !== null) {
            this.activationPointEditWindow.destroy();
            this.activationPointEditWindow = null;
        }

        var me = this;
        var apType = point.getType().split("_")[1];
        var apName = (apType === "1" ? "inmeld" : (apType === "2" ? "uitmeld" : "voorinmeld")) + "Punt";
        var label = point.getLabel() == null ? "" : point.getLabel();

        var map = newMap;
        var movements = null;
        if(!map) {
            if(this.editor.activeMovement){
                var mvmnt = this.editor.activeRseq.getMovementById(this.editor.activeMovement);

                map = rseq.findMapForPoint(mvmnt.config.id, point.config.id);
                if (!mvmnt || !map) {
                    alert("Kan geen movements vinden voor activation point!");
                    return;
                }
                movements = [{
                    movement: mvmnt,
                    map:map
                }];
            } else {
                movements = rseq.findMovementsForPoint(point);
                if (movements.length === 0) {
                    alert("Kan geen movements vinden voor activation point!");
                    return;
                }
                map = movements[0].map;
            }
        }
        var oldSignalGroupNumber = map.config.signalGroupNumber;

        if(apType === "1"){
            label += " (Signaalgroep " + oldSignalGroupNumber + ")";
        }
        
        var triggerTypes = null;
        var triggerType = map.config.triggerType;
        if (this.editor.getCurrentVehicleType() === "OV") {
            triggerTypes = [
                {value: 'STANDARD', desc: 'Standaard: door vervoerder bepaald'},
                {value: 'FORCED', desc: 'Automatisch: altijd melding'},
                {value: 'MANUAL', desc: 'Handmatig: handmatig door chauffeur'}
            ];
        }else{
            if(Ext.Array.contains(["MANUAL", "STANDARD", "FORCED"],triggerType)){
                switch(triggerType){
                    case "STANDARD":
                        triggerType = "0";
                        break;
                    case "FORCED":
                        triggerType = "1";
                        break;
                    case "MANUAL":
                        triggerType = "2";
                        break;
                }
            }
            triggerTypes = [
                {value: '0', desc: 'Automatisch: zwaailicht aan'},
                {value: '1', desc: 'Automatisch: altijd melding'},
                {value: '2', desc:'Handmatig: handmatig door chauffeur'},
                {value: '3', desc:'Handmatig: linksaf'},
                {value: '4', desc:'Handmatig: rechtdoor'},
                {value: '5', desc:'Handmatig: rechtsaf'},
                {value: '6', desc:'Automatisch: smeerpunten tram'},
                {value: '7', desc:'Handmatig: kazernepunt'},
                {value: '8', desc:'Type = 8'},
                {value: '9', desc:'Type = 9'},
                {value: '10', desc:'Type = 10'},
                {value: '11', desc:'Type = 11'}
            ];
        }
        
        var ov = [];
        var hulpdienst = [];
        var children = [];
        if(this.editor.getCurrentVehicleType() === "OV") {
            children.push({
                id: 'ov-node',
                text: 'OV',
                checked: false,
                iconCls: "noTreeIcon",
                expanded:false,
                leaf: false,
                children: ov
            });
        } else {
            children.push({
                id: 'hulpdienst-node',
                text: 'Hulpdiensten',
                checked: false,
                iconCls: "noTreeIcon",
                expanded:false,
                leaf: false,
                children: hulpdienst
            });
        }
        var vehicles = {
            root: {
                text: 'Alle',
                id: 'root',
                iconCls: "noTreeIcon",
                expanded: true,
                checked: false,
                children: children
            }
        };
        var selectedVehicleTypes = [];
        if(!map.config.vehicleTypes){
            map.config.vehicleTypes = new Array();
        }
        Ext.Array.each(vehicleTypes, function(vt) {
            var selected = map.config.vehicleTypes.indexOf(vt.nummer) !== -1;
            if(selected || vt.omschrijving.indexOf('Gereserveerd') === -1) {
                var leaf = {
                        id: vt.nummer,
                        text: vt.omschrijving,
                        checked: false,
                        iconCls: "noTreeIcon",
                        leaf: true};
                if(vt.groep === "OV") {
                    ov.push(leaf);
                    if(selected && this.editor.getCurrentVehicleType() === "OV") {
                        selectedVehicleTypes.push(vt.nummer);
                    }
                }else{
                    hulpdienst.push(leaf);
                    if(selected && this.editor.getCurrentVehicleType() === "Hulpdiensten") {
                        selectedVehicleTypes.push(vt.nummer);
                    }
                }
            }
        }, this);

        var vehicleTypesStore = Ext.create('Ext.data.TreeStore', vehicles);

        var editingUitmeldpunt = map.config.commandType === 2;
        var edittingInmeldpunt = map.config.commandType === 1;
        var signalItems = [{
            xtype: 'numberfield',
            fieldLabel: 'Afstand tot stopstreep',
            name: 'distanceTillStopLine',
            value: map.getDistanceTillStopLine()
        },
        {
            xtype: 'numberfield',
            minValue: 0,
            listeners: {
                change: function(field, value) {
                    value = parseInt(value, 10);
                    field.setValue(value);
                }
            },
            fieldLabel: 'Virtual local loop number',
            name: 'virtualLocalLoopNumber',
            value: map.getVirtualLocalLoopNumber()
        },{
            xtype: 'treecombo',
            valueField: 'id',
            editable:false,
            rootVisible: false,
            allowBlank: false,
            value:  selectedVehicleTypes.join(","),
            fieldLabel: 'Voertuigtypes',
            treeWidth:290,
            treeHeight: 300,
            name: 'vehicleTypes',
            store: vehicleTypesStore
        },
        {
            xtype: 'combo',
            fieldLabel: 'Triggertype',
            name: 'triggerType',
            allowBlank: false,
            blankText: 'Selecteer een optie',
            editable: false,
            anyMatch: true,
            hidden:!edittingInmeldpunt,
            displayField: 'desc',
            valueField: 'value',
            value: triggerType,
            store: Ext.create('Ext.data.Store', {
                fields: ['value', 'desc'],
                data: triggerTypes
            })
        }];
        if(editingUitmeldpunt) {
            var dirs = { root:{
                text: 'Alle',
                id: 'root',
                iconCls: "noTreeIcon",
                expanded: true,
                checked: false,
                children :[
                    {
                        id: 1,
                        text: 'Linksaf',
                        checked: false,
                        leaf: true,
                        iconCls: 'noTreeIcon'
                    },
                    {
                        id: 2,
                        text: 'Rechtsaf',
                        checked: false,
                        leaf: true,
                        iconCls: 'noTreeIcon'
                    },
                    {
                        id: 3,
                        text: 'Rechtdoor',
                        checked: false,
                        leaf: true,
                        iconCls: 'noTreeIcon'
                    }
                ]
            }};

            var directionStore = Ext.create('Ext.data.TreeStore', dirs);
            var direction = map.config.direction ? map.config.direction : "";
            var dir = direction.join ? direction.join(",") : direction;
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
                value: map.config.signalGroupNumber
            },
            {
                xtype: 'treecombo',
                valueField: 'id',
                editable:false,
                value:  dir,
                fieldLabel: 'Richting(en)',
                treeWidth:290,
                treeHeight: 150,
                name: 'direction',
                store: directionStore
            }
        ]);
        }
        
        var cancelFunction = function() {
            me.activationPointEditWindow.destroy();
            me.activationPointEditWindow = null;
            if(cancelHandler) {
                cancelHandler();
            }
            me.editor.activeMovement = null;
        };

        var okFunction = function() {
            var form = Ext.getCmp('activationForm').getForm();
            if(!form.isValid()) {
                Ext.Msg.alert('Ongeldige gegevens', 'Controleer aub de geldigheid van de ingevulde gegevens.');
                return;
            }

            // merge label naar point
            var formValues = form.getValues();
            point.setConfig(objectSubset(formValues, ["label"]));
            // deze waardes naar alle maps van movements die dit
            // pointId gebruiken
            var pointSignalValues = objectSubset(formValues, ["distanceTillStopLine", "virtualLocalLoopNumber", "vehicleTypes", "triggerType"]);

            // Alleen bij uitmeldpunt kunnen deze worden ingesteld
            // maar moeten voor alle movements worden toegepast op
            // alle signals
            if(editingUitmeldpunt) {
                allSignalValues = objectSubset(formValues, ["signalGroupNumber","direction"]);
            }

            // nieuw punt, alleen naar map mergen
            if(!movements) {
                map.setConfig(pointSignalValues);
                if(editingUitmeldpunt) {
                    map.setConfig(allSignalValues);
                }
            } else {

                // merge naar alle movements

                Ext.Array.each(movements, function(mvmtAndMap) {
                    //console.log("movement nummer " + mvmtAndMap.movement.nummer);
                    if(mvmtAndMap.map) {
                        //console.log("merging distanceTillStopLine and triggerType values to movement nummer " + mvmtAndMap.movement.nummer + " map pointId " + mvmtAndMap.map.pointId);
                        mvmtAndMap.map.setConfig(pointSignalValues);
                    }
                    if(editingUitmeldpunt) {
                        var movement = mvmtAndMap.movement;
                        
                        // de allSignalValues moeten alleen naar de punten die hetzelfde signaalgroepnummer
                        // hadden gepropageerd worden. Dit om te voorkomen dat alle punten in de huidige* beweging geupdatet worden met het nieuwe signaalgroepnummer
                        //* bij het bewerken van 1 beweging zal movements op r628 alleen die beweging bevatten die geselecteerd is. Als via de kaart is geselecteerd,
                        // bevat movements alle bewegingen die het aangeklikte punt bevat, en wordt het in alle bewegingen doorgevoerd.
                        
                        var maps = movement.getMapsForSignalgroup(oldSignalGroupNumber);
                        // merge allSignalValues naar alle MovementActivationPoints
                        // van movements die dit pointId gebruiken
                        Ext.each(maps, function(theMap) {
                            if(theMap.getBeginEndOrActivation() === "ACTIVATION") {
                                //console.log("merging signalGroupNumber, virtualLocalLoopNumber and vehicleType values point signal values to movement nummer " + mvmtAndMap.movement.nummer + " map pointId " + theMap.pointId);
                               theMap.setConfig(allSignalValues);
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
            me.editor.activeMovement = null;
        };

        this.activationPointEditWindow = Ext.create('Ext.window.Window', {
            title: 'Bewerken ' + apName.toLowerCase() + " " + label,
            height: map.config.commandType === 2 ? 290 : 265,
            width: 490,
            modal: true,
            icon: karTheme[apName],
            layout: 'fit',
            listeners: {
                afterRender: function(thisForm, options){
                    this.keyNav = Ext.create('Ext.util.KeyNav', this.el, {
                        enter: okFunction,
                        scope: this
                    });
                    var fields = this.query("field");
                    var button = Ext.getCmp("activationPointOkButton");
                    var f = function(){
                        button.setDisabled( false );
                    };
                    Ext.each(fields, function(field){
                        field.addListener("change", f);
                    });
                },
                close:function(){
                    editor.activeMovement = null;
                    cancelFunction();
                }
            },
            items: {
                xtype: 'form',
                id: 'activationForm',
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
                    value: point.config.label,
                    maxLength: 255,
                    maxLengthText: "Maximale lengte is 255 karakters",
                    id: 'labelEdit'
                }],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {
                            xtype: 'container',
                            style: {
                                fontWeight: 'bold'
                            },
                            flex: 1,
                            html: 'Voertuigtype: ' + this.editor.getCurrentVehicleType()
                        },
                        {
                            text: 'OK',
                            disabled: true,
                            id: "activationPointOkButton",
                            handler: okFunction
                        },
                        {
                            text: 'Annuleren',
                            handler: cancelFunction
                        }
                    ]
                }]
            }
        }).show();
        Ext.getCmp("labelEdit").selectText(0);
        Ext.getCmp("labelEdit").focus(false, 100);
    },

    editCoordinates : function (pointObject, okHandler, cancelHandler, extraLabel){
        var me = this;
        var coords = [];
        if(pointObject instanceof RSEQ){
            coords = pointObject.getLocation().coordinates;
        } else if(pointObject) {
            coords = pointObject.getGeometry().coordinates;
        }
        var rdX = "";
        var rdY = "";
        var wgsX = "";
        var wgsY = "";
        if(coords.length > 0) {
            rdX = coords[0];
            rdY = coords[1];
            var point = new Proj4js.Point(rdX, rdY);

            var wgs = new Proj4js.Proj("EPSG:4236");
            var rd = new Proj4js.Proj("EPSG:28992");
            Proj4js.transform(rd, wgs, point);
            wgsX = point.x;
            wgsY = point.y;
        }
        var okFunction = function() {
            var form = Ext.getCmp('coordinatesForm').getForm();
            if(!form.isValid()) {
                Ext.Msg.alert('Ongeldige gegevens', 'Controleer aub de geldigheid van de ingevulde gegevens.');
                return;
            }

            var formValues= form.getValues();
            if(rdX == formValues.rdX && rdY == formValues.rdY) {
                if(wgsX != formValues.wgsX || wgsY != formValues.wgsY){
                    // converteer nieuwe wgs naar rd en sla ze op
                    var point = new Proj4js.Point(formValues.wgsX, formValues.wgsY);
                    Proj4js.transform(wgs, rd, point);
                    rdX = point.config.x;
                    rdY = point.config.y;
                }
            } else {
                rdX = formValues.rdX;
                rdY = formValues.rdY;
            }

            var coordObj = {coordinates:[parseFloat(rdX),parseFloat(rdY)], type: 'Point'};
            if(pointObject) {
                if (pointObject instanceof RSEQ) {
                    pointObject.setLocation(coordObj);
                } else {
                    pointObject.setGeometry(coordObj);
                }
                me.editor.fireEvent("activeRseqUpdated", me.editor.activeRseq);
            }
            me.editCoords.destroy();
            me.editCoords = null;

            if(okHandler) {
                okHandler(coordObj);
            }
        };
        this.editCoords = Ext.create('Ext.window.Window', {
            title: 'Voer coördinaten in' + (extraLabel || ""),
            height: 240,
            width: 400,
            modal: true,
            icon: karTheme.gps,
            listeners: {
                afterRender: function(thisForm, options){
                    this.keyNav = Ext.create('Ext.util.KeyNav', this.el, {
                        enter: okFunction,
                        scope: this
                    });
                }
            },
            layout: 'fit',
            items: {
                xtype: 'form',
                id: 'coordinatesForm',
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
                        items:[{
                                fieldLabel: 'GPS y-coordinaat',
                                name: 'wgsY',
                                value: wgsY,
                                id: 'wgsY'
                            },
                            {
                                fieldLabel: 'GPS x-coordinaat',
                                name: 'wgsX',
                                value: wgsX,
                                id: 'wgsX'
                            }]
                    }
                    ],
                dockedItems: [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {
                            xtype: 'container',
                            style: {
                                fontWeight: 'bold'
                            },
                            flex: 1,
                            html: 'Voertuigtype: ' + this.editor.getCurrentVehicleType()
                        },
                        {
                            text: 'OK',
                            handler:okFunction
                        },
                        {
                            text: 'Annuleren',
                            handler: function() {
                                me.editCoords.destroy();
                                me.editCoords = null;

                                if(cancelHandler) {
                                    cancelHandler();
                                }
                            }
                        }
                    ]
                }]
            }
        }).show();
    },

    showCarriers: function (carriers, okFunction) {
        var carrierStore = Ext.create('Ext.data.Store', {
            storeId: 'carriersStore',
            fields: ['id', 'mail', 'username'],
            data: {'items': carriers},
            proxy: {
                type: 'memory',
                reader: {
                    type: 'json',
                    root: 'items'
                }
            }
        });
        var me = this;
        var defaultCarriers = this.editor.activeRseq.vehicleType === 'OV' ? profile.defaultCarriers : this.editor.activeRseq.vehicleType === 'Hulpdiensten' ? profile.defaultHelpCarriers : '';
        this.carriersWindow = Ext.create('Ext.window.Window', {
            title: 'Selecteer vervoerder(s)',
           // height: 200,
            width: 720,
            items: [{
                    fieldLabel: 'Selecteer de vervoerder(s)',
                    labelWidth: 150,
                    width: 700,
                    store: carrierStore,
                    queryMode: 'local',
                    name: "carrierIds",
                    id: "carrierIds",
                    anyMatch: true,
                    multiSelect: true,
                    value: defaultCarriers,
                    displayField: 'username',
                    valueField: 'id',
                    xtype: "combo"
                }],
            bbar: [
                {xtype: 'button', text: 'Informeren', handler: function(){
                        var ids = Ext.getCmp("carrierIds").getValue();
                        me.carriersWindow.destroy();
                        me.carriersWindow = null;
                        okFunction(ids);
                }},
                {xtype: 'button', text: 'Annuleren', handler: function(){
                    me.carriersWindow.destroy();
                    me.carriersWindow = null;
                }}
            ]
        });
        this.carriersWindow.show();
    },

    adminExportWindow: function () {
        var me = this;
        var dataownerStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'code', 'omschrijving'],
            data: dataOwners
        });
        var overviewStore = Ext.create('Ext.data.Store',{
            storeId : 'overviewStore',
            fields : ['dataowner','totalvri','vriwithouterrors','vriwithouterrorsready'],
            data : {
                'items' : []
            },
            proxy : {
                type : 'memory',
                reader : {
                    type : 'json',
                    rootProperty : 'items'
                }
            }
        });
        this.adminExport = Ext.create('Ext.window.Window', {
            title: 'Export beheerders',
            height: 600,
            width: 720,
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [
                {
                    xtype: 'fieldcontainer',
                    layout: 'hbox',
                    defaults: {
                        anchor: '100%',
                        labelWidth: 100
                    },
                    margin: 10,
                    items: [{
                        flex: 1,
                        id: 'dataowner',
                        xtype: "tagfield",
                        fieldLabel: 'Kies beheerder(s)',
                        store: dataownerStore,
                        queryMode: 'local',
                        displayField: 'omschrijving',
                        disabled: false,
                        anyMatch: true,
                        valueField: 'id',
                        emptyText: "Selecteer",
                        listeners:{
                            scope:this,
                            change: function(){
                                Ext.getCmp("makeExport").setDisabled(false);
                            }
                        }
                    }, {
                        xtype: 'button',
                        margin: "0 0 0 10",
                        disabled:true,
                        id: 'makeExport',
                        text: 'Maak overzicht',
                        listeners: {
                            scope: this,
                            click: function () {
                                var grid = Ext.getCmp("grid");
                                overviewStore.removeAll();
                                grid.setLoading("data ophalen...");
                                var dos = Ext.getCmp("dataowner").getValue();
                                Ext.Ajax.request({
                                    url : exportActionBeanUrl,
                                    method : 'GET',
                                    scope : me,
                                    timeout: 120000,
                                    params : {
                                        "adminExport":true,
                                        "dos": dos.toString(),
                                        "exportType" :"JSON"
                                    },
                                    success : function (response){
                                        var grid = Ext.getCmp("grid");
                                        var msg = Ext.JSON.decode(response.responseText);
                                        if(msg.success){
                                            overviewStore.add(msg.items);
                                        } else{
                                            Ext.MessageBox.show({
                                                title : "Fout",
                                                msg : "Kan data niet ophalen. Probeer het opnieuw of neem contact op met de applicatie beheerder."+msg.error,
                                                buttons : Ext.MessageBox.OK,
                                                icon : Ext.MessageBox.ERROR
                                            });
                                        }
                                        grid.setDisabled(false);
                                        grid.setLoading(false);
                                    },
                                    failure : function (response){
                                        Ext.MessageBox.show({
                                            title : "Ajax fout",
                                            msg : "Kan data niet ophalen. Probeer het opnieuw of neem contact op met de applicatie beheerder."+response.responseText,
                                            buttons : Ext.MessageBox.OK,
                                            icon : Ext.MessageBox.ERROR
                                        });
                                        grid.setDisabled(false);
                                        grid.setLoading(false);
                                    }
                                });
                            }
                        }
                    }]
                },
                {
                    title: 'Overzicht',
                    xtype: "grid",
                    id: "grid",
                    flex: 1,
                    disabled:true,
                    store: overviewStore,
                    columns: [
                        {
                            text: 'Beheerder',
                            dataIndex: 'dataowner',
                            flex: 1
                        },
                        {
                            text: 'Totaal VRI\'s',
                            dataIndex: 'totalvri'
                        },
                        {
                            text: 'VRI\'s zonder KV9 fouten',
                            dataIndex: 'vriwithouterrors'
                        },
                        {
                            text: 'VRI\'s zonder KV9 fouten en gereed voor export',
                            dataIndex: 'vriwithouterrorsready'
                        }
                    ]
                }],
                bbar: [
                    {xtype: 'button', text: 'Export', handler: function () {
                        var ids = Ext.getCmp("dataowner").getValue();
                        var url = exportActionBeanUrl;
                        url += exportActionBeanUrl.indexOf("?") !== -1 ? '&' : "?";
                        url += "dos=" + ids + "&adminExport=" + true + "&exportType=CSV";
                        document.location.href = url;
                        me.adminExport.destroy();
                        me.adminExport = null;
                    }},
                    {xtype: 'button', text: 'Annuleren', handler: function(){
                        me.adminExport.destroy();
                        me.adminExport = null;
                    }}
                ]
        });
        this.adminExport.show();
    },
    
    hasOpenWindows : function(){
        var windows = new Array();
        windows.push(this.rseqEditWindow);
        windows.push(this.karAttributesEditWindow);
        windows.push(this.pointEditWindow);
        windows.push(this.activationPointEditWindow);
        windows.push(this.editCoords);
        windows.push(this.editDirectionWindow);
        windows.push(this.adminExport);

        for( var i = 0 ; i < windows.length; i++){
            var window = windows[i];
            if(window && !window.isHidden() ){
                return true;
            }
        }

        return false;
    }

});
