<#import "sitemanager_common.ftl" as common>
<#escape x as x?html>
<@common.page> 

<p>List of subresources:</p>
<ul>
	<li><a href="${it.publicBaseUri}entityhub/sites/referenced">/entityhub/sites/referenced</a></li>
	<li><a href="${it.publicBaseUri}entityhub/sites/entity">/entityhub/sites/entity</a></li>
	<li><a href="${it.publicBaseUri}entityhub/sites/find">/entityhub/sites/find</a></li>
	<li><a href="${it.publicBaseUri}entityhub/sites/query">/entityhub/sites/query</a></li>
</ul>

<#include "inc_referenced.ftl">
<#include "inc_entity.ftl">
<#include "inc_find.ftl">
<#include "inc_query.ftl">

</@common.page>
</#escape>
