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
<h3>Subresource /referenced</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>This service returns a json array containing the IDs of all 
    		referenced sites. Sites returned by this Method can be accessed via the SITE 
    		service endpoint.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /entityhub/sites/referenced</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>none</th>
	</tr>
	<tr>
		<th>Produces</th>
		<td>application/json</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>
<pre>curl "${it.publicBaseUri}entityhub/sites/referenced"</pre>

<h4>Example response</h4>
<pre>["http:\/\/localhost:8080\/entityhub\/site\/dbpedia\/",
"http:\/\/localhost:8080\/entityhub\/site\/musicbrainz\/"]</pre>

<h4>Test</h4>

<p>You can check the referenced sites in this installation by
<a href="#" onclick="listReferencedSites(); return false;">clicking here</a>.</p>

<script language="javascript">
function listReferencedSites() {
 $("#listReferencedSitesResult").show();	  
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/sites/referenced",
   data: "",
   dataType: "text",
   cache: false,
   success: function(result) {
     $("#listReferencedSitesResultText").text(result);
   },
   error: function(result) {
     $("#listReferencedSitesResultText").text(result);
   }
 });		  
}
</script>

<div id="listReferencedSitesResult" style="display: none">
<p><a href="#" onclick="$('#listReferencedSitesResult').hide(); return false;">Hide results</a>
<pre id="listReferencedSitesResultText">... waiting for results ...</pre>
</div>