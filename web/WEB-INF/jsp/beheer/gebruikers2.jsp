<%@include file="/WEB-INF/jsp/taglibs.jsp" %>
<%@page errorPage="/WEB-INF/jsp/commons/errorpage.jsp" %>

<%@page import="nl.b3p.kar.hibernate.Gebruiker" %>

<stripes:layout-render name="/WEB-INF/jsp/commons/siteTemplate2.jsp">

    <stripes:layout-component name="headerlinks" >
        <%@include file="/WEB-INF/jsp/commons/headerlinks2.jsp" %>
    </stripes:layout-component>
    <stripes:layout-component name="content">

<h2>Gebruikersbeheer 2</h2>

<stripes:form beanclass="nl.b3p.kar.stripes.GebruikersActionBean">
    <stripes:hidden name="gebruiker"/>
    
    <c:if test="${!empty actionBean.gebruikers}">
        <script type="text/javascript">
            function loadScroll() {
                var scroll = getCookie("gebruikersScroll");
                if(scroll != undefined) {
                    scroll = parseInt(scroll);
                    if(!isNaN(scroll)) {
                        document.getElementById("gebruikerScroller").scrollTop = scroll;
                    }
                }
            }

            function saveScroll() {
                setCookie("gebruikersScroll", document.getElementById("gebruikerScroller").scrollTop);
            }

            function scrollToBottom() {
                var div = document.getElementById("gebruikerScroller");
                div.scrollTop = div.scrollHeight;
                saveScroll();
            }

            if(document.addEventListener) {
                    document.addEventListener("mouseup", saveScroll, false);
                    window.addEventListener("load", loadScroll, false);
            } else if(document.attachEvent) {
                    document.attachEvent("onmouseup", saveScroll);
                    window.attachEvent("onload", loadScroll);
            }
        </script>        
        
        Aantal gebruikers: <b>${fn:length(actionBean.gebruikers)}</b>
        <p>
        <div id="gebruikerScroller" style="max-height: 180px; overflow: auto; padding: 1px; width: 600px">
        <table border="1" cellpadding="3" style="font-size: 8pt; border-collapse: collapse" class="table-autosort:0 table-stripeclass:alternate">
            <thead>
                <tr>
                    <th class="table-sortable:default">Gebruikersnaam</th>
                    <th class="table-sortable:default">Naam</th>
                    <th class="table-sortable:default">E-mail</th>
                    <th class="table-sortable:numeric">Telefoonnummer</th>                
                    <th></th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="g" varStatus="status" items="${actionBean.gebruikers}">
                    <c:set var="col" value=""/>
                    <c:if test="${g.id == actionBean.gebruiker.id}">
                        <c:set var="col" value="#cccccc"/>
                    </c:if>
                    <tr bgcolor="${col}">
                        <td style="width: 150px">
                            <stripes:link beanclass="nl.b3p.kar.stripes.GebruikersActionBean" event="edit">
                                <stripes:param name="gebruiker" value="${g.id}"/>
                                <c:out value="${g.username}"/>
                            </stripes:link>
                            <c:if test="${g.beheerder}">
                                <img src="${contextPath}/images/star.png">
                            </c:if>
                        </td>
                        <td style="width: 200px"><c:out value="${g.fullname}"/></td>
                        <td style="width: 200px"><c:out value="${g.email}"/></td>
                        <td style="width: 100px"><c:out value="${g.phone}"/></td>
                        <td>
                            <%-- niet gebruiker zichzelf laten verwijderen --%>
                            <c:if test="${g.username != pageContext.request.userPrincipal.username}">
                                <stripes:link beanclass="nl.b3p.kar.stripes.GebruikersActionBean" event="delete">
                                    <stripes:param name="gebruiker" value="${g.id}"/>
                                    <img src="${contextPath}/images/delete.gif" border="0">
                                </stripes:link>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        </div>
        <p>
    </c:if>
        
    <stripes:errors/>
    <stripes:messages/>        

    <c:if test="${empty actionBean.gebruiker}">
        <stripes:submit name="add" onclick="scrollToBottom();">Nieuw account toevoegen</stripes:submit>
                        
    </c:if>
    
    <c:if test="${!empty actionBean.gebruiker}">
        <stripes:submit name="save"><fmt:message key="button.save"/></stripes:submit>
        <c:if test="${actionBean.gebruiker.id != null}">
            <%-- niet gebruiker zichzelf laten verwijderen --%>
            <c:if test="${actionBean.gebruiker.id != pageContext.request.userPrincipal.id}">
                <stripes:submit name="delete"><fmt:message key="button.remove"/></stripes:submit>
            </c:if>
        </c:if>
        <stripes:url var="cancelPage" beanclass="nl.b3p.kar.stripes.GebruikersActionBean"/>
        <stripes:button name="cancel" onclick="window.location.href='${cancelPage}'"><fmt:message key="button.cancel"/></stripes:button>
        
        <p>
        <table style="font-size: 8pt">
            <tr>
                <td valign="top">
        <table style="font-size: 8pt">
            <tr>
                <td><fmt:message key="gebruiker.username"/></td>
                <td><stripes-dynattr:text name="gebruiker.username" size="20" autofocus="true"/></td>
            </tr>
            <tr>
                <td><fmt:message key="gebruiker.fullName"/></td>
                <td><stripes:text name="gebruiker.fullname" size="30"/></td>
            </tr>
            <tr>
                <td><fmt:message key="gebruiker.email"/></td>
                <td><stripes-dynattr:text name="gebruiker.email" size="30" type="email"/></td>
            </tr>
            <tr>
                <td><fmt:message key="gebruiker.password"/></td>
                <td>
                    <stripes:password name="password" size="20"/>
                </td>
            </tr>
            <c:if test="${actionBean.gebruiker.id != null}">
                <tr>
                    <td></td>
                    <td><i>Laat dit veld leeg om het wachtwoord niet te wijzigen.</i></td>
                </tr>
            </c:if>
            <tr>
                <td><fmt:message key="gebruiker.phone"/></td>
                <td><stripes-dynattr:text name="gebruiker.phone" size="15" type="tel"/></td>
            </tr>            
            <tr>
                <td style="vertical-align: top"><fmt:message key="gebruiker.role"/></td>
                <td>
                    <script type="text/javascript">
                        function checkRole(e) {
                            if(!e) { e = window.event };
                            var target = e.target ? e.target : e.srcElement;

                            var beheerder = document.getElementById("role_beheerder").checked;
                            document.getElementById("beheerder").style.display = beheerder ? "block" : "none";
                            document.getElementById("nietBeheerder").style.display = !beheerder ? "block" : "none";
                            document.getElementById("daoedit").style.display = !beheerder ? "block" : "none";
                        }
                    </script>
                    <c:forEach var="r" items="${actionBean.allRoles}">
                        <label><stripes:radio name="role" value="${r.id}" id="role_${r.role}" onclick="blur();" onchange="checkRole(event);"/><c:out value="${r.role}"/><br>
                    </c:forEach>
                </td>

            </tr>
        </table>
                </td>
                <td valign="top">
                    <c:set var="isBeheerder" value="${false}"/>
                    <c:if test="${!empty gebruiker}">
                        <c:set var="isBeheerder"><%= ((Gebruiker)request.getAttribute("gebruiker")).isInRole("beheerder") %></c:set>
                    </c:if>
            <div id="roListHeader">
                <div id="beheerder" style="display: ${isBeheerder ? 'block' : 'none'} ">
                    Een beheerder kan van alle wegbeheerders gegevens bewerken en valideren.
                </div>
                <div id="nietBeheerder" style="display: ${isBeheerder ? 'none' : 'block'}">
                    Gebruiker kan gegevens van de onderstaande wegbeheerders bewerken of valideren:
                </div>
            </div>
            <div id="daoedit" style="display: ${isBeheerder ? 'none' : 'block'}">
            <div id="roList">
                <table id="roListTable">
                    <tr>
                        <th style="width: 35px">Code</th>
                        <th style="width: 160px">Naam</th>
                        <th style="width: 60px">Bewerken</th>
                        <th style="width: 60px">Valideren</th>
                    </tr>
                    <c:forEach var="dor" items="${dataOwnerRights}">
                        <tr>
                            <td>${dor.dataOwner.code}</td>
                            <td>${dor.dataOwner.name}</td>
                            <td style="text-align: center"><html:multibox property="dataOwnersEditable" value="${dor.dataOwner.id}" onclick="this.blur()" onchange="checkDORemove(event)"/></td>
                            <td style="text-align: center"><html:multibox property="dataOwnersValidatable" value="${dor.dataOwner.id}" onclick="this.blur()" onchange="checkDORemove(event)"/></td>
                        </tr>
                    </c:forEach>
                </table>
            </div>
                    Toevoegen wegbeheerder:<br>
                    <select id="availableDataOwners" onchange="checkDOAdd()">
                        <option>Selecteer een wegbeheerder...
                    </select>
            </div>

