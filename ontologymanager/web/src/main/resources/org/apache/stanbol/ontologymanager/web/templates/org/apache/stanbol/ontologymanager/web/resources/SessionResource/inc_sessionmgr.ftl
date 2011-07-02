<h4>Subresource ontonet/session</h4>
<p>Service to get/clear the set of registered and/or active ontology scopes.

<h4> GET ontonet/session</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to get the set of registered and/or active ontology scopes</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>GET <code>/ontonet/session</code></td>
    </tr>
    <tr>
      <th>Parameter</th>
      <td><code>with-inactive</code>: include registered inactive scopes (optional, default is false)</td>
    </tr>
    <tr>
      <th>Produces</th>
      <td>An ontology. Format depends on requested media type</td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -H &quot;Accept:application/rdf+xml&quot; "${it.publicBaseUri}ontonet/ontology?with-inactive=true"</pre>


<h4> DELETE ontonet/session</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to clear all ontology scopes and stored ontologies</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>DELETE <code>/ontonet/ontology</code></td>
    </tr>
        <tr>
      <th>Parameters</th>
      <td><span style="font-style:italic">none</span></td>
    </tr>
    <tr>
      <th>Produces</th>
      <td><span style="font-style:italic">nothing</span></td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -X DELETE "${it.publicBaseUri}ontonet/ontology"</pre>