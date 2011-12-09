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
<h3> Subresource LDPath (/ldpath?ldpath={ldpath}&context={context})</h3>
<table>
<tbody>
	<tr>
		<th>Description</th>
        <td>This service can be used to execute an LDPath program on one or more
        Entities (contexts). Both a POST and a GET version are available.</td>
	</tr>
	<tr>
		<th>Request</th>
        <td><ul>
            <li>GET /entityhub/ldpath?ldpath=ldpath&context={context1}[&context={contextn}]</li>
            <li>POST -d "ldpath=ldpath&context={context1}[&context={contextn}]" /entityhub/ldpath</li>
            </ul>
        </td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td><ul><li>ldpath: The LDPath program to execute.</li>
    			<li>context: The id of the entity used as context for the execution
    			of the LDPath program. This parameter can occur multiple times</li>
    		</ul>
    	</td>
	</tr>
	<tr>
		<th>Produces:</th>
		<td>Produces an RDF Graph with the parsed context(s) as subject the
		field selected by the LDPath program as properties and the selected
		values as object. All RDF serialisations are supported however
		JSON-LD seams to be a natural fit for the data created by LDPath.
		JSON-LD is also the default encoding.</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>
<p>The following LDPath statement will be executed on the defined contexts</p>

<pre>curl -X POST -d "context=http://dbpedia.org/resource/Paris&ldpath=name%20%3D%20rdfs%3Alabel%5B%40en%5D%3B" ${it.publicBaseUri}entityhub/sites/ldpath</pre>

<p> NOTE: the LDPath MUST BE URLEncoded. The decoded string of the above example is 
"name = rdfs:label[@en];" and would select the english label of Paris in the field "name".</p>

<h4>Test</h4>

<p>Execute the LDPath on the Context:<br>
<form name="ldpathExample" id="ldpathExample">
    <input type="text" size="150" name="context" value="http://dbpedia.org/resource/Paris">
    <textarea class="input" name="ldpath" rows="10">@prefix dct : <http://purl.org/dc/terms/subject/> ;
@prefix geo : <http://www.w3.org/2003/01/geo/wgs84_pos#> ;
name = rdfs:label[@en] :: xsd:string;
labels = rdfs:label :: xsd:string;
comment = rdfs:comment[@en] :: xsd:string;
categories = dct:subject :: xsd:anyURI;
homepage = foaf:homepage :: xsd:anyURI;
location = fn:concat("[",geo:lat,",",geo:long,"]") :: xsd:string;</textarea>
    <input type="submit" value="Execute" onclick="executeLDPath(); return false;" /></p>
</form>
</p>

<script language="javascript">
function executeLDPath() {
 $("#ldpathExampleResult").hide();
 $("#ldpathExampleResultText").text("... waiting for results ...");
 $("#ldpathExampleResult").show();
 $.ajax({
   type: 'POST',
   url: "${it.publicBaseUri}entityhub/sites/ldpath" ,
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
</script>

<div id="ldpathExampleResult" style="display: none">
<p><a href="#" onclick="$('#findSymbolResult').hide(); return false;">Hide results</a>
<pre id="ldpathExampleResultText">... waiting for results ...</pre>
</div>
