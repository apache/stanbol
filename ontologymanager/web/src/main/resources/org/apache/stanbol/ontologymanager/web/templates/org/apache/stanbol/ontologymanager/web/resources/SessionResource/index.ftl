<#import "/imports/common.ftl" as common>
<#import "/imports/ontonetDescription.ftl" as ontonetDescription>

<#escape x as x?html>
  <@common.page title="Apache Stanbol OntoNet session manager" hasrestapi=false>
		
    <div class="panel" id="webview">
      <p>This is the start page of the ontology session manager.</p>
    </div>
    
    <hr>
    <#include "inc_sessionmgr.ftl">

  </@common.page>
</#escape>