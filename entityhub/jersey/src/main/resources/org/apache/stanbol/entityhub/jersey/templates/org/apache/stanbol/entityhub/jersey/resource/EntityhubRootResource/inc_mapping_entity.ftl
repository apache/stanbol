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
<h3>Subresource /mapping/entity</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>This service allows to retrieve the mapping for an entity managed by
		a <a href="${it.publicBaseUri}entityhub/sites">Referenced Site</a>.
	    If no mapping for the parsed URI is defined, the service returns a 404 
	    "Not Found".</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /mapping/entity?id={uri} </td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>id: The URI of the entity</td>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h3>Example</h3>

<pre>curl ${it.publicBaseUri}entityhub/mapping/entity?id=http://dbpedia.org/resource/Paris</pre>

<h4>Test</h4>

<form id="getMappingForEntityForm">
<p>Get mapping for entity
<input type="text" size="50" id="mappingEntityId" name="id" value="" />
<input type="submit" value="Get Mapping" onclick="getMappingForEntity(); return false;" /></p>
</form>

<script language="javascript">
function getMappingForEntity() {
 $("#mappingForEntityResult").show();
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/mapping/entity?id=" + $("#mappingEntityId").val(),
   dataType: "json",
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#mappingForEntityResultText").text(data.toString());
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#mappingForEntityResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="mappingForEntityResult" style="display: none">
<p><a href="#" onclick="$('#mappingForEntityResult').hide(); return false;">Hide results</a>
<pre id="mappingForEntityResultText">... waiting for results ...</pre>
</div>
