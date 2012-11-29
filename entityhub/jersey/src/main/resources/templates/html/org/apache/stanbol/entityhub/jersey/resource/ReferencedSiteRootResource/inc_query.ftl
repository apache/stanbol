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
<h3>Subresource /query</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>Allows to parse JSON serialized field queries to the sites endpoint.</td>
	</tr>
	<tr>
		<th>Request</th>
        <td><code>-X POST -H "Content-Type:application/json" --data "@fieldQuery.json" /entityhub/site/{siteId}/query<code></td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>query: the JSON serialised FieldQuery (see section "FieldQuery JSON format" 
           below)</td>
	</tr>
	<tr>
		<th>Produces</th>
        <td>The results of the query serialised in the format as specified by the
        Accept header</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>

<pre>curl -X POST -H "Content-Type:application/json" --data "@fieldQuery.json" ${it.publicBaseUri}entityhub/site/dbpedia/query</pre>

<p><em>Note</em>: "@fieldQuery.json" links to a local file that contains the parsed
    Fieldquery (see ection "FieldQuery JSON format" for examples).</p>
    
<!-- TODO: Add Example for queries 
<h4>Test</h4>
<p>Execute the FieldQuery:<br>
<form name="ldpathExample" id="ldpathExample">
<table><tr>
<td><strong>Selected:</strong> :</td>
<td><textarea class="input" name="selected" rows="4"></textarea></td>
</tr><tr>
<td><strong>LDPath:</strong> :</td>
<td><textarea class="input" name="ldpath" rows="6">@prefix dct : <http://purl.org/dc/terms/subject/> ;
@prefix geo : <http://www.w3.org/2003/01/geo/wgs84_pos#> ;
name = rdfs:label[@en] :: xsd:string;
labels = rdfs:label :: xsd:string;
comment = rdfs:comment[@en] :: xsd:string;
categories = dct:subject :: xsd:anyURI;
homepage = foaf:homepage :: xsd:anyURI;
latidude = geo:lat :: xsd:decimal;
longitude = geo:long :: xsd:decimal;</textarea></td>
</tr><tr>
<td><strong>Constraints:</strong> :</td>
<td><textarea class="input" name="constraints" rows="4">TODO: add Example</textarea></td>
</tr><tr>
<td><strong>Limit:</strong> :</td>
<td><input type="text" size="5" name="limit" value="10"></td>
</tr><tr>
<td><strong>Offset:</strong> :</td>
<td><input type="text" size="5" name="offset" value="0"></td>
</tr></table>
<input type="submit" value="Query" onclick="executeQuery(); return false;" /></p>
</form>
</p>

<script language="javascript">
function executeQuery() {
 var relpath = "/ldpath";
 var base = window.location.href.replace(/\/$/, "");
 if(base.lastIndexOf(relpath) != (base.length-relpath.length)){
   base = base+relpath;
 }
 $("#ldpathExampleResult").hide();
 $("#ldpathExampleResultText").text("... waiting for results ...");
 $("#ldpathExampleResult").show();
 $.ajax({
   type: 'POST',
   url: base ,
   data: $("#ldpathExample").serialize(),
   dataType: "text",
   cache: false,
   success: function(data) {
     $("#ldpathExampleResultText").text(data);
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#ldpathExampleResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });          
}
</script> -->
