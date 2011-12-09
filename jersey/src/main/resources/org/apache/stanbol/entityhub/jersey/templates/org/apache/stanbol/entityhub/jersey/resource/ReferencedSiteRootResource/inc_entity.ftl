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
		<td>This service retrieves entities of this referenced site by id. 
    If the requested entity can not be found a 404 is returned.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /entityhub/site/{siteId}/entity?id={URI}</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td><ul>
		    <li>siteId: the id of the referenced Site</li>
		    <li>id: the URI of the requested Entity</li>
		</ul></td>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>

<pre>curl "${it.publicBaseUri}entityhub/site/dbpedia/entity?id=http://dbpedia.org/resource/Paris"</pre>

<h4>Test</h4>

<a href="#" onclick="searchEntityParis(); return false;">Search for entity 'Paris' in DBPedia</a>.

<script language="javascript">
function searchEntityParis() {
 var relpath = "/entity";
 var base = window.location.href.replace(/\/$/, "");
 if(base.lastIndexOf(relpath) != (base.length-relpath.length)){
   base = base+relpath;
 }
 $("#searchEntityParisResult").show();	  
 $.ajax({
   type: "GET",
   url: base,
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