<script type="text/javascript">
    setOnload(initAvailableDataOwners);

    var dataOwners = ${dataOwnersJson};
    var usedDataOwners = {};

    var codeNameSeparator = " - ";

    function initAvailableDataOwners() {

        usedDataOwners = {};
        var editable = document.forms[0].dataOwnersEditable;
        var validatable = document.forms[0].dataOwnersValidatable;
        if(editable != undefined) {
            for(var i = 0; i < editable.length; i++) {
                var value = editable[i].value;
                if(editable[i].checked || validatable[i].checked) {
                    usedDataOwners[value] = true;
                }
            }
        }

        var availableDO = document.forms[0].availableDataOwners;
        for(var i = 0; i < dataOwners.length; i++) {
            var dao = dataOwners[i];
            var used = usedDataOwners[dao.id + ""] ? true : false;
            if(!used) {
                availableDO.options[availableDO.length] = new Option(dao.code + codeNameSeparator + dao.name, dao.id);
            }
        }
    }

    function checkDOAdd() {
        var availableDO = document.forms[0].availableDataOwners;
        var selectedIndex = availableDO.selectedIndex;
        if(selectedIndex > 0) {
            var value = parseInt(availableDO.options[selectedIndex].value);
            var text = availableDO.options[selectedIndex].text;
            var code = text.substring(0, text.indexOf(codeNameSeparator)); /* XXX deze separator is hardcoded... */
            var name = text.substring(text.indexOf(codeNameSeparator) + codeNameSeparator.length, text.length);

            addDataOwner(value, code, name);

            availableDO.remove(selectedIndex);
            
            availableDO.selectedIndex = 0;
        }
    }

    function checkDORemove(e) {
        if(!e) e = window.event;
        var target = e.target ? e.target : e.srcElement;

        var id = target.value;

        var bothUnchecked = !isChecked("dataOwnersEditable", id) && !isChecked("dataOwnersValidatable", id);

        if(bothUnchecked) {
            if(confirm("Wilt u deze wegbeheerder uit de lijst verwijderen?")) {
                removeDataOwner(id);
            } else {
                target.checked = true;
            }
        }
    }

    function isChecked(name, value) {
        var options = document.forms[0][name];
        if(options.length == undefined) {
            return options.value == value && options.checked;
        } else {
            for(var i = 0; i < options.length; i++) {
                if(options[i].value == value) {
                    return options[i].checked;
                }
            }
            return false;
        }
    }
    
    function addDataOwner(id, code, name) {
        /* zoek positie waarop table row geinsert moet worden */
        var table = document.getElementById("roListTable");
        var index = table.rows.length;
        for(var i = 0; i < table.rows.length; i++) {
            var rowValue = table.rows[i].cells[2].firstChild.value;
            if(id < rowValue) {
                index = i;
                break;
            }
        }

        var row = table.insertRow(index);
        /* helaas is row.innerHTML in IE read-only... */
        var cell = row.insertCell(0);
        cell.appendChild(document.createTextNode(code));
        cell = row.insertCell(1);
        cell.appendChild(document.createTextNode(name));
        cell = row.insertCell(2);
        cell.style.textAlign = "center";
        var input = document.createElement("input");
        input.name = "dataOwnersEditable";
        input.type = "checkbox";
        input.value = id + "";
        input.checked = true;
        input.onchange = checkDORemove;
        input.onclick = function() { this.blur() };
        cell.appendChild(input);
        cell = row.insertCell(3);
        cell.style.textAlign = "center";
        input = document.createElement("input");
        input.name = "dataOwnersValidatable";
        input.type = "checkbox";
        input.value = id + "";
        input.checked = false;
        input.onchange = checkDORemove;
        input.onclick = function() { this.blur() };
        cell.appendChild(input);
    }

    function removeDataOwner(id) {
        var code, name;

        var table = document.getElementById("roListTable");
        for(var i = 0; i < table.rows.length; i++) {
            var rowValue = table.rows[i].cells[2].firstChild.value;
            if(id == rowValue) {
                code = table.rows[i].cells[0].firstChild.nodeValue;
                name = table.rows[i].cells[1].firstChild.nodeValue;
                table.deleteRow(i);
                break;
            }
        }
        
        /* zoek positie waarop select option geinsert moet worden */
        var availableDO = document.forms[0].availableDataOwners;
        var insertBefore = null;
        for(var i = 0; i < availableDO.options.length; i++) {
            if(parseInt(id) < parseInt(availableDO.options[i].value)) {
                insertBefore = availableDO.options[i];
                break;
            }
         }
        var option = new Option(code + codeNameSeparator + name, id);
        availableDO.add(option, insertBefore);
    }

