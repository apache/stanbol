<#import "/imports/common.ftl" as common>
<#import "/imports/ontonetDescription.ftl" as ontonetDescription>

<#escape x as x?html>
  <@common.page title="Apache Stanbol OntoNet" hasrestapi=true>

    <@ontonetDescription.view />
		
    <div class="panel" id="webview">
      <p>This is the start page of the ontology network manager.</p>
    </div>

    <div class="panel" id="restapi" style="display: none;">

      <h3>Service Endpoints</h3>
      
        <p>The RESTful API of the Ontology Network Manager is structured as follows.</p>

          <ul>
            <li>Homepage @<a href="${it.publicBaseUri}ontonet">/ontonet</a>:
              This page.
            </li>
          </ul>

          <h4>Ontology Scope Management (<code>"/ontonet/ontology"</code>):</h4>
          
          <ul>
            <li>Ontology scope registry @<a href="${it.publicBaseUri}ontonet/ontology">/ontonet/ontology</a>:
              Perform CRUD operations on ontology scopes.
            </li>
            <li>Ontology scope @<code>/ontonet/ontology/{scopeName}</code>:
              Perform CRUD operations on ontology scopes.
            </li>
            <li>Ontology scope <a href="${it.publicBaseUri}ontonet/ontology/get">/ontonet/ontology/get</a>:
              Perform CRUD operations on ontology scopes.
            </li>
            <li>Ontology within scope @<code>/ontonet/ontology/{scopeName}/{ontologyID}</code>:
              Perform CRUD operations on ontology scopes.
            </li>
          </ul>

          <h4>Session Management (<code>"/ontonet/session"</code>):</h4>
      
          <ul>
            <li>Ontology scope registry @<a href="${it.publicBaseUri}ontonet/session">/ontonet/session</a>:
              Perform CRUD operations on ontology scopes.
            </li>
            <li>Ontology scope @<code>/ontonet/session/{sessionId}</code>:
              Perform CRUD operations on ontology scopes.
            </li>
          </ul>
      
          <h4>Graph Management (<code>"/ontonet/graphs"</code>):</h4>
          
          <ul>
            <li>Graph storage @<a href="${it.publicBaseUri}ontonet/graphs">/ontonet/graphs</a>:
              Storage and retrieval operation of RDF graphs, scope-independent.
            </li>
          </ul>
      
    </div>

  </@common.page>
</#escape>