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