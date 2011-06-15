<#import "site_common.ftl" as common>
<#escape x as x?html>
<@common.page> 

<p>List of subresources:</p>
<ul>
	<li><a href="entity">/entityhub/site/{siteId}/entity</a></li>
	<li><a href="find">/entityhub/site/{siteId}/find</a></li>
	<li><a href="query">/entityhub/site/{siteId}/query</a></li>
</ul>
<hr>
<#include "inc_metadata.ftl">
<hr>
<#include "inc_entity.ftl">
<hr>
<#include "inc_find.ftl">
<hr>
<#include "inc_query.ftl">

</@common.page>
</#escape>
