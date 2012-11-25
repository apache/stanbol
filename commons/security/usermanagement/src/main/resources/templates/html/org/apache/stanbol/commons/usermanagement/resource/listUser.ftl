<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />
<table id="user-table" class="nicetable noauto">
    <thead><tr><th>Name</th><th>login</th><th>email</th><th>groups</th><th>&nbsp;</th></tr></thead>
    <tbody>
        <@ldpath path="fn:sort(^rdf:type)">
        <#assign fullName>
            <@ldpath path="foaf:name :: xsd:string"/>
        </#assign>
        <#assign userName>
            <@ldpath path="platform:userName :: xsd:string"/>
        </#assign>
        <tr>
            <td>${fullName}</td>
            <td>${userName}</td>

            <#assign mbox>
            <@ldpath path="foaf:mbox" />
            </#assign>
            
            <#assign email>
            <#if mbox != "">${mbox?substring(7)}</#if>
            </#assign>
            
            <td>${email}</td>

            <td>
            <@ldpath path="fn:sort(sioc:has_function)">
               <@ldpath path="dc:title :: xsd:string"/>
            </@ldpath>
            </td>
            <td>
                <ul class="icons ui-widget">
                    <li class="dynhover ui-state-default ui-corner-all" title="Edit" onClick="javascript:editUser('${userName}')"><span class="ui-icon ui-icon-edit">&nbsp;</span></li>
                    <li class="dynhover ui-state-default ui-corner-all" title="delete" onClick="javascript:removeUser('${userName}')"><span class="ui-icon ui-icon-trash">&nbsp;</span></li>
                </ul>
            </td>
        </tr>
        </@ldpath>
    </tbody>
</table>