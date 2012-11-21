<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<#import "/imports/common.ftl" as common>
<#import "/imports/ontonetDescription.ftl" as ontonetDescription>

<#escape x as x?html>
  <@common.page title="Apache Stanbol OntoNet" hasrestapi=true>
  
  <p>
  Stanbol OntoNet implements the API section for managing OWL/OWL2 ontologies, in order to prepare them for consumption by reasoning services, refactorers, rule engines and the like. Once loaded internally from their remote or local resources, ontologies live and are known within the realm they were loaded in. This allows loose-coupling and (de-)activation of ontologies in order to scale the data sets for reasoners to process and optimize them for efficiency. 
  </p>
  
	<span style="font-style:italic">From here you can reach the following sub-endpoints:</span>
	<ul>
	  <li><b><a href="${it.publicBaseUri}ontonet/ontology" title="Apache Stanbol OntoNet Scope Manager">Scope Manager</a></b></li>
	  <li><b><a href="${it.publicBaseUri}ontonet/session" title="Apache Stanbol OntoNet Session Manager">Session Manager</a></b></li>
	  <li><b><a href="${it.publicBaseUri}ontonet/registry" title="Apache Stanbol OntoNet Ontology Libraries">Ontology Libraries</a></b></li>
	</ul>
	
    <div class="panel" id="webview"> 
      <#include "webview.ftl">
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
            <li>Scope manager @<a href="${it.publicBaseUri}ontonet/ontology">/ontonet/ontology</a>:
              Perform CRUD operations on ontology scopes.
            </li>
            <li>Ontology scope @<code>/ontonet/ontology/{scopeName}</code>:
              Manage the set of ontologies loaded within a single scope.
            </li>
<!--
            <li>Ontology retrieval <a href="${it.publicBaseUri}ontonet/ontology/get">/ontonet/ontology/get</a>:
              Manage ontologies whose ID is known but not the scope(s) using it.
            </li>
-->
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
<!--      
          <h4>Graph Management (<code>"/ontonet/graphs"</code>):</h4>
          
          <ul>
            <li>Graph storage @<a href="${it.publicBaseUri}ontonet/graphs">/ontonet/graphs</a>:
              Storage and retrieval operation of RDF graphs, scope-independent.
            </li>
          </ul>
-->          
          <hr>
          <#include "/imports/inc_scopemgr.ftl">
          <#include "/imports/inc_scope.ftl">
          <#include "/imports/inc_sessionmgr.ftl">
    </div>

  </@common.page>
</#escape>