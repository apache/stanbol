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
<@namespace dc="http://purl.org/dc/elements/1.1/" />

<table id="role-table" class="nicetable noauto">
    <thead><tr><th>Name</th></tr></thead>
    <tbody>
        <@ldpath path="fn:sort(^rdf:type)">
        <#assign roleName>
        <@ldpath path="dc:title :: xsd:string"/>
        </#assign>
        <tr>
            <td>${roleName}</td>
            <td>
                <ul class="icons ui-widget">
                    <li class="dynhover ui-state-default ui-corner-all" title="Edit" onClick="javascript:editRole('${roleName}')"><span class="ui-icon ui-icon-edit">&nbsp;</span></li>
                    <li class="dynhover ui-state-default ui-corner-all delete" title="Delete" onClick="javascript:removeRole('${roleName}')"><span class="ui-icon ui-icon-trash">&nbsp;</span>
                        <div id="remove${roleName}" class="hidden delete-dialog" title="Remove Role"><p>
                                <br/>Delete role : ${roleName}?</p></div></li>
                </ul>
            </td>
        </tr>
        </@ldpath>
    </tbody>
</table>
