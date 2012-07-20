<br>
<h3>The RESTful API of the Contenthub Index</h3>

<h3>Get SemanticIndexes</h3>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to get the SemanticIndex representations according to the given parameters in application/json format or HTML view.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/index</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>name:</b> Name of the indexes to be returned<br>
      <b>endpointType:</b> String representation of the EndpointType of indexes to be returned<br>
      <b>multiple:</b> If this parameter is set to <code>true</code> it returns one or more indexes matching the given conditions, otherwise it returns only one index representation it there is any satisfying the conditions.
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>returns the SemanticIndex representations according to the given parameters in application/json format or HTML view.</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>IndexException</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
Get all indexes:
<pre>
curl -i -X GET "${it.publicBaseUri}contenthub/index"
</pre>

Get default index:
<pre>
curl -i -X GET "${it.publicBaseUri}contenthub/index?name=contenthub"
</pre>

<hr>