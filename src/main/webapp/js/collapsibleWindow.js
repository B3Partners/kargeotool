/* global Ext */

/**
 * KAR Geo Tool - applicatie voor het registreren van KAR meldpunten
 *
 * Copyright (C) 2009-2017 B3Partners B.V.
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
Ext.define("CollapsibleWindow", {
    alias: ['widget.collapsiblewindow'],
    extend: "Ext.util.Observable",
    statics: {
        dockedWindows: [],
        dock: null
    },
    config: {
        width: 275,
        height: 200,
        x: -10,
        y: 40,
        items: [],
        title: "",
        id: "",
        layout: null
    },
    window: null,
    DEFAULT_DOCKED_WIDTH: 200,
    DEFAULT_DOCKED_HEIGHT: 50,
    STATE_VERSION: 1,
    constructor: function(config) {
        this.initConfig(config);
        CollapsibleWindow.superclass.constructor.call(this, config);
        var window = this.createWindow();
        var state = Ext.state.Manager.get(this.config.id);
        if(!state || (state._state_version || -1) < this.STATE_VERSION) {
            window.show();
            window.alignTo("editorBody", "tr-tr", [ this.config.x, this.config.y ]);
        }
    },
    createWindow: function() {
        var config = {
            itemId: this.config.id,
            closable: false,
            titleCollapse:true,
            collapsible: true,
            animCollapse: false,
            stateful: {
                width: true,
                height: true,
                x: true,
                y: true,
                collapsed: false,
                _kar_position: true,
                _state_version: true
            },
            constrain: true,
            stateId: this.config.id,
            collapseDirection: 'bottom',
            width: this.config.width,
            height: this.config.height,
            x: this.config.x,
            y: this.config.y,
            items: this.config.items,
            title: this.config.title,
            layout: this.config.layout || {
                type: 'vbox',
                align: 'stretch'
            },
            onEsc: Ext.emptyFn,
            scrollable: true,
            listeners: {
                scope: this,
                beforecollapse: this.windowCollapse,
                beforeexpand: this.windowExpand,
                staterestore: this.stateRestored,
                beforeclose: function() { return false; }
            }
        };
        this.window = Ext.create('Ext.window.Window', config);
        this.window._state_version = this.STATE_VERSION;
        return this.window;
    },
    windowCollapse: function(window) {
        var statics = this.statics();
        statics.dockedWindows.push(window);
        window._kar_position = {
            x: window.getX(),
            y: window.getY(),
            width: window.getWidth(),
            height: window.getHeight(),
            collapsed: true
        };
        this.dockWindow(window, statics.dockedWindows.length);
    },
    windowExpand: function(window) {
        var statics = this.statics();
        Ext.Array.remove(statics.dockedWindows, window);
        if(!window._kar_position) {
            window._kar_position = {
                x: this.config.x,
                y: this.config.y,
                width: this.config.width,
                height: this.config.height
            };
        }
        window.animate({
            to: {
                x: window._kar_position.x,
                y: window._kar_position.y,
                width: window._kar_position.width,
                height: window._kar_position.height
            }
        });
        window._kar_position.collapsed = false;
        this.repositionOtherDockedWindows(statics);
    },
    repositionOtherDockedWindows: function(statics) {
        for(var i = 0; i < statics.dockedWindows.length; i++) {
            this.dockWindow(statics.dockedWindows[i], i + 1);
        }
    },
    dockWindow: function(window, windowPosition) {
        var xPos = -20 + -1*this.DEFAULT_DOCKED_WIDTH;
        var yPos = windowPosition * this.DEFAULT_DOCKED_HEIGHT;
        var alignToPos = window.getAlignToXY("editorBody", "tr", [xPos, yPos]);
        window.animate({
            to: {
                x: alignToPos[0],
                y: alignToPos[1],
                width: this.DEFAULT_DOCKED_WIDTH
            }
        });
    },
    stateRestored: function(window, state) {
        window.show();
        if(state._kar_position && state._kar_position.collapsed) {
            function initialStateCollapse() {
                window.un("collapse", initialStateCollapse);
                window._kar_position = state._kar_position;
            }
            window.on("collapse", initialStateCollapse);
            window.collapse();
        }
    }
});