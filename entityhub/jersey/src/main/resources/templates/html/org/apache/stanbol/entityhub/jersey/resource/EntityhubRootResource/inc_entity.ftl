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
<h3>Subresource entityhub/entity</h3>
<p>Service to get/create/update and delete Entities managed by the Entityhub.
<h4> GET entityhub/entity</h4>
<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>Service to get</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET /entityhub/entity?id={uri}</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>id: the URI of the entity</th>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>

<pre>curl "${it.publicBaseUri}entityhub/entity?id=</pre>
<h4>Test</h4>

<form id="getEntityForUriForm">
<p>Get entity for URI
<input type="text" size="50" id="entityId" name="id" value="" />
<input type="submit" value="Get Entity" onclick="getEntityForUri(); return false;" /></p>
</form>

<script language="javascript">
function getEntityForUri() {
 $("#entityResult").show();
 $.ajax({
   type: "GET",
   url: "${it.publicBaseUri}entityhub/entity",
   data: $("#getEntityForUriForm").serialize(),
   dataType: "text",
   cache: false,
   success: function(data, textStatus, jqXHR) {
     $("#entityResultText").text(data);
   },
   error: function(jqXHR, textStatus, errorThrown) {
     $("#entityResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
   }
 });		  
}
</script>

<div id="entityResult" style="display: none">
<p><a href="#" onclick="$('#entityResult').hide(); return false;">Hide results</a>
<pre id="entityResultText">... waiting for results ...</pre>
</div>

<h4> Create an Entity</h4>
<table>
<tbody>
    <tr>
        <th>Description</th>
        <td>Service to create entities for the Entityhub.</td>
    </tr>
    <tr>
        <th>Request</th>
        <td>POST /entityhub/entity?[id={uri}]&[update=true/false]</td>
    </tr>
    <tr>
        <th>Parameter</th>
        <td><b>id:</b> optional the id of the Entity to add. If an id is parsed it is
        ensured that regardless of the included data only the entity with the
        parsed id is created. Information for other ids will be ignored.<br>
        <b>update:</b> Switch that allows to allow updates to existing entities
        for POST requests. Default is <code>false</code>
    </tr>
    <tr>
        <th>Produces</th>
        <td>201 with an link to the created entity</td>
    </tr>
</tbody>
</table>
<h4>Examples:</h4>
<p> The following request would create all Entities defines within {file.rdf} in
the entityhub. If any of such Entities already exists within the Entityhub the
request would fail with BAD REQUEST</p>
<pre>curl -i -X POST -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/entity</pre>
<p> Here the same request, but now it would be also allowed to update existing
Entities</p>
<pre>curl -i -X POST -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/entity?update=true</pre>
<p>This request would only import the Entity with the id {id} while ignoring
all triples with a subject other that {id} contained within {file.rdf}</p>
<pre>curl -i -X POST -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/entity?id={id}</pre>

<h4> Update an Entity</h4>
<table>
<tbody>
    <tr>
        <th>Description</th>
        <td>Service to update an Entity already managed by the Entityhub</td>
    </tr>
    <tr>
        <th>Request</th>
        <td>PUT /entityhub/entity?[id={uri}]&[create=true/false]</td>
    </tr>
    <tr>
        <th>Parameter</th>
        <td><b>id:</b> optional the id of the Entity to update. If an id is parsed it is
        ensured that regardless of the parsed data only information of this entity
        are updated.<br>
        <b>create:</b> Switch that allows to enable/disable the creation of new
        Entities for update (PUT) requests. The default <code>true</code>.
        </td>
        <td>
    </tr>
    <tr>
        <th>Produces</th>
        <td>200 with the data of the entity encoded in the format specified by
        the Accept header</td>
    </tr>
</tbody>
</table>
<h4>Examples:</h4>
<p> The following request would update/create all Entities defines within {file.rdf} in
the entityhub. Non existent Entities will be created and already existing one will be
updated (replaced with the submitted version).</p>
<pre>curl -i -X PUT -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/entity</pre>
<p> This request would only update Entities. If any of the Entities in {file.rdf}
would not already be present within the Entityhub this request would return a
BAD REQUEST.</p>
<pre>curl -i -X PUT -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/entity?create=false</pre>
<p>This request would update the Entity with the id {id} while ignoring
all triples with a subject other that {id} contained within {file}. If an
entity with {id} is not yet present within the Entityhub, than a BAD REQUEST would
be returned</p>
<pre>curl -i -X POST -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/entity?id={id}&create=false</pre>

<h4> Delete an/all Entities</h4>
<table>
<tbody>
    <tr>
        <th>Description</th>
        <td>Service to delete an/all entities managed by the Entityhub</td>
    </tr>
    <tr>
        <th>Request</th>
        <td>DELETE /entityhub/entity?id={uri}</td>
    </tr>
    <tr>
        <th>Parameter</th>
        <td>id: The {uri} of the Entity to delete or '*' to delete all Entities</th>
    </tr>
    <tr>
        <th>Produces</th>
        <td> 
          Status "200 OK" with:
          <ul>
            <li>the deleted entity encoded in the format specified by the Accept header</li>
            <li>an empty response if all entities where deleted ('*' was parsed as URI</li>
          </ul>
          Status "404 NOT FOUND" if no Entity with the parsed URI is managed 
          by the Entityhub
        </td>
    </tr>
</tbody>
</table>
