<#--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
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
      <th valign="top">Description</th>
      <td>Service to load an ontology into the custom space of the scope.</td>
    </tr>
    <tr>
      <th valign="top">Request</th>
      <td>POST <code>/ontonet/ontology/</code>{scopeName}
        <br/>
        Content types :
        <code>application/owl+xml</code>, <code>application/rdf+json</code>, 
        <code>application/rdf+xml</code>, <code>application/x-turtle</code>, 
        <code>multipart/form-data</code>, <code>text/owl-functional</code>, 
        <code>text/owl-manchester</code>, <code>text/plain</code>, 
        <code>text/rdf+n3</code>, <code>text/rdf+nt</code>, <code>text/turtle</code>
      </td>
    </tr>
    <tr>
      <th valign="top">Parameters</th>
      <td>
      For <code>multipart/form-data</code> content:<br/>
      <ul>
        <li><code>file</code>: the physical URL of the ontology to be loaded. </li>
        <li><code>format</code>: combined with <code>file</code>, the format of the submitted file. 
      If not supplied, all known parsers will be tried until one succeeds or all fail. </li>
        <li><code>library</code>: the identifier of the ontology library whose ontologies have to be loaded. 
      The available libraries are shown on the /ontonet/registry endpoint.</li>
        <li><code>stored</code>: the public key of the ontology to be loaded, if already stored in Stanbol.</li>
        <li><code>url</code>: the physical URL of the ontology to be loaded.</li>
      </ul>
      <br/>
      For <code>text/plain</code> content: will be interpreted as <code>url</code>.
      <br/>
      <br/>
      For any other content type: will be interpreted as <code>file</code>.
      <br/>
      <br/>
      Limitations:
      <ul>
        <li>Only one of <code>file</code>, <code>library</code> or <code>url</code> 
        can be specified in a single POST.
        <li>Only one <i>value</i> for <code>file</code>, <code>library</code> or <code>url</code> per POST is accepted.
        <li><code>stored</code> can have multiple values and can be used in combination with the above parameters.
      </ul>
      </td>
    </tr>
    <tr>
      <th valign="top">Response</th>
      <td>
        <ul>
          <li><code>201 Created</code> if ontology loading was successful.</li> 
          <li><code>303 See Other</code> if ontology loading was successful (for <code>multipart/form-data</code> content).</li> 
          <li><code>400 Bad Request</code> if no proper content or parameters were supplied, 
          or if <code>library</code> or <code>url</code> are not well-formed or do not match an existing resource.</li> 
          <li><code>403 Forbidden</code> if the scope is locked and cannot be modified.</li> 
          <li><code>404 Not Found</code> if no such scope was registered.</li> 
          <li><code>409 Conflict</code> if the supplied ontology was found to have the same ID as one already loaded.</li>          
          <li><code>500 Internal Server Error</code> if ontology loading failed for some other reason.</li> 
        </ul>
     </td>
    </tr>
  </tbody>
</table>

<h5>Examples</h5>
Load and store the SKOS thesaurus of ISO 3166-1 country codes into a scope that manages Geographical content.
<pre>curl -X POST -F "url=http://eulersharp.sourceforge.net/2003/03swap/countries" ${it.publicBaseUri}ontonet/ontology/Geographical</pre>
  or
<pre>curl -H "Content-type: text/plain" -d http://eulersharp.sourceforge.net/2003/03swap/countries ${it.publicBaseUri}ontonet/ontology/Geographical</pre>
<br/>
Load an ontology from local file <tt>acme-hierarchy.owl</tt> in the scope about the ACME organization, knowing the file is in RDF/XML format.
<pre>curl -X POST -F file=@acme-hierarchy.owl -F format=application/rdf+xml ${it.publicBaseUri}ontonet/ontology/ACME</pre>
  or
<pre>curl -H "Content-type: application/rdf+xml" -d @acme-hierarchy.owl ${it.publicBaseUri}ontonet/ontology/ACME
</pre>

<!-- 
  ============= PUT =============
-->
<h4> PUT ontonet/ontology/{scopeName}</h4>
<table>
  <tbody>
    <tr>
      <th valign="top">Description</th>
      <td>Service to get the root ontology of the scope.</td>
    </tr>
    <tr>
      <th valign="top">Request</th>
      <td>PUT <code>/ontonet/ontology/</code>{scopeName}</td>
    </tr>
    <tr>
      <th valign="top">Parameters</th>
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
      <td><code>activate</code>: If <code>true</code>, the ontology scope will be set as active upon creation.
        <br/>
        This parameter is optional, default is <code>false</code>.<
      /td>
    </tr>
    <tr>
      <th>Response</th>
      <td>
        <ul>
          <li><code>201 Created</code></li>
        </ul>
     </td>
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
      <td>Unregisters the ontology scope and unloads its resources, 
      but does not necessarily delete its ontologies.</td>
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
      <th>Response</th>
      <td>
        <ul>
          <li><code>200 OK</code> if deletion was successful.</li>
          <li><code>404 Not Found</code> if no such scope was registered.</li>
          <li><code>500 Internal Server Error</code> if the scope was found but deletion failed for some other reason.</li>
        </ul>
     </td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -X DELETE "${it.publicBaseUri}ontonet/ontology/Users</pre>