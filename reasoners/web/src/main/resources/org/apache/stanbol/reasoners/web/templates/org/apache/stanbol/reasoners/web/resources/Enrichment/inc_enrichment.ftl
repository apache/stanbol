<h4>Subresource reasoners/enrichment</h4>
<p>Service to run the reasoner over the input data applying the given scope, a session and recipe and returns all the inferred knowledge.</p>

<h4> POST reasoners/enrichment</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to perform a rule based reasoning.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>POST <code>/reasoners/enrichment</code></td>
    </tr>
    <tr>
      <th>Parameter</th>
      <td>
      <ul>
      	<li><code>session</code>: the session IRI used to classify the input.</li>
      	<li><code>scope</code>: the scope IRI used to classify the input.</li>
      	<li><code>recipe</code>: the recipe IRI to apply.</li>
      	<li><code>file</code>: a file to be given as input classified. (Cannot be used in conjunction with <code>input-graph</code>.)</li>
      	<li><code>input-graph</code>: a reference to a graph IRI in the knowledge store. (Cannot be used in conjunction with <code>file</code>.)</li>
      	<li><code>owllink-endpoint</code>: reasoner server end-point URL. (If this parameter is not provided, the system will use the HermiT reasoner HermiT).</li>
      </ul>
      </td>
    </tr>
    <tr>
      <th>Consumes</th>
      <td><code>Content-type:multipart/form-data</code</td>
    </tr>
    <tr>
      <th>Produces</th>
      <td>An ontology. Format depends on requested media type</td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>INSERIRE ESEMPIO1</pre>
<pre>INSERIRE ESEMPIO2</pre>
<pre>INSERIRE ESEMPIO3</pre>

