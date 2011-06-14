<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="KReS Storage interface">

<div class="contentTag">

<p>The storage is a third component external to KReS. You can configure the storage that KReS should use through the
<a href="/system/console">Apache Felix OSGi console</a>. What KReS provide is just an interface that uses REST services
in order to comunicate with the external storage and is designed to be full compliant with the FISE store.</p>
<fieldset>
<legend> Load a graph</legend>
<div class="fieldset">
Select a graph:
<select id="graphs">
<#list it.storedGraphs as g>
<option value="${g}">${g}
</#list>
</select>
<br><br>
<input type="button" value="view" onClick="javascript:var storage=new Storage(); storage.loadGraph(document.getElementById('graphs').value);">
<div id="graphDIV" class="hide"></div>
</div>
</fieldset>

<fieldset>
<legend>Store a graph</legend>
<div class="fieldset">
<form action="/ontonet/graphs" method="post" enctype="multipart/form-data"> 
Add a graph to the store <input type="file" id="fileGraph" name="graph">
<br><br>
with id <input class="code" type="text" name="id">
<input type="submit" value="store">
</div>

</form>
</fieldset>
</div>



</@common.page>
</#escape>
