<@namespace platform="http://clerezza.org/2009/08/platform#" />
<@namespace permission="http://clerezza.org/2008/10/permission#" />
<@namespace sioc="http://rdfs.org/sioc/ns#" />
<@namespace dc="http://purl.org/dc/elements/1.1/" />

<table id="role-table" class="nicetable noauto">
    <thead><tr><th>Name</th></tr></thead>
    <tbody>
        <@ldpath path="fn:sort(^rdf:type)">
        <#assign name>
            <@ldpath path="dc:title :: xsd:string"/>
        </#assign>
        <tr>
            <td>${name}</td>
        </tr>
        </@ldpath>
    </tbody>
</table>