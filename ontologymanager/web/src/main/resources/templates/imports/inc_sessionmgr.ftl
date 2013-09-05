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
<h4>Subresource ontonet/session</h4>
<p>Service to manage the set of active OntoNet sessions.

<h4> GET ontonet/session</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Gets an RDF graph that describes the registered sessions.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>GET <code>/ontonet/session</code></td>
    </tr>
    <tr>
      <th>Parameters</th>
      <td><i>none</i></td>
    </tr>
    <tr>
      <th>Produces</th>
      <td>
        <tt>application/owl+xml</tt>,
        <tt>application/rdf+json</tt>,
        <tt>application/rdf+xml</tt>,
        <tt>application/x-turtle</tt>,
        <tt>text/owl+functional</tt>,
        <tt>text/owl+manchester</tt>,
        <tt>text/plain</tt>,
        <tt>text/rdf+n3</tt>, 
        <tt>text/rdf+n3</tt>,
        <tt>text/turtle</tt>
      </td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -X GET -H "Accept: text/turtle" ${it.publicBaseUri}ontonet/session</pre>

<h4> PUT ontonet/session/[id]</h4>
Creates a session with the specified ID, if not used already.

<h4> POST ontonet/session</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Creates a session and lets Stanbol choose its identifier.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>POST <code>/ontonet/session</code></td>
    </tr>
    <tr>
      <th>Parameters</th>
      <td><i>none</i></td>
    </tr>
    <tr>
      <th>Produces</th>
      <td><ul>
      <li>201 CREATED, with the session URI in the Location response header.
      <li>503 FORBIDDEN if the session could not be created due to session quota exceeded.
      </ul></td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -X POST ${it.publicBaseUri}ontonet/session

HTTP/1.1 201 Created
Location: ${it.publicBaseUri}ontonet/session/1341413780858
</pre>

<!--
<h4> DELETE ontonet/session</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to remove an ontology from an OntoNet session, or the whole session.</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>DELETE <code>/ontonet/session</code></td>
    </tr>
    <tr>
      <th>Parameters</th>
      <td><code>scope</code>: the ID of the scope.</td>
    </tr>
    <tr>
      <th></th>
      <td><code>session</code>: the ID of the session to perform the deletion on.</td>
    </tr>
    <tr>
      <th></th>
      <td><code>delete</code>: the ID of the ontology to remove from the session. This parameter is optional, if unspecified will result in the removal of the entire session.</td>
    </tr>
    <tr>
      <th>Produces</th>
      <td>Nothing. Returns Status 200 if successful, 500 otherwise.</td>
    </tr>
  </tbody>
</table>

<h5>Example</h5>

<pre>curl -X DELETE "${it.publicBaseUri}ontonet/session?scope=[scope_id]&session=[session_id]&delete=[ontology_id]"</pre>
-->