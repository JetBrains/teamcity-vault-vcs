<%@ page import="java.io.File" %>
<%--
~ Copyright 2000-2014 JetBrains s.r.o.
~
~ Licensed under the Apache License, Version 2.0 (the "License");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" BASIS,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
--%>

<%@include file="/include.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="vaultApiPresent" scope="request" type="java.lang.Boolean"/>
<jsp:useBean id="teamCityDataDir" scope="request" type="java.io.File"/>

<table class="runnerFormTable">
    <c:set var="fileSeparator"><%=File.separator%></c:set>
    
    <l:settingsGroup title="Vault Settings">
        <c:if test="${not vaultApiPresent}">
            <tr>
                <td colspan="2">
                    <div class="icon_before icon16 attentionComment">
                      <font color="red">SourceGear Vault integration could not find Vault Java API jars.</font>
                      <br><br>
                      These files can be found at Vault Java Command Line Client under the <strong>vaultJavaCLC##</strong>${fileSeparator}lib
                      directory where <strong>##</strong> are digits corresponding to Vault version.
                      <br><br>
                      To install these files, create the <strong>${teamCityDataDir}${fileSeparator}plugins${fileSeparator}VaultAPI/lib</strong> directory
                      and put all of the *.jar files from the Java Command Line Client under this directory. The server should be restarted afterwards.
                      <br><br>
                      If you have no Vault Java Command Line Client you can find the related information on the SourceGear Vault
                      <a showdiscardchangesmessage="false" target="_blank" href="http://sourcegear.com/vault">site</a>.
                    </div>
                </td>
            </tr>
        </c:if>
        <tr>
            <th><label for="vault.server">Vault server URL: <l:star/></label>
            </th>
            <td><props:textProperty name="vault.server" className="longField"/>
                <span class="error" id="error_vault.server"></span>
            </td>                
        </tr>
        <tr>
            <th><label for="vault.repo">Repository name: <l:star/></label>
            </th>
            <td><props:textProperty name="vault.repo" className="longField"/>
                <span class="error" id="error_vault.repo"></span></td>
        </tr>
        <tr>
            <th><label for="vault.user">Username:  <l:star/></label>
            </th>
            <td><props:textProperty name="vault.user" className="longField"/>
                <span class="error" id="error_vault.user"></span></td>
        </tr>
        <tr>
            <th><label for="secure:vault.password">Password: </label>
            </th>
            <td><props:passwordProperty name="secure:vault.password" className="longField"/>
                <span class="error" id="error_secure:vault.password"></span></td>
        </tr>
    </l:settingsGroup>
</table>
