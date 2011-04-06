<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Entityhub" hasrestapi=false> 

<p>This is the index page of the entity hub.</p>

<ul>
	<li><a href="${it.publicBaseUri}entityhub/sites/referenced">List referenced sites (JSON)</a>
</ul>

</@common.page>
</#escape>
