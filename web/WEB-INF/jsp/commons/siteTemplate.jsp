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
                crossing:                imgPath + 'icons/vri.png',
                crossing_selected:       imgPath + 'icons/vri_selected.png',
                guard:                   imgPath + 'icons/wri.png',
                guard_selected:          imgPath + 'icons/wri_selected.png',
                bar:                     imgPath + 'icons/afsluitingssysteem.png',
                bar_selected:            imgPath + 'icons/afsluitingssysteem_selected.png',

                punt:                    imgPath + '/icons/radio_zwart.png',
                punt_selected:           imgPath + '/icons/radio_zwart_selected.png',
                startPunt:               imgPath + '/icons/beginpunt.png',
                startPunt_selected:      imgPath + '/icons/beginpunt_selected.png',
                eindPunt:                imgPath + '/icons/eindpunt.png',
                eindPunt_selected:       imgPath + '/icons/eindpunt_selected.png',

                voorinmeldPunt:          imgPath + '/icons/radio_blauw.png',
                voorinmeldPunt_selected: imgPath + '/icons/radio_blauw_selected.png',
                inmeldPunt:              imgPath + '/icons/radio_groen.png',
                inmeldPunt_selected:     imgPath + '/icons/radio_groen_selected.png',
                uitmeldPunt:             imgPath + '/icons/radio_rood.png',
                uitmeldPunt_selected:    imgPath + '/icons/radio_rood_selected.png'
            };
        </script>
        <script type="text/javascript" src="${contextPath}/openlayers/OpenLayers.js"></script>
        <script type="text/javascript" src="${contextPath}/js/ext/ext-all-debug.js"></script>
        <script type="text/javascript" src="${contextPath}/js/models.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/OpenLayersController.js" ></script>
        <script type="text/javascript" src="${contextPath}/js/contextmenu.js" ></script>
        <link rel="stylesheet" href="${contextPath}/js/ext/ext-all.css" type="text/css" media="screen" />
        <link rel="stylesheet" href="${contextPath}/styles/geo-ov.css" type="text/css" media="screen" />
    </head>
    <body class="editor" id="editorBody">
        <div id="viewportcontainer">
            <stripes:layout-component name="headerlinks"/>
            <div id="contentcontainer">
                <stripes:layout-component name="content"/>
            </div>
        </div>
    </body>
</html>
</stripes:layout-definition>