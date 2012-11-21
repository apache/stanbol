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
<h3>Subresource /mapping?id={URI}</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>Service to get/add/update and remove Entities managed by the
		Entityhub.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /entityhub/entity?id={uri}</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>id: the URI of the Entity</th>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>

<pre>curl "${it.publicBaseUri}entityhub/mapping?id=</pre>
<h4>Test</h4>

<form id="getMappingForUriForm">
<p>Get mapping for URI
<input type="text" size="50" id="mappingId" name="id" value="" />
<input type="submit" value="Get Mapping" onclick="getMappingForUri(); return false;" /></p>
</form>

<script language="javascript">
function getMappingForUri() {
 $("#mappingResult").show();
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/mapping",
   data: $("#getMappingForUriForm").serialize(),
   dataType: "text",
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#mappingResultText").text(data);
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#mappingResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="mappingResult" style="display: none">
<p><a href="#" onclick="$('#mappingResult').hide(); return false;">Hide results</a>
<pre id="mappingResultText">... waiting for results ...</pre>
</div>
