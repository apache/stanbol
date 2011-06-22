<#macro page>
<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="FactStore Facts" hasrestapi=false> 

<div class="panel" id="restapi">
<h3>Service Endpoint <a href="${it.publicBaseUri}factstore/facts">/factstore/facts</a></h3>

<#nested>

</div>

</@common.page>
</#escape>
</#macro>