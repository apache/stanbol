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
<#escape x as x?html>
<@common.page title="FactStore" hasrestapi=true> 

<div class="panel" id="webview">

<h3>What is the FactStore?</h3>

<p>The FactStore is a Stanbol service to store facts about entities. We interpret the relation between
N entities as a fact. Each entity of such a relation is identified by its URI. For example, the relation that
a person works for a company can be stored as a fact. This is done by relating the entity URI of the person
to the entity URI of the organization and naming this relation 'WorksFor'.</p>

<p>The FactStore allows you to store relations with an arbitrary number of participants. We call this
N-ary facts. For example, the fact that a person sent a letter to another person is a (person x mail x
person) relation and could be named 'PersonWritesToPerson'. What kind of facts you want to store depends
on your use case.</p>

<p>Each participant of a fact is stored by its URI. On a technical level you relate URIs to URIs. On a
semantic level you relate entities represented by their URIs. In consequence, the FactStore is not designed
for storing properties of entities. The FactStore assumes that you already have stored your entities and
that you are able to resolve these entities by their URIs.</p>

<h4>FactStore and EntityHub</h4>

<p>To store and manage entities you should use the <a href="${it.publicBaseUri}entityhub">Stanbol EntityHub.</a>
This service allows you to create entities with their properties. For example, the entity of a person with
its name, date of birth, and address. The Stanbol EntityHub will assign an URI to each entity. This URI can
be used within the FactStore.</p>

<p><em>Note</em>: At the moment the FactStore is not integrated with the EntityHub. But you can use both
services in combination through their RESTful API.</p>

<h3>Specification</h3>

<p>The FactStore specification proposal can be found online at the Apache Stanbol
<a href="http://incubator.apache.org/stanbol/docs/trunk/factstore/specification.html" target="_blank">web site</a>.</p>

<h3>Implementation Status</h3>

<p>The current implementation of the FactStore provides these features:</p>

<ul>
	<li><a href="${it.publicBaseUri}factstore/facts#Create_a_New_Fact_Schema">Creation of Fact Schemas</a></li>
	<li><a href="${it.publicBaseUri}factstore/facts#Store_Facts">Storing of Facts</a></li>
	<li><a href="${it.publicBaseUri}factstore/facts#Query_for_Facts_of_a_Certain_Type">Querying for Single Facts</a></li>
</ul>

<p>The following features are planned but not yet implemented:</p>

<ul>
	<li>Querying for Combination of Facts (Simple Reasoning)</li>
	<li>Integration with EntityHub to resolve entities</li>
</ul>

<h3>Example</h3>

<p>Imagine you want to store the fact that the person named John Doe works for
the company Winzigweich. John Doe is represented by the URI
http://www.doe.com/john and the company by http://www.winzigweich.de. This
fact is stored as a relation between the entity http://www.doe.com/john and
http://www.winzigweich.de.</p>

<p>For this, we first need to create a so called fact schema that tells the
FactStore what we would like to store. A fact schema has a unique name (often
an URI is used) to identify it. To specify what kinds of entities we would
like to store, we specify the type of the entities. Each type has an URI and
should be defined by some ontology. For example, we can use the ontology
specified by <a href="http://schema.org/">http://schema.org/</a>.</p>

<p>According to http://schema.org/ a person is of type http://schema.org/Person
and an organization is of type http://schema.org/Organization. We will use
these type information to specify the fact schema
http://factschema.org/worksFor. The specification of a fact schema is written
in JSON-LD, like this:</p>

<pre>{
  "@context" : {
    "#types"  : {
      "person"       : "http://schema.org/Person",
      "organization" : "http://schema.org/Organization"
    }
  }
}</pre>

<p>To create this fact schema in the FactStore we have to store it in a *.json
file, e.g. worksFor.json, and PUT it into the FactStore. The path to put the
fact schema is `/factstore/facts/{factSchemaName}`. So for our example this
would be `/factstore/facts/http://factschema.org/worksFor`. Unfortunately,
this is not a valid URI so that we have to URL-encode the name of the fact
schema. This leads to
`/factstore/facts/http%3A%2F%2Ffactschema.org%2FworksFor`.</p>

<p><em>Note</em>: If you want to avoid this URL-encoding step, you should chose another
name for your fact schema that is not an URI by itself. You are free to do so!</p>

<p>Now to PUT the `worksFor` fact schema we can use this cURL command.</p>

<pre>curl http://localhost:8080/factstore/facts/http%3A%2F%2Ffactschema.org%2FworksFor -T worksFor.json</pre>

<p>After creating the fact schema we can store the fact that John Doe works for
Winzigweich by POSTing it to the FactStore. The fact is specified in JSON-LD
syntax. The `@profile` defines the fact schema where this fact belongs to.</p>

<pre>{
  "@profile"     : "http://factschema.org/worksFor",
  "person"       : { "@iri" : "http://www.doe.com/john" },
  "organization" : { "@iri" : "http://www.winzigweich.de"}
}</pre>

<p>Now we can POST this fact, e.g. stored in fact.json, to the FactStore at
`/factstore/facts`. By using cURL it would be this command:</p>

<pre>curl -d @fact.json -H "Content-Type: application/json" http://localhost:8080/factstore/facts</pre>

<p>On success this will return a 201 (Created) and the URI of the newly created
fact in the location header of the response. To retrieve a fact you can GET it
from the returned URI.</p>

</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Service Endpoints</h3>

<p>The FactStore supports the following service endpoints:</p>

<ul>
	<li>Store @ <a href="${it.publicBaseUri}factstore/facts">/factstore/facts</a></li>
</ul>

</div>

</@common.page>
</#escape>
