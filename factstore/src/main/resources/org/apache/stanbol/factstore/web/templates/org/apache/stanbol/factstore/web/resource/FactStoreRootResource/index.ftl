<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="FactStore" hasrestapi=true> 

<div class="panel" id="webview">
<p>The FactStore implements a store for facts plus the ability to query for single facts and for combinations
of facts which is equal to the required IKS reasoning capability. In summary, the FactStore implements:</p>

<ul>
	<li>Persistence storage for n-ary facts about entities</li>
    <li>Query language to query for a single fact</li>
    <li>Query language to query for combinations of facts (reasoning)</li> 
</ul>

<p>The FactStore specification proposal can be found online at the
<a href="http://wiki.iks-project.eu/index.php/FactStore_Specification" target="_blank">IKS Wiki page</a>.</p>
</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Service Endpoints</h3>

<p>The FactStore supports the following service endpoints:</p>

<ul>
	<li>Store @ <a href="${it.publicBaseUri}factstore/facts">/factstore/facts</a></li>
</ul>

</div>

</@common.page>
</#escape>
