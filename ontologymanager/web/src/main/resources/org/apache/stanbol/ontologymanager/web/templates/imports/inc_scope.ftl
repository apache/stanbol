<h4>Subresource ontonet/ontology/{scopeName}</h4>
<p>Service for full CRUD operations on ontology scopes.</p>

<!-- 
  ============= GET =============
-->
<h4> GET ontonet/ontology/{scopeName}</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to get the root ontology of the scope.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>GET <code>/ontonet/ontology/{scopeName}</code></td>
    </tr>
    <tr>
      <th>Parameters</th>
      <td><span style="font-style:italic">none</span></td>
    </tr>
    <tr>
      <th>Produces</th>
      <td>An ontology. Format depends on requested media type. 404 if the scope does not exist.</td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -H &quot;Accept:application/rdf+xml&quot; "${it.publicBaseUri}ontonet/ontology/User</pre>

<!-- 
  ============= POST =============
-->
<h4> POST ontonet/ontology/{scopeName}</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to load an ontology into the custom space of the scope.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>POST <code>/ontonet/ontology/</code>{scopeName}</td>
    </tr>
    <tr>
      <th>Parameters</th>
      <td><code>location</code>: the physical URL of the ontology to be loaded. 
      </td>
    </tr>
    <tr>
      <th>Produces</th>
      <td>Nothing. Returns Status 200 if successful.</td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -X POST "${it.publicBaseUri}ontonet/ontology/User</pre>

<!-- 
  ============= PUT =============
-->
<h4> PUT ontonet/ontology/{scopeName}</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to get the root ontology of the scope.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>PUT <code>/ontonet/ontology/</code>{scopeName}</td>
    </tr>
    <tr>
      <th>Parameters</th>
      <td><code>corereg</code>: the physical URL of the registry that points to the ontologies to be loaded into the core space. 
        <br/>
        This parameter overrides <code>coreont</code> if both are specified.
      </td>
    </tr>
    <tr>
      <th></th>
      <td><code>coreont</code>: the physical URL of the top ontology to be loaded into the core space. 
        <br/>
        This parameter is ignored if <code>corereg</code> is specified.
      </td>
    </tr>
    <tr>
      <th></th>
      <td><code>customreg</code>: the physical URL of the registry that points to the ontologies to be loaded into the custom space. 
        <br/>
        This parameter is optional. Overrides <code>customont</code> if both are specified.
      </td>
    </tr>
    <tr>
      <th></th>
      <td><code>customont</code>: the physical URL of the top ontology to be loaded into the custom space. 
        <br/>
        This parameter is optional. Ignored if <code>customreg</code> is specified.
      </td>
    </tr>
    <tr>
      <th></th>
      <td><code>activate</code>: If <code>true</code>, the ontology scope will be set as active upon creation.
        <br/>
        This parameter is optional, default is <code>false</code>.<
      /td>
    </tr>
    <tr>
      <th>Produces</th>
      <td>Nothing. Returns Status 200 if successful, 500 otherwise.</td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -X PUT "${it.publicBaseUri}ontonet/ontology/User?corereg=[registry_location]&customont=[ontology_location]</pre>

<!-- 
  ============= DELETE =============
-->
<h4> DELETE ontonet/ontology/{scopeName}</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to unregister the ontology scope and unload its resources.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>DELETE <code>/ontonet/ontology/</code>{scopeName}</td>
    </tr>
    <tr>
      <th>Parameters</th>
      <td>
        None.
      </td>
    </tr>
    <tr>
      <th>Produces</th>
      <td>Nothing. Returns Status 200 if successful, 500 otherwise.</td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -X PUT "${it.publicBaseUri}ontonet/ontology/User?corereg=[registry_location]&customont=[ontology_location]</pre>