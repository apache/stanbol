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
<@common.page title="Individual Context of ${it.metadata.individualMetaInformation.URI}" hasrestapi=true>
<div class="panel" id="webview">
<script type="text/javascript" src="static/scripts/individualUpdater.js"></script>
<#if it.metadata.containerClasses.classMetaInformation?size != 0>
	<fieldset>
		<legend><b>Types</b></legend>
		<div class="ontology">
			<div class="collapsed">
				<fieldset>
				    <h4 class="ontologySubmitHeader">Add Type</h4>
				  	<div class="ontologyCollapsable">
					  <p>Class URI: <textarea class="typeInput"rows="1" name="classURI"></textarea></p>
					  <p><button onClick="javascript: PSTORE.HTTPHelper.send('POST', '${it.metadata.individualMetaInformation.href}/types', false, false, 'containerClassURIs', $('.typeInput')[0].value)">Add Type</button></p>
					 </div>
				  </fieldset>
			</div>
		</div>
		<#list it.metadata.containerClasses.classMetaInformation?sort_by("URI") as cls>
			<div class="ontology contClss ${cls_index}"> 
				<div class="collapsed">
					<a class="imgOntClass" href="${cls.href}">${cls.URI}</a>
					<button class="delete" title="Delete ${cls.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.individualMetaInformation.href}/types/${cls.URI}', true, false, null, null)"></button>	
					<div class ="ontologyHeader"></div>
					<ul class= "ontologyCollapsable">
						<li><b>Description:</b> ${cls.description}</li>
						<li><b>Namespace:</b> ${cls.namespace}</li>
						<li><b>Local Name:</b> ${cls.localName}</li>
					</ul>
				</div>	
			</div>
		</#list>
		<@buttons.prevNextButtons className="contClss"/>
	</fieldset>
</#if>
	<fieldset>
		<legend><b>Property Assertions</b></legend>
		<div class="props">
			<fieldset>
				<div class="ontology">
					<div class="collapsed">
				    <h4 class="ontologySubmitHeader">Add Property Assertions</h4>
				    	<div class="ontologyCollapsable">
					      <input class="radioData" type="radio" name="group1" value="data" checked>Data Property<br>
						  <input class="radioObject" type="radio" name="group1" value="object" >Object Property<br>
						  <p>Property URI: <textarea class="propertyInput"rows="1" ></textarea></p>
						  <p>Value: <textarea class="valueInput"rows="1" ></textarea></p>
						  <p><button  name="Property Assertion" onClick="javascript: addPropAssAndPost()">Add Property Assertion</button></p>
					 	</div>
					</div>
				</div>
			</fieldset>
<#if it.metadata.propertyAssertions.propertyAssertion?size != 0>
			<table>
				<tbody>
					<tr>
						<th>Property</th><th>Value</th>
					</tr>
					<#list it.metadata.propertyAssertions.propertyAssertion as propAssert> 
							<tr class="propAss ${propAssert_index}">
								<td>
									<#if propAssert.individualMetaInformationOrLiteral?first?is_hash>
										<p class="imgOntObjectProp">&nbsp;&nbsp;&nbsp;&nbsp;</p>
									<#else>
										<p class="imgOntDataProp">&nbsp;&nbsp;&nbsp;&nbsp;</p>
									</#if>
									<a href="${propAssert.propertyMetaInformation.href}">${propAssert.propertyMetaInformation.URI}</a>
								</td>
								<td>
								<#list propAssert.individualMetaInformationOrLiteral as propValue>
									<#if propValue?is_hash>
										<div class="collapsed">
											<a class="imgOntInd" href="${propValue.href}">${propValue.URI}</a>
											<button class="delete" title="Delete ${propValue.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.individualMetaInformation.href}/propertyAssertions/'+PSTORE.HTTPHelper.normalize('${propAssert.propertyMetaInformation.URI}') +'/objects/${propValue.URI}', true, false, null, null)"></button>
											</ul>
										</div>	
										<br>
									<#else>
										${propValue}
										<button class="delete" title="Delete ${propValue}" onClick="javascript: PSTORE.HTTPHelper.send('POST', '${it.metadata.individualMetaInformation.href}/propertyAssertions/'+PSTORE.HTTPHelper.normalize('${propAssert.propertyMetaInformation.URI}') +'/literals', false, true, 'value', '${propValue}')"></button>
										<br>
									</#if>
								</#list>
								</td>
							</tr>
					</#list>
				</tbody>
			</table>
			<@buttons.prevNextButtons className="propAss"/>
		</div>	
