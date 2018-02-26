/* global Ext */

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
 * In dit .js bestand worden de models gedefineerd.
 */
function objectSubset(object, keys) {
    var j = { };

    Ext.Object.each(object, function(key, value) {
        var processedKey = key.charAt(0) === '_' ? key.substring(1) : key;
        
        if(Ext.Array.contains(keys, processedKey)) {
            j[processedKey] = value;
        }
    });

    return j;
}
/**
 * Roadside Equipment model.
 */
Ext.define('RSEQ', {
    readyIsSet:null,
    config:{
        id: null,
        location:null,
        description:null,
        validFrom:null,
        validUntil:null,
        movements:null,
        karAddress:null,
        crossingCode: null,
        dataOwner:null,
        points:null,
        town:null,
        type:null, // ACTIVATION_1: Inmeldpunt, ACTIVATION_2: uitmeldpunt, ACTIVATION_3: voorinmeldpunt
        memo:null,
        attributes: null,
        editable:null,
        validationErrors: null,
        vehicleType:null,
        readyForExport:null
    },
    /**
     *@constructor
     * @param config.id het id van de RSEQ
     * @param config.location de locatie als geometry
     * @param config.description de omschrijving
     * @param config.validFrom geldig vanaf
     * @param config.validUntil geldig tot
     * @param config.movements de movements die bij deze RSEQ horen.
     * @param config.karAddress het kar adres
     * @param config.crossingCode het kruispunt nummer
     * @param config.dataOwner de eigenaar van de bijhorende data
     * @param config.points de punten die bij deze RSEQ horen
     * @param config.town de plaats waar deze RSEQ bij hoort
     * @param config.type het type van de RSEQ
     */
    constructor: function(config) {
        if(!config.description){
            config.description = "";
        }
        if(!config.points) {
            config.points = [];
        }
        if(!config.movements) {
            config.movements = [];
        }
        if(!config.validFrom) {
            config.validFrom = Ext.Date.format(new Date(), "Y-m-d");
        }
        if(!config.memo){
            config.memo = "";
        }
        if(!config.attributes){
            config.attributes = { "ES": [ [], [], [] ], "PT": [ [], [], [] ], "OT": [ [], [], [] ]};
            var vts = ["ES","PT","OT"];
            var defaults = profile.defaultKarAttributes;
            Ext.Array.each(vts, function(vt) {
                for(var j = 0; j < 24; j++) {
                    if(profile.defaultKarAttributes) {
                        config.attributes[vt][0].push(defaults[vt][0][j]);
                        config.attributes[vt][1].push(defaults[vt][0][j]);
                        config.attributes[vt][2].push(defaults[vt][0][j]);
                    } else {
                        config.attributes[vt][0].push(true);
                        config.attributes[vt][1].push(true);
                        config.attributes[vt][2].push(true);
                    }
                }
            });
        }
        this.initConfig(config);
        this.readyIsSet = null;
    },
    /**
     * Haal het punt op met behulp van het id van het punt
     * @param id het id van het gewenste punt
     * @return het gevonden punt of null indien niet gevonden.
     */
    getPointById : function (id){
        if(this.getPoints()){
            for (var i = 0 ; i < this.getPoints().length ;i++){
                if(this.getPoints()[i].getId() == id){
                    return this.getPoints()[i];
                }
            }
        }
        return null;
    },
    /**
     * Haal de movement op met behulp van het id.
     * @param id het id van de gewenste movement.
     * @return het movement object of null indien niet gevonden.
     */
    getMovementById : function (id){
        for (var i = 0 ; i < this.getMovements().length ;i++){
            if(this.getMovements()[i].getId() == id){
                return this.getMovements()[i];
            }
        }
        return null;
    },
    /**
     * Voegt een punt toe
     * @param point het toe te voegen punt
     */
    addPoint : function (point){
       /*if(!this.getPoints()){
            this.points = new Array();
        }*/
        this.getPoints().push(point);
    },
    /**
     * Voegt een movement toe
     * @param movement de toe te voegen movement
     */
    addMovement: function (movement){
        /*if(!this.movements){
            this.movements = new Array();
        }*/
        this.getMovements().push(movement);
    },


    /**
     * Haal alle movements in deze rseq op waarin het gegeven point voorkomt
     * @return een array van movements met objecten met "movement" en "map"
     *  properties (de movement en de map van het gegeven punt)
     */
    findMovementsForPoint: function(point) {
        var movements = [];

        Ext.Array.each(this.getMovements(), function(movement) {
            Ext.Array.each(movement.getMaps(), function(map) {
                if(map.getPointId() === point.getId()) {
                    movements.push({
                        movement: movement,
                        map: map
                    });
                }
            });
        });
        return movements;
    },

    findMapForPoint : function(movementId,pointId){
        var movement = this.getMovementById(movementId);
        var m = null
        Ext.Array.each(movement.getMaps(), function(map) {
            if(map.getPointId() === pointId) {
                m = map;
            }
        });
        return m;
    },

    /**
     * Controleert of er in een movement van het gegeven uitmeldpunt een
     * eindpunt is.
     */
    heeftUitmeldpuntEindpunt: function(uitmeldpunt) {

        var pointMovements = this.findMovementsForPoint(uitmeldpunt);

        var result = false;
        Ext.Array.each(pointMovements, function(mvmtAndMap) {
            Ext.Array.each(mvmtAndMap.movement.getMaps(), function(map) {
                if(map.getBeginEndOrActivation() === "END") {
                    result = true;
                    // Stop met Ext.Array.each() (break)
                    return false;
                }
                // Ga verder met volgende Ext.Array.each();
                return true;
            });
            // Indien nog geen eindpunt gevonden ga verder met volgende movement
            return result === false;
        });
        return result;
    },

    /**
     * Voeg een nieuw uitmeldpunt toe aan deze rseq.
     */
    addUitmeldpunt: function(uitmeldpunt, map, isBestandUitmeldpunt) {
        if(!isBestandUitmeldpunt){
            this.addPoint(uitmeldpunt);
        }
        this.addMovement(Ext.create(Movement, {
            id: Ext.id(),
            nummer: null,
            maps: [map],
            vehicleType: editor.getCurrentVehicleType()
        }));
    },
    addUitmeldpuntToMovement : function (point,map, movementId,after){
        var movement = this.getMovementById(movementId);
        movement.addMapAfter(map,after);
        this.addPoint(point);
    },

    /**
     * Voeg een nieuw of bestaand eindpunt toe aan alle movements van het gegeven
     * uitmeldpunt van deze rseq.
     */
    addEindpunt: function(uitmeldpunt, eindpunt, isBestaandEindpunt, movement) {
        if(!isBestaandEindpunt) {
            this.addPoint(eindpunt);
        }

        var pointMovements = null;
        if(movement){
            var mvmnt = this.getMovementById(movement);

            var map = this.findMapForPoint(mvmnt.getId(), uitmeldpunt.getId());
            pointMovements=[{
                map: map,
                movement: mvmnt
            }];
        }else{
            pointMovements = this.findMovementsForPoint(uitmeldpunt);
        }
        // Indien maar een enkele movement zonder eindpunt, voeg alleen eindpunt
        // toe aan dat movement

        if(pointMovements.length === 0) {
            alert("Ongeldige state, moet altijd movement zijn!");
        }
        var previousPoints = [];

        for(var i = 0 ; i < pointMovements.length ;i++){
            var mvmtAndMap = pointMovements[i];
            var movement = mvmtAndMap.movement;
            var hasEnd = false;
            var currentPoints =[];
            Ext.Array.each(movement.maps, function(map) {
                if(map.getBeginEndOrActivation() === "END") {
                    hasEnd = true;
                }
                currentPoints.push (map.getPointId());
            });
            
            var found = false;
            for(var j = 0 ; j < previousPoints.length;j++){
                if(movement.equals(previousPoints[j],false)){
                    found = true;
                    break;
                }
            }
            if(found){
                continue;
            }else{
                previousPoints.push(currentPoints);
            }
           
            if(!hasEnd) {
                newMap = Ext.create(MovementActivationPoint, {
                    id: Ext.id(),
                    beginEndOrActivation: "END",
                    pointId: eindpunt.getId()
                });
                movement.getMaps().push(newMap);
                continue;
            }
            // Kopieer alle punten van andere movements voor dit uitmeldpunt behalve
            // eindpunten
             var newMovement = Ext.create(Movement, {
                id: Ext.id(),
                nummer: null,
                maps: [],
                vehicleType: editor.getCurrentVehicleType()
            });

            var newMap;

            Ext.Array.each(mvmtAndMap.movement.maps, function (map) {
                if (map.getBeginEndOrActivation() != "END") {

                    var config = {
                        id: Ext.id(),
                        beginEndOrActivation: map.getBeginEndOrActivation(),
                        pointId: map.getPointId()
                    };
                    if (map.beginEndOrActivation == "ACTIVATION") {
                        Ext.Object.merge(config, {
                            commandType: map.getCommandType(),
                            distanceTillStopLine: map.getDistanceTillStopLine(),
                            signalGroupNumber: map.getSignalGroupNumber(),
                            virtualLocalLoopNumber: map.getVirtualLocalLoopNumber(),
                            triggerType: map.getTriggerType(),
                            vehicleTypes: map.getVehicleTypes()
                        });
                    }
                    newMap = Ext.create(MovementActivationPoint, config);
                    newMovement.maps.push(newMap);
                }
            });


            newMap = Ext.create(MovementActivationPoint, {
                id: Ext.id(),
                beginEndOrActivation: "END",
                pointId: eindpunt.getId()
            });

            newMovement.maps.push(newMap);

            this.addMovement(newMovement);
        }
    },

    /**
     * Voeg een nieuw of bestaand (voor)inmeldpunt toe aan movements van het
     * gegeven puntVanMovement.
     */
    addInmeldpunt: function(puntVanMovement, inmeldpunt, theMap, isBestaandInmeldpunt, isVoorinmeldpunt, movement) {
        if(!isBestaandInmeldpunt) {
            this.addPoint(inmeldpunt);
        }

        var pointMovements = null;
        if(movement){
            var mvmnt = this.getMovementById(movement);

            var map = this.findMapForPoint(mvmnt.getId(), puntVanMovement.getId());
            pointMovements=[{
                map: map,
                movement: mvmnt
            }];
        }else{
            pointMovements = this.findMovementsForPoint(puntVanMovement);
        }

        // Insert inmeldpunt op alle movements van uitmeldpunt

        Ext.Array.each(pointMovements, function(mvmtAndMap) {

            /* Maak een nieuwe MovementActivationPoint instance voor elke
             * movement, "theMap" is alleen gebruikt voor het form
             */
            var newMap = Ext.create(MovementActivationPoint, {
                id: Ext.id(),
                beginEndOrActivation: "ACTIVATION",
                commandType: isVoorinmeldpunt ? 3 : 1,
                pointId: inmeldpunt.getId()
            });
            // De voor dit inmeldpunt ingestelde inmeldpuntwaardes
            newMap.setConfig(objectSubset(theMap, ["distanceTillStopLine","virtualLocalLoopNumber", "triggerType"]));

            // De signal waardes komen van de MovementActivationPoint van het uitmeldpunt
            newMap.setConfig(objectSubset(mvmtAndMap.map, ["signalGroupNumber","vehicleTypes"]));

            mvmtAndMap.movement.getMaps().splice(0, 0, newMap);
        });
    },

    addBeginpunt: function(puntVanMovement, beginpunt, isBestaandBeginpunt) {
        if(!isBestaandBeginpunt) {
            this.addPoint(beginpunt);
        }

        var pointMovements = this.findMovementsForPoint(puntVanMovement);

        // Insert beginpunt op alle movements van uitmeldpunt

        Ext.Array.each(pointMovements, function(mvmtAndMap) {

            /* Maak een nieuwe MovementActivationPoint instance voor elke
             * movement, "theMap" is alleen gebruikt voor het form
             */
            var newMap = Ext.create(MovementActivationPoint, {
                id: Ext.id(),
                beginEndOrActivation: "BEGIN",
                pointId: beginpunt.getId()
            });

            mvmtAndMap.movement.getMaps().splice(0, 0, newMap);
        });
    },

    removeSingleCheckoutPoint : function(checkout, movement){

        var mvmnt = this.getMovementById(movement);

        var map = this.findMapForPoint(mvmnt.getId(), checkout.getId());
        var point = this.getPointById(map.getPointId());
        mvmnt.removeMapForPoint(point);

        var mvmnts = this.findMovementsForPoint(checkout);
        if(mvmnts.length === 0 ){
            this.removePoint(point);
        }
    },

    removeCheckoutPoint : function(checkout){

        var mvmnts = this.findMovementsForPoint(checkout);
        for(var i= 0 ; i < mvmnts.length ;i++){
            var movement = mvmnts[i].movement;
            for (var j = movement.getMaps().length-1 ;j >= 0 ; j--){
                var point = this.getPointById(movement.getMaps()[j].getPointId());
                if(point.getId() !== checkout.getId()){
                    var ms = this.findMovementsForPoint(point);
                    if(ms.length === 1){
                        this.removePoint(point);
                    }else{
                        movement.removeMapForPoint(point);
                    }
                }
            }
            this.removeMovement(movement.getId());
        }
        this.removePoint(checkout);
    },

    removeMovement: function(id){
        for (var i = 0 ; i < this.getMovements().length ;i++){
            if(this.getMovements()[i].getId() == id){
                this.getMovements().splice(i,1);
                break;
            }
        }
    },

    removePoint: function (point, movementId){
        var mvmnts = this.findMovementsForPoint(point);
        for(var i= 0 ; i < mvmnts.length ;i++){
            var movement = mvmnts[i].movement;
            if(movementId){
                if( parseInt(movementId) === movement.getId()){
                    movement.removeMapForPoint(point);
                }
            }else{
                movement.removeMapForPoint(point);
            }
        }


        var currentsMvmnts = this.findMovementsForPoint(point);
        if(currentsMvmnts.length === 0){
            for(var j = this.getPoints().length - 1 ; j >= 0 ; j--){
                if(this.getPoints()[j].getId() == point.getId()){
                    this.getPoints().splice(j,1);
                }
            }
        }
    },
    reorderMaps: function(mapsPerMovement){
        /*
         * {
         * movementId :{
         * [mapId1, mapId2, mapId3]
         * }
         */
        for(var key in mapsPerMovement){
            if(mapsPerMovement.hasOwnProperty(key)){
                this.reorderMovement(key, mapsPerMovement[key]);
            }
        }

    },
    reorderMovement : function(movementId,mapIds){
        var movement = this.getMovementById(movementId);
        var newMaps = [];
        var oldMaps = movement.getMaps();
        var oldIndexed = {};
        for (var i = 0 ; i < oldMaps.length ; i++){
            oldIndexed [oldMaps[i].getId()] = oldMaps[i];
        }

        Ext.Array.each(mapIds,function(id){
            var map = oldIndexed[id];
            newMaps.push(map);
        });
        movement.setMaps(newMaps);
    },
    /**
     * Geeft een GeoJSON object terug dat deze RSEQ representeert
     * @return GeoJSON object
     */
    toGeoJSON : function (onlyRSEQ, noRseq, vehicleType){
        if(onlyRSEQ === null || onlyRSEQ === undefined){
            onlyRSEQ = false;
        }
        var rseq ={
            type: "Feature",
            editable: this.getEditable(),
            geometry: this.getLocation(),
            properties:{
                id: this.getId(),
                description:this.getDescription(),
                validFrom:this.getValidFrom(),
                validUntil:this.getValidUntil(),
                karAddress:this.getKarAddress(),
                dataOwner:this.getDataOwner(),
                crossingCode: this.getCrossingCode(),
                town:this.getTown(),
                type:this.getType(),
                className: this.$className,
                validationErrors:this.getValidationErrors(),
                memo: this.getMemo(),
                readyForExport: this.getReadyForExport()
            }
        };
        if(onlyRSEQ){
            return rseq;
        }
        var points = new Array();
        var pointsLookup = {};
        if (this.getPoints()) {
            for (var i = 0; i < this.getPoints().length; i++) {
                var point = this.getPoints()[i];
                pointsLookup[point.getId()] = point.toGeoJSON();
            }
        }
        var mvmnts = this.getMovements();
        if(mvmnts){
            for(var i = 0 ; i < mvmnts.length ;i++){
                var mvmnt = mvmnts[i];
                if(mvmnt.getVehicleType() === vehicleType){
                    var maps = mvmnt.getMaps();
                    for(var j = 0 ; j < maps.length;j++){
                        var map = maps[j];
                        var point = pointsLookup[map.getPointId()];
			points.push(point);
                    }
                }
            }
        }
        if(!noRseq){
            points.push(rseq);
        }
        var json = {
            "type" : "FeatureCollection",
            "features" :points
        };
        return json;
    },
    /**
     * Geeft een JSON object terug dat deze RSEQ representeert
     * @return json object
     */
    toJSON: function() {
        var j = objectSubset(this, ["id", "description", "validFrom","validUntil",
            "karAddress", "dataOwner", "crossingCode", "town", "type",
            "location","memo","attributes","readyForExport"]);

        j.points = [];
        Ext.Array.each(this.getPoints(), function(point) {
            j.points.push(point.toJSON());
        });

        j.movements = [];
        Ext.Array.each(this.getMovements(), function(mvmt) {

            // Do not save movements without signalgroupnumber
            // Should not exist, but has happened (movement with only endpoint)
            var signalGroupNumber = null;
            for(var i = 0 ; i < mvmt.getMaps().length ;i++){
                var map = mvmt.getMaps()[i];
                if(map.getSignalGroupNumber () !== null){
                    signalGroupNumber = map.getSignalGroupNumber();
                    break;
                }
            }

            if(signalGroupNumber === null) {
                console.log("Not saving movement with no signal group number!!! ", mvmt.toJSON());
            } else {
                j.movements.push(mvmt.toJSON());
            }
        });

        return j;
    },
    getOverviewJSON : function(){
        var signalGroups = {};
        var movements = this.getMovements();
        for(var i = 0 ; i < movements.length; i++){
            var mvmnt = movements[i];
            var signalGroupNumber = null;
            for(var j = 0 ; j < mvmnt.getMaps().length ;j++){
                var map = mvmnt.getMaps()[j];
                if(map.getSignalGroupNumber () !== null){
                    signalGroupNumber = map.getSignalGroupNumber();
                    if(!signalGroups.hasOwnProperty(signalGroupNumber)){
                        signalGroups[signalGroupNumber] = new Object();
                    }

                    if(!(signalGroups[signalGroupNumber]).hasOwnProperty( mvmnt.getId())){

                         signalGroups[signalGroupNumber][ mvmnt.getId()] = new Object();
                         signalGroups[signalGroupNumber][ mvmnt.getId()]["id"] = mvmnt.getId();
                         signalGroups[signalGroupNumber][ mvmnt.getId()]["points"] = new Array();
                    }
                    break;

                }
            }
            if(signalGroupNumber !== null){
                for(var k = 0 ; k < mvmnt.getMaps().length;k++){
                    var point = this.getPointById(mvmnt.getMaps()[k].getPointId());
                    if(point){
                        signalGroups[signalGroupNumber][ mvmnt.getId()]["points"].push(point);
                    }
                }
            }
        }
        return signalGroups;
    },
    areVehicletypesConsistent : function(){
        for (var i = 0 ; i < this.getMovements().length; i++){
            var movement = this.getMovements()[i];
            if(!movement.isConsistent()){
                return false;
            }
        }
        return true;
    },
    hasEndpointsNearCheckoutPoints: function(threshold){
        for(var i = 0 ; i < this.getMovements().length; i++){
            var m = this.getMovements()[i];
            if(m.getVehicleType() !== "OV"){
                continue;
            }
            var end, checkout;
            for(var j = 0 ; j < m.getMaps().length ;j ++){
                var map = m.getMaps()[j];
                if(map.getCommandType() === 2){
                    checkout = map;
                }
                if(map.getBeginEndOrActivation() === "END"){
                    end = map;
                }
                
            }
            
            if(end && checkout){
                var endPoint = this.getPointById(end.getPointId());
                var checkoutPoint = this.getPointById(checkout.getPointId());
                var dist = this.distance(endPoint.getGeometry().coordinates, checkoutPoint.getGeometry().coordinates);
                if(dist < threshold){
                    return true;
                }
            }
        }
        return false;
    },
    distance: function(c1,c2){
        var distance = Math.sqrt(Math.pow(c1[0] - c2[0], 2) + Math.pow(c1[1] - c2[1], 2));
        return distance;
    }
    
});

