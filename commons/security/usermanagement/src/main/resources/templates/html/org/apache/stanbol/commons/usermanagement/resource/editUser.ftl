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


    <input id="currentLogin" type="hidden" name="currentLogin" value="${userName}" />
    <input id="create-or-edit" type="hidden" name="create-or-edit" value="edit" />

    <fieldset>
        <label for="newLogin">Login <span class="important">*</span></label>
        <input id="newLogin" type="text" name="newLogin" value="${userName}" class="text ui-widget-content ui-corner-all" />
        <label for="fullName">Full Name</label>
        <input id="fullName" type="text" name="fullName" value="<@ldpath path="foaf:name :: xsd:string"/>" class="text ui-widget-content ui-corner-all" />
               <label for="email">Email</label>
        <input id="email" type="text" name="email" value="${email}" class="text ui-widget-content ui-corner-all" />
        <label for="password" id="password-label">Password <span class="important">*</span></label>
        <input id="password" type="password" name="password" value="" class="text ui-widget-content ui-corner-all" />
    </fieldset>

    <fieldset id="roles-checkboxes">
         </fieldset> 
    
    <fieldset id="permissions-checkboxes" class="labelCheckbox">
    </fieldset> 
    
    <fieldset>
        <div class="labelTextbox">
            <label for="newPermission">Add Permission</label>
            <br/>
            <input type="text" id="newPermission" name="newPermission">
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