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
<h3>Subresource /entity/find?name={name}</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
        <td>This service can be used to search for Entities in the Entityhub.
        Both a POST and a GET version are available.</td>
	</tr>
	<tr>
		<th>Request</th>
        <td><ul>
            <li>GET /entityhub/find?name={query}&field={field}&lang={lang}&limit={limit}&offset={offset}</li>
            <li>POST -d "name={query}&field={field}&lang={lang}&limit={limit}&offset={offset}" /entityhub/find</li>
            </ul>
        </td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td><ul><li>name: The name of the Entity to search. Supports '*' and '?'</li>
    			<li>field: The name of the field to search the name (optional, 
    			default is "http://www.iks-project.eu/ontology/rick/model/label").</li>
    			<li>language: The language of the parsed name (default: any)</li>
    			<li>limit: The maximum number of returned Entities (optional)</li>
    			<li>offset: The offset of the first returned Entity (default: 0)</li>
    			<li>select: A list of fields included for returned Entities (optional)</li>
    		</ul>
    	</td>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>
<p>The following query would search for Entities with a 'rdfs:label' that starts
with 'Pari'.</p>

<pre>curl -X POST -d "name=Pari*&field=http://www.w3.org/2000/01/rdf-schema#label" ${it.publicBaseUri}entityhub/sites/find</pre>

<h4>Test</h4>

<p>Find symbol by searching for 'Paris' for the language 'de'.<br>
<a href="#" onclick="findSymbol(); return false;">Go and find symbol</a>
<form name="findSymbolForm" id="findSymbolForm" style="display: none">
	<input type="hidden" name="name" value="Paris">
	<input type="hidden" name="lang" value="de">
</form>
</p>

<script language="javascript">
function findSymbol() {
 $("#findSymbolResult").hide();
 $("#findSymbolResultText").text("... waiting for results ...");
 $("#findSymbolResult").show();
 $.ajax({
   type: 'POST',
   url: "${it.publicBaseUri}entityhub/find",
   data: $("#findSymbolForm").serialize(),
   dataType: "text",
   cache: false,
   success: function(data) {
     $("#findSymbolResultText").text(data);
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#findSymbolResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="findSymbolResult" style="display: none">
<p><a href="#" onclick="$('#findSymbolResult').hide(); return false;">Hide results</a>
<pre id="findSymbolResultText">... waiting for results ...</pre>
</div>