/**
 * Class voor Point
 */
Ext.define('Point', {
    config:{
        id: null,
        geometry:null,
        label:null,
        movementNumbers:null,
        type:null,
        nummer:null,
        signalGroupNumbers:null
    },
    /**
     * @constructor
     * @param config.id id van dit punt
     * @param config.geometry geometry van dit punt
     * @param config.label het label van dit punt
     * @param config.movementNumbers nummers van de bijhorende movements
     * @param config.type type van dit punt
     * @param config.nummer nummer van dit punt
     * @param config.signalGroupNumbers nummers van de bijhorende signaal groepen
     */
    constructor: function(config) {
        if(!config.label){
            config.label = "";
        }
        if(!config.id) {
            config.id = Ext.id();
        }
        this.initConfig(config);
    },
    /**
     * Geeft dit object als GeoJSON terug
     * @return een GeoJSON representatie van dit object.
     */
    toGeoJSON : function(){
        var json = {
            type: "Feature",
            geometry : this.getGeometry(),
            properties:{
                id: this.getId(),
                label:this.getLabel(),
                movementNumbers:this.getMovementNumbers(),
                type:this.getType(),
                nummer:this.getNummer(),
                signalGroupNumbers:this.getSignalGroupNumbers(),
                className: this.$className
            }
        };
        return json;
    },
    toJSON: function() {
        return objectSubset(this, ["id", "geometry", "nummer", "label"]);
    }
});
/**
 * Class van een activatie punt van een movement.
 */
