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
<#import "/imports/prevNextButtons.ftl" as buttons>
<@common.page title="Ontology Manager - Store" hasrestapi=true >
<div class="panel" id="webview">
<button class="rightFloat" onClick="javascript: clearStore()">Clear Persistence Store</button>
<br/>
<br/>
<div class="ontology">
	<div class="collapsed">
		<h4 class="ontologySubmitHeader">Submit a new Ontology</h4>
		<form class="ontologyCollapsable" method="POST" accept-charset="utf-8" enctype="application/x-www-form-urlencoded">
		  <fieldset>
			  <legend>Submit raw text in RDF/XML format</legend>
			  <p>Ontology URI: <textarea rows="1" name="ontologyURI"></textarea></p>
			  <p>Ontology Content:<textarea rows="15" name="ontologyContent"></textarea></p>
			  <p><input type="submit" value="Submit Ontology"></p>
		  </fieldset>
		</form>
	</div>
</div>

<#if it.ontologies?size == 0>
	<p><em>There is no ontologies installed, a DBPedia ontology can be submitted by activating eu.iksproject.fise.stores.persistencestore.dbPedia.DBPediaClient component in Configuration tab of Apache Felix Web Console.</em></p>
<#else>
	<fieldset>
		<legend><b>Installed Ontologies</b></legend>
    <#list it.ontologies as ontology>
		<div class="ontology ontologyList ${ontology_index}"> 
			<div class="collapsed">
				<a class="imgOnt" href="${ontology.href}">${ontology.URI}</a>
				<button class="delete" title="Delete ${ontology.URI}" onClick="javascript: deleteOnt('${ontology.href}')"></button>
				<button class="ontologyHeader"></button>
				<ul class= "ontologyCollapsable">
					<li><b>Description:</b> ${ontology.description}</li>
					<li><b>URI:</b> ${ontology.URI}</li>
				</ul>
			</div>
		</div>	
    </#list>
    	<@buttons.prevNextButtons className="ontologyList"/>
    </fieldset>
</#if>
	<script>
	PAGING.adjustVisibility("ontologyList");
	
	function clearStore()
	{
		xmlhttp=new XMLHttpRequest();
		xmlhttp.open('DELETE',"/ontology",false);
		xmlhttp.send();
		location.reload('true');
	}
	
	function deleteOnt(uri)
	{
		xmlhttp=new XMLHttpRequest();
		xmlhttp.open('DELETE',uri,false);
		xmlhttp.send();
		location.reload('true');
	}
	
	$(".ontology .ontologySubmitHeader").click(function () {
	  $(this).parents("div").toggleClass("collapsed");
	});
	
	$(".ontology .ontologyHeader").click(function () {
	  $(this).parent().toggleClass("collapsed");
	}); 
</script>
</div>
<div class="panel" id="restapi" style="display: none;">
<h3>Getting ontologies from the persistence store</h3>
	<p>A list of previously registered ontologies can be obtained by following command:<p>
<pre>
curl -i -X GET -H "Accept:application/xml" http://localhost:8080/persistencestore/ontologies
</pre>
<p>Response:</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:AdministeredOntologies xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink"&gt;
    &lt;ns1:OntologyMetaInformation ns2:href="ontologies/http://www.co-ode.org/ontologies/pizza/pizza.owl"&gt;
        &lt;ns1:URI&gt;http://www.co-ode.org/ontologies/pizza/pizza.owl&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
    &lt;/ns1:OntologyMetaInformation&gt;
    &lt;ns1:OntologyMetaInformation ns2:href="ontologies/http://oiled.man.example.net/facts"&gt;
        &lt;ns1:URI&gt;http://oiled.man.example.net/facts&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
    &lt;/ns1:OntologyMetaInformation&gt;
&lt;/ns1:AdministeredOntologies&gt;

</pre>

<p><b>OntologyMetaInformation</b> element contains URI, and description of an ontology and provides a relative link to that ontology resource.</p>	
<p>This link can be resolved into an absolute URI using http://localhost:8080/persistencestore/ as base URI.</p>  	 	
<h3>Submitting new ontologies to the persistence store</h3>
<p>New ontologies can be submitted with following command</p>
<pre>
curl -i -X POST -H "Accept:application/xml" --data-urlencode ontologyURI=http://iks-project.eu/sample --data-urlencode ontologyContent@sampleOntology.owl http://localhost:8080/persistencestore/ontologies
</pre>
<p>Response :</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:OntologyMetaInformation xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink" ns2:href="ontologies/http://iks-project.eu/sample"&gt;
    &lt;ns1:URI&gt;http://iks-project.eu/sample&lt;/ns1:URI&gt;
    &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
&lt;/ns1:OntologyMetaInformation&gt;
</pre>
<p>Following URIs can be used to observe classes, datatype properties, object properties and individuals respectively.</p>
<ul>
	<li>http://localhost:8080/persistencestore/ontologies/http://iks-project.eu/sample/classes</li>
	<li>http://localhost:8080/persistencestore/ontologies/http://iks-project.eu/sample/datatypeProperties</li>
	<li>http://localhost:8080/persistencestore/ontologies/http://iks-project.eu/sample/objectProperties</li>
	<li>http://localhost:8080/persistencestore/ontologies/http://iks-project.eu/sample/individuals</li>
</ul>
<h3>Clearing the persistence store</h3>
<p>Persistence Store can be cleared using following command</p>
<pre>
curl -i -X DELETE http://localhost:8080/persistencestore/ontologies
</pre>
<p>Response :</p>
<pre>
HTTP/1.1 204 No Content
Server: Jetty(6.1.x)
</pre>
</div>
</@common.page>