</script>


                </td>
            </tr>
        </table>
        
    </c:if>
        
        
        
    
</stripes:form>
        
</stripes:layout-component>
        
</stripes:layout-render>
        
<%--
<c:set var="form" value="${gebruikersForm.map}"/>

<html:javascript formName="gebruikersForm" staticJavascript="false"/>
<%- -script type="text/javascript" src="<html:rewrite module="" page="/js/table.js"/>"></script- -%>

<html:form action="/gebruikers.do" method="POST" onsubmit="return validateGebruikersForm(this)" style="\" autocomplete='off'">

    <c:set var="focus" value="username" scope="request"/>
    <tiles:insert definition="setFocus"/>

    <html:hidden property="id"/>

<c:if test="${!empty gebruikers}">

    <script type="text/javascript">
        function loadScroll() {
            var scroll = getCookie("gebruikersScroll");
            if(scroll != undefined) {
                scroll = parseInt(scroll);
                if(!isNaN(scroll)) {
                    document.getElementById("gebruikerScroller").scrollTop = scroll;
                }
            }
        }

        function saveScroll() {
            setCookie("gebruikersScroll", document.getElementById("gebruikerScroller").scrollTop);
        }

        function scrollToBottom() {
            var div = document.getElementById("gebruikerScroller");
            div.scrollTop = div.scrollHeight;
            saveScroll();
        }
        
        if(document.addEventListener) {
                document.addEventListener("mouseup", saveScroll, false);
                window.addEventListener("load", loadScroll, false);
        } else if(document.attachEvent) {
                document.attachEvent("onmouseup", saveScroll);
                window.attachEvent("onload", loadScroll);
        }
    </script>
    Aantal gebruikers: <b>${fn:length(gebruikers)}</b>
    <p>
    <div id="gebruikerScroller" style="max-height: 180px; overflow: auto; padding: 1px; width: 600px">
    <table border="1" cellpadding="3" style="border-collapse: collapse" class="table-autosort:0 table-stripeclass:alternate">
        <thead>
            <tr>
                <th class="table-sortable:default">Gebruikersnaam</th>
                <th class="table-sortable:default">Naam</th>
                <th class="table-sortable:default">E-mail</th>
                <th class="table-sortable:numeric">Telefoonnummer</th>                
                <th></th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="g" varStatus="status" items="${gebruikers}">
                <c:set var="editLink"><html:rewrite page="/gebruikers.do?edit=t&amp;id=${g.id}"/></c:set>
                <c:set var="col" value=""/>
                <c:if test="${g.id == form.id}">
                    <c:set var="col" value="#cccccc"/>
                </c:if>
                <tr bgcolor="${col}">
                    <td style="width: 150px">
                        <html:link href="${editLink}"><c:out value="${g.username}"/></html:link>
                        <c:if test="<%= ((Gebruiker)pageContext.getAttribute(\"g\")).isInRole(\"beheerder\") %>">
                            <html:img page="/images/star.png" module="/"/>
                        </c:if>
                    </td>
                    <td style="width: 200px"><c:out value="${g.fullname}"/></td>
                    <td style="width: 200px"><c:out value="${g.email}"/></td>
                    <td style="width: 100px"><c:out value="${g.phone}"/></td>
                    <td>
                        <%- - niet gebruiker zichzelf laten verwijderen - -%>
                        <c:if test="${g.username != pageContext.request.userPrincipal.username}">
                            <html:link page="/gebruikers.do?delete=t&amp;id=${g.id}">
                                <html:img page="/images/delete.gif" altKey="button.remove" module="" border="0"/>
                            </html:link>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
    </div>
