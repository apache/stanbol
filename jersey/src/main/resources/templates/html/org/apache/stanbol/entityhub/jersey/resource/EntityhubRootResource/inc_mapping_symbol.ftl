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
<h3>Subresource /mapping/symbol</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>This service allows to retrieve the mapping for a entity managed by
		the Entityhub. If no mapping for the parsed URI is
		defined, the service returns a 404 "Not Found". 
		You can retrieve such an URI by 
		<a href="${it.publicBaseUri}entityhub/find">searching</a> for Entities 
		managed by the Entityhub.
		</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /mapping/symbol?id={uri} </td>
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

<h4>Example</h4>

<pre>curl ${it.publicBaseUri}entityhub/mapping/symbol?id=urn:org.apache.stanbol:entityhub:symbol.1265dfa5-f39a-a033-4d0a-0dc0bfb53fb7</pre>

<h4>Test</h4>

<form id="getMappingForSymbolForm">
<p>Get mapping for symbol
<input type="text" size="50" id="mappingSymbolId" name="id" value="" />
<input type="submit" value="Get Mapping" onclick="getMappingForSymbol(); return false;" /></p>
</form>

<script language="javascript">
function getMappingForSymbol() {
 $("#mappingForSymbolResult").show();
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/mapping/symbol?id=" + $("#mappingSymbolId").val(),
   dataType: "text",
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#mappingForSymbolResultText").text(data.toString());
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#mappingForSymbolResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="mappingForSymbolResult" style="display: none">
<p><a href="#" onclick="$('#mappingForSymbolResult').hide(); return false;">Hide results</a>
<pre id="mappingForSymbolResultText">... waiting for results ...</pre>
</div>
