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
<#import "facts_common.ftl" as common>
<#escape x as x?html>
<@common.page>

<ul>
	<li><a href="#Create_a_New_Fact_Schema">Create a New Fact Schema</a></li>
	<li><a href="#Get_Fact_Schema">Get Fact Schema</a></li>
	<li><a href="#Store_Facts">Store Facts</a></li>
	<li><a href="#Query_for_Facts_of_a_Certain_Type">Query for Facts of a Certain Type</a></li>
</ul>

<a name="Create_a_New_Fact_Schema" id="Create_a_New_Fact_Schema"></a><h4>Create a New Fact Schema</h4>
<table>
	<tr>
		<th valign="top">Description: </th>
		<td>Allows clients to publish new fact schemata to the FactStore. Each fact is an n-tuple where each
		element of that tuple defines a certain type of entity. A fact schema defines which types of entities
		and their roles are part of instances of that fact.</td>
	</tr>
	<tr>
		<th valign="top">Path: </th>
		<td>/factstore/facts/{fact-schema-name}</td>
	</tr>
	<tr>
		<th valign="top">Method:</th>
		<td>PUT with data type application/json returns HTTP 201 (created) on success.</td>
	</tr>
	<tr>
		<th valign="top">Consumes:</th>
		<td>application/json</td>
	</tr>
	<tr>
		<th valign="top">Produces:</th>
		<td>text/plain on error messages</td>
	</tr>
	<tr>
		<th valign="top">Data:</th>
		<td>The fact schema is sent as the PUT payload in JSON-LD format as a JSON-LD profile. The name of the
		fact is given by the URL. The elements of the schema are defined in the "@types" section of the
		JSON-LD "#context". Each element is specified using a unique role name for that entity plus the entity
		type specified by an URN.</td>
	</tr>
	<tr>
		<th valign="top">Example 1:</th>
		<td>PUT /factstore/facts/http%3A%2F%2Fiks-project.eu%2Font%2FemployeeOf<br>
		with the following data 
<pre>{
 "@context" :
 {
  "iks"     : "http://iks-project.eu/ont/",
  "@types"  :
  {
    "person"       : "iks:person",
    "organization" : "iks:organization"
  }
 }
}</pre>
		<p>will create the new fact schema for &quot;employeeOf&quot; at the given URL which is in decoded
		   representation: /factstore/facts/http://iks-project.eu/ont/employeeOf</p>
		<p>Instead one can use the cURL tool for this. Store the fact schema in a JSON file and then use this
		   command.</p>
<pre>curl ${it.publicBaseUri}factstore/facts/http%3A%2F%2Fiks-project.eu%2Font%2FemployeeOf -T spec-example1.json</pre>
		</td>
	</tr>
	<tr>
		<th valign="top">Example 2:</th>
		<td>PUT /factstore/facts/http%3A%2F%2Fwww.schema.org%2FEvent.attendees<br>
		with the following data
<pre>{
 "@context" :
 {
  "sorg"       : "http://www.schema.org/",
  "@types"     :
  {
    "event"    : "sorg:Event",
    "attendee" : ["sorg:Person","sorg:Organization"]
  }
 }
}</pre>
		<p>will create the new fact schema for "attendees" at the given URL which is in decoded
		representation: /factstore/facts/http://www.schema.org/Event.attendees.</p>
		<p><i>Note</i>: That this fact schema uses the ability to define more than one possible type for
		a role. The role 'attendee' can be of type http://www.schema.org/Person or
		http://www.schema.org/Organization.</p>
		</td>
	</tr>
</table>

<script language="javascript">
function putNewFactSchema() {
 $("#newFactSchemaResult").show();
 $.ajax({
   type: "PUT",
   url: '${it.publicBaseUri}factstore/facts/' + encodeURIComponent($("#newFactSchemaName").val()),
   dataType: "text/plain",
   contentType: "application/json",
   data: $("#newFactSchema").val(),
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#newFactSchemaResultText").text(textStatus + " (" + jqXHR.status + ")");
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#newFactSchemaResultText").text(("Error putting new fact schema.\n" + jqXHR.statusText + "\n" + jqXHR.responseText));
   } 
 });		  
}
</script>