</#if>
	</fieldset>

</div>
<div class="panel" id="restapi" style="display: none;">
<h3>Getting individual context of an individual</h3>
<pre>
curl -i -X GET -H "Accept:application/xml" http://localhost:8080/persistencestore/${it.metadata.individualMetaInformation.href}
</pre>
<p>Response :</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:IndividualContext xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink"&gt;
    &lt;ns1:IndividualMetaInformation ns2:href="ontologies/http://cohse.semanticweb.org/ontologies/people/individuals/http://cohse.semanticweb.org/ontologies/people/Walt"&gt;
        &lt;ns1:URI&gt;http://cohse.semanticweb.org/ontologies/people#Walt&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
        &lt;ns1:Namespace&gt;http://cohse.semanticweb.org/ontologies/people#&lt;/ns1:Namespace&gt;
        &lt;ns1:LocalName&gt;Walt&lt;/ns1:LocalName&gt;
    &lt;/ns1:IndividualMetaInformation&gt;
    &lt;ns1:ContainerClasses&gt;
        &lt;ns1:ClassMetaInformation ns2:href="ontologies/http://cohse.semanticweb.org/ontologies/people/classes/http://cohse.semanticweb.org/ontologies/people/person"&gt;
            &lt;ns1:URI&gt;http://cohse.semanticweb.org/ontologies/people#person&lt;/ns1:URI&gt;
            &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
            &lt;ns1:Namespace&gt;http://cohse.semanticweb.org/ontologies/people#&lt;/ns1:Namespace&gt;
            &lt;ns1:LocalName&gt;person&lt;/ns1:LocalName&gt;
        &lt;/ns1:ClassMetaInformation&gt;
    &lt;/ns1:ContainerClasses&gt;
    &lt;ns1:PropertyAssertions&gt;
        &lt;ns1:PropertyAssertion&gt;
            &lt;ns1:PropertyMetaInformation ns2:href="ontologies/http://cohse.semanticweb.org/ontologies/people/objectProperties/http://cohse.semanticweb.org/ontologies/people/has_pet"&gt;
                &lt;ns1:URI&gt;http://cohse.semanticweb.org/ontologies/people#has_pet&lt;/ns1:URI&gt;
                &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
                &lt;ns1:Namespace&gt;http://cohse.semanticweb.org/ontologies/people#&lt;/ns1:Namespace&gt;
                &lt;ns1:LocalName&gt;has_pet&lt;/ns1:LocalName&gt;
            &lt;/ns1:PropertyMetaInformation&gt;
            &lt;ns1:IndividualMetaInformation ns2:href="ontologies/http://cohse.semanticweb.org/ontologies/people/individuals/http://cohse.semanticweb.org/ontologies/people/Huey"&gt;
                &lt;ns1:URI&gt;http://cohse.semanticweb.org/ontologies/people#Huey&lt;/ns1:URI&gt;
                &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
                &lt;ns1:Namespace&gt;http://cohse.semanticweb.org/ontologies/people#&lt;/ns1:Namespace&gt;
                &lt;ns1:LocalName&gt;Huey&lt;/ns1:LocalName&gt;
            &lt;/ns1:IndividualMetaInformation&gt;
            &lt;ns1:IndividualMetaInformation ns2:href="ontologies/http://cohse.semanticweb.org/ontologies/people/individuals/http://cohse.semanticweb.org/ontologies/people/Louie"&gt;
                &lt;ns1:URI&gt;http://cohse.semanticweb.org/ontologies/people#Louie&lt;/ns1:URI&gt;
                &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
                &lt;ns1:Namespace&gt;http://cohse.semanticweb.org/ontologies/people#&lt;/ns1:Namespace&gt;
                &lt;ns1:LocalName&gt;Louie&lt;/ns1:LocalName&gt;
            &lt;/ns1:IndividualMetaInformation&gt;
            &lt;ns1:IndividualMetaInformation ns2:href="ontologies/http://cohse.semanticweb.org/ontologies/people/individuals/http://cohse.semanticweb.org/ontologies/people/Dewey"&gt;
                &lt;ns1:URI&gt;http://cohse.semanticweb.org/ontologies/people#Dewey&lt;/ns1:URI&gt;
                &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
                &lt;ns1:Namespace&gt;http://cohse.semanticweb.org/ontologies/people#&lt;/ns1:Namespace&gt;
                &lt;ns1:LocalName&gt;Dewey&lt;/ns1:LocalName&gt;
            &lt;/ns1:IndividualMetaInformation&gt;
        &lt;/ns1:PropertyAssertion&gt;
    &lt;/ns1:PropertyAssertions&gt;
