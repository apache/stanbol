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

<form method="post" action="/user-management/store-user">
	
<#assign userName>
    <@ldpath path="platform:userName :: xsd:string"/>
</#assign>	
	
<#if userName?? && userName != "">
Current login: ${userName} <br/>
<input type="hidden" name="currentUserName" value="${userName}" />
</#if>

login: <input type="text" name="newUserName" value="" /><br/>
Full Name: <input type="text" name="fullName" value="<@ldpath path="foaf:name :: xsd:string"/>" /><br/>

<#assign mbox>
<@ldpath path="foaf:mbox" />
</#assign>

<#attempt>
   <#assign email>
      <#if mbox != "">${mbox?substring(7)}</#if>
   </#assign>
<#recover>
</#attempt>

Email : <input type="text" name="email" value="${email}" /><br/>
Password : <input type="password" name="password" value="" /><br/>
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
	<button name="addPermission">Add permission</button>
	Groups:
	<ol>
	<@ldpath path="fn:sort(sioc:has_function)">
		<li class="permission">
		<@ldpath path="dc:title :: xsd:string"/>
		</li>
	</@ldpath>
	</ol>
 <input type="submit" value="Submit">
</form>