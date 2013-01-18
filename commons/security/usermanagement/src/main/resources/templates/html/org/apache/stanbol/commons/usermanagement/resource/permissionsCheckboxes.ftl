<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />
<@namespace dc="http://purl.org/dc/elements/1.1/" />

<@ldpath path="fn:sort(^rdf:type)">

<#assign permission>
<@ldpath path="permission:javaPermissionEntry :: xsd:string"/>
</#assign>
<!-- div class="permission" --> <!-- labelCheckbox  -->
    <input class="permission" type="checkbox" id="${permission}" name="${permission}" value="${permission}" checked="checked"  />
    <label for="${permission}">${permission}</label>
    <br/>

</@ldpath>