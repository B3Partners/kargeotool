/*
 KAR Geo Tool - applicatie voor het registreren van KAR meldpunten               
 
 Copyright (C) 2009-2013 B3Partners B.V.                                   
 
 This program is free software: you can redistribute it and/or modify      
 it under the terms of the GNU Affero General Public License as            
 published by the Free Software Foundation, either version 3 of the        
 License, or (at your option) any later version.                           
 
 This program is distributed in the hope that it will be useful,           
 but WITHOUT ANY WARRANTY; without even the implied warranty of            
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the              
 GNU Affero General Public License for more details.                       
 
 You should have received a copy of the GNU Affero General Public License  
 along with this program. If not, see <http://www.gnu.org/licenses/>.      
 */

var rseqType = {
    "" : "nieuw verkeerssysteem",
    "CROSSING" : "VRI",
    "GUARD" : "bewakingssysteem nadering voertuig",
    "BAR" : "afsluitingssysteem"
};

var rightsStore;
var rightsGrid;

function rseqFrame (){
    var me = this;
    Ext.create('Ext.form.Panel',{
        title : 'Bewerken ' + me.rseqType[rseq.type] + (rseq.karAddress == null ? "" : " met KAR adres " + rseq.karAddress),
        height : 420,
        width : 450,
        renderTo : 'rseq',
        icon : rseq.type == "" ? karTheme.crossing : karTheme[rseq.type.toLowerCase()],
        layout : 'fit',
        items : {
            id : 'rseqForm',
            xtype : 'form',
            bodyStyle : 'padding: 5px 5px 0',
            fieldDefaults : {
                msgTarget : 'side',
                disabled : true,
                disabledCls : 'myDisabledClass',
                labelWidth : 150
            },
            defaultType : 'textfield',
            defaults : {
                anchor : '100%'
            },
            items : [{
                    xtype : 'fieldcontainer',
                    fieldLabel : 'Soort verkeerssysteem',
                    defaultType : 'radiofield',
                    layout : 'vbox',
                    items : [{
                            name : 'type',
                            inputValue : 'CROSSING',
                            checked : rseq.type == 'CROSSING',
                            boxLabel : me.rseqType['CROSSING']
                        },{
                            name : 'type',
                            inputValue : 'GUARD',
                            checked : rseq.type == 'GUARD',
                            boxLabel : me.rseqType['GUARD']
                        },{
                            name : 'type',
                            inputValue : 'BAR',
                            checked : rseq.type == 'BAR',
                            boxLabel : me.rseqType['BAR']
                        }]
                }
                ,{
                    fieldLabel : 'Beheerder',
                    name : 'dataOwner',
                    value : dataOwner
                }
                ,{
                    fieldLabel : 'Beheerdersaanduiding',
                    name : 'crossingCode',
                    value : rseq.crossingCode
                },{
                    fieldLabel : 'KAR adres',
                    name : 'karAddress',
                    allowBlank : false,
                    minValue : 0,
                    value : rseq.karAddress,
                    listeners : {
                        change : function (field,value){
                            value = parseInt(value,10);
                            field.setValue(value);
                        }
                    }
                },{
                    fieldLabel : 'Plaats',
                    name : 'town',
                    value : rseq.town
                },{
                    fieldLabel : 'Locatie',
                    name : 'description',
                    value : rseq.description
                },{
                    format : 'Y-m-d',
                    fieldLabel : 'Geldig vanaf',
                    name : 'validFrom',
                    value : rseq.validFrom
                },{
                    format : 'Y-m-d',
                    fieldLabel : 'Geldig tot',
                    name : 'validUntil',
                    value : rseq.validUntil
                }]
        }
    });

    var checkboxRenderer = function (p1,p2,record,row,column,store,grid){
        var field = ["","read","write"][column];
        var checked = record.get(field);
        return Ext.String.format("<input type='checkbox' {0} "
                + "onclick='rightsClick(event,{1},{2})'>"
                + "</input>",
                checked ? "checked='checked'" : "",
                row,
                column
                );
    };

    rightsStore = Ext.create('Ext.data.Store',{
        storeId : 'rightsStore',
        fields : ['userId','fullname','read','write'],
        data : {
            'items' : rights
        },
        proxy : {
            type : 'memory',
            reader : {
                type : 'json',
                root : 'items'
            }
        }
    });
    rightsGrid = Ext.create(Ext.grid.Panel,{
        title : 'Geselecteerde verkeerssystemen',
        xtype : "grid",
        id : "grid",
        store : rightsStore,
        columns : [
            {
                text : 'Gebruiker',
                dataIndex : 'fullname',
                flex : 1,
                draggable : false
            },
            {
                text : 'Lezen',
                dataIndex : 'read',
                renderer : checkboxRenderer,
                draggable : false
            },
            {
                text : 'Schrijven',
                dataIndex : 'write',
                renderer : checkboxRenderer,
                draggable : false
            }
        ],
        height : 420,
        width : 400,
        renderTo : 'rseqRights',
        dockedItems : [{
                xtype : 'toolbar',
                dock : 'bottom',
                ui : 'footer',
                items : [
                    {
                        xtype : "combo",
                        id : "gebruikerCombo",
                        fieldLabel : 'Gebruiker',
                        store : Ext.create('Ext.data.Store',{
                            fields : ['fullname','id'],
                            data : me.gebruikers,
                            sorters:[
                                {
                                    property: "fullname"
                                }
                            ]
                        }),
                        queryMode : 'local',
                        displayField : 'fullname',
                        valueField : 'id',
                        name : 'gebruikerCombo',
                        emptyText : "Selecteer",
                        flex : 1
                    },
                    {
                        xtype : 'button',
                        text : 'Voeg toe',
                        handler : function (){
                            var combo = Ext.getCmp("gebruikerCombo");
                            var userId = combo.getValue();
                            if(userId){
                                var displayValue = combo.getDisplayValue();
                                addUser(userId,displayValue);
                            }
                        }
                    },
                    {
                        xtype : 'button',
                        text : 'Verwijder',
                        handler : function (){
                            var sm = rightsGrid.getSelectionModel();
                            var selection = sm.getSelection()[0];
                            if(selection){
                                removeUser(selection.data.userId);
                            }
                        }
                    }
                ]
            }]
    });

}

