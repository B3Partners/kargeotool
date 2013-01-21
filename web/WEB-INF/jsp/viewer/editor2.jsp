<%@include file="/WEB-INF/jsp/taglibs.jsp" %>
<!DOCTYPE html> 
<%@page errorPage="/WEB-INF/jsp/commons/errorpage.jsp" %>
<stripes:layout-render name="/WEB-INF/jsp/commons/siteTemplate2.jsp">

    <stripes:layout-component name="headerlinks" >
        <%@include file="/WEB-INF/jsp/commons/headerlinks2.jsp" %>

    </stripes:layout-component>
    <stripes:layout-component name="content">

        <script type="text/javascript" src="${contextPath}/js/layout.js"></script>
        <script type="text/javascript">

          
            var editorActionBeanUrl = "<stripes:url beanclass="nl.b3p.kar.stripes.EditorActionBean" />";
            var contextPath = "${contextPath}";
   
/*
            setOnload(function() 
            {
                document.getElementById("walapparaatnummer").focus();
                if("${magWalapparaatMaken}" == "true"){
                    document.getElementById("newRseq").disabled = false;
                }else{
                    document.getElementById("newRseq").disabled = true;
                }
            });
            */
        </script>

        <div id="leftbar">
            
            <div id="searchform">
                Adres verkeerssysteem: <input id="addressInput" name="address" value="9999" size="6"> <input type="button" value="Zoeken" onclick="editor.loadRseqInfo({karAddress:Ext.get('addressInput').getValue()});"><br />
                Adres zoeken: <input id="geocodeAddressInput" name="geocode_address" value="" size="17"> <input type="button" value="Zoeken" onclick="editor.geocode(Ext.get('geocodeAddressInput').getValue());">
                <div id="geocoderesults"></div>
            </div>
            
            <div id="rseqInfoPanel">
                Huidig geselecteerde VRI: <span id="context_vri"></span>
                
            </div>
            
            <%--div id="tree">
                <div id="treeTop">
                    <div id="treeTitel">Objectenboom</div>
                    <div id="loading"><html:img page="/images/ajax-loader.gif" module=""/></div>
                    <label title="asdfwe">Zoek op walapparaatnummer:</label> <input title="asdf" id="walapparaatnummer" type="text" size="10" onkeypress="walapparaatnummerKeyPressed(event);">
                    <input type="button" name="zoekWalapparatuur" value="Zoeken" onclick="zoekWalapparatuur()">
                </div>
                <div id="objectTree"></div>
                <div id="options">
                    <input id="zoomButton" type="button" value="Zoom naar object" title="Zoom in op een geselecteerd object uit de objectenboom. Met Auto-zoom aangevinkt wordt direct ingezoomd op een geselecteerd object. Het zoomniveau is instelbaar door de minimale diameter (in meters) op te geven van het gebied rond het object. Klik na het wijzigen van de diameter op Zoom naar object om te zoomen naar het opgegeven niveau." onclick="options_zoomToObject();">
                    <label><input id="autoZoom" type="checkbox" value="autoZoom" checked="true">Auto-zoom</label>
                    <input id="zoomExtent" name="zoomExtent" type="text" value="375" size="2" maxlength="4" style="text-align: right"> <label><input id="showSelected" type="checkbox" value="showSelected" onclick="toggleVisibleSelected()">Toon geselecteerden</label>
                    <br>
                    <b>Nieuwe:</b>
                    <input id="newRseq" type="button" value="Walapparatuur" title="Selecteer deze optie voor het toevoegen van een nieuw walapparaat. Het is niet mogelijk signaalgroepen of triggerpunten aan te maken zonder eerst een walapparaat te selecteren of aan te maken." onclick="newRseq()">
                    <input id="newAg" type="button" value="Signaalgroep" title="Selecteer deze optie voor het vastleggen van de stopstreep (van signaalgroep) bij de geselecteerde VRI. (Het is niet mogelijk een signaalgroep aan te maken zonder eerst een walapparaat te selecteren of aan te maken.)" disabled="true" onclick="newAg()">
                    <input id="newA" type="button" value="Triggerpunt" title="Selecteer deze optie voor het koppelen van een nieuw triggerpunt aan de geselecteerde signaalgroep. (Het is niet mogelijk een triggerpunt aan te maken zonder eerst een signaalgroep te selecteren of aan te maken.)" disabled="true" onclick="newA()">
                </div>
            </div--%>

            <div id="form">
                <div id="formContainer">
                    Formcontainer
                    <span style="display: none">Status: <span id="formStatus" style="font-weight: bold">Geen object</span></span>
                    <iframe frameborder="0" name="form" src="${contextPath}/empty.jsp"></iframe>
                </div>
            </div>

        </div>

        <div id="kaart">
            <div id="map" style="width: 100%; height: 100%;"></div>
            <!--div id="overview" style="width:19%;border:1px solid #000; float:right; height:300px;overflow:hidden;"></div-->
        </div>
        
        <div id="rightbar">
            <div id="legend">
                <div id="walapparatuur" class="legendseparator">
                    <input type="checkbox" checked="checked" onclick="toggleLayer('walapparatuur');"/> Walapparatuur<br/>
                    <img src="<c:url value="/images/"/>icons/vri.png" alt="VRI" class="legendimg" /> VRI<br />
                    <img src="<c:url value="/images/"/>icons/wri.png" alt="Waarschuwingssysteem" class="legendimg" /> Waarschuwingssysteem<br />
                    <img src="<c:url value="/images/"/>icons/afsluitingssysteem.png" alt="Afsluitingssysteem" class="legendimg" /> Afsluitingssysteem<br />
                </div>
                <div id="signaalgroepen" class="legendseparator">
                    <input type="checkbox" checked="checked" onclick="toggleLayer('signaalgroepen');"/> Signaalgroepen<br/>
                    <img src="http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map&amp;version=1.1.1&amp;service=WMS&amp;request=GetLegendGraphic&amp;layer=signaalgroepen&amp;format=image/png"/>
                </div>
                <div id="triggerpunten" class="legendseparator">
                    <input type="checkbox" checked="checked" onclick="toggleLayer('triggerpunten');"/> Triggerpunten<br/>
                    <img src="<c:url value="/images/"/>/icons/radio_zwart.png" alt="Onbekend" class="legendimg" /> Onbekend<br />
                    <img src="<c:url value="/images/"/>/icons/radio_groen.png" alt="Inmeldpunt" class="legendimg" /> Inmeldpunt<br />
                    <img src="<c:url value="/images/"/>/icons/radio_rood.png" alt="Uitmeldpunt" class="legendimg" /> Uitmeldpunt<br />
                    <img src="<c:url value="/images/"/>/icons/radio_blauw.png" alt="Vooraanmeldpunt" class="legendimg" /> Vooraanmeldpunt<br />
                </div>
                <div id="starteindpunten" class="legendseparator">
                    <input type="checkbox" checked="checked" /> Begin- en eindpunten<br/>
                    <img src="<c:url value="/images/"/>/icons/beginpunt.png" alt="Beginpunt" class="legendimg" /> Beginpunt<br />
                    <img src="<c:url value="/images/"/>/icons/eindpunt.png" alt="Eindpunt" class="legendimg" /> Eindpunt<br />
                </div><br/>
                <strong>OV-informatie</strong><br/>
                <input type="checkbox" onclick="toggleLayer('buslijnen');"/>Buslijnen<br/>
                <div style="display:none;" id="buslijnen"><img src="http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map&amp;version=1.1.1&amp;service=WMS&amp;request=GetLegendGraphic&amp;layer=buslijnen&amp;format=image/png"/></div>
                <input type="checkbox" onclick="toggleLayer('bushaltes');"/>Bushaltes<br/>
                <div style="display:none;" id="bushaltes"><img src="http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map&amp;version=1.1.1&amp;service=WMS&amp;request=GetLegendGraphic&amp;layer=bushaltes_symbol&amp;format=image/png"/></div><br/>
                <strong>Achtergrond</strong><br/>
                <input type="checkbox" onclick="toggleLayer('Luchtfoto');"/>Luchtfoto<br/>
                <input type="checkbox" checked="checked" onclick="toggleLayer('BRT');"/>BRT<br/>
            </div>
            <script type="text/javascript" src="<c:url value="/js/editor.js"/>"></script>
            <script type="text/javascript">
                
                var mapfilePath = "http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map";
                
                var editor = null;
                Ext.onReady(function() {
                    editor = Ext.create(Editor, "map", mapfilePath);    
                });
                
                function toggleLayer(layer) {
                    var legend = document.getElementById(layer);
                    var visible = editor.olc.isLayerVisible(layer);

                    editor.olc.setLayerVisible(layer,!visible);
                    if(legend){
                        var attr = !visible ?  'block' : 'none' ;
                        legend.setAttribute("style", 'display:' +attr);
                    }
                }
                
