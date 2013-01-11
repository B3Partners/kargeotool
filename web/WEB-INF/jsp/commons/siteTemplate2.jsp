<%@include file="/WEB-INF/jsp/taglibs.jsp" %>
<%@page errorPage="/WEB-INF/jsp/commons/errorpage.jsp" %>

<!DOCTYPE HTML>

<stripes:layout-definition>
    <html>
    <head>
        <%--
        <title><fmt:message key="index.title"/></title>
        <script language="JavaScript" type="text/JavaScript" src="<html:rewrite page="/js/validation.jsp" module=""/>"></script>
        <link rel="stylesheet" href="<html:rewrite page="/styles/geo-ov.css" module=""/>" type="text/css" media="screen" />
        <link rel="stylesheet" href="<html:rewrite page="/styles/jquery-ui-1.7.2.custom.css" module=""/>" type="text/css" media="screen" />
        <script type="text/javascript" src="<html:rewrite page="/js/json2.js" module=""/>"></script>
        <script type="text/javascript" src="<html:rewrite page="/js/utils.js" module=""/>"></script>
        <script type="text/javascript" src="<html:rewrite page="/js/simple_treeview.js" module=""/>"></script>
        <script type="text/javascript" src="<html:rewrite page="/dwr/engine.js" module=""/>"></script>
        <script type="text/javascript" src="<html:rewrite page="/dwr/interface/Editor.js" module=""/>"></script>
        <script type="text/javascript" src="<html:rewrite page="/js/swfobject.js" module=""/>"></script>
        <script type="text/javascript" src="<html:rewrite page='/js/jquery-1.3.2.min.js' module=''/>"></script>
        <script type="text/javascript" src="<html:rewrite page='/js/jquery-ui-1.7.2.custom.min.js' module=''/>"></script>
        <script type="text/javascript" src="<html:rewrite page='/openlayers/OpenLayers.js' module=''/>"></script>
        <script type="text/javascript" src="<html:rewrite page="/js/OpenLayersController.js" module=""/>"></script>
        
        --%>

        <title><fmt:message key="index.title"/></title>
        <script type="text/javascript" src="${contextPath}/js/json2.js"></script>
        <script type="text/javascript" src="${contextPath}/js/utils.js"></script>
        <script type="text/javascript" src="${contextPath}/js/simple_treeview.js"></script>
        <script src="${contextPath}/js/jquery-1.3.2.min.js"></script>
        <script src="${contextPath}/js/jquery-ui-1.7.2.custom.min.js"></script>
        <script type="text/javascript" src="${contextPath}/openlayers/OpenLayers.js"></script>
        <script type="text/javascript" src="${contextPath}/js/OpenLayersController.js" ></script>
        <script type="text/javascript" src="${contextPath}/dwr/engine.js"></script>
        <script type="text/javascript" src="${contextPath}/dwr/interface/Editor.js"></script>
        <link rel="stylesheet" href="${contextPath}/styles/geo-ov.css" type="text/css" media="screen" />
        <!--[if IE 7]> <link href="${contextPath}/styles/geo-ov-ie7.css" rel="stylesheet" media="screen" type="text/css" /> <![endif]-->
        
        <script type="text/javascript" src="${contextPath}/js/ext/ext-all.js"></script>
    </head>
    <body class="editor" id="editorBody">
        <stripes:layout-component name="headerlinks"/>
        <!--div id="contentcontainer">
            <stripes:layout-component name="content"/>
        </div-->
    </body>
</html>
</stripes:layout-definition>
