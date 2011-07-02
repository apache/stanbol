<#import "/imports/common.ftl" as common>
<#import "/imports/reasonersDescription.ftl" as reasonersDescription>

<#escape x as x?html>
<@common.page title="Apache Stanbol Ontology Consistent Refactoring" hasrestapi=false> 

   <div class="panel" id="webview">
      <p>This is the start page of the ontology consistent refactoring.</p>
    </div>
    
    <hr>
    <#include "inc_consistentRefactoring.ftl">

  </@common.page>
</#escape>