&lt;/ns1:IndividualContext&gt;
</pre>

<p>Individual Context contains following information:
	<ul>
		<li><b>Container Classes</b> as a list of ClassMetaInformation</li>
		<li><b>Property Assertions</b> as a list of Property Assertion which includes
			<ul>
				<li><b>PropertyMetaInformation</b>, property of the assertion</li>
				<li>IndividualMetaInformation or a literal value, object of the assertion</li>
			</ul>
		</li>
	</ul>
</p>

<h3>Updating individual context of a datatype property</h3>

<fieldset>
	<legend><b>SubPath: /types  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>containerClassURI</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing class URIs which will be added as a type to this individual</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath:  /types/{classURI} Method : DELETE</b></legend> 
	<p>Delete class indicated by class at path parameter from the type list of this object property</p>
</fieldset>

<fieldset>
	<legend><b>SubPath: /propertyAssertions/{propertyURI}  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>objectValues</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing resource URIs which will be added as an assertion on property indicated by {propertyURI} on individual</td>
			</tr>
			<tr>
				<td>literalValues</td>
				<td>List&lt;String&gt;</td>
				<td>A list of literal values which will be added as an assertion on property indicated by {propertyURI} on individual</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath:  /propertyAssertions/{propertyURI}/objects/{resourceURI} Method : DELETE</b></legend> 
	<p>Delete resource indicated by {resourceURI} at path parameter from the list of property assertion of this individual on property indicated by {propertyURI}</p>
</fieldset>

<fieldset>
	<legend><b>SubPath:  /propertyAssertions/{propertyURI}/literals Method : DELETE</b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>value</td>
				<td>List&lt;String&gt;</td>
				<td>A literal  which will be deleted from assertions on property indicated by {propertyURI} on individual</td>
			</tr>
		</body>
	</table> 
	<p>Delete resource indicated by {resourceURI} at path parameter from the list of property assertion of this individual on property indicated by {propertyURI}</p>
	<p><b>You should use POST method with X-HTTP-Method-Override header with value DELETE</b></p>
</fieldset>


</div>
<script type="text/javascript">
 	
 	PAGING.adjustVisibility("contClss");
 	PAGING.adjustVisibility("propAss");
 	
 	var indUtil = PSTORE.indUtil;
 	var individual = indUtil.individual;
 	
 	function addTypeAndPost(){
 		individual.containerClass = ($('.typeInput')[0].value); 
 		 indUtil.post('${it.metadata.individualMetaInformation.href}');
 	}
 	
 	function addPropAssAndPost(){
 		individual.property = ($('.propertyInput')[0].value);
 		if($('.radioData')[0].checked){
 			PSTORE.HTTPHelper.send('POST','${it.metadata.individualMetaInformation.href}/propertyAssertions/' + $('.propertyInput')[0].value, true,false, 'literalValues', $('.valueInput')[0].value  );
 			//individual.literal = ($('.valueInput')[0].value);
 		}else if ($('.radioObject')[0].checked){
 			PSTORE.HTTPHelper.send('POST','${it.metadata.individualMetaInformation.href}/propertyAssertions/' + $('.propertyInput')[0].value, true,false, 'objectValues', $('.valueInput')[0].value  );
 			//individual.individual = ($('.valueInput')[0].value);
 		}
 		// indUtil.post('${it.metadata.individualMetaInformation.href}');
 	}
 	
 	
	
	$(".ontology .ontologyHeader").click(function () {
	  $(this).parents("div").toggleClass("collapsed");
	});
	
	$(".ontology .ontologySubmitHeader").click(function () {
	  $(this).parents("div").toggleClass("collapsed");
	}); 
</script>
</@common.page>