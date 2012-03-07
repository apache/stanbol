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
<@common.page title="Object Properties Of ${it.metadata.ontologyMetaInformation.URI}" hasrestapi=true>
<div class="panel" id="webview">
		<h4 class="addHeader">Create a new Object Property</h4>
		<form method="POST" accept-charset="utf-8" accept="text/html" enctype="application/x-www-form-urlencoded">
		  <fieldset>
			  <legend>Enter Object Property URI to be created</legend>
			  <p>Object Property URI: <textarea rows="1" name="objectPropertyURI"></textarea></p>
			  <p><input type="submit" value="Create Object Property"></p>
		  </fieldset>
		</form>
	<#if it.metadata.propertyMetaInformation?size == 0>
	<p><em>Currently there is no classes in this ontology.</em></p>
		
	<#else>
	<fieldset>
		<legend><b>Object Properties</b></legend>
		<#list it.metadata.propertyMetaInformation?sort_by("URI") as prop>
			<div class="ontology ontOPropList ${prop_index}"> 
			<div class="collapsed">
					<a class="imgOntObjectProp" href="${prop.href}">${prop.URI}</a>
					<button class="delete" title="Delete ${prop.URI}" onClick="javascript: deleteClass('${prop.href}')"></button>
					<div class="ontologyHeader"></div>
				<ul class= "ontologyCollapsable">
					<li><b>Description:</b> ${prop.description}</li>
					<li><b>Namespace:</b> ${prop.namespace}</li>
					<li><b>Local Name:</b> ${prop.localName}</li>
				</ul>
			</div>	
		</div>
		</#list>
			<@buttons.prevNextButtons className="ontOPropList"/>
	</fieldset>
	</#if>
<script>
	PAGING.adjustVisibility("ontOPropList");

	function deleteClass(uri)
	{
		xmlhttp=new XMLHttpRequest();
		xmlhttp.open('DELETE',uri,false);
		xmlhttp.send();
		location.reload('true');
	}

	$(".ontology .ontologyHeader").click(function () {
	  $(this).parent().toggleClass("collapsed");
	}); 
</script>
</div>
<div class="panel" id="restapi" style="display: none;">
<h3>Getting object properties of ontology</h3>
<pre>
curl -i -X GET -H "Accept:application/xml" http://localhost:8080/persistencestore/${it.metadata.ontologyMetaInformation.href}/objectProperties
</pre>
<p>Response :</p>

<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:ObjectPropertiesForOntology xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink"&gt;
    &lt;ns1:OntologyMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology/"&gt;
        &lt;ns1:URI&gt;http://dbpedia.org/ontology/&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
    &lt;/ns1:OntologyMetaInformation&gt;
    &lt;ns1:PropertyMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology//objectProperties/http://dbpedia.org/ontology/actingheadteacher"&gt;
        &lt;ns1:URI&gt;http://dbpedia.org/ontology/actingheadteacher&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
        &lt;ns1:Namespace&gt;http://dbpedia.org/ontology/&lt;/ns1:Namespace&gt;
        &lt;ns1:LocalName&gt;actingheadteacher&lt;/ns1:LocalName&gt;
    &lt;/ns1:PropertyMetaInformation&gt;
&lt;/ns1:ObjectPropertiesForOntology&gt;
</pre>
<h3>Creating a new object property</h3>
<pre>
curl -i -X POST -H "Accept:application/xml" --data-urlencode objectPropertyURI=http://iks-project.eu/objectProps#SampleObjectProperty http://localhost:8080/persistencestore/${it.metadata.ontologyMetaInformation.href}/objectProperties
</pre>
<p>Response</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:PropertyMetaInformation xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink" ns2:href="ontologies/http://dbpedia.org/ontology//objectPro
perties/http://iks-project.eu/objectProps/SampleObjectProperty"&gt;
    &lt;ns1:URI&gt;http://iks-project.eu/objectProps#SampleObjectProperty&lt;/ns1:URI&gt;
    &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
    &lt;ns1:Namespace&gt;http://iks-project.eu/objectProps#&lt;/ns1:Namespace&gt;
    &lt;ns1:LocalName&gt;SampleObjectProperty&lt;/ns1:LocalName&gt;
&lt;/ns1:PropertyMetaInformation&gt;

</pre>
</div>
</@common.page>