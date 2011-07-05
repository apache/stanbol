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
<pre>curl "${it.publicBaseUri}entityhub/sites/referenced"</pre>

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