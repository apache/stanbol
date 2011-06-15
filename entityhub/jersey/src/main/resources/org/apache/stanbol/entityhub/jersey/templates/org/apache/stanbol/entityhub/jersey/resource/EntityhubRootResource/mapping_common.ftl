<#macro page>
<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Entityhub Mappings" hasrestapi=false> 

<div class="panel" id="restapi">
<h3>Service Endpoint <a href="${it.publicBaseUri}entityhub/mapping">/entityhub/mapping</a></h3>

<#nested>

</div>

</@common.page>
</#escape>
</#macro>