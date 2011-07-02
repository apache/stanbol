<#import "/imports/common.ftl" as common>
<#import "/imports/reasonersDescription.ftl" as reasonersDescription>

<#escape x as x?html>
<@common.page title="Apache Stanbol Reasoners" hasrestapi=true> 

    <@reasonersDescription.view />
		
    <div class="panel" id="webview">
      <p>This is the start page of the reasoners responsible for classification, consistency checking, and enrichment of an ontology. 
      Reasoning services provided through the built-in HermiT reasoner.</p>
    </div>

    <div class="panel" id="restapi" style="display: none;">

      <h3>Service Endpoints</h3>
      
        <p>The RESTful API of the Reasoners is structured as follows.</p>

          <ul>
            <li>Homepage @<a href="${it.publicBaseUri}reasoners">/reasoners</a>:
              This page.
            </li>
          </ul>

          <h4>Ontology classification (<code>"/reasoners/classify"</code>):</h4>
          
          <ul>
            <li>Ontology classification @<a href="${it.publicBaseUri}reasoners/classify">/reasoners/classify</a>:
              run a classifying reasoner on a RDF input File or IRI on the base of a scope (or an ontology) and a recipe.
            </li>
          </ul>

          <h4>Ontology consistency check (<code>"/reasoners/check-consistency"</code>):</h4>
      
          <ul>
            <li>Ontology consistency @<code>/reasoners/check-consistency?uri={ontology uri}</code>:
             check the consistency of an Ontology.
            </li>
            <li>Ontology consistency @<code>/reasoners/check-consistency/{scope}</code>:
             check the consistency of a Scope.
            </li>
          </ul>
      
          <h4>Ontology enrichment (<code>"/reasoners/enrichment"</code>):</h4>
          
          <ul>
            <li>Ontology enrichment @<a href="${it.publicBaseUri}reasoners/enrichment">/reasoners/enrichment</a>:
              perform a rule based reasoning with a given recipe and scope (or an ontology) to a RDF input specify via its IRI.
            </li>
          </ul>
          
    </div>

  </@common.page>
</#escape>