Ext.define('MovementActivationPoint', {
    config:{
        id: null,
        beginEndOrActivation:null,
        commandType:null,
        distanceTillStopLine:null,
        pointId:null,
        signalGroupNumber:null,
        virtualLocalLoopNumber: null,
        triggerType:null,
        vehicleTypes:null,
        direction:null
    },
    /**
     *@constructor
     *@param config.id id van dit activatie punt
     *@param config.beginEndOrActivation BEGIN,END of ACTIVATION
     *@param config.commandType het commando type
     *@param config.distanceTillStopLine afstand tot stoplijn
     *@param config.pointId het id van het bijhorende punt
     *@param config.signalGroupNumber het signaal group nummer waar dit object bij hoort
     *@param config.virtualLocalLoopNumber
     *@param config.triggerType type trigger
     *@param config.vehicleTypes typen voertuigen waar dit punt op moet reageren.
     */
    constructor: function(config) {
        if(!config.triggerType) {
            config.triggerType = 'STANDARD';
        }
        if(!config.id) {
            config.id = Ext.id();
        }
        this.initConfig(config);
    },
    /**
     * Geeft een JSON object terug van dit object
     * @return een JSON object dat dit object representeert.
     */
    toJSON: function() {
        var keys = ["id", "beginEndOrActivation", "pointId"];
        if(this.getBeginEndOrActivation() == "ACTIVATION") {
            keys = Ext.Array.merge(keys, ["commandType", "distanceTillStopLine",
                "triggerType", "signalGroupNumber", "virtualLocalLoopNumber",
                "vehicleTypes", "direction"]);
        }
        return objectSubset(this, keys);
    }
});
/**
 * Class voor de movement
 */
