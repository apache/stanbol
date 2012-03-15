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
<#import "/imports/common.ftl" as common>

<@common.page title="${it.metadata.URI}" hasrestapi=true>
<div class="panel" id="webview">
	<fieldset>
		<legend><b>Resource Classification</b></legend>
		<ul>
			<li><a href="${it.metadata.href}/classes">Classes</a></li>
			<li><a href="${it.metadata.href}/objectProperties">Object Properties</a></li>
			<li><a href="${it.metadata.href}/datatypeProperties">Datatype Properties</a></li>
			<li><a href="${it.metadata.href}/individuals">Individuals</a></li>
			<li><a href="${it.metadata.href}/imports">Imports</a></li>
		</ul>
	</fieldset>
</div>
<div class="panel" id="restapi" style="display: none;">
<h3>Getting OntologyMetaInformation</h3>
<pre>
curl -i -X GET -H "Accept:application/xml" http://localhost:8080/persistencestore/${it.metadata.href}
</pre>
<p>Response :</p>
<pre>
<script type="text/javascript">
	function init(){};
	document.write(getResponse("GET", "${it.metadata.href}"));
</script>
</pre>
<h3>Getting RDF/XML Serialization of Ontology</h3>
<pre>
curl -i -X GET -H "Accept:application/rdf+xml" http://localhost:8080/persistencestore/${it.metadata.href}
</pre>
<h3>Deleting Ontology</h3>
<pre>
curl -i -X DELETE http://localhost:8080/persistencestore/${it.metadata.href}
</pre>
<p>Response :</p>
<pre>
HTTP/1.1 204 No Content
Server: Jetty(6.1.x)
</pre>
</div>
</@common.page>
