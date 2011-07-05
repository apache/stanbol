<h4>Subresource /find?name={name}</h4>

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

<pre>curl -X POST -d "name=Bishofsh*&limit=10&offset=0" ${it.publicBaseUri}entityhub/sites/find</pre>

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
   dataType: "text",
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