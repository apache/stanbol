<#import "/imports/common.ftl" as common>
<#import "/imports/reasonersDescription.ftl" as reasonersDescription>

<#escape x as x?html>
<@common.page title="Apache Stanbol Ontology Enrichment" hasrestapi=false> 

   <div class="panel" id="webview">
      <p>This is the start page of the ontology enrichment.</p>
    </div>
    
    <hr>
    <#include "inc_enrichment.ftl">

  </@common.page>
</#escape>