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
<@common.page title="Imports Of ${it.metadata.ontologyMetaInformation.URI}" hasrestapi=true>
<div class="panel" id="webview">
		<h4 class="addHeader">Add a new Import</h4>
		<form method="POST" accept="text/html" accept-charset="utf-8" enctype="application/x-www-form-urlencoded">
		  <fieldset>
			  <legend>Enter Ontology URI to be imported</legend>
			  <p>Ontology URI: <textarea id="importURIIn" rows="1" name="importURI"></textarea></p>
			  <p><input id="submitImport" type="submit" value="Add Import"></p>
		  </fieldset>
		</form>
	<#if it.metadata.ontologyImport?size == 0>
  <p><em>No imported ontologies.</em></p>
<#else>
  <fieldset>
    <legend><b>Installed Ontologies</b></legend>
    <#list it.metadata.ontologyImport as ontology>
    <div class="ontology ontologyList ${ontology_index}"> 
      <div class="collapsed">
        <a class="imgOnt" href="${ontology.href}">${ontology.URI}</a>
        <button class="delete" title="Delete ${ontology.URI}" onClick="javascript: deleteImport('${it.metadata.ontologyMetaInformation.href}/imports?importURI=${ontology.URI}')"></button>
        <button class="ontologyHeader"></button>
        <ul class= "ontologyCollapsable">
          <li><b>URI:</b> ${ontology.URI}</li>
        </ul>
      </div>
    </div>  
    </#list>
      <!--<@buttons.prevNextButtons className="ontologyList"/>-->
    </fieldset>
</#if>
<script>
	PAGING.adjustVisibility("ontologyClassesList");

	function deleteImport(uri)
	{
		xmlhttp=new XMLHttpRequest();
		xmlhttp.open('DELETE',uri,false);
		xmlhttp.send();
		location.reload('true');
	}
	
	$("#submitImport").click(function(e){
	  e.preventDefault()
	  $.ajax({
      type: 'POST',
      data: { importURI: $("#importURIIn").val() },
      success: function(){
        location.reload()
      }
      
    });
	});

	$(".ontology .ontologyHeader").click(function () {
	  $(this).parent().toggleClass("collapsed");
	}); 
</script>
</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Getting Classes of ontology</h3>
<pre>
curl -i -X GET -H "Accept:application/xml" http://localhost:8080/persistencestore/${it.metadata.ontologyMetaInformation.href}/classes
</pre>
<p>Response :</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:ClassesForOntology xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink"&gt;
    &lt;ns1:OntologyMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology/"&gt;
        &lt;ns1:URI&gt;http://dbpedia.org/ontology/&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
    &lt;/ns1:OntologyMetaInformation&gt;
    &lt;ns1:ClassMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology//classes/http://dbpedia.org/ontology/PopulatedPlace"&gt;
        &lt;ns1:URI&gt;http://dbpedia.org/ontology/PopulatedPlace&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
        &lt;ns1:Namespace&gt;http://dbpedia.org/ontology/&lt;/ns1:Namespace&gt;
        &lt;ns1:LocalName&gt;PopulatedPlace&lt;/ns1:LocalName&gt;
    &lt;/ns1:ClassMetaInformation&gt;
&lt;/ns1:ClassesForOntology&gt;
</pre>
<h3>Creating a new Class</h3>
<pre>
curl -i -X POST -H "Accept:application/xml" --data-urlencode classURI=http://iks-project.eu/klazzes#SampleClass http://localhost:8080/persistencestore/${it.metadata.ontologyMetaInformation.href}/classes
</pre>
<p>Response</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:ClassMetaInformation xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink" ns2:href="ontologies/http://dbpedia.org/ontology//classes/http://iks-project.eu/klazzes/SampleClass"&gt;
    &lt;ns1:URI&gt;http://iks-project.eu/klazzes#SampleClass&lt;/ns1:URI&gt;
    &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
    &lt;ns1:Namespace&gt;http://iks-project.eu/klazess#&lt;/ns1:Namespace&gt;
    &lt;ns1:LocalName&gt;SampleClass&lt;/ns1:LocalName&gt;
&lt;/ns1:ClassMetaInformation&gt;
</pre>
</div>
</@common.page>