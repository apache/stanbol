<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Entityhub Entity" hasrestapi=true> 

<div class="panel" id="webview">
  <p>
    This is the page to get/create/update/delete Entities Managed by the entity
    hub.
  </p>
  <p>TODO:</p>
</div>

<div class="panel" id="restapi" style="display: none;">
  <h3>Service Endpoint <a href="${it.publicBaseUri}entityhub/entity">/entityhub/entity</a></h3>

  <#include "inc_entity.ftl">

</div>

</@common.page>
</#escape>
