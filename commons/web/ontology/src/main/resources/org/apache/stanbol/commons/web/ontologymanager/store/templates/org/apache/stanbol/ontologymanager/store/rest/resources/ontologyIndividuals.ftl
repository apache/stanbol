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
<@common.page title="Datatype Properties Of ${it.metadata.ontologyMetaInformation.URI}" hasrestapi=true>
<div class="panel" id="webview">
		<h4 class="addHeader">Create a new Individual</h4>
		<form method="POST" accept-charset="utf-8" accept="text/html" enctype="application/x-www-form-urlencoded">
		  <fieldset>
			  <legend>Enter Individual URI to be created</legend>
			  <p>Individual URI: <textarea rows="1" name="individualURI"></textarea></p>
			  <p>Class URI: <textarea rows="1" name="classURI"></textarea></p>
			  <p><input type="submit" value="Create Individual"></p>
		  </fieldset>
		</form>
	<#if it.metadata.individualMetaInformation?size == 0>
		<p><em>Currently there is no individuals in this ontology.</em></p>
	<#else>
	<fieldset>
		<legend><b>Individuals</b></legend>
		<#list it.metadata.individualMetaInformation?sort_by("URI") as ind>
			<div class="ontology ontIndList ${ind_index}"> 
			<div class="collapsed">
					<a class="imgOntInd" href="${ind.href}">${ind.URI}</a>
					<button class="delete" title="Delete ${ind.URI}" onClick="javascript: deleteClass('${ind.href}')"></button>
					<div class ="ontologyHeader"></div>
				<ul class= "ontologyCollapsable">
					<li><b>Description:</b> ${ind.description}</li>
					<li><b>Namespace:</b> ${ind.namespace}</li>
					<li><b>Local Name:</b> ${ind.localName}</li>
				</ul>
			</div>	
		</div>
		</#list>
			<@buttons.prevNextButtons className="ontIndList"/>
	</fieldset>
	</#if>
<script>
	PAGING.adjustVisibility("ontIndList");
	
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
<h3>Getting individuals of ontology</h3>
<pre>
curl -i -X GET -H "Accept:application/xml" http://localhost:8080/persistencestore/${it.metadata.ontologyMetaInformation.href}/individuals
</pre>
<p>Response :</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:IndividualsForOntology xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink"&gt;
    &lt;ns1:OntologyMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology/"&gt;
        &lt;ns1:URI&gt;http://dbpedia.org/ontology/&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
    &lt;/ns1:OntologyMetaInformation&gt;
    &lt;ns1:IndividualMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology//individuals/http://iks-project.eu/individuals/SampleIndividual"&gt;
        &lt;ns1:URI&gt;http://iks-project.eu/inds#SampleIndividual&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
        &lt;ns1:Namespace&gt;http://iks-project.eu/inds#&lt;/ns1:Namespace&gt;
        &lt;ns1:LocalName&gt;SampleIndividual&lt;/ns1:LocalName&gt;
    &lt;/ns1:IndividualMetaInformation&gt;
&lt;/ns1:IndividualsForOntology&gt;

</pre>
<h3>Creating a new individual</h3>
<pre>
curl -i -X POST -H "Accept:application/xml" --data-urlencode individualURI=http://iks-project.eu/inds#SampleIndividual --data-urlencode classURI=http://dbpedia.org/ontology/Actor http://localhost:8080/persistencestore/${it.metadata.ontologyMetaInformation.href}/individuals
</pre>
<p>Response</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:IndividualMetaInformation xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink" ns2:href="ontologies/http://dbpedia.org/ontology//individuals/http://iks-project.eu/inds/SampleIndividual"&gt;
    &lt;ns1:URI&gt;http://iks-project.eu/individuals#SampleIndividual&lt;/ns1:URI&gt;
    &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
    &lt;ns1:Namespace&gt;http://iks-project.eu/individuals#&lt;/ns1:Namespace&gt;
    &lt;ns1:LocalName&gt;SampleIndividual&lt;/ns1:LocalName&gt;
&lt;/ns1:IndividualMetaInformation&gt;
</pre>
</div>
</@common.page>