<p>
<tiles:insert definition="infoblock"/>

    <c:if test="${!empty form.id}">
        <p>
        <c:choose>
            <c:when test="${!empty form.confirmAction}">
                <html:hidden property="confirmAction"/>
                <html:submit property="${form.confirmAction}"><fmt:message key="button.ok"/></html:submit>
                <html:cancel><fmt:message key="button.cancel"/></html:cancel>
            </c:when>
            <c:otherwise>
                <html:submit property="save"><fmt:message key="button.save"/></html:submit>
                <c:if test="${form.id != -1}">
                    <%- - niet gebruiker zichzelf laten verwijderen - -%>
                    <c:if test="${form.id != pageContext.request.userPrincipal.id}">
                        <html:submit property="delete" onclick="bCancel=true;"><fmt:message key="button.remove"/></html:submit>
                    </c:if>
                </c:if>
                <html:cancel><fmt:message key="button.cancel"/></html:cancel>
            </c:otherwise>
        </c:choose>
        <p>
        <table>
            <tr>
                <td valign="top">
        <table>
            <tr>
                <td><fmt:message key="gebruiker.username"/></td>
                <td><html:text property="username" size="20" maxlength="30"/></td>
            </tr>
            <tr>
                <td><fmt:message key="gebruiker.fullName"/></td>
                <td><html:text property="fullName" size="30" maxlength="50"/></td>
            </tr>
            <tr>
                <td><fmt:message key="gebruiker.email"/></td>
                <td><html:text property="email" size="30" maxlength="50"/></td>
            </tr>
            <tr>
                <td><fmt:message key="gebruiker.password"/></td>
                <td>
                    <html:password property="password" size="20" maxlength="50"/>
                </td>
            </tr>
            <c:if test="${form.id != -1}">
                <tr>
                    <td></td>
                    <td><i>Laat dit veld leeg om het wachtwoord niet te wijzigen.</i></td>
                </tr>
            </c:if>
            <tr>
                <td><fmt:message key="gebruiker.phone"/></td>
                <td><html:text property="phone" size="15" maxlength="15"/></td>
            </tr>            
            <tr>
                <td style="vertical-align: top"><fmt:message key="gebruiker.role"/></td>
                <td>
                    <script type="text/javascript">
                        function checkRole(e) {
                            if(!e) { e = window.event };
                            var target = e.target ? e.target : e.srcElement;

                            var beheerder = document.getElementById("role_beheerder").checked;
                            document.getElementById("beheerder").style.display = beheerder ? "block" : "none";
                            document.getElementById("nietBeheerder").style.display = !beheerder ? "block" : "none";
                            document.getElementById("daoedit").style.display = !beheerder ? "block" : "none";
                        }
                    </script>
                    <c:forEach var="r" items="${availableRoles}">
                        <html:radio property="role" value="${r.role}" styleId="role_${r.role}" onclick="blur();" onchange="checkRole(event);"/><c:out value="${r.role}"/><br>
                    </c:forEach>
                </td>

            </tr>
        </table>
                </td>
                <td valign="top">
                    <c:set var="isBeheerder" value="${false}"/>
                    <c:if test="${!empty gebruiker}">
                        <c:set var="isBeheerder"><%= ((Gebruiker)request.getAttribute("gebruiker")).isInRole("beheerder") %></c:set>
                    </c:if>
            <div id="roListHeader">
                <div id="beheerder" style="display: ${isBeheerder ? 'block' : 'none'} ">
                    Een beheerder kan van alle wegbeheerders gegevens bewerken en valideren.
                </div>
                <div id="nietBeheerder" style="display: ${isBeheerder ? 'none' : 'block'}">
                    Gebruiker kan gegevens van de onderstaande wegbeheerders bewerken of valideren:
                </div>
            </div>
            <div id="daoedit" style="display: ${isBeheerder ? 'none' : 'block'}">
            <div id="roList">
                <table id="roListTable">
                    <tr>
                        <th style="width: 35px">Code</th>
                        <th style="width: 160px">Naam</th>
                        <th style="width: 60px">Bewerken</th>
                        <th style="width: 60px">Valideren</th>
                    </tr>
                    <c:forEach var="dor" items="${dataOwnerRights}">
                        <tr>
                            <td>${dor.dataOwner.code}</td>
                            <td>${dor.dataOwner.name}</td>
                            <td style="text-align: center"><html:multibox property="dataOwnersEditable" value="${dor.dataOwner.id}" onclick="this.blur()" onchange="checkDORemove(event)"/></td>
                            <td style="text-align: center"><html:multibox property="dataOwnersValidatable" value="${dor.dataOwner.id}" onclick="this.blur()" onchange="checkDORemove(event)"/></td>
                        </tr>
                    </c:forEach>
                </table>
            </div>
                    Toevoegen wegbeheerder:<br>
                    <select id="availableDataOwners" onchange="checkDOAdd()">
                        <option>Selecteer een wegbeheerder...
                    </select>
            </div>

