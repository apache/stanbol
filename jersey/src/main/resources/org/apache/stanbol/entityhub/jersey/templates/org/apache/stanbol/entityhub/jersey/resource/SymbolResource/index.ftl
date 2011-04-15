<#import "symbol_common.ftl" as common>
<#escape x as x?html>
<@common.page> 

<p>List of subresources:</p>
<ul>
	<li><a href="${it.publicBaseUri}entityhub/symbol/lookup">/entityhub/symbol/lookup</a></li>
</ul>

<#include "inc_lookup.ftl">
<#include "inc_find.ftl">

</@common.page>
</#escape>
