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

<p id="validateTips" class="important">* required fields</p>
<form>

    <#assign userName>
    <@ldpath path="platform:userName :: xsd:string"/>
    </#assign>	

    <#assign mbox>
    <@ldpath path="foaf:mbox" />
    </#assign>

    <#attempt>
    <#assign email><#if mbox != "">${mbox?substring(7)}</#if></#assign>
    <#recover></#attempt>	


    <input type="hidden" name="currentUserName" value="${userName}" />

    <fieldset>
        <label for="login">Login <span class="important">*</span></label>
        <input type="text" name="newUserName" value="${userName}" class="text ui-widget-content ui-corner-all" />
        <label for="fullName">Full Name</label>
        <input type="text" name="fullName" value="<@ldpath path="foaf:name :: xsd:string"/>" id="fullName" class="text ui-widget-content ui-corner-all" />
               <label for="email">Email</label>
        <input type="text" name="email" id="email" value="${email}" class="text ui-widget-content ui-corner-all" />
        <label for="password">Password <span class="important">*</span></label>
        <input type="password" name="password" id="password" value="" class="text ui-widget-content ui-corner-all" />
    </fieldset>

    <fieldset id="roles-checkboxes" class="labelCheckbox">
        <legend>Roles</legend>
        <input type="hidden" id="BasePermissionsRole" name="BasePermissionsRole" value="BasePermissionsRole" />

        <@ldpath path="fn:sort(sioc:has_function)">
        <#assign roleName>
        <@ldpath path="dc:title :: xsd:string"/>
        </#assign>

        <input type="checkbox" id="${roleName?html}" name="${roleName?html}" value="${roleName?html}" checked="checked" />
        <label for="${roleName?html}">${roleName?html}</label>

        </@ldpath>
    </fieldset> 

    <!-- <button name="addPermission">Add permission</button> -->
</form>

<!--

        Permissions: <ul>
        <@ldpath path="fn:sort(permission:hasPermission)">
                <#assign permission>
                <@ldpath path="permission:javaPermissionEntry :: xsd:string"/>
                </#assign>
                <li class="permission">
                <input type="text" name="permission[]" value="${permission?html}" />
                </li>
        </@ldpath>
        </ul>

        <ol>
        <@ldpath path="fn:sort(sioc:has_function)">
                <li class="permission">
                <@ldpath path="dc:title :: xsd:string"/>
                </li>
        </@ldpath>
        </ol>
-->