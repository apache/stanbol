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
<@namespace rdfs="http://www.w3.org/2000/01/rdf-schema#" />
<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />

<form>

    <#assign roleName>
    <@ldpath path="dc:title :: xsd:string"/>
    </#assign>	

    <#assign comment>
    <@ldpath path="rdfs:comment :: xsd:string"/>
    </#assign>	

    <input id="create-or-edit" type="hidden" name="create-or-edit" value="edit" />

    <fieldset>
        <label for="roleName">Role Name</label>
        <input id="roleName" type="text" name="roleName" value="${roleName}" class="text ui-widget-content ui-corner-all" />
        <label for="comment">Comment</label>
        <input id="comment" type="text" name="comment" value="${comment}" class="text ui-widget-content ui-corner-all" />
    </fieldset>

    <fieldset class="labelCheckbox">
        <legend>Permissions</legend>
        <div id="role-permissions-checkboxes">
            <#include "permissionsCheckboxes.ftl">
        </div>
    </fieldset> 

    <fieldset>
        <div class="labelTextbox" id="permission-inputs">
            <label for="newPermission">Add Permission</label>
            <br/>
            <input type="text" class="inputPermission" />
            <div class="dynhover ui-state-default ui-corner-all" title="Add Permission Field" onClick="javascript:addPermissionField()"><span class="ui-icon ui-icon-circle-plus">&nbsp;</span></div>
        </div>
    </fieldset>
    <p>e.g. (org.osgi.framework.ServicePermission "*" "get")</p>
    <!-- <button name="addPermission">Add permission</button> -->
</form>
<!--
        <@ldpath path="fn:sort(sioc:has_function)">
                <li class="permission">
                <@ldpath path="dc:title :: xsd:string"/>
                </li>
        </@ldpath>
        </ol>
-->