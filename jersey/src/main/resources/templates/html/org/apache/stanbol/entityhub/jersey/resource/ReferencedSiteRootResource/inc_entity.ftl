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
<h3>Subresource /entity?id={URI}</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>This service retrieves entities of this referenced site by id. 
    If the requested entity can not be found a 404 is returned.</td>
	</tr>
	<tr>
		<th>Request</th>
		<td>GET ${it.publicBaseUri}entityhub/site/{siteId}/entity?id={URI}</td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td><ul>
		    <li>siteId: the id of the referenced Site</li>
		    <li>id: the URI of the requested Entity</li>
		</ul></td>
	</tr>
	<tr>
		<th>Produces</th>
		<td>Depends on requested media type</td>
	</tr>
</tbody>
</table>

<h4>Example</h4>

<pre>curl "${it.publicBaseUri}entityhub/site/${it.site.id}/entity?id=http://dbpedia.org/resource/Paris"</pre>

<h4>Test</h4>

<a href="#" onclick="searchEntityParis(); return false;">Dereference Entity 'http://dbpedia.org/resource/Paris' on Site '${it.site.id}'</a>.

<script language="javascript">
function searchEntityParis() {
 var relpath = "/entity";
 var base = window.location.href.replace(/\/$/, "");
 if(base.lastIndexOf(relpath) != (base.length-relpath.length)){
   base = base+relpath;
 }
 $("#searchEntityParisResult").show();	  
 $.ajax({
   type: "GET",
   url: base,
   data: "id=http://dbpedia.org/resource/Paris",
   dataType: "text",
   cache: false,
   success: function(result) {
     $("#searchEntityParisResultText").text(result);
   },
   error: function(result) {
     $("#searchEntityParisResultText").text(result);
   }
 });		  
}
</script>

<div id="searchEntityParisResult" style="display: none">
<p><a href="#" onclick="$('#searchEntityParisResult').hide(); return false;">Hide results</a>
<pre id="searchEntityParisResultText">... waiting for results ...</pre>
</div>

<#if it.managedSite> <#-- create/update/delete is only supported by managed Sites -->
    
    
    <h4> Create an Entity</h4>
    <table>
    <tbody>
        <tr>
            <th>Description</th>
            <td>Service to create entities for the Managed Site.</td>
        </tr>
        <tr>
            <th>Request</th>
            <td>POST ${it.publicBaseUri}entityhub/site/{siteId}/entity?[id={uri}]&[update=true/false]</td>
        </tr>
        <tr>
            <th>Parameter</th>
            <td><b>id:</b> optional the id of the Entity to add. If an id is parsed it is
            ensured that regardless of the included data only the entity with the
            parsed id is created. Information for other ids will be ignored.<br>
            <b>update:</b> Switch that allows to allow updates to existing entities
            for POST requests. Default is <code>false</code>. It is recommended to
            use PUT in case users want to create/update Entities as this is the
            default behavioure of PUT.
        </tr>
        <tr>
            <th>Produces</th>
            <td>201 if you create a single entity <br>
                204 when creating multipel Entities</td>
        </tr>
    </tbody>
    </table>
    <h4>Examples:</h4>
    <p> The following request would create all Entities defines within {file.rdf} in
    the Managed Site. If any of such Entities already exists within the Managed Site the
    request would fail with BAD REQUEST</p>
    <pre>curl -i -X POST -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/site/${it.site.id}/entity"</pre>
    <p> Here the same request, but now it would be also allowed to update existing
    Entities</p>
    <pre>curl -i -X POST -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/site/${it.site.id}/entity?update=true"</pre>
    <p>This request would only import the Entity with the id {id} while ignoring
    all triples with a subject other that {id} contained within {file.rdf}</p>
    <pre>curl -i -X POST -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/site/${it.site.id}/entity?id={id}"</pre>
    
    <h4> Update an Entity</h4>
    <table>
    <tbody>
        <tr>
            <th>Description</th>
            <td>Service to update an Entity already managed by the Managed Site</td>
        </tr>
        <tr>
            <th>Request</th>
            <td>PUT ${it.publicBaseUri}entityhub/site/{siteId}/entity?[id={uri}]&[create=true/false]</td>
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
            <td>204 if the parsed Entities where updated successfully</td>
        </tr>
    </tbody>
    </table>
    <h4>Examples:</h4>
    <p> The following request would update/create all Entities defines within {file.rdf} in
    the Managed Site. Non existent Entities will be created and already existing one will be
    updated (replaced with the submitted version).</p>
    <pre>curl -i -X PUT -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/site/${it.site.id}/entity</pre>
    <p> This request would only update Entities. If any of the Entities in {file.rdf}
    would not already be present within the Managed Site this request would return a
    BAD REQUEST.</p>
    <pre>curl -i -X PUT -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/site/${it.site.id}/entity?create=false</pre>
    <p>This request would update the Entity with the id {id} while ignoring
    all triples with a subject other that {id} contained within {file}. If an
    entity with {id} is not yet present within the Managed Site, than a BAD REQUEST would
    be returned</p>
    <pre>curl -i -X POST -H "Content-Type:application/rdf+xml" -T {file.rdf} "${it.publicBaseUri}entityhub/site/${it.site.id}/entity?id={id}&create=false</pre>
    
    <h4> Delete an/all Entities</h4>
    <table>
    <tbody>
        <tr>
            <th>Description</th>
            <td>Service to delete an/all entities managed by the Managed Site</td>
        </tr>
        <tr>
            <th>Request</th>
            <td>DELETE ${it.publicBaseUri}entityhub/site/{siteId}/entity?id={uri}</td>
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
              by the Managed Site
            </td>
        </tr>
    </tbody>
    </table>
</#if>
