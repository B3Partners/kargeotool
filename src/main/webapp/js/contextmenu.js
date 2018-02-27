/* global editor, Ext, karTheme, contextPath, isBeheerder */

/**
 * KAR Geo Tool - applicatie voor het registreren van KAR meldpunten
 *
 * Copyright (C) 2009-2018 B3Partners B.V.
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
 * Context menu class dat alle rechtermuis clicks afhandelt.
 */
Ext.define("ContextMenu", {
    mixins: {
        observable: 'Ext.util.Observable'
    },
    menuContext: null,
    editor: null,
    
    // menu's 
    rseq:null,
    uitmeldpunt:null,
    inmeldpunt:null,
    nonActivationPoint: null,
    onbekend:null,
    
    defaultMenu:null,
    
    point_with_line:null,
    /**
     * 
     * @param editor A reference to the editor.
     *@constructor
     */
    constructor: function(editor) {
        this.mixins.observable.constructor.call(this);
        this.editor = editor;
    },
    /**
     * Maak de menu's aan.
     */
    createMenus: function() {
        var me = this;
        
        // Context menu voor een klik in de kaart indien geen rseq actief is
        
        this.defaultMenu = Ext.create ("Ext.menu.Menu",{
            floating: true,
            renderTo: Ext.getBody(),
            items: [
            {
                id: 'addRseq',
                text: 'Hier verkeerssysteem toevoegen',
                icon: karTheme.crossing,
                iconCls : 'overviewTree'
            },
            {
                id: 'exportAdmin',
                text: 'Export beheerders',
                icon: contextPath + "/images/silk/transmit_go.png",
                hidden: !isBeheerder,
                iconCls : 'overviewTree'
            },
            {
                id: 'cancelSelecting',
                text: 'Annuleer selecteren bestaand punt',
                icon: karTheme.cursor_delete,
                hidden: true,
                iconCls : 'overviewTree'
            }
            ],
            listeners: {
                click: function(menu,item,e, opts) {
                    
                    // Gebruik huidige positie om om die plek wat te doen
                    var pos = {
                        x: menu.x - Ext.get(this.editor.domId).getX(),
                        y: menu.y
                    };
                    var lonlat = this.editor.olc.map.getLonLatFromPixel(pos);
                    
                    switch (item.getId()) {
                        case 'cancelSelecting':
                            this.editor.cancelSelection();
                            break;                            
                        case 'addRseq':
                            // Voeg op huidige positie nieuwe Rseq toe 
                            this.editor.addRseq(lonlat.lon, lonlat.lat);
                            break;  
                         case 'exportAdmin':
                            this.editor.editForms.adminExportWindow();
                            break; 
                    }
                },
                hide: {
                    scope: me,
                    fn:this.hideFired
                },
                scope:me
            }
        });
                
        
        this.point_with_line = Ext.create ("Ext.menu.Menu",{
            floating: true,
            renderTo: Ext.getBody(),
            items: [
            {
                id: 'resetMeasure',
                text: 'Meten vanaf vorig punt',
                icon: contextPath + "/images/silk/ruler.png",
                iconCls : 'overviewTree'
            }
            ],
            listeners: {
                click: function(menu,item,e, opts) {
                    switch (item.getId()) {
                        case 'resetMeasure':
                            this.editor.resetMeasure();
                            break;
                    }
                },
                hide: {
                    scope: me,
                    fn:this.hideFired
                },
                scope:me
            }
        });
        
        // Context menu voor rseq
        
        this.rseq = Ext.create ("Ext.menu.Menu",{
            floating: true,
            renderTo: Ext.getBody(),
            items: [
            {
                id: 'editRseq',
                text: 'Bewerken...',
                icon: contextPath + "/images/silk/table_edit.png",
                iconCls : 'overviewTree'
            },
            {
                id: 'saveRseq',
                text: 'Opslaan',
                icon: contextPath + "/images/silk/table_save.png",
                iconCls : 'overviewTree'
            },
            
            {
                id: 'exportMenu',
                text: 'Exporteer',
                menu: {
                    items:[
                        {
                            id: 'exportXml',
                            text: 'Exporteer KV9 XML',
                            icon: contextPath + "/images/silk/transmit_go.png",
                            iconCls : 'overviewTree'
                        },
                        {
                            id: 'exportPtx',
                            text: 'Exporteer Incaa PTX',
                            icon: contextPath + "/images/silk/transmit_go.png",
                            iconCls : 'overviewTree'
                        }
                    ],
                    listeners: {
                        click:
                        function(menu,item,e, opts) {
                            var me = this;
                            me.type = item.getId();
                            var f = function(){
                                if (!me.editor.activeRseq.getReadyForExport()) {
                                    Ext.Msg.alert('Fout! Kan niet exporteren', "Verkeerssysteem is nog niet klaar om te exporteren.");
                                    return;
                                }
                                if (me.editor.activeRseq.getValidationErrors() !== 0) {
                                    Ext.Msg.alert('Fout! Kan niet exporteren', "Verkeerssysteem heeft nog validatiefouten.");
                                    return;
                                }
                                switch (me.type) {
                                    case 'exportXml':
                                        me.editor.exportXml();
                                        break;
                                    case 'exportPtx':
                                        me.editor.exportPtx();
                                        break;
                                }
                            };
                            if (me.editor.changeManager.changeDetected) {
                                Ext.Msg.show({
                                    title: "Opslaan verkeerssyteem",
                                    msg: "Alleen een opgeslagen verkeerssysteem kan worden gexporteerd. Wilt u nu opslaan?",
                                    fn: function (button) {
                                        if (button === 'yes') {
                                            me.editor.saveOrUpdate(f);
                                        }
                                    },
                                    scope: this,
                                    buttons: Ext.Msg.YESNO,
                                    buttonText: {
                                        no: "Nee",
                                        yes: "Ja"
                                    },
                                    icon: Ext.Msg.WARNING

                                });
                            }else{
                                f();
                            }
                        },
                        scope:me
                    }
                }
            },            
            {
                id: 'setCoordsRseq',
                text: 'Voer coördinaten in',
                icon: contextPath + "/images/icons/gps.png",
                iconCls : 'overviewTree'
            },
            {
                id: 'removeRseq',
                text: 'Verwijderen',
                icon: contextPath + "/images/silk/table_delete.png",
                iconCls : 'overviewTree'
            },
            {
                id: "addMemo",
                text: "Memo...",
                icon: contextPath + "/images/silk/attach.png",
                iconCls : 'overviewTree'
            },
            {
                id: "informCarrier",
                text: "Informeer vervoerder",
                icon: contextPath + "/images/silk/email_go.png",
                iconCls : 'overviewTree'
            },
            {
                id: "showMessages",
                text: "Export informatie",
                icon: contextPath + "/images/silk/email_open.png",
                iconCls : 'overviewTree'
            },{
                id: "uppervri",
                xtype: 'menuseparator'
            },
            {
                id: 'addUitmeldpunt',
                text: 'Voeg uitmeldpunt toe',
                icon: karTheme.uitmeldPunt,
                iconCls : 'overviewTree'
            },
            {
                id: 'advancedRseq',
                text: 'Geavanceerd',
                menu: {
                    items:[
                        {
                            id: 'selectUitmeldpuntAndereSignaalgroepRseq',
                            text: 'Selecteer uitmeldpunt van andere fasecyclus',
                            icon: contextPath + "/images/silk/cursor.png",
                            iconCls : 'overviewTree'
                        },
                        {
                            id: 'uploadDxf',
                            text: 'Upload DXF voor deze vri',
                            icon: contextPath + "/images/silk/cursor.png",
                            iconCls : 'overviewTree'
                        },
                        {
                            id: 'addUitmeldpuntCoordinates',
                            text: 'Voeg uitmeldpunt toe op basis van coördinaten',
                            icon: karTheme.uitmeldPunt,
                            iconCls : 'overviewTree'
                        }
                    ],
                    listeners: {
                        click:
                        function(menu,item,e, opts) {
                            switch (item.getId()) {
                                case 'selectUitmeldpuntAndereSignaalgroepRseq':
                                    this.editor.selectExistingUitmeldpunt(this.selectedMovement);
                                    break;
                                case 'uploadDxf':
                                    var url = dxfActionBeanUrl + "&rseq=" + this.editor.activeRseq.getId();
                                    document.location.href = url;
                                    break;
                                case 'addUitmeldpuntCoordinates':
                                    this.editor.addByCoordinates('uitmeldpunt');
                                    break;
                            }
                        },
                        scope:me
                    }
                }
            }
            ],
            listeners: {
                click: function(menu,item,e, opts) {
                    // Huidige positie van muis wordt niet gebruikt voor dit context menu
                    
                    switch (item.getId()) {
                        case 'addUitmeldpunt':
                            this.editor.addUitmeldpunt();
                            break;
                        case 'editRseq':
                            this.editor.editSelectedObject();
                            break;
                        case 'removeRseq':
                            this.editor.removeRseq();
                            break;
                        case 'addMemo':
                            this.editor.addMemo();
                            break;
                        case 'saveRseq':
                            this.editor.saveOrUpdate();
                            break;
                        case 'setCoordsRseq':
                            this.editor.editForms.editCoordinates(this.editor.selectedObject);
                            break;
                        case 'showMessages':
                            Ext.create("MessagesOverview").show(this.editor.selectedObject.id);
                            break;
                        case 'informCarrier':
                            this.editor.showCarriers();
                            break;
                    }
                },
                hide: {
                    scope: me,
                    fn:this.hideFired
                },
                scope: me
            }
        });

        // Context menu voor een onbekend punt verwijderen
        this.onbekend = Ext.create ("Ext.menu.Menu",{
            floating: true,
            renderTo: Ext.getBody(),
            items: [
            {
                id: 'removeOnbekend',
                text: 'Verwijderen',
                icon: contextPath + "/images/silk/table_delete.png",
                iconCls : 'overviewTree'
            }],
            listeners: {
                click: function(menu,item,e, opts) {
                    var pos = {
                        x: menu.x - Ext.get(editor.domId).getX(),
                        y: menu.y
                    };
                    var lonlat = editor.olc.map.getLonLatFromPixel(pos);
                    switch (item.getId()) {
                        case 'removeOnbekend':
                            this.editor.removeOtherPoint();
                            break;
                    }
                },
                hide: {
                    scope: me,
                    fn:this.hideFired
                },
                scope:me
            }
        });
        // Context menu voor een uitmeldpunt
        
        this.uitmeldpunt = Ext.create ("Ext.menu.Menu",{
            floating: true,
            renderTo: Ext.getBody(),
            items: [
            {
                id: 'editUitmeldpunt',
                text: 'Bewerken...',
                icon: contextPath + "/images/silk/table_edit.png",
                iconCls : 'overviewTree'
            },{
                id: 'moveCheckoutPoint',
                text: 'Verplaats',
                icon: contextPath + "/images/silk/table_go.png",
                iconCls : 'overviewTree'
            },
            {
                id: 'removeUitmeldpunt',
                text: 'Verwijderen',
                icon: contextPath + "/images/silk/table_delete.png",
                iconCls : 'overviewTree'
            },
            {
                id: 'setCoordsUitmeld',
                text: 'Voer coördinaten in',
                icon: contextPath + "/images/icons/gps.png",
                iconCls : 'overviewTree'
            },{
                id: "uppercheckout",
                xtype: 'menuseparator'
            },
            {
                id: 'addEindpunt',
                text: 'Voeg nieuw eindpunt toe',
                icon: karTheme.eindPunt,
                iconCls : 'overviewTree'
            },
            {
                id: 'selectEindpunt',
                text: 'Selecteer bestaand eindpunt',
                icon: contextPath + "/images/silk/cursor.png",
                iconCls : 'overviewTree'
            },
            {
                id: 'addInmeldpunt',
                text: 'Voeg nieuw inmeldpunt toe',
                disabled:true,
                icon: karTheme.inmeldPunt,
                iconCls : 'overviewTree'
            },
            {
                id: 'selectInmeldpunt',
                text: 'Selecteer bestaand inmeldpunt',
                disabled:true,
                icon: contextPath + "/images/silk/cursor.png",
                iconCls : 'overviewTree'
            },
            {
                id : "lowercheckout",
                xtype: 'menuseparator'
            },
            {
                id: 'advancedCheckout',
                text: 'Geavanceerd',
                menu: {
                    items:[
                        {
                            id: 'addBeginpunt',
                            text: 'Voeg beginpunt toe',
                            icon: karTheme.startPunt,
                            iconCls : 'overviewTree'
                        },
                        {
                            id: 'addEindpuntCoord',
                            text: 'Voeg nieuw eindpunt toe op basis van coördinaten',
                            icon: karTheme.eindPunt,
                            iconCls : 'overviewTree'
                        },
                        {
                            id: 'addInmeldpuntCoord',
                            text: 'Voeg nieuw inmeldpunt toe op basis van coördinaten',
                            disabled:true,
                            icon: karTheme.inmeldPunt,
                            iconCls : 'overviewTree'
                        },
                        {
                            id: 'addBeginpuntCoord',
                            text: 'Voeg beginpunt toe op basis van coördinaten',
                            icon: karTheme.startPunt,
                            iconCls : 'overviewTree'
                        }
                    ],
                    listeners: {
                        click:
                        function(menu, item, e, opts) {
                            if(!item) return;
                            switch (item.getId()) {
                                case 'addBeginpunt':
                                    this.editor.addBeginpunt();
                                    break;
                                case 'addEindpuntCoord':
                                    this.editor.addByCoordinates('eindpunt');
                                    break;
                                case 'addInmeldpuntCoord':
                                    this.editor.addByCoordinates('inmeldpunt');
                                    break;
                                case 'addBeginpuntCoord':
                                    this.editor.addByCoordinates('beginpunt');
                                    break;
                            }
                        },
                        scope:me
                    }
                }
            }
            ],
            listeners: {
                click: function(menu,item,e, opts) {
                    var pos = {
                        x: menu.x - Ext.get(editor.domId).getX(),
                        y: menu.y
                    };
                    var lonlat = editor.olc.map.getLonLatFromPixel(pos);
                    if(!item) return;
                    switch (item.getId()) {
                        case 'editUitmeldpunt':
                            this.editor.editSelectedObject();
                            break;                        
                        case 'removeUitmeldpunt':
                            this.editor.removeCheckoutPoint(this.editor.activeMovement);
                            break;                            
                        case 'addEindpunt':
                            editor.addEindpunt();
                            break;
                        case 'selectEindpunt':
                            editor.selectEindpunt();
                            break;
                        case 'addInmeldpunt':
                            editor.addInmeldpunt();
                            break;
                        case 'selectInmeldpunt':
                            editor.selectInmeldpunt();
                            break;              
                        case 'setCoordsUitmeld':
                            this.editor.editForms.editCoordinates(this.editor.selectedObject);
                            break;
                        case 'moveCheckoutPoint':
                            this.editor.movePoint(this.editor.selectedObject);
                            break;
                    }
                },
                hide: {
                    scope: me,
                    fn:this.hideFired
                },
                scope:me
            }
        });
        
        // Context menu voor een inmeldpunt
        
        this.inmeldpunt = Ext.create ("Ext.menu.Menu",{
            floating: true,
            renderTo: Ext.getBody(),
            items: [
            {
                id: 'editInmeldpunt',
                text: 'Bewerken...',
                icon: contextPath + "/images/silk/table_edit.png",
                iconCls : 'overviewTree'
            },{
                id: 'moveCheckinPoint',
                text: 'Verplaats',
                icon: contextPath + "/images/silk/table_go.png",
                iconCls : 'overviewTree'
            },
            {
                id: 'removeInmeldpunt',
                text: 'Verwijderen...',
                icon: contextPath + "/images/silk/table_delete.png",
                iconCls : 'overviewTree'
            },
            {
                id: 'setCoordsInmeld',
                text: 'Voer coördinaten in',
                icon: contextPath + "/images/icons/gps.png",
                iconCls : 'overviewTree'
            },{
                id: "upperIncheckin",
                xtype: 'menuseparator'
            },
            {
                id: 'addVoorinmeldpunt',
                text: 'Voeg nieuw voorinmeldpunt toe',
                icon: karTheme.voorinmeldPunt,
                iconCls : 'overviewTree'
            },
            {
                id: 'selectVoorInmeldPunt',
                text: 'Selecteer bestaand voorinmeldpunt',
                icon: contextPath + "/images/silk/cursor.png",
                iconCls : 'overviewTree'
            },
            {
                id: 'advancedCheckin',
                text: 'Geavanceerd',
                menu: {
                    items:[
                        {
                            id: 'addVoorinmeldpuntCoord',
                            text: 'Voeg nieuw voorinmeldpunt toe op basis van coördinaten',
                            icon: karTheme.voorinmeldPunt,
                            iconCls : 'overviewTree'
                        }
                    ],
                    listeners: {
                        click:
                            function(menu, item, e, opts) {
                                if(!item) return;
                                switch (item.getId()) {
                                    case 'addVoorinmeldpuntCoord':
                                        this.editor.addByCoordinates('voorinmeldpunt');
                                        break;
                                }
                            },
                        scope: me
                    }
                }
            }/*
            {
                id: 'voegBeginpuntToecheckin',
                text: 'Voeg beginpunt toe',
                icon: karTheme.startPunt
            }*/
            ],
            listeners: {
                click: function(menu,item,e, opts) {
                    if(!item) return;
                    var pos = {
                        x: menu.x - Ext.get(editor.domId).getX(),
                        y: menu.y
                    };
                    var lonlat = editor.olc.map.getLonLatFromPixel(pos);
                    switch (item.getId()) {
                        case 'editInmeldpunt':
                            this.editor.editSelectedObject();
                            break;
                        case 'removeInmeldpunt':
                            this.editor.removeOtherPoint(this.editor.activeMovement);
                            break;                               
                        case 'addVoorinmeldpunt':
                            editor.addVoorinmeldpunt();
                            break;
                        case 'setCoordsInmeld':
                            this.editor.editForms.editCoordinates(this.editor.selectedObject);
                            break;
                        case 'selectVoorInmeldPunt':
                            this.editor.selectVoorInmeldpunt(this.editor.activeMovement);
                            break;
                        case 'moveCheckinPoint':
                            this.editor.movePoint(this.editor.selectedObject);
                            break;
                    }
                },
                hide: {
                    scope: me,
                    fn:this.hideFired
                },
                scope:me
            }
        });
        
        // Context menu voor punten die alleen bewerkt kunnen worden (begin, eind, voorinmeldpunt)
        
        this.nonActivationPoint = Ext.create ("Ext.menu.Menu",{
            floating: true,
            renderTo: Ext.getBody(),
            items: [
            {
                id: 'editNAPoint',
                text: 'Bewerken...',
                icon: contextPath + "/images/silk/table_edit.png",
                iconCls : 'overviewTree'
            },{
                id: 'movePrecheckinPoint',
                text: 'Verplaats',
                icon: contextPath + "/images/silk/table_go.png",
                iconCls : 'overviewTree'
            },{
                id: 'removeNAPoint',
                text: 'Verwijderen',
                icon: contextPath + "/images/silk/table_delete.png",
                iconCls : 'overviewTree'
            },{
                id: 'setCoordsNon',
                text: 'Voer coördinaten in',
                icon: contextPath + "/images/icons/gps.png",
                iconCls : 'overviewTree'
            }
            ],
            listeners: {
                click: function(menu,item,e, opts) {
                    if(!item) return;
                    switch (item.getId()) {
                        case 'editNAPoint':
                            editor.editSelectedObject();
                            break;
                        case 'removeNAPoint':
                            this.editor.removeOtherPoint();
                            break;
                        case 'setCoordsNon':
                            this.editor.editForms.editCoordinates(this.editor.selectedObject);
                            break
                        case 'movePrecheckinPoint':
                            this.editor.movePoint(this.editor.selectedObject);
                            break;
                    }
                },
                hide: {
                    scope: me,
                    fn:this.hideFired
                },
                scope:me
            }
        });
        
        this.menuContext ={
            "standaard" : this.defaultMenu,
            "ACTIVATION_1" : this.inmeldpunt,
            "ACTIVATION_2" : this.uitmeldpunt ,
            "ACTIVATION_3" : this.inmeldpunt, // zelfde menu ook voor voorinmeldpunt
            
            "END" : this.nonActivationPoint,
            "BEGIN" : this.nonActivationPoint,
            
            "CROSSING" : this.rseq,
            "GUARD" : this.rseq,
            "BAR" : this.rseq,
            
            "ADDPOINT_WITH_LINE" : this.point_with_line,
            "UNKNOWN" : this.onbekend
        };
        // Get control of the right-click event:
        document.oncontextmenu = function(e){
            e = e?e:window.event;
            
            var f = editor.olc.getFeatureFromEvent(e);
            
            if(f&& !f.cluster){
                var x = e.clientX;
                var y = e.clientY;
                if(f.layer.name === "RseqSelect"){
                    editor.loadRseqInfo({
                        rseq: f.data.id
                    },function(){
                        editor.contextMenu.show(x,y);
                    });
                }else{
                    editor.setSelectedObject(f);
                    editor.contextMenu.show(x,y);
                }
            }
            if (e.preventDefault) 
                e.preventDefault(); // For non-IE browsers.
            else {
                return false; // For IE browsers.
            }
        };
    },
    /**
     * Toon het menu op de gewenste x en y pixel
     * @param x de x pixel
     * @param y de y pixel
     * @param forceDefault Laat het standaard menu zien
     */
    show : function(x,y,forceDefault){
        var context = this.getMenuContext();
        if(forceDefault === true && context !== this.point_with_line){
            context = this.menuContext['standaard'];
        }
        if(context){
            context.showAt(x, y);
        }
    },
    hideFired : function(menu, eventOptions){
        var me = this;
        setTimeout(function(){
            me.fireEvent("hide",menu, eventOptions);
        },500);
    },
    /**
 * Haal het nodige menu op door te kijken wat het type is van het geselecteerde object
 * @return het gewenste menu
 */
    getMenuContext: function() {
        if(editor.selectedObject){
            var type = editor.selectedObject.getType();

            // Update state van disabled / enabled items op basis van selectedObject

            if(type === "ACTIVATION_2") {
                // Voor een uitmeldpunt kan alleen een inmeldpunt worden toegevoegd
                // indien voor dat uitmeldpunt in een movement een eindpunt aanwezig 
                // is
                
                var heeftEindpunt = editor.activeRseq.heeftUitmeldpuntEindpunt(editor.selectedObject);
                Ext.getCmp("addInmeldpunt").setDisabled(!heeftEindpunt);
                Ext.getCmp("addInmeldpuntCoord").setDisabled(!heeftEindpunt);
                Ext.getCmp("selectInmeldpunt").setDisabled(!heeftEindpunt);
            }else if (type === "CROSSING") {
                Ext.getCmp("saveRseq").setDisabled(!editor.activeRseq.getEditable());
            }
            
            if(this.editor.olc.isMeasuring() ){
                type = "ADDPOINT_WITH_LINE";
            }
            var menu = this.menuContext[type];
            if(menu){
                return menu;
            }else{
                return this.menuContext["UNKNOWN"];
            }
        }else{
            return this.menuContext["standaard"];
        }
    },
    /**
    * Deactiveer het menu (verberg)
    */
    deactivateContextMenu: function() {
        for (var key in this.menuContext){
            var mc = this.menuContext[key];
            if(!mc) {
                alert("mc for key " + key + " is null");
            }
            mc.hide();
        }
    },
    showCancelSelecting: function(action){
        var cancel = this.defaultMenu.items.getByKey("cancelSelecting");
        cancel.show();
        this.editor.actionToCancel = action;
    },
    hideCancelSelecting: function(){
        var cancel = this.defaultMenu.items.getByKey("cancelSelecting");
        cancel.hide();
    }
});