<script type="text/javascript">
    setOnload(initAvailableDataOwners);

    var dataOwners = ${dataOwnersJson};
    var usedDataOwners = {};

    var codeNameSeparator = " - ";


    function initAvailableDataOwners() {

        usedDataOwners = {};
        var editable = document.forms[0].dataOwnersEditable;
        var validatable = document.forms[0].dataOwnersValidatable;
        if(editable != undefined) {
            for(var i = 0; i < editable.length; i++) {
                var value = editable[i].value;
                if(editable[i].checked || validatable[i].checked) {
                    usedDataOwners[value] = true;
                }
            }
        }

        var availableDO = document.forms[0].availableDataOwners;
        for(var i = 0; i < dataOwners.length; i++) {
            var dao = dataOwners[i];
            var used = usedDataOwners[dao.id + ""] ? true : false;
            if(!used) {
                availableDO.options[availableDO.length] = new Option(dao.code + codeNameSeparator + dao.name, dao.id);
            }
        }
    }

    function checkDOAdd() {
        var availableDO = document.forms[0].availableDataOwners;
        var selectedIndex = availableDO.selectedIndex;
        if(selectedIndex > 0) {
            var value = parseInt(availableDO.options[selectedIndex].value);
            var text = availableDO.options[selectedIndex].text;
            var code = text.substring(0, text.indexOf(codeNameSeparator)); /* XXX deze separator is hardcoded... */
            var name = text.substring(text.indexOf(codeNameSeparator) + codeNameSeparator.length, text.length);

            addDataOwner(value, code, name);

            availableDO.remove(selectedIndex);
            
            availableDO.selectedIndex = 0;
        }
    }

    function checkDORemove(e) {
        if(!e) e = window.event;
        var target = e.target ? e.target : e.srcElement;

        var id = target.value;

        var bothUnchecked = !isChecked("dataOwnersEditable", id) && !isChecked("dataOwnersValidatable", id);

        if(bothUnchecked) {
            if(confirm("Wilt u deze wegbeheerder uit de lijst verwijderen?")) {
                removeDataOwner(id);
            } else {
                target.checked = true;
            }
        }
    }

    function isChecked(name, value) {
        var options = document.forms[0][name];
        if(options.length == undefined) {
            return options.value == value && options.checked;
        } else {
            for(var i = 0; i < options.length; i++) {
                if(options[i].value == value) {
                    return options[i].checked;
                }
            }
            return false;
        }
    }
    
    function addDataOwner(id, code, name) {
        /* zoek positie waarop table row geinsert moet worden */
        var table = document.getElementById("roListTable");
        var index = table.rows.length;
        for(var i = 0; i < table.rows.length; i++) {
            var rowValue = table.rows[i].cells[2].firstChild.value;
            if(id < rowValue) {
                index = i;
                break;
            }
        }

        var row = table.insertRow(index);
        /* helaas is row.innerHTML in IE read-only... */
        var cell = row.insertCell(0);
        cell.appendChild(document.createTextNode(code));
        cell = row.insertCell(1);
        cell.appendChild(document.createTextNode(name));
        cell = row.insertCell(2);
        cell.style.textAlign = "center";
        var input = document.createElement("input");
        input.name = "dataOwnersEditable";
        input.type = "checkbox";
        input.value = id + "";
        input.checked = true;
        input.onchange = checkDORemove;
        input.onclick = function() { this.blur() };
        cell.appendChild(input);
        cell = row.insertCell(3);
        cell.style.textAlign = "center";
        input = document.createElement("input");
        input.name = "dataOwnersValidatable";
        input.type = "checkbox";
        input.value = id + "";
        input.checked = false;
        input.onchange = checkDORemove;
        input.onclick = function() { this.blur() };
        cell.appendChild(input);
    }

    function removeDataOwner(id) {
        var code, name;

        var table = document.getElementById("roListTable");
        for(var i = 0; i < table.rows.length; i++) {
            var rowValue = table.rows[i].cells[2].firstChild.value;
            if(id == rowValue) {
                code = table.rows[i].cells[0].firstChild.nodeValue;
                name = table.rows[i].cells[1].firstChild.nodeValue;
                table.deleteRow(i);
                break;
            }
        }
        
        /* zoek positie waarop select option geinsert moet worden */
        var availableDO = document.forms[0].availableDataOwners;
        var insertBefore = null;
        for(var i = 0; i < availableDO.options.length; i++) {
            if(parseInt(id) < parseInt(availableDO.options[i].value)) {
                insertBefore = availableDO.options[i];
                break;
            }
         }
        var option = new Option(code + codeNameSeparator + name, id);
        availableDO.add(option, insertBefore);
    }

</script>


                </td>
            </tr>
        </table>

    </c:if>
    <c:if test="${empty form.id}">
        <html:submit property="create" onclick="scrollToBottom(); bCancel=true;">Nieuw account toevoegen</html:submit>
    </c:if>

</c:if>
<c:if test="${empty gebruikers}">
    <tiles:insert definition="infoblock"/>
</c:if>

</html:form>

--%>
