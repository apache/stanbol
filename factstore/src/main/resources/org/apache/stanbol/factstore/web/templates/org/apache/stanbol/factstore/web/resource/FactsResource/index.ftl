<#import "facts_common.ftl" as common>
<#escape x as x?html>
<@common.page> 

<a name="Publish_a_New_Fact_Schema" id="Publish_a_New_Fact_Schema"></a><h4>Publish a New Fact Schema</h4>
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
		<th valign="top">Data:</th>
		<td>The fact schema is sent as the PUT payload in JSON-LD format as a JSON-LD profile. The name of the
		fact is given by the URL. The elements of the schema are defined in the "#types" section of the
		JSON-LD "#context". Each element is specified using a unique role name for that entity plus the entity
		type specified by an URN.</td>
	</tr>
	<tr>
		<th valign="top">Example:</th>
		<td>PUT /factstore/facts/http%3A%2F%2Fiks-project.eu%2Font%2FemployeeOf<br />
			with the following data
<pre>{
 "@context" :
 {
  "iks"     : "http://iks-project.eu/ont/",
  "#types"  :
  {
    "person"       : "iks:person",
    "organization" : "iks:organization"
  }
 }
}</pre>
		<p>will create the new fact schema for &quot;employeeOf&quot; at the given URL which is in decoded
		   representation: /factstore/facts/http://iks-project.eu/ont/employeeOf</p>
		</td>
	</tr>
</table>


</@common.page>
</#escape>
