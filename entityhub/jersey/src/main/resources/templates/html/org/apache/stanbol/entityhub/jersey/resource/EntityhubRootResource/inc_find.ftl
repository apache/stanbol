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
    			<li>ldpath: The LDPath program executed for entities selected by the find query (optionally). 
    			The LDPath program needs to be URLEncoded.</li>
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

<form name="findForm" id="findForm">
<p>Find entities with <br>
<table><tr>
    <td><strong>Name:</strong></td>
    <td><input type="text" name="name" size="40" value="Paderb*"/> (required)<br>
    This supports Wildcards such as 'Exam?le*'</td>
  </tr><tr>
    <td><strong>Language:</strong></td>
    <td><input type="text" name="lang" size="10" value="en" />
      (optional, default: any)</td>
  </tr><tr>
    <td><strong>Field:</strong> </td>
    <td><input type="text" name="field" size="60" />
      (optional, reasonable default - usually rdfs:label)</td>
  </tr><tr>
    <td><strong>Limit:</string></td>
    <td> <input type="text" name="limit" size="6" maxlength="6" value="10" />
      (optional, number, default: 10) The maximum number of results</td>
  </tr><tr>
    <td><strong>Offset:</strong> </td>
    <td><input type="text" name="offset" size="6" maxlength="6" value="0" />
      (optional, number, default: 0) The offset of the first returned result</td>
  </tr><tr>
    <td><strong>LDPath:</strong>
    </td><td><textarea class="input" name="ldpath" rows="10">name = rdfs:label[@en] :: xsd:string;
comment = rdfs:comment[@en] :: xsd:string;
categories = dc:subject :: xsd:anyURI;
homepage = foaf:homepage :: xsd:anyURI;
location = fn:concat("[",geo:lat,",",geo:long,"]") :: xsd:string;</textarea><br>
      (optional). LDPath programs can be used to specify what information to return for
      Entities selected by the /find request. This example selects the english
      labels, comments, categories, homepage and builds a string representing the
      location '[{latitude},{longitude}]'.<br></td>
  </tr><tr>
    <td><strong>Output Format:</strong></td>
    <td><select name="format" id="findOutputFormat">
        <option value="application/json">JSON</option>
        <option value="application/rdf+xml">RDF/XML</option>
        <option value="application/rdf+json">RDF/JSON</option>
        <option value="text/turtle">Turtle</option>
        <option value="text/rdf+nt">N-TRIPLES</option>
      </select> (Accept header set to the request)</td>
  </tr>
</table>
<input type="submit" value="Search" onclick="startTestSearch(); return false;" /></p>
</form>

<script language="javascript">
function startTestSearch() {
 $("#testSearchResultText").text("... waiting for results ...");
 $("#testSearchResult").show();
 $.ajax({
   type: "POST",
   beforeSend: function(req) {
        req.setRequestHeader("Accept", $("#findOutputFormat").val());
   },
   url: "${it.publicBaseUri}entityhub/find",
   data: $("#findForm").serialize(),
   dataType: "text",
   cache: false,
   success: function(result) {
     $("#testSearchResultText").text(result);
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#testSearchResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });          
}
</script>

<div id="testSearchResult" style="display: none">
<p><a href="#" onclick="$('#testSearchResult').hide(); return false;">Hide results</a>
<pre id="testSearchResultText">... waiting for results ...</pre>
</div>
