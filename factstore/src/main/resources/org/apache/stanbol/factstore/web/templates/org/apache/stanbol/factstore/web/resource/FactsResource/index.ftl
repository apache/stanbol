<#import "facts_common.ftl" as common>
<#escape x as x?html>
<@common.page>

<ul>
	<li><a href="#Publish_a_New_Fact_Schema">Publish a New Fact Schema</a></li>
	<li><a href="#Get_Fact_Schema">Get Fact Schema</a></li>
</ul>

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
		<th valign="top">Example 1:</th>
		<td>PUT /factstore/facts/http%3A%2F%2Fwww.schema.org%2FEvent.attendees<br>
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
	<tr>
		<th valign="top">Example 2:</th>
		<td>PUT /factstore/facts/http%3A%2F%2Fwww.schema.org%2FEvent.attendees<br>
		with the following data
<pre>{
 "@context" :
 {
  "sorg"       : "http://www.schema.org/",
  "#types"     :
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
 $("#newFactSchemaNameResult").show();
 $.ajax({
   type: "PUT",
   url: '${it.publicBaseUri}factstore/facts/' + encodeURIComponent($("#newFactSchemaName").val()),
   dataType: "json",
   contentType: "text/plain",
   data: $("#newFactSchema").val(),
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#newFactSchemaNameResultText").text(textStatus + " (" + jqXHR.status + ")");
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#newFactSchemaNameResultText").text(("Error putting new fact schema.\n" + jqXHR.statusText + "\n" + jqXHR.responseText));
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
	<div id="newFactSchemaNameResult" style="display: none">
<p><a href="#" onclick="$('#newFactSchemaNameResult').hide(); return false;">Hide results</a>
<pre id="newFactSchemaNameResultText">... waiting for results ...</pre>
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
  "#types"  :
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

</@common.page>
</#escape>
