<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Entityhub" hasrestapi=true> 

<div class="panel" id="webview">
<p>This is the start page of the entity hub.</p>

</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Service Endpoint "/entityhub/sites"</h3>

<h4>Subresource /referenced</h4>

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

<h5>Example</h5>
<pre>curl "http://localhost:8080/entityhub/sites/referenced"</pre>

<h5>Example response</h5>
<pre>["http:\/\/localhost:8080\/entityhub\/site\/dbpedia\/",
"http:\/\/localhost:8080\/entityhub\/site\/musicbrainz\/"]</pre>

<h5>Test</h5>

<p>You can check the referenced sites in this installation by
<a href="#" onclick="listReferencedSites(); return false;">clicking here</a>.</p>

<script language="javascript">
function listReferencedSites() {
 $("#listReferencedSitesResult").show();	  
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/sites/referenced",
   data: "",
   dataType: "text/plain",
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

<h4>Subresource /entity?id={URI}</h4>

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

<h5>Example</h5>

<pre>curl "http://localhost:8080/entityhub/sites/entity?id=http://dbpedia.org/resource/Paris"</pre>

<h5>Test</h5>

<a href="javascript:searchEntityParis()">Search for entity 'Paris' in DBPedia</a>.

<script language="javascript">
function searchEntityParis() {
 $("#searchEntityParisResult").show();	  
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/sites/entity",
   data: "id=http://dbpedia.org/resource/Paris",
   dataType: "text/plain",
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

<h4>Subresource /find?name={query}</h4>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>This service can be used to search all referenced sites for 
    entities with the parsed name. Both a POST and a GET version are available.</td>
	</tr>
	<tr>
		<th>Requests</th>
		<td><ul>
			<li>GET /entityhub/sites/find?name={query}&field={field}&lang={lang}&limit={limit}&offset={offset}</li>
            <li>POST -d "name={query}&field={field}&lang={lang}&limit={limit}&offset={offset}" /entityhub/sites/find</li>
            </ul>
        </td>
	</tr>
	<tr>
		<th>Parameters</th>
		<td><ul>
			<li>name: the name of the entity (supports wildcards e.g. "Frankf*")</li>
            <li>field: the name of the field used for the query. One MUST parse the full
                name. Namespace prefixes are not supported yet. (default is rdfs:label)</li>
    		<li>lang: optionally the language of the parsed name can be defined</li>
    		<li>limit: optionally the maximum number of results</li>
    		<li>offset: optionally the offset of first result</li>
    		</ul>
    	</td>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h5>Example</h5>

<pre>curl -X POST -d "name=Bishofsh*&limit=10&offset=0" http://localhost:8080/entityhub/sites/find</pre>

<h5>Test</h5>

<form>
<p>Start test search for
<input type="text" id="testSearchValue" value="Paderb*" />
<input type="submit" value="Search" onclick="startTestSearch(); return false;" /></p>
</form>

<script language="javascript">
function startTestSearch() {
 $("#testSearchResultText").text("... waiting for results ...");
 $("#testSearchResult").show();
 var data = "name=" + $("#testSearchValue").val() + "&limit=10&offset=0";
 $.ajax({
   type: "POST",
   url: "${it.publicBaseUri}entityhub/sites/find",
   data: data,
   dataType: "text/plain",
   cache: false,
   success: function(result) {
     $("#testSearchResultText").text(result);
   },
   error: function(result) {
     $("#testSearchResultText").text(result);
   }
 });		  
}
</script>

<div id="testSearchResult" style="display: none">
<p><a href="#" onclick="$('#testSearchResult').hide(); return false;">Hide results</a>
<pre id="testSearchResultText">... waiting for results ...</pre>
</div>

<h4>Subresource /query&query={query}</h4>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>Allows to parse JSON serialized field queries to the sites endpoint.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>POST -d "query={query}" /entityhub/sites/query</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>query: the JSON serialized FieldQuery (see section "FieldQuery JSON format" 
           below)</td>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h5>Example</h5>

<pre>curl -X POST -F "query=@fieldQuery.json" http://localhost:8080/entityhub/site/dbpedia/query</pre>

<p><em>Note</em>: "@fieldQuery.json" links to a local file that contains the parsed
    Fieldquery (see ection "FieldQuery JSON format" for examples).</p>
<p><em>Note</em>: This method suffers form very bad performance on SPARQL endpoints that do 
    not support extensions for full text searches. On Virtuoso endpoints do 
    performance well under normal conditions.</p>
<p><em>Note</em>: Optional selects suffers form very bad performance on any SPRQL endpoint.
    It is recommended to select only fields that are used for constraints. If
    more data are required it is recommended to dereference found entities after
    receiving initial results of the query.</p>

</div>

</@common.page>
</#escape>
