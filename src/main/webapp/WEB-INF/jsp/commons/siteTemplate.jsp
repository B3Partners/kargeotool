<%--
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
--%>

<%@include file="/WEB-INF/jsp/taglibs.jsp" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage="/WEB-INF/jsp/commons/errorpage.jsp" %>

<!DOCTYPE html>

<stripes:layout-definition>
    <html>
    <head>
        <title><fmt:message key="index.title"/></title>
        <script type="text/javascript">
            var imgPath = "<c:url value="/images/"/>";
            var karTheme = {
                crossing:                       imgPath + 'icons/vri.png',
                crossing2:                       imgPath + 'icons/vri.svg',
                crossing_attachment:            imgPath + 'icons/vri_attachment.png',
                crossing_selected:              imgPath + 'icons/vri_selected.png',
                crossing_selected_attachment:   imgPath + 'icons/vri_selected_attachment.png',
                guard:                          imgPath + 'icons/wri.png',
                guard_attachment:               imgPath + 'icons/wri_attachment.png',
                guard_selected:                 imgPath + 'icons/wri_selected.png',
                guard_selected_attachment:      imgPath + 'icons/wri_selected_attachment.png',
                bar:                            imgPath + 'icons/afsluitingssysteem.png',
                bar_attachment:                 imgPath + 'icons/afsluitingssysteem_attachment.png',
                bar_selected:                   imgPath + 'icons/afsluitingssysteem_selected.png',
                bar_selected_attachment:        imgPath + 'icons/afsluitingssysteem_selected_attachment.png',

                punt:                           imgPath + '/icons/radio_zwart.png',
                punt_selected:                  imgPath + '/icons/radio_zwart_selected.png',
                startPunt:                      imgPath + '/icons/beginpunt.png',
                startPunt_selected:             imgPath + '/icons/beginpunt_selected.png',
                eindPunt:                       imgPath + '/icons/eindpunt.png',
                eindPunt_selected:              imgPath + '/icons/eindpunt_selected.png',
                cursor_delete:                  imgPath + '/silk/cursor_delete.png',

                voorinmeldPunt:                 imgPath + '/icons/radio_blauw.png',
                voorinmeldPunt_selected:        imgPath + '/icons/radio_blauw_selected.png',
                inmeldPunt:                     imgPath + '/icons/radio_groen.png',
                inmeldPunt_selected:            imgPath + '/icons/radio_groen_selected.png',
                uitmeldPunt:                    imgPath + '/icons/radio_rood.png',
                uitmeldPunt_selected:           imgPath + '/icons/radio_rood_selected.png',
                
                cluster:                        imgPath + '/icons/cluster.png',
                
                richting:                       imgPath + '/icons/richting.png',
                copy_movment:                   imgPath + '/silk/table_multiple.png',
                gps:                            imgPath + '/icons/gps.png'
            };
        </script>
        <script type="text/javascript" src="${contextPath}/openlayers/OpenLayers.js"></script>
        <script type="text/javascript" src="${contextPath}/js/ext/ext-all${param.debug == true ? '-debug' : ''}.js"></script>
        <script type="text/javascript" src="${contextPath}/js/models.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/styles.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/measureTooltip.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/measure.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/search.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/OpenLayersController.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/contextmenu.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/changemanager.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/collapsibleWindow.js" ></script>
        <link rel="stylesheet" href="${contextPath}/js/ext/resources/css/crisp/theme-crisp-all.css" type="text/css" media="screen" />
        <link rel="stylesheet" href="${contextPath}/styles/geo-ov.css" type="text/css" media="screen" />
        <stripes:layout-component name="head"/>        
    </head>
    <body class="editor" id="editorBody">
        <div id="viewportcontainer">
            <stripes:layout-component name="headerlinks"/>
            <div id="contentcontainer">
                <noscript><h1><font color="red">Uw browser ondersteund geen javascript, of u heeft javascript uitgezet. Zet javascript aan om de website te gebruiken.</font></h1></noscript>
                <stripes:layout-component name="content"/>
                
            </div>
        </div>
    </body>
</html>
</stripes:layout-definition>
