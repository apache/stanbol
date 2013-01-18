<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />
<@namespace dc="http://purl.org/dc/elements/1.1/" />

<@ldpath path="fn:sort(^rdf:type)">

<#assign name>
<@ldpath path="dc:title :: xsd:string"/>
</#assign>

<#if name != "BasePermissionsRole"><!-- all users have it, so hide -->
<!-- div class="labelCheckbox role" -->
    <input class="role" type="checkbox" id="${name}" name="${name}" value="${name}" />
    <label for="${name}">${name}</label>

<br />
</#if>
</@ldpath>

