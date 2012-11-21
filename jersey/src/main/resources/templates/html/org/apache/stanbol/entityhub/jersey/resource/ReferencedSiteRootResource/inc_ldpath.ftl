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

<h3>Subresource LDPath (/ldpath?ldpath={ldpath}&context={context})</h3>
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
		<td><ul><li>ldpath: The LDPath program to execute. For a detailed description 
		          of the language see the end of this page.</li>
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

<pre>curl -X POST -d "context=http://dbpedia.org/resource/Paris&ldpath=name%20%3D%20rdfs%3Alabel%5B%40en%5D%3B" ${it.publicBaseUri}entityhub/site/dbpedia/ldpath</pre>

<p> NOTE: the LDPath MUST BE URLEncoded. The decoded string of the above example is 
"name = rdfs:label[@en];" and would select the english label of Paris in the field "name".</p>

<h4>Test</h4>

<p>Execute the LDPath on the Context:<br>
<form name="ldpathExample" id="ldpathExample">
	<strong>Context:</strong> <input type="text" size="120" name="context" value="http://dbpedia.org/resource/Paris"><br>
	<strong>LD-Path:</strong><br>
	<textarea class="input" name="ldpath" rows="10">schema:name = rdfs:label[@en];
rdfs:label;
schema:description = rdfs:comment[@en];
categories = dc:subject :: xsd:anyURI;
schema:url = foaf:homepage :: xsd:anyURI;
location = fn:concat("[",geo:lat,",",geo:long,"]") :: xsd:string;</textarea><br>
<strong>Format:</strong> <select name="format" id="findOutputFormat">
        <option value="application/json">JSON-LD</option>
        <option value="application/rdf+xml">RDF/XML</option>
        <option value="application/rdf+json">RDF/JSON</option>
        <option value="text/turtle">Turtle</option>
        <option value="text/rdf+nt">N-TRIPLES</option>
      </select> (Accept header set to the request)<p>
    <input type="submit" value="Execute" onclick="executeLDPath(); return false;" /></p>
</form>

<script language="javascript">
function executeLDPath() {
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
   beforeSend: function(req) {
        req.setRequestHeader("Accept", $("#findOutputFormat").val());
   },
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
<p><a href="#" onclick="$('#ldpathExampleResult').hide(); return false;">Hide results</a>
<pre id="ldpathExampleResultText">... waiting for results ...</pre>
</div>

<p><strong>Other LDPath Examples:</strong><ul>
<li> This select all persons a) directly connected b) member of the same category c)
member of a (direct) sub-category:
      <textarea rows="2" readonly>schema:name = rdfs:label[@en] :: xsd:string ;
people = (* | dc:subject/^dc:subject | dc:subject/^skos:broader/^dc:subject)[rdf:type is dbp-ont:Person] :: xsd:anyURI;</textarea>
<li> Schema translation: The following example converts dbpedia data for to schema.org
      <textarea rows="6" readonly>schema:name = rdfs:label[@en];
schema:description = rdfs:comment[@en];
schema:image = foaf:depiction;
schema:url = foaf:homepage;
schema:birthDate = dbp-ont:birthDate;
schema:deathDate = dbp-ont:deathDate;</textarea>
<li> Simple reasoning: The following shows an example how LD Path can be used to
for deduce additional knowledge for a given context.<br>
In this case sub-property, inverse- and transitive relations as defined by the
SKOS ontology are expressed in LD Path.<br>
NOTE: the rule for 'skos:narrowerTransitive' will not scale in big Thesaurus (e.g. 
for the root node it will return every concept in the Thesaurus).
In contrast the 'skos:broaderTransitive' rule is ok even for big Thesaurus as 
long as they are not cyclic (such as the DBPedia Categories).<br>
<textarea rows="10" readonly>skos:prefLabel;
skos:altLabel;
skos:hiddenLabel;
rdfs:label = (skos:prefLabel | skos:altLabel | skos:hiddenLabel);
skos:notation

skos:inScheme;

skos:broader = (skos:broader | ^skos:narrower);
skos:broaderTransitive = (skos:broader | ^skos:narrower)+;

skos:narrower = (^skos:broader | skos:narrower);
skos:narrowerTransitive = (^skos:broader | skos:narrower)+;

skos:related = (skos:related | skos:relatedMatch);
skos:relatedMatch;
skos:exactMatch = (skos:exactMatch)+;
skos:closeMatch = (skos:closeMatch | (skos:exactMatch)+);
skos:broaderMatch = (^skos:narrowMatch | skos:broaderMatch);
skos:narrowMatch = (skos:narrowMatch | ^skos:broaderMatch);</textarea>
</ul>
</p>