Ext.define('Movement', {
    config:{
        id: null,
        maps:null,
        nummer:null,
        vehicleType:null
    },
    /**
     * @constructor
     * @param config.id het id van de movement
     * @param config.maps een array van movement activatie punten
     * @param config.nummer nummer van de movement
     */
    constructor: function(config) {
        this.initConfig(config);
    },
    /**
     * voeg een Movement Activatie Punt toe.
     * @param map het Movement Activatie Punt dat toegevoegd moet worden.
     */
    addMap : function (map){
       /* if(!this.getMaps()){
            this.getMaps() = new Array();
        }*/
        this.getMaps().push(map);
    },
     /**
     * Voeg een punt toe na een ander punt
     * @param mapToAdd Het punt dat moet toegevoegd worden aan de rseq.
     * @param after Het punt waar het toe te voegen punt achter geplaatst moet worden.
     */
    addMapAfter : function (mapToAdd, after){
        var index = this.getMaps().length;
        for(var i = 0 ; i < this.getMaps().length;i++){
            if(this.getMaps()[i].pointId == after){
                index = i+1;
                break;
            }
        }

        this.getMaps().splice(index,0,mapToAdd);
    },
    /**
     * Geeft een JSON object van dit object terug
     * @return een JSON object dat dit object representeert
     */
    toJSON: function() {
        var j = objectSubset(this, ["id", "nummer"]);

        j.maps = [];
        Ext.Array.each(this.getMaps(), function(map) {
            j.maps.push(map.toJSON());
        });
        return j;
    },
    getMapForPoint : function (point){
        var map = null;
        for (var i = 0 ; i < this.getMaps().length; i++){
            if(this.getMaps()[i].getPointId() == point.getId()){
                map = this.getMaps()[i];
                break;
            }
        }
        return map;
    },
    getMapsForSignalgroup: function(signalgroupNumber){
        var maps = [];
        for (var i = 0 ; i < this.getMaps().length; i++){
            if(this.getMaps()[i].getSignalGroupNumber() === signalgroupNumber){
                maps.push(this.getMaps()[i]);
            }
        }
        return maps;
    },
    removeMapForPoint:function(point){
        for (var i = this.getMaps().length-1 ; i >= 0 ; i--){
            if(this.getMaps()[i].getPointId() === point.getId()){
                this.getMaps().splice(i,1);
            }
        }
    },
    isConsistent : function(){
        var inmeldpunt = null;
        var uitmeldpunt = null;
        for ( var i = 0 ; i < this.getMaps().length; i++){
            var map = this.getMaps()[i];
            if(map.getBeginEndOrActivation() === "ACTIVATION"){
                if(map.getCommandType() === 1){
                    inmeldpunt = map;
                }else if(map.getCommandType() === 2){
                    uitmeldpunt = map;
                }
            }
        }
        if (inmeldpunt && uitmeldpunt) {
            var diffAB = Ext.Array.difference(uitmeldpunt.getVehicleTypes(), inmeldpunt.getVehicleTypes());
            var diffBA = Ext.Array.difference(inmeldpunt.getVehicleTypes(), uitmeldpunt.getVehicleTypes());
            return diffAB.length === 0 && diffBA.length === 0;
        } else {
            return true;
        }

    },
    equals : function(points, includeEndPoints){
        if (points.length === this.getMaps().length) {
            for (var j = 0; j < this.getMaps().length; j++) {
                var foundInner = false;
                for (var i = 0; i < points.length; i++) {
                    if (this.getMaps()[j].pointId === points[i]) {
                        foundInner = true;
                        break;
                    }
                }
                if (!foundInner) {
                    if(includeEndPoints && this.getMaps()[j].beginEndOrActivation === "END"){
                        return false;
                    }
                }
            }
            return true;
        }else{
            return false;
        }
    }
});
/**
 * Maak een Roadside Equipment object op basis van een JSON object
 * @param json het json object
 * @return een RSEQ object.
 */
