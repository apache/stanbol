<#import "/imports/common.ftl" as common>
<#import "/imports/ontonetDescription.ftl" as ontonetDescription>

<#escape x as x?html>
  <@common.page title="Apache Stanbol OntoNet scope manager" hasrestapi=false>
		
    <div class="panel" id="webview">
      <p>This is the start page of the ontology scope manager.</p>
    </div>
    
    <hr>
    <#include "/imports/inc_scopemgr.ftl">
    <#include "/imports/inc_scope.ftl">

  </@common.page>
</#escape>