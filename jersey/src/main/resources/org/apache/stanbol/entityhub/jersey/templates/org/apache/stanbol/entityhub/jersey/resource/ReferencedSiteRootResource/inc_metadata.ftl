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
<h3>Referenced Site Metadata </h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>A call to the URI of the referenced site with a Accept header
		supported for Representations will retrieve metadata about the
		Referenced site.<p>
		Supported MediaTypes are: <ul>
		<li>application/json (default)</li>
        <li>application/rdf+xml</li>
        <li>text/turtle</li>
        <li>application/x-turtle</li>
        <li>text/rdf+nt</li>
        <li>text/rdf+n3</li>
        <li>application/rdf+json</li>
		</ul>
		</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET -H "Accept: application/rdf+xml" /entityhub/site/{siteId}</td>
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
<pre>curl -H "Accept: application/rdf+xml" "${it.publicBaseUri}entityhub/site/dbpedia"</pre>

<h4>Test</h4>

<p>You can the metadata of this referenced site by
<a href="#" onclick="getSiteMetadata(); return false;">clicking here</a>.</p>

<script language="javascript">
function getSiteMetadata() {
 $("#siteMetadataResult").show();	  
 $.ajax({
   beforeSend: function(req) {
        req.setRequestHeader("Accept", "application/rdf+xml");
   },
   type: "GET",
   url: window.location.href.replace(/\/$/, ""),
   cache: false,
   data: "",
   dataType: "text",
   success: function(result) {
     $("#siteMetadataResultText").text(result);
   },
   error: function(result) {
     $("#siteMetadataResultText").text(result);
   }
 });		  
}
</script>

<div id="siteMetadataResult" style="display: none">
<p><a href="#" onclick="$('#siteMetadataResult').hide(); return false;">Hide results</a>
<pre id="siteMetadataResultText">... waiting for results ...</pre>
</div>