/*                
                var domId = 'map';
                var oc = new ol();
                var cm = new ContextMenu();
                cm.createMenus(domId);
                oc.createMap(domId);
                oc.addLayer("TMS","BRT",'http://geodata.nationaalgeoregister.nl/tiles/service/tms/1.0.0','brtachtergrondkaart', true, 'png8');
                oc.addLayer("TMS","Luchtfoto",'http://luchtfoto.services.gbo-provincies.nl/tilecache/tilecache.aspx/','IPOlufo', false,'png?LAYERS=IPOlufo');
                oc.addLayer("WMS","walapparatuur","http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map",'walapparatuur', false);
                oc.addLayer("WMS","signaalgroepen","http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map",'signaalgroepen', false);
                oc.addLayer("WMS","roadside_equipment2","http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map",'roadside_equipment2', false);
                oc.addLayer("WMS","activation_point2","http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map",'activation_point2', false);
                oc.addLayer("WMS","triggerpunten","http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map",'triggerpunten', false);
                oc.addLayer("WMS","buslijnen","http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map",'buslijnen', false);
                oc.addLayer("WMS","bushaltes","http://x13.b3p.nl/cgi-bin/mapserv?map=/home/matthijsln/geo-ov/transmodel_connexxion_edit.map",'bushaltes', false);
                
                if(!oc.setCenterFromHash()) {
                    oc.map.zoomToMaxExtent();
                }

                function toggleLayer(layer){
                    var legend = document.getElementById(layer);
                    var visible = oc.isLayerVisible(layer);

                    oc.setLayerVisible(layer,!visible);
                    if(legend){
                        var attr = !visible ?  'block' : 'none' ;
                        legend.setAttribute("style", 'display:' +attr);
                    }
                }
*/                    
            </script>
        </div>

    </stripes:layout-component>
</stripes:layout-render>