<html>
    <head>
        <title>Welcome to: <@ldpath path="rdfs:label[@en]"/></title>
    </head>

    <body>
        <h1><@ldpath path="rdfs:label[@en]"/></h1>

        <p>
            Comment: <@ldpath path="rdfs:comment[@en]"/>
        </p>

        <ul>
            <@ldpath path="fn:sort(rdf:type)">
                <#if evalLDPath("rdfs:label[@en] :: xsd:string")??>
                    <li><@ldpath path="rdfs:label[@en] :: xsd:string"/></li>
                </#if>
            </@ldpath>
        </ul>
        <#include "../included.ftl">
    </body>

</html>