<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />

<form method="post" action="/user-management/store-user">
login: <@ldpath path="platform:userName :: xsd:string"/> <br/>
<input type="hidden" name="currentUserName" value="<@ldpath path="platform:userName :: xsd:string"/>" />
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