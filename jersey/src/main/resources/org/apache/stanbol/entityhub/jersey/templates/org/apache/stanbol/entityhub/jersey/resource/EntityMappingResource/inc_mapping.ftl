<h4>Subresource /mapping?id={URI}</h4>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>Service to get a mapping by its ID which is encoded as an URI. You need to know the mapping URI
		to use this service and you can retrieve such a mapping URI by creating a new symbol using the
		<a href="${it.publicBaseUri}entityhub/symbol/lookup">symbol/lookup</a> endpoint.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /entityhub/mapping?id={uri}</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>id: the URI of the mapping</th>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h5>Example</h5>

<pre>curl "${it.publicBaseUri}entityhub/mapping?id=urn:org.apache.stanbol:entityhub:symbol.1e05cc86-07bb-7281-5a19-0d4d0d8e054e"</pre>

<h5>Test</h5>

<p>To test this 

<form id="getMappingForUriForm">
<p>Get mapping for URI
<input type="text" size="50" id="mappingId" name="id" value="" />
<input type="submit" value="Get Mapping" onclick="getMappingForUri(); return false;" /></p>
</form>

<script language="javascript">
function getMappingForUri() {
 $("#mappingResult").show();
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/mapping",
   data: $("#getMappingForUriForm").serialize(),
   dataType: "json",
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#mappingResultText").text(data);
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#mappingResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="mappingResult" style="display: none">
<p><a href="#" onclick="$('#mappingResult').hide(); return false;">Hide results</a>
<pre id="mappingResultText">... waiting for results ...</pre>
</div>
