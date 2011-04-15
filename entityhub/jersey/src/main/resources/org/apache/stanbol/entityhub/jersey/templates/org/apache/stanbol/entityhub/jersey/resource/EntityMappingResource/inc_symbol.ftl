<h4>Subresource /mapping/symbol</h4>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>This service allows to retrieve the mapping for a symbol. If no mapping for the parsed URI is
		defined, the service returns a 404 "Not Found". You can retrieve such an URI by looking up the symbol
		of the entity using the <a href="${it.publicBaseUri}entityhub/symbol/lookup">symbol/lookup</a>
		endpoint.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /mapping/symbol?id={uri} </td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>id: The URI of the entity</td>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h5>Example</h5>

<pre>curl ${it.publicBaseUri}entityhub/mapping/symbol?id=urn:org.apache.stanbol:entityhub:symbol.1265dfa5-f39a-a033-4d0a-0dc0bfb53fb7</pre>

<h5>Test</h5>

<form id="getMappingForSymbolForm">
<p>Get mapping for symbol
<input type="text" size="50" id="mappingSymbolId" name="id" value="" />
<input type="submit" value="Get Mapping" onclick="getMappingForSymbol(); return false;" /></p>
</form>

<script language="javascript">
function getMappingForSymbol() {
 $("#mappingForSymbolResult").show();
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/mapping/symbol?id=" + $("#mappingSymbolId").val(),
   dataType: "text/plain",
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#mappingForSymbolResultText").text(data.toString());
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#mappingForSymbolResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="mappingForSymbolResult" style="display: none">
<p><a href="#" onclick="$('#mappingForSymbolResult').hide(); return false;">Hide results</a>
<pre id="mappingForSymbolResultText">... waiting for results ...</pre>
</div>
