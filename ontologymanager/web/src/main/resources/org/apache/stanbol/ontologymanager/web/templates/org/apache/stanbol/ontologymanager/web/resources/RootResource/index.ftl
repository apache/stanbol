<#import "/imports/common.ftl" as common>
<#import "/imports/ontonetDescription.ftl" as ontonetDescription>

<#escape x as x?html>
  <@common.page title="Apache Stanbol Ontonet" hasrestapi=true>

    <@ontonetDescription.view />
		
    <div class="panel" id="webview">
      <p>This is the start page of the ontology network manager.</p>
    </div>

    <div class="panel" id="restapi" style="display: none;">

      <h3>Service Endpoints</h3>
      
        <p>The RESTful API of the Ontology Network Manager is structured as follows.</p>

          <h4>Ontology Scope Management (<code>"/ontonet/ontology"</code>):</h4>
          
            <ul>
              <li>Ontology scope list @<a href="${it.publicBaseUri}ontonet/ontology">/ontonet/ontology</a>:
                Perform CRUD operations on ontology scopes.
              </li>
            </ul>

          <h4>Session Management (<code>"/ontonet/session"</code>):</h4>
      
    </div>

  </@common.page>
</#escape>