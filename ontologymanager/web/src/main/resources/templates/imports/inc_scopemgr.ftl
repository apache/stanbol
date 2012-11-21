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
<h4>Subresource ontonet/ontology</h4>
<p>Service to get/clear the set of registered and/or active ontology scopes.</p>

<h4> GET ontonet/ontology</h4>
<table>
  <tbody>
    <tr>
      <th>Description</th>
      <td>Service to get the set of registered and/or active ontology scopes</td>
    </tr>
    <tr>
      <th>Request</th>
      <td>GET <code>/ontonet/ontology</code></td>
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

<pre>curl -H &quot;Accept:application/rdf+xml&quot; "${it.publicBaseUri}ontonet/ontology?with-inactive=true</pre>


<h4> DELETE ontonet/ontology</h4>
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

<pre>curl -X DELETE "${it.publicBaseUri}ontonet/ontology</pre>