<p>To test this feature you can put your own fact schema using the following form.</p>
<form name="newFactSchemaTestForm" id="newFactSchemaTestForm">
<table>
<tr>
	<th valign="top" width="10%">Enter JSON-LD profile that specifies a fact schema:</th>
	<td>
		<b>Fact Schema name:</b>
		<input type="text" name="newFactSchemaName" id="newFactSchemaName" size="40" maxlength="96" /><br>
		<b>Fact Schema definition:</b><br>
		<textarea name="newFactSchema" id="newFactSchema" rows="10" cols="40"></textarea><br>
		<input type="submit" name="putNewFactSchemaButton" value="Put" onclick="putNewFactSchema(); return false;" />
	</td>
</tr>
<tr>
	<th valign="top">Result</th>
	<td>
		<div id="newFactSchemaResult" style="display: none">
			<p><a href="#" onclick="$('#newFactSchemaResult').hide(); return false;">Hide results</a>
			<pre id="newFactSchemaResultText">... waiting for results ...</pre>
		</div>
	</td>
</tr>
</table>
</form>

<a name="Get_Fact_Schema" id="Get_Fact_Schema"></a><h4>Get Fact Schema</h4>
<table>
	<tr>
		<th valign="top">Description: </th>
		<td>Allows clients to get the definition of an existing fact schema.</td>
	</tr>
	<tr>
		<th valign="top">Path: </th>
		<td>/factstore/facts/{fact-schema-name}</td>
	</tr>
	<tr>
		<th valign="top">Method:</th>
		<td>GET with data type application/json returns HTTP 200 on success.</td>
	</tr>
	<tr>
		<th valign="top">Produces:</th>
		<td>application/json</td>
	</tr>	
	<tr>
		<th valign="top">Data:</th>
		<td>The fact schema is returned as a JSON-LD profile.</td>
	</tr>
	<tr>
		<th valign="top">Example:</th>
		<td>GET /factstore/facts/http%3A%2F%2Fiks-project.eu%2Font%2FemployeeOf<br>
		will return the following data:
<pre> {
 "@context" :
 {
  "@types"  :
  {
    "person"       : "http://iks-project.eu/ont/person",
    "organization" : "http://iks-project.eu/ont/organization"
  }
 }
}</pre>
		</td>
	</tr>
</table>

<script language="javascript">
function getFactSchema() {
 $("#getFactSchemaResult").show();
 $.ajax({
   type: "GET",
   url: '${it.publicBaseUri}factstore/facts/' + encodeURIComponent($("#factSchemaURN").val()),
   dataType: "json",
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#getFactSchemaResultText").text(JSON.stringify(data, null, 2));
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#getFactSchemaResultText").text(("Error putting new fact schema.\n" + jqXHR.statusText + "\n" + jqXHR.responseText));
   } 
 });		  
}
</script>

<p>To test this feature you can get an existing fact schema by entering its name.</p>
<form name="getFactSchemaTestForm" id="getFactSchemaTestForm">
<table>
<tr>
	<th valign="top" width="10%">Enter fact schema name:</th>
	<td>
		<input type="text" name="factSchemaURN" id="factSchemaURN" size="40" maxlength="96" /><br>
		<input type="submit" name="getFactSchemaButton" value="Get" onclick="getFactSchema(); return false;" />
	</td>
</tr>
<tr>
	<th valign="top">Result</th>
	<td>
	<div id="getFactSchemaResult" style="display: none">
<p><a href="#" onclick="$('#getFactSchemaResult').hide(); return false;">Hide results</a>
<pre id="getFactSchemaResultText">... waiting for results ...</pre>
</div>
	</td>
</tr>
</table>
</form>