function getRights (){
    var rightsList = Ext.get("rightsList").dom;
    rightsList.value = Ext.JSON.encode(rights);
}

function rightsClick (event,row,column){
    if(!event){
        event = window.event;
    }
    var checked = event.target.checked;
    var field = ["","read","write"][column];
    var user = rightsStore.data.items[row].data.userId;
    var settings = null;
    for (var i = 0;i < rights.length;i++){
        if(rights[i].userId == user){
            settings = rights[i];
            break;
        }
    }
    if(settings != null){
        settings[field] = checked;
    } else{
        alert("probleem");
    }
}

function addUser (userId,displayValue){
    rightsStore.add({
        userId : userId,
        fullname : displayValue,
        read : false,
        write : false
    });

    var settings = new Object();
    settings.userId = userId;
    settings.read = false;
    settings.write = false;
    rights.push(settings);


    var combo = Ext.getCmp("gebruikerCombo");
    var comboStore = combo.getStore();
    var index = comboStore.find("id",userId);
    if(index != null && index >= 0){
        comboStore.removeAt(index);
        combo.select(null);
    }
}

function removeUser (userId){
    
    var fullName = rightsStore.findRecord("userId",userId).data.fullname;
    var combo = Ext.getCmp("gebruikerCombo");
    var comboStore = combo.getStore();

    comboStore.add({
        id : userId,
        fullname : fullName
    });

    var index = rightsStore.find("userId",userId);
    if(index != null && index >= 0){
        rightsStore.removeAt(index);
    }
   
    for (var i = 0;i < rights.length;i++){
        if(rights[i].userId == userId){
            delete rights[i];
        }
    }
}