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