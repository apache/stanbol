<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Entityhub" hasrestapi=true> 

<div class="panel" id="webview">
<p>This is the start page of the entity hub.</p>

</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Service Endpoints</h3>

<p>The Entityhub supports the following service endpoints:</p>

<ul>
	<li>Site Manager @ <a href="${it.publicBaseUri}entityhub/sites">/entityhub/sites</a></li>
	<li>Symbols @ <a href="${it.publicBaseUri}entityhub/symbol">/entityhub/symbol</a></li>
	<li>Mappings @ <a href="${it.publicBaseUri}entityhub/mapping">/entityhub/mapping</a></li>
</ul>

</div>

</@common.page>
</#escape>
