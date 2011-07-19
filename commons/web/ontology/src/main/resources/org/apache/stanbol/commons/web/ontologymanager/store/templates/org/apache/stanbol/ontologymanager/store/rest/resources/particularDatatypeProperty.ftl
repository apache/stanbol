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
<#import "/imports/propertySerializer.ftl" as propSerializer>
<@common.page title="Datatype Property Context of ${it.metadata.propertyMetaInformation.URI}" hasrestapi=true>
<div class="panel" id="webview">
	<!--FIXME Currently superProperties of this property are not changed using html interface-->
	<script type="text/javascript" src="static/scripts/propertyUpdater.js"></script>
<!-- Property Features-->
	<fieldset>
		<legend><b>Property Feature</b></legend>
			<input class="horizontal" name="propFeatFunc"   type="checkbox" <#if it.metadata.isIsFunctional()>checked</#if>>Functional<br>
			<input type="button" value="Save Changes" onClick="javascript: PSTORE.propUtil.post('${it.metadata.propertyMetaInformation.href}')">
	</fieldset>
<!--Domains -->
	<fieldset>
		<legend><b>Datatype Property Domains</b></legend>
		<div class="ontology">
			<div class="collapsed">
				<fieldset>
				    <h4 class="ontologySubmitHeader">Add Domain</h4>
				  	<div class="ontologyCollapsable">
					  <p>Domain URI: <textarea class="domainInput"rows="1" name="classURI"></textarea></p>
					  <p><button  name="Add Domain" onClick="javascript: PSTORE.HTTPHelper.send('POST', '${it.metadata.propertyMetaInformation.href}/domains', false, false, 'domainURIs',$('.domainInput')[0].value )">Add Domain</button></p>
					 </div>
				  </fieldset>
			</div>
		</div>
		<#if it.metadata.domain.classMetaInformationOrBuiltInResource?size == 0>
			<em>There is no ranges declared for this Datatype property.</em>
			
		<#else>
			<#list it.metadata.domain.classMetaInformationOrBuiltInResource as domain>
				<#if domain?keys?seq_contains("href")>
					<div class="ontology domainList ${domain_index}"> 
						<div class="collapsed">
							<a class= "imgOntClass" href="${domain.href}">${domain.URI}</a>
							<button class="delete" title="Delete ${domain.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.propertyMetaInformation.href}/domains/${domain.URI}', true, false, null, null)"></button>	
							<ul class= "ontologyCollapsable">
								<li><b>Description:</b> ${domain.description}</li>
								<li><b>URI:</b> ${domain.URI}</li>
							</ul>
						</div>
					</div>
				<#else>
					<div class="ontology ontologyHeader domainList ${domain_index}">
						<b>URI:</b> ${domain.URI}<button class="delete" title="Delete ${domain.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.propertyMetaInformation.href}/domains/${domain.URI}', true, false, null, null)"></button>
					<div>
				</#if>
			</#list>
				<@buttons.prevNextButtons className="domainList"/>
			
		</#if>
	</fieldset>
<!--Ranges -->
	<fieldset>
		<legend><b>Datatype Property Ranges</b></legend>
		<div class="ontology">
			<div class="collapsed">
				<fieldset>
				    <h4 class="ontologySubmitHeader">Add Range</h4>
				  	<div class="ontologyCollapsable">
					  <p>Range URI: <textarea class="rangeInput"rows="1" name="classURI"></textarea></p>
					  <p><button  name="Add Range" onClick="javascript: PSTORE.HTTPHelper.send('POST', '${it.metadata.propertyMetaInformation.href}/ranges', false, false, 'rangeURIs',$('.rangeInput')[0].value )">Add Range</button></p>
					 </div>
				  </fieldset>
			</div>
		</div>
		<#if it.metadata.range.classMetaInformationOrBuiltInResource?size == 0>
			<em>There is no ranges declared for this Datatype property.</em>
			
		<#else>
			<#list it.metadata.range.classMetaInformationOrBuiltInResource as range>
				<#if range?keys?seq_contains("href")>
					<div class="ontology rangeList ${range_index}"> 
						<div class="collapsed">
							<p class ="ontologyHeader">
							<a class ="imgOntClass" href="${range.href}">${range.URI}</a>
								<button class="delete" title="Delete ${range.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.propertyMetaInformation.href}/ranges/${range.URI}', true, false, null, null)"></button>
							</p>
						</div>
					</div>
				<#else>
					<div class="ontology rangeList ${range_index}">
						<b>URI:</b> ${range.URI}&nbsp;&nbsp;<button class="delete" title="Delete ${range.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.propertyMetaInformation.href}/ranges/${range.URI}', true, false, null, null)"></button>
					</div>
				</#if>
			</#list>
				<@buttons.prevNextButtons className="rangeList"/>
			</fieldset>
		</#if>
	
	
<!-- Equivalent Properties -->
	<#if it.metadata.equivalentProperties.propertyMetaInformation?size !=0>
		<fieldset>
			<legend><b>Equivalent Properties</b></legend>
			<#list it.metadata.equivalentProperties.propertyMetaInformation?sort_by("URI") as prop>
				<div class="ontology equProps ${prop_index}"> 
				<div class="collapsed">
					<a  class="imgOntDataProp" href="${prop.href}">${prop.URI}</a>		
				</div>
			</div>
			</#list>
				<@buttons.prevNextButtons className="equProps"/>
		</fieldset>
	</#if>
	