function makeRseq (json){
    var rseq = Ext.create("RSEQ",json);
    var movements = makeMovements(json.movements);
    var points = makePoints(json.points);
    rseq.setMovements(movements);
    rseq.setPoints(points);
    return rseq;
}
/**
 * Maak movements objecten op basis van een JSON array
 * @param json de json array
 * @return een array van movement objecten.
 */
function makeMovements(json){
    var movements = new Array();
    for (var i = 0 ; i < json.length; i++){
        var mvJson = json[i];
        var movement = makeMovement(mvJson);
        var maps = makeMAPs(mvJson.maps);
        movement.setMaps(maps);
        movements.push(movement);
    }

    return movements;
}
/**
 * Maak Movement Activatie Punt objecten op basis van een JSON array
 * @param json de json array
 * @return een array van Movement Activatie Punten
 */
function makeMAPs(json){
    var maps = new Array();
    for(var i = 0 ; i < json.length ; i++){
        var map = Ext.create("MovementActivationPoint",json[i]);
        maps.push(map);
    }
    return maps;
}

/**
 * Maak een Movement object op basis van een JSON object
 * @param json het json object
 * @return een movement object.
 */
function makeMovement(json){
    return Ext.create("Movement",json);
}

/**
 * Maak een array van Point objecten op basis van een JSON array
 * @param json de json array
 * @return een array van Points.
 */
function makePoints(json){
    var points = new Array();
    for(var i = 0 ; i < json.length ; i++){
        var point = Ext.create("Point",json[i]);
        points.push(point);
    }
    return points;
}
