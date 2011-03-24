<#import "/imports/contentitem.ftl" as contentitem>
<#escape x as x?html>

<@contentitem.view />

<h3>Raw RDF output</h3>
<pre>${it.rdfMetadata}</pre>
</div>
</#escape>