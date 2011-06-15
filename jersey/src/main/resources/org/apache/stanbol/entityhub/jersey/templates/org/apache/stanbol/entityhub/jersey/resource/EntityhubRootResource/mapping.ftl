<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Entityhub Mappings" hasrestapi=false> 

<p>List of subresources:</p>
<ul>
	<li><a href="${it.publicBaseUri}entityhub/mapping/symbol">/entityhub/mapping/symbol</a></li>
	<li><a href="${it.publicBaseUri}entityhub/mapping/entity">/entityhub/mapping/entity</a></li>
</ul>

<#include "inc_mapping.ftl">
<#include "inc_mapping_entity.ftl">
<#include "inc_mapping_symbol.ftl">

</@common.page>
</#escape>
