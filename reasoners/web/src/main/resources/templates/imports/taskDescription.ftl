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
<#macro view name path description>
      <h5 style="font-size: 1em;">${name}</h5>
      <p>Endpoint: @<a href="${it.publicBaseUri}${path}" title="Task: ${name}">/${path}</a></p>
      <div style="padding-left: 10px;">  
          <table>
	          <tbody>
	          <tr><th>Request</th><td><tt>GET</tt> or <tt>POST</tt>:<ul><li><tt>/${path}</tt>: run as foreground job;</li><li><tt>/${path}/job</tt>: run as background job</li> </td>
	          <tr><th>Description</th><td>${description}</td></tr>
	          <tr>
	          	<th>Parameters</th>
	          	<td>
		          	<ul>
		          		<li><tt>url</tt>: a URL pointing to a resource in RDF; the service loads the input from the given url;</li>
		          		<li><tt>file</tt>: an RDF file sent from the client; the service loads the file (only using method <tt>POST</tt> with <tt>Content-type: multipart/form+data</tt>)</li>
	<#if name != "Check">
						<li><tt>target</tt>: a graph name; if given, the service saves the result in the triple store.</li>
    </#if>
						<li><tt>scope</tt>: an Ontonet scope ID; the service adds the scope to the input stream.</li>
						<li><tt>recipe</tt>: a Recipe defined in the Rules module; the service adds the recipe to the reasoner.</li>
						<li><tt>session</tt>: an Ontonet session ID; the service adds the session to the input stream</li>
		          	</ul>
	          	</td>
	          </tr>
	          <tr>
	          	<th>Produces</th>
	          	<td>
	          	If run as background job (<tt>{service}/job</tt>), returns <tt>201 Created</tt>, with the header <tt>Location: {job-monitoring-url}</tt>.<br/>
	<#if name != "Check">
				The result comes as <tt>200 Ok</tt>, if no errors occurred, or <tt>409 Conflict</tt>, if the input is not consistent.</br>
	          	According to the requested media type, the result can be of type: 
	          		<tt>application/rdf+xml</tt>; 
	          		<tt>text/turtle</tt>;
	          		<tt>text/n3</tt>;
	          		<tt>text/html</tt>
	<#else>
				<ul><li><tt>200 Ok</tt>: the input is consistent; or</li><li><tt>409 Conflict</tt>: the input is <b>not</b> consistent.</li></ul>
	</#if>
	          	</td>
	          </tr>
	          </tbody>
          </table>
          <p><b>Examples</b>:
<pre>
$ curl "${it.publicBaseUri}${path}?url=http://xmlns.com/foaf/0.1/"
</pre>
or
<pre>
curl -X POST -H "Content-type: multipart/form-data" -H "Accept: text/turtle" -F file=@foaf.rdf "${it.publicBaseUri}${path}"
</pre>
		  </p>
      </div>
</#macro>
<#macro li name path description>
      <li>${name}: @<a href="${it.publicBaseUri}${path}" title="Task: ${name}">/${path}</a>: ${description}</li>
</#macro>