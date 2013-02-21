<#--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />
<table id="user-table" class="nicetable noauto">
    <thead><tr><th>Name</th><th>Login</th><th>Email</th><th>Roles</th></tr></thead>
    <tbody>
        <@ldpath path="fn:sort(^rdf:type)">
        <#assign fullName>
        <@ldpath path="foaf:name :: xsd:string"/>
        </#assign>
        <#assign userName>
        <@ldpath path="platform:userName :: xsd:string"/>
        </#assign>

        <#assign mbox>
        <@ldpath path="foaf:mbox" />
        </#assign>

        <#assign email>
        <#if mbox != "">${mbox?substring(7)}</#if>
        </#assign>

        <#assign roles>
        <@ldpath path="fn:sort(sioc:has_function)">
        
        <#assign role>
        <@ldpath path="dc:title :: xsd:string"/>
        </#assign>
        <#if role != "BasePermissionsRole">${role}</#if>
        </@ldpath>
        </#assign>
        
        <tr>
            <td>${fullName}</td>
            <td>${userName}</td>
            <td>${email}</td>
            <td>${roles}</td>

            <td>
                <ul class="icons ui-widget">
                    <li class="dynhover ui-state-default ui-corner-all" title="Edit" onClick="javascript:editUser('${userName}')"><span class="ui-icon ui-icon-edit">&nbsp;</span></li>
                    <li class="dynhover ui-state-default ui-corner-all delete" title="Delete" onClick="javascript:removeUser('${userName}')"><span class="ui-icon ui-icon-trash">&nbsp;</span><div id="remove${userName}" class="hidden delete-dialog" title="Remove User"><p><br/>Delete user : ${userName}?</p></div></li>
                </ul>
            </td>
        </tr>
        </@ldpath>
    </tbody>
</table>