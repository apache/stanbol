<h4>Subresource /mapping/entity</h4>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>This service allows to retrieve the mapping for an entity. If no mapping for the parsed URI is
		defined, the service returns a 404 "Not Found".</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /mapping/entity?id={uri} </td>
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

<pre>curl ${it.publicBaseUri}entityhub/mapping/entity?id=http://dbpedia.org/resource/Paris</pre>

<h5>Test</h5>

<form id="getMappingForEntityForm">
<p>Get mapping for entity
<input type="text" size="50" id="mappingEntityId" name="id" value="" />
<input type="submit" value="Get Mapping" onclick="getMappingForEntity(); return false;" /></p>
</form>

<script language="javascript">
function getMappingForEntity() {
 $("#mappingForEntityResult").show();
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/mapping/entity?id=" + $("#mappingEntityId").val(),
   dataType: "json",
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#mappingForEntityResultText").text(data.toString());
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#mappingForEntityResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="mappingForEntityResult" style="display: none">
<p><a href="#" onclick="$('#mappingForEntityResult').hide(); return false;">Hide results</a>
<pre id="mappingForEntityResultText">... waiting for results ...</pre>
</div>
