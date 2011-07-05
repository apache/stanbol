<h4>Referenced Site Metadata </h4>

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

<h5>Example</h5>
<pre>curl -H "Accept: application/rdf+xml" "${it.publicBaseUri}entityhub/site/dbpedia"</pre>

<h5>Test</h5>

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
   url: window.location.href,
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