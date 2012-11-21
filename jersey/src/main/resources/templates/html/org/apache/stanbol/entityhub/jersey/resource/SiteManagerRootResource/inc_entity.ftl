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
<h3>Subresource /entity?id={URI}</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>This service searches all referenced sites for the entity with the 
    parsed URI and returns the result in the requested entity in the media type. 
    If the requested entity can not be found a 404 is returned.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /entityhub/sites/entity?id={URI}</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>id: the URI of the requested Entity</th>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>

<pre>curl "${it.publicBaseUri}entityhub/sites/entity?id=http://dbpedia.org/resource/Paris"</pre>

<h5>Test</h5>

<a href="#" onclick="searchEntityParis(); return false;">Search for entity 'Paris' in DBPedia</a>.

<script language="javascript">
function searchEntityParis() {
 $("#searchEntityParisResult").show();	  
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/sites/entity",
   data: "id=http://dbpedia.org/resource/Paris",
   dataType: "text",
   cache: false,
   success: function(result) {
     $("#searchEntityParisResultText").text(result);
   },
   error: function(result) {
     $("#searchEntityParisResultText").text(result);
   }
 });		  
}
</script>

<div id="searchEntityParisResult" style="display: none">
<p><a href="#" onclick="$('#searchEntityParisResult').hide(); return false;">Hide results</a>
<pre id="searchEntityParisResultText">... waiting for results ...</pre>
</div>
