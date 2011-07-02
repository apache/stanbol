<#import "/imports/common.ftl" as common>
<#import "/imports/reasonersDescription.ftl" as reasonersDescription>

<#escape x as x?html>
<@common.page title="Apache Stanbol Ontology Consistency Check" hasrestapi=false> 

   <div class="panel" id="webview">
      <p>This is the start page of the ontology consistency check.</p>
    </div>
    
    <hr>
    <#include "inc_consistencyCheck.ftl">

  </@common.page>
</#escape>