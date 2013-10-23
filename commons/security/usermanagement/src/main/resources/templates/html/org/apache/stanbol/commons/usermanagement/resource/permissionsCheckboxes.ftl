<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />
<@namespace dc="http://purl.org/dc/elements/1.1/" />

<!-- @ldpath path="fn:sort(^rdf:type)" -->
 <@ldpath path="fn:sort(permission:hasPermission)">
 
    <#assign permission>
    <@ldpath path="permission:javaPermissionEntry :: xsd:string"/>
    </#assign>
    <div class="role">
        <input class="checkboxPermission" type="checkbox" id="${permission?html}" name="${permission?html}" value="${permission?html}" checked="checked"  />
        <label for="${permission?html}">${permission?html}</label>
    </div>
    
</@ldpath>


<!--
[]    a       <http://xmlns.com/foaf/0.1/Agent> ;
      <http://clerezza.org/2008/10/permission#hasPermission>
              [ <http://clerezza.org/2008/10/permission#javaPermissionEntry>
                        "(java.security.AllPermission \"\" \"\")"
              ] ;
Get the names of all persons that link the current person as foaf:knows:

friends = ^foaf:knows / foaf:name :: xsd:string;
-->