<a name="Store_Facts" id="Store_Facts"></a><h4>Store Facts</h4>
<table>
	<tr>
		<th valign="top">Description: </th>
		<td>Allows clients to store a new facts according to a defined fact schema that was previously
		published to the FactStore. Each new fact is an n-tuple according to its schema where each tuple
		element identifies an entity using its unique IRI.</td>
	</tr>
	<tr>
		<th valign="top">Path: </th>
		<td>/factstore/facts</td>
	</tr>
	<tr>
		<th valign="top">Method:</th>
		<td>POST with data type application/json returns HTTP 201 (created) on success.</td>
	</tr>
	<tr>
		<th valign="top">Consumes:</th>
		<td>application/json</td>
	</tr>
	<tr>
		<th valign="top">Produces:</th>
		<td>text/plain on error messages</td>
	</tr>	
	<tr>
		<th valign="top">Data:</th>
		<td>The facts are sent as the POST payload in JSON-LD format referring to the defined JSON-LD profile.
		The name of the fact is given by the "@profile" element of the JSON-LD object.
		The JSON-LD object contains a list of facts under the attribute "facts" where each
		element of that list is an n-tuple of entity instances according to the fact schema. The instance of
		an entity can be specified either by its unique IRI or by specifying the instance by example.<br>
		Using the instance by example variant requires the FactStore to resolve the entity in an EntityHub.
		An entity by example is specified by defining attributes and required values of the searched entity.
		A fact can only be stored if all entities can be uniquely identified either by their IRI or by
		example.</td>
	</tr>
	<tr>
		<th valign="top">Example 1:</th>
		<td>POST /factstore/facts<br>
		with the following data 
<pre>{
 "@context" : {
   "iks" : "http://iks-project.eu/ont/",
   "upb" : "http://upb.de/persons/"
 },
 "@profile"     : "iks:employeeOf",
 "person"       : { "@iri" : "upb:bnagel" },
 "organization" : { "@iri" : "http://uni-paderborn.de"}
}</pre>
		<p>creates a new fact of type http://iks-project.eu/ont/employeeof specifying that the person
		http://upb.de/persons/bnagel is employee of the organization defined by the IRI
		http://uni-paderborn.de.</p>
		<p>You can store the facts in a JSON file and use the cURL tool like this:</p>
<pre>
curl -d @fact-example1.json -H "Content-Type: application/json" ${it.publicBaseUri}factstore/facts
</pre>		
		</td>
	</tr>
	<tr>
		<th valign="top">Example 2:</th>
		<td>POST /factstore/facts<br>
		with the following data to create several facts of the same type at once
<pre>{
 "@context" : {
   "iks" : "http://iks-project.eu/ont/",
   "upb" : "http://upb.de/persons/"
 },
 "@profile"     : "iks:employeeOf",
 "@subject" : [
   { "person"       : { "@iri" : "upb:bnagel" },
     "organization" : { "@iri" : "http://uni-paderborn.de" }
   },
   { "person"       : { "@iri" : "upb:fchrist" },
     "organization" : { "@iri" : "http://uni-paderborn.de" }
   }
 ]
}</pre>
		<p>creates two new facts of type http://iks-project.eu/ont/employeeof specifying that the persons
		http://upb.de/persons/bnagel and http://upb.de/persons/fchrist are employees of the organization
		defined by the IRI http://uni-paderborn.de.</p>
		</td>
	</tr>
	<tr>
		<th valign="top">Example 3:</th>
		<td>POST /factstore/facts<br>
		with the following data to create several facts of different type
<pre>{
 "@context" : {
   "iks" : "http://iks-project.eu/ont/",
   "upb" : "http://upb.de/persons/"
 },
 "@subject" : [
   { "@profile"     : "iks:employeeOf",
     "person"       : { "@iri" : "upb:bnagel" },
     "organization" : { "@iri" : "http://uni-paderborn.de" }
   },
   { "@profile"     : "iks:friendOf",
     "person"       : { "@iri" : "upb:bnagel" },
     "friend"       : { "@iri" : "upb:fchrist" }
   }
 ]
}</pre>
		<p>creates two new facts. The first one of type http://iks-project.eu/ont/employeeof specifying that
		the person http://upb.de/persons/bnagel is employee of the organization defined by the IRI
		http://uni-paderborn.de. The second of type http://iks-project.eu/ont/friendOf specifying that
		http://upb.de/persons/fchrist is a friend of http://upb.de/persons/bnagel.</p>
		</td>
	</tr>
