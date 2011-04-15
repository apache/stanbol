<h4>Subresource /symbol/find</h4>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>Find a symbol by specifying name, field, language, limit, offset, and selected fields.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>POST /symbol/find</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td><ul><li>name: </li>
    			<li>field: </li>
    			<li>language: </li>
    			<li>limit: </li>
    			<li>offset: </li>
    			<li>select: </li>
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

<pre>curl ${it.publicBaseUri}entityhub/symbol/find</pre>

<h5>Test</h5>

<p>Find symbol by searching for 'Paris' for the language 'de'.<br>
<a href="#" onclick="findSymbol(); return false;">Go and find symbol</a>
<form name="findSymbolForm" id="findSymbolForm" style="display: none">
	<input type="hidden" name="name" value="Paris">
	<input type="hidden" name="lang" value="de">
</form>
</p>

<script language="javascript">
function findSymbol() {
 $("#findSymbolResult").hide();
 $("#findSymbolResultText").text("... waiting for results ...");
 $("#findSymbolResult").show();
 $.ajax({
   type: 'POST',
   url: "${it.publicBaseUri}entityhub/symbol/find",
   data: $("#findSymbolForm").serialize(),
   dataType: "text/plain",
   cache: false,
   success: function(data) {
     $("#findSymbolResultText").text(data);
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#findSymbolResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="findSymbolResult" style="display: none">
<p><a href="#" onclick="$('#findSymbolResult').hide(); return false;">Hide results</a>
<pre id="findSymbolResultText">... waiting for results ...</pre>
</div>
