<%--
  ~ Copyright 2000-2009 JetBrains s.r.o.
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

<table class="runnerFormTable">
    <l:settingsGroup title="Vault Settings">
        <tr class="noBorder" id="xmlReportParsing.reportType.container">
            <td colspan="2">
                Make sure Vault Java API jars are present at &lt;TeamCity web application&gt;/WEB-INF/lib  directory.
            </td>
        </tr>
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
            <th><label for="vault.user">User name:  <l:star/></label>
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