</table>

<script language="javascript">
function postNewFact() {
 $("#newFactResult").show();
 $.ajax({
   type: "POST",
   url: '${it.publicBaseUri}factstore/facts',
   dataType: "text/plain",
   contentType: "application/json",
   data: $("#newFact").val(),
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#newFactResultText").text(textStatus + " (" + jqXHR.status + ")");
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#newFactResultText").text(("Error posting new facts.\n" + jqXHR.statusText + "\n" + jqXHR.responseText));
   } 
 });		  
}
</script>

<p>To test this feature you can publish your own fact using the following form. Note that the fact
schema must exist before one can publish a fact.</p>
<form name="newFactTestForm" id="newFactTestForm">
<table>
<tr>
	<th valign="top" width="10%">Enter JSON-LD that specifies facts:</th>
	<td>
		<textarea name="newFact" id="newFact" rows="10" cols="40"></textarea><br>
		<input type="submit" name="postNewFactButton" value="Publish" onclick="postNewFact(); return false;" />
	</td>
</tr>
<tr>
	<th valign="top">Result</th>
	<td>
		<div id="newFactResult" style="display: none">
			<p><a href="#" onclick="$('#newFactResult').hide(); return false;">Hide results</a>
			<pre id="newFactResultText">... waiting for results ...</pre>
		</div>
	</td>
</tr>
</table>
</form>

<a name="Query_for_Facts_of_a_Certain_Type" id="Query_for_Facts_of_a_Certain_Type"></a>
<h4>Query for Facts of a Certain Type</h4>
<table>
	<tr>
		<th valign="top">Description: </th>
		<td>Allows clients to query stored facts of a specific type defined by the fact's schema. The clients
		specify the desired fact plus an arbitrary number of entities that play some role in the fact. </td>
	</tr>
	<tr>
		<th valign="top">Path: </th>
		<td>/factstore/query</td>
	</tr>
	<tr>
		<th valign="top">Method:</th>
		<td>POST with data type application/json returns application/json.</td>
	</tr>
	<tr>
		<th valign="top">Consumes:</th>
		<td>application/json</td>
	</tr>
	<tr>
		<th valign="top">Produces:</th>
		<td>application/json</td>
	</tr>	
	<tr>
		<th valign="top">Data:</th>
		<td>The query is specified by a JSON-LD object in the payload of the request. The query defines a
		"select" to specify the desired type of result to be returned in the result set. The "from" part
		specifies the fact type to query and the "where" clause specifies constraints to be fulfilled.<br />
		<br />
	    <i>Note</i>: For the moment constraints only support the equals "=" relation. There may be more
	    relations like ">" in future versions of this specification. If there is more than one constraint all
	    constraints are concatenated by "AND".</td>
	</tr>
	<tr>
		<th valign="top">Example 1:</th>
		<td>POST /factstore/query<br>
		with the following data 
<pre>{
 "@context" : {
   "iks" : "http://iks-project.eu/ont/"
 },
 "select" : [ "person" ],
 "from"   : "iks:employeeOf",
 "where"  : [
   {
     "="  : {
       "organization" : { "@iri" : "http://uni-paderborn.de" }
     }
   }
 ]
}</pre>
		<p>returns the list of all persons participating in the fact of type
		http://iks-project.eu/ont/employeeOf where the organization is http://uni-paderborn.de.</p>
		<p>To send such a query via cURL, store the query in a JSON file and use this command:<p>
<pre>curl -d @query-spec-example1.json -H "Content-Type: application/json" http://localhost:8080/factstore/query</pre>
		<p>The result is sent back in JSON-LD format with the result set specified by the select clause.</p>
<pre>{
 "resultset": [
   { "PERSON" : { "@iri" : "http://upb.de/persons/gengels" } },
   { "PERSON" : { "@iri" : "http://upb.de/persons/ssauer"  } },
   { "PERSON" : { "@iri" : "http://upb.de/persons/bnagel"  } },
   { "PERSON" : { "@iri" : "http://upb.de/persons/fchrist" } }
 ]
}</pre>		
		</td>
	</tr>
</table>

</@common.page>
</#escape>
