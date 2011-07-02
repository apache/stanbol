<h4>Subresource reasoners/refactor</h4>
<p>Refactoring services that employ a DL reasoner for ensuring/checking consistency.</p>


<h4> GET /reasoners/refactor/consistent</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service that applies refactoring considering consistency check.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>GET <code>/reasoners/refactor/consistent</code></td>
    </tr>
    <tr>
      <th>Parameter</th>
      <td>
      <ul>
      	<li><code>recipe</code>: the IRI of the recipe to apply.</li>
      	<li><code>input-graph</code>: the IRI of the graph to be refactored.</li>
      	<li><code>output-graph</code>: the IRI of the graph to fill with refactored data. If the output-graph param is present, the output is put in the graph; elsewhere is returned back.</li>
      </ul>
      </td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>INSERIRE ESEMPIO1</pre>

<h4> POST /reasoners/refactor/consistent</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service that applies refactoring considering consistency check over a file given as input.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>POST <code>/reasoners/refactor/consistent</code></td>
    </tr>
    <tr>
      <th>Parameter</th>
      <td>
      <ul>
      	<li><code>recipe</code>: the IRI of the recipe to apply.</li>
      	<li><code>input</code>: file containing the RDF data to be refactored.</li>
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

