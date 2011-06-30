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
            <li>Scope registry @<a href="${it.publicBaseUri}ontonet/ontology">/ontonet/ontology</a>:
              Perform CRUD operations on ontology scopes.
            </li>
            <li>Ontology scope @<code>/ontonet/ontology/{scopeName}</code>:
              Manage the set of ontologies loaded within a single scope.
            </li>
            <li>Ontology retrieval <a href="${it.publicBaseUri}ontonet/ontology/get">/ontonet/ontology/get</a>:
              Manage ontologies whose ID is known but not the scope(s) using it.
            </li>
            <li>Ontology within scope @<code>/ontonet/ontology/{scopeName}/{ontologyID}</code>:
              Load/Unload operations on a single ontology loaded within a scope.
            </li>
          </ul>

          <h4>OntoNet Session Management (<code>"/ontonet/session"</code>):</h4>
      
          <ul>
            <li>Session registry @<a href="${it.publicBaseUri}ontonet/session">/ontonet/session</a>:
              Perform CRUD operations on ontology sessions.
            </li>
            <li>OntoNet Session @<code>/ontonet/session/{sessionId}</code>:
              Manage metadata for a single OntoNet session.
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