<!-- Super Properties -->
	<#if it.metadata.superProperties.propertyMetaInformation?size !=0>
		<fieldset>
			<legend><b>Super Properties</b></legend>
			<#list it.metadata.superProperties.propertyMetaInformation?sort_by("URI") as prop>
				<div class="ontology superProps ${prop_index}"> 
				<div class="collapsed">
					<a class="imgOntDataProp" href="${prop.href}">${prop.URI}</a>	
				</div>
			</div>
			</#list>
				<@buttons.prevNextButtons className="superProps"/>
		</fieldset>
	</#if>
	<br>
</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Getting datatype property context of a  class</h3>
<pre>
curl -i -X GET -H "Accept:application/xml" http://localhost:8080/persistencestore/${it.metadata.propertyMetaInformation.href}
</pre>
<p>Response :</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:DatatypePropertyContext xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink" ns1:isFunctional="true"&gt;
    &lt;ns1:PropertyMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology//datatypeProperties/http://dbpedia.org/ontology/added"&gt;
        &lt;ns1:URI&gt;http://dbpedia.org/ontology/added&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
        &lt;ns1:Namespace&gt;http://dbpedia.org/ontology/&lt;/ns1:Namespace&gt;
        &lt;ns1:LocalName&gt;added&lt;/ns1:LocalName&gt;
    &lt;/ns1:PropertyMetaInformation&gt;
    &lt;ns1:Domain&gt;
        &lt;ns1:ClassMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology//classes/http://dbpedia.org/ontology/HistoricPlace"&gt;
            &lt;ns1:URI&gt;http://dbpedia.org/ontology/HistoricPlace&lt;/ns1:URI&gt;
            &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
            &lt;ns1:Namespace&gt;http://dbpedia.org/ontology/&lt;/ns1:Namespace&gt;
            &lt;ns1:LocalName&gt;HistoricPlace&lt;/ns1:LocalName&gt;
        &lt;/ns1:ClassMetaInformation&gt;
    &lt;/ns1:Domain&gt;
    &lt;ns1:Range&gt;
        &lt;ns1:BuiltInResource&gt;
            &lt;ns1:URI&gt;http://www.w3.org/2001/XMLSchema#date&lt;/ns1:URI&gt;
        &lt;/ns1:BuiltInResource&gt;
    &lt;/ns1:Range&gt;
    &lt;ns1:EquivalentProperties/&gt;
    &lt;ns1:SuperProperties/&gt;
&lt;/ns1:DatatypePropertyContext&gt;
</pre>

<p>Datatype property Context contains following information:
	<ul>
	    <li><b>Equivalent Properties</b> as a list of PropertyMetaInformation</li>
		<li><b>Super Properties</b> as a list of PropertyMetaInformation</li>
		<li>Whether the datatype property is <b>functional</b> or not</li>
		<li><b>Domains</b> as a list of resource URIs or ClassMetaInformation</li>
		<li><b>Ranges</b> as a list of resource URIs</li>
	</ul>
</p>

<h3>Updating datatype property context of an datatype property</h3>
<fieldset>
	<legend>Form Paramaters</legend>
	<table class="formParams">
		<head>
			<th width="20%">Parameter Name</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
			<tr>
				<td>isFunctional</td>
				<td>boolean</td>
				<td>Boolean value indicating whether the datatype property is functional</td>
			</tr>
		<body>
		</body>
	</table>
</fieldset>

<fieldset>
	<legend><b>SubPath: /domains  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>domainURIs</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing class URIs which will be added as a domain to this datatype property</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath:  /domains/{domainURI} Method : DELETE</b></legend> 
	<p>Delete domain indicated by domainURI at path parameter from the domain list of this datatype property</p>
</fieldset>

<fieldset>
	<legend><b>SubPath: /ranges  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>rangeURIs</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing class URIs which will be added as a range to this datatype property</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath:  /ranges/{rangeURI} Method : DELETE</b></legend> 
	<p>Delete range indicated by rangeURI at path parameter from the range list of this datatype property</p>
</fieldset>

<fieldset>
	<legend><b>SubPath: /superProperties  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>superPropertyURIs</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing datatype property URIs which will be added as a super property to this datatype property</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath:  /superProperties/{propertyURI} Method : DELETE</b></legend> 
	<p>Delete datatype property indicated by propertyURI at path parameter from the super properties list of this datatype property</p>
</fieldset>
<p>Example curl command</p>
<pre>
curl -i -X POST -H "Accept:application/xml" --data-urlencode isFuntional=true  http://localhost:8080/persistencestore/${it.metadata.propertyMetaInformation.href}
</pre>
<p>Response is the updated class context of the class</p>

</div>
<script>
	
	PAGING.adjustVisibility("domainList");
	PAGING.adjustVisibility("rangeList");
	PAGING.adjustVisibility("equProps");
	PAGING.adjustVisibility("superProps");
	<@propSerializer.property it=it.metadata/>
 		
 	
	
	var propUtil = this.PSTORE.propUtil; 

	$('[name="propFeatFunc"]').click(function(){
		propUtil.setFunctional($(this)[0].checked);
	});

	$(".ontology .ontologyHeader").click(function () {
	  $(this).parent().toggleClass("collapsed");
	});
	
	$(".ontology .ontologySubmitHeader").click(function () {
	  $(this).parents("div").toggleClass("collapsed");
	});
</script>
</@common.page>