<#import "mapping_common.ftl" as common>
<#escape x as x?html>
<@common.page> 

<p>List of subresources:</p>
<ul>
	<li><a href="${it.publicBaseUri}entityhub/mapping/symbol">/entityhub/mapping/symbol</a></li>
	<li><a href="${it.publicBaseUri}entityhub/mapping/entity">/entityhub/mapping/entity</a></li>
</ul>

<#include "inc_mapping.ftl">

</@common.page>
</#escape>
