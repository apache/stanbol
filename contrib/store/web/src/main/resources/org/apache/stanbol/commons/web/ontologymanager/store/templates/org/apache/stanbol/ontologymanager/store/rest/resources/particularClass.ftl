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
<#import "/imports/classConstraint.ftl" as constraint>
<@common.page title="Class Context of ${it.metadata.classMetaInformation.URI}" hasrestapi=true>
<div class="panel" id="webview">

	<!-- Equivalent Classes -->
	<fieldset>
			<legend><b>Equivalent Classes</b></legend>
			<div class="ontology">
				<div class="collapsed">
					<fieldset>
					    <h4 class="ontologySubmitHeader">Add Equivalent Class</h4>
					  	<div class="ontologyCollapsable">
						  <p>Domain URI: <textarea class="equivalentClassInput"rows="1" name="classURI"></textarea></p>
						  <p><button  name="Add Domain" onClick="javascript: PSTORE.HTTPHelper.send('POST', '${it.metadata.classMetaInformation.href}/equivalentClasses', false, false, 'equivalentClassURIs',$('.equivalentClassInput')[0].value )">Add Equivalent Class</button></p>
						 </div>
					  </fieldset>
				</div>
			</div>
	<#if it.metadata.equivalentClasses.classMetaInformation?size !=0>
			<#list it.metadata.equivalentClasses.classMetaInformation?sort_by("URI") as eqCls>
				<div class="ontology equClasses ${eqCls_index}"> 
				<div class="collapsed">
					<a class="imgOntClass" href="${eqCls.href}">${eqCls.URI}</a>
					<button class="delete" title="Delete ${eqCls.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.classMetaInformation.href}/equivalentClasses/${eqCls.URI}', true, false, null, null)"></button>
				</div>
			</div>
			</#list>
				<@buttons.prevNextButtons className="equClasses"/>
	</#if>
	</fieldset>	
	
		<!-- Super Classes -->
		<fieldset>
			<legend><b>Super Classes</b></legend>
			<div class="ontology">
				<div class="collapsed">
					<fieldset>
					    <h4 class="ontologySubmitHeader">Add Super Class</h4>
					  	<div class="ontologyCollapsable">
						  <p>Domain URI: <textarea class="superClassInput"rows="1" name="classURI"></textarea></p>
						  <p><button  name="Add Super Class" onClick="javascript: PSTORE.HTTPHelper.send('POST', '${it.metadata.classMetaInformation.href}/superClasses', false, false, 'superClassURIs',$('.superClassInput')[0].value )">Add Super Class</button></p>
						 </div>
					  </fieldset>
				</div>
			</div>
	<#if it.metadata.superclasses.classMetaInformation?size !=0>
			<#list it.metadata.superclasses.classMetaInformation?sort_by("URI") as supCls>
				<div class="ontology supClasses ${supCls_index}"> 
				<div class="collapsed">
					<a class="imgOntClass" href="${supCls.href}">${supCls.URI}</a>
					<button class="delete" title="Delete ${supCls.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.classMetaInformation.href}/superClasses/${supCls.URI}', true, false, null, null)"></button>
				</div>
			</div>
			</#list>
				<@buttons.prevNextButtons className="supClasses"/>
		
	</#if>
	</fieldset>
		<!-- Disjoint Classes -->
		<fieldset>
			<legend><b>Disjoint Classes</b></legend>
			<div class="ontology">
				<div class="collapsed">
					<fieldset>
					    <h4 class="ontologySubmitHeader">Add Disjoint Class</h4>
					  	<div class="ontologyCollapsable">
						  <p>Domain URI: <textarea class="disjointClassInput"rows="1" name="classURI"></textarea></p>
						  <p><button  name="Add Disjoint Class" onClick="javascript: PSTORE.HTTPHelper.send('POST', '${it.metadata.classMetaInformation.href}/disjointClasses', false, false, 'disjointClassURIs',$('.disjointClassInput')[0].value )">Add Disjoint Class</button></p>
						 </div>
					  </fieldset>
				</div>
			</div>
	<#if it.metadata.disjointClasses.classMetaInformation?size !=0>
		
			<#list it.metadata.disjointClasses.classMetaInformation?sort_by("URI") as disjCls>
				<div class="ontology disClasses ${disjCls_index}"> 
				<div class="collapsed">
					<a class="imgOntClass" href="${disjCls.href}">${disjCls.URI}</a>
					<button class="delete" title="Delete ${disjCls.URI}" onClick="javascript: PSTORE.HTTPHelper.send('DELETE', '${it.metadata.classMetaInformation.href}/disjointClasses/${disjCls.URI}', true, false, null, null)"></button>
				</div>
			</div>
			</#list>
				<@buttons.prevNextButtons className="disClasses"/>
	</#if>
	</fieldset>
	
	
		<!-- Class Constraints -->
	<#if it.metadata.classConstraint?size != 0>
		<fieldset>
			<legend><b>Class Constraints</b></legend>
			<#list it.metadata.classConstraint?sort_by("type") as clsCons>
				<p class="clasCons ${clsCons_index}">
					<@constraint.processConstraint original=clsCons constraint=clsCons/>
				</p>
			</#list>
		</fieldset>
	</#if>
</div>
<div class="panel" id="restapi" style="display: none;">
<h3>Getting class context of a  class</h3>
<pre>
curl -i -X GET -H "Accept:application/xml" http://localhost:8080/persistencestore/${it.metadata.classMetaInformation.href}
</pre>
<p>Response :</p>
<pre>
HTTP/1.1 200 OK
Content-Type: application/xml
Transfer-Encoding: chunked
Server: Jetty(6.1.x)

&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
&lt;ns1:ClassContext xmlns:ns1="model.rest.persistence.iks.srdc.com.tr" xmlns:ns2="http://www.w3.org/1999/xlink"&gt;
    &lt;ns1:ClassMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology//classes/http://dbpedia.org/ontology/AdultActor"&gt;
        &lt;ns1:URI&gt;http://dbpedia.org/ontology/AdultActor&lt;/ns1:URI&gt;
        &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
        &lt;ns1:Namespace&gt;http://dbpedia.org/ontology/&lt;/ns1:Namespace&gt;
        &lt;ns1:LocalName&gt;AdultActor&lt;/ns1:LocalName&gt;
    &lt;/ns1:ClassMetaInformation&gt;
    &lt;ns1:EquivalentClasses/&gt;
    &lt;ns1:Superclasses&gt;
        &lt;ns1:ClassMetaInformation ns2:href="ontologies/http://dbpedia.org/ontology//classes/http://dbpedia.org/ontology/Actor"&gt;
            &lt;ns1:URI&gt;http://dbpedia.org/ontology/Actor&lt;/ns1:URI&gt;
            &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
            &lt;ns1:Namespace&gt;http://dbpedia.org/ontology/&lt;/ns1:Namespace&gt;
            &lt;ns1:LocalName&gt;Actor&lt;/ns1:LocalName&gt;
        &lt;/ns1:ClassMetaInformation&gt;
    &lt;/ns1:Superclasses&gt;
    &lt;ns1:DisjointClasses/&gt;
&lt;/ns1:ClassContext&gt;
</pre>

<p><b>Class</b> Context contains following information:
	<ul>
		<li><b>Equivalent Classes</b> as a list of ClassMetaInformation</li>
		<li><b>Super Classes</b> as a list of ClassMetaInformation</li>
		<li><b>Disjoint Classes</b> as a list of ClassMetaInformation</li>
		<li><b>Class Constraint</b> list containing
			<ul>
				<li>owl:allValuesFrom</li>
				<li>owl:someValuesFrom</li>
				<li>owl:hasValue</li>
				<li>owl:maxCardinality</li>
				<li>owl:minCardinality</li>
				<li>owl:cardinality</li>
				<li>owl:intersectionOf</li>
				<li>owl:unionOf</li>
			</ul>
			constrains defined on this class
		</li>
		Here is an example class constraint that defines a class which has either a student or a happy friend : 
<pre>
&lt;ns1:ClassConstraint ns1:type="union_of">
    &lt;ns1:ClassConstraint ns1:type="some_values_from"&gt;
        &lt;ns1:PropertyMetaInformation ns2:href="ontologies/http://oiled.man.example.net/facts/objectProperties/http://oiled.man.example.net/facts/hasFriend"&gt;
            &lt;ns1:URI&gt;http://oiled.man.example.net/facts#hasFriend&lt;/ns1:URI&gt;
            &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
            &lt;ns1:Namespace&gt;http://oiled.man.example.net/facts#&lt;/ns1:Namespace&gt;
            &lt;ns1:LocalName&gt;hasFriend&lt;/ns1:LocalName&gt;
        &lt;/ns1:PropertyMetaInformation&gt;
        &lt;ns1:ClassMetaInformation ns2:href="ontologies/http://oiled.man.example.net/facts/classes/http://oiled.man.example.net/facts/Happy"&gt;
            &lt;ns1:URI&gt;http://oiled.man.example.net/facts#Happy&lt;/ns1:URI&gt;
            &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
            &lt;ns1:Namespace&gt;http://oiled.man.example.net/facts#&lt;/ns1:Namespace&gt;
            &lt;ns1:LocalName&gt;Happy&lt;/ns1:LocalName&gt;
        &lt;/ns1:ClassMetaInformation&gt;
    &lt;/ns1:ClassConstraint&gt;
    &lt;ns1:ClassConstraint ns1:type="some_values_from"&gt;
        &lt;ns1:PropertyMetaInformation ns2:href="ontologies/http://oiled.man.example.net/facts/objectProperties/http://oiled.man.example.net/facts/hasFriend"&gt;
            &lt;ns1:URI&gt;http://oiled.man.example.net/facts#hasFriend&lt;/ns1:URI&gt;
            &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
            &lt;ns1:Namespace&gt;http://oiled.man.example.net/facts#&lt;/ns1:Namespace&gt;
            &lt;ns1:LocalName&gt;hasFriend&lt;/ns1:LocalName&gt;
        &lt;/ns1:PropertyMetaInformation&gt;
        &lt;ns1:ClassMetaInformation ns2:href="ontologies/http://oiled.man.example.net/facts/classes/http://oiled.man.example.net/facts/Student"&gt;
            &lt;ns1:URI&gt;http://oiled.man.example.net/facts#Student&lt;/ns1:URI&gt;
            &lt;ns1:Description&gt;&lt;/ns1:Description&gt;
            &lt;ns1:Namespace&gt;http://oiled.man.example.net/facts#&lt;/ns1:Namespace&gt;
            &lt;ns1:LocalName&gt;Student&lt;/ns1:LocalName&gt;
        &lt;/ns1:ClassMetaInformation&gt;
    &lt;/ns1:ClassConstraint&gt;
&lt;/ns1:ClassConstraint&gt;
		
</pre>
	</ul>
</p>

<h3>Updating class context of a class</h3>
<fieldset>
	<legend><b>SubPath: /superClasses  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>superClassURIs</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing class URIs which will be added as a super class to this class</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath:  /superClasses/{classURI} Method : DELETE</b></legend> 
	<p>Delete class indicated by classURI at path parameter from the super class list of this class</p>
</fieldset>

<fieldset>
	<legend><b>SubPath: /equivalentClasses  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>equivalentClassURIs</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing class URIs which will be added as an equivalent class to this class</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath: /equivalentClasses/{classURI} Method : DELETE</b></legend> 
	<p>Delete class indicated by classURI at path parameter from the equivalent class list of this class</p>
</fieldset>
<fieldset>
	<legend><b>SubPath: /disjointClasses  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>disjointClassURIs</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing class URIs which will be added as a disjoint class to this class</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath: /disjointClasses/{classURI} Method : DELETE</b></legend> 
	<p>Delete class indicated by classURI at path parameter from the super disjoint class  list of this class</p>
</fieldset>

<fieldset>
	<legend><b>SubPath: /unionClasses  Method: POST </b></legend>
	<table class="formParams">
		<head>
			<th width="20%">POST Parameter</th>
			<th width="10%">Type</th>
			<th width="70%">Explanation</th>
		</head>
		<body>
			<tr>
				<td>unionClassURIs</td>
				<td>List&lt;String&gt;</td>
				<td>A list of existing class URIs which will be added as a union class to this class and this class is converted to union class of this list</td>
			</tr>
		</body>
	</table>
</fieldset>
<fieldset>
	<legend><b>SubPath: /unionClasses/{classURI} Method : DELETE</b></legend> 
	<p>Delete class indicated by classURI at path parameter from the union class list of this class</p>
</fieldset>
</div>
<script>
	PAGING.adjustVisibility("equClasses");
	PAGING.adjustVisibility("supClasses");
	PAGING.adjustVisibility("disClasses");
	PAGING.adjustVisibility("clasCons");
	
	$(".ontology .ontologyHeader").click(function () {
	  $(this).parents("div").toggleClass("collapsed");
	}); 
	
	$(".ontology .ontologySubmitHeader").click(function () {
	  $(this).parents("div").toggleClass("collapsed");
	}); 

	$(".updateRadio").change(
		function(){
			$(".superClass").addClass("invisible");
			$(".equivalentClass").addClass("invisible");
			$(".disjointClass").addClass("invisible");
			$(".unionOf").addClass("invisible");
			$(".superClass").children("p").children("textarea")[0].value="";
			$(".equivalentClass").children("p").children("textarea")[0].value="";
			$(".disjointClass").children("p").children("textarea")[0].value="";
			$(".unionOf").children("p").children("textarea")[0].value="";
			var rb = $("input:checked")[0];
			if(rb != null){ 
				var checked = $("input:checked")[0].value;
				if(checked == "SP"){
					$(".superClass").removeClass("invisible");
				}else if(checked == "EQ"){
					$(".equivalentClass").removeClass("invisible");
				}else if(checked == "DS"){
					$(".disjointClass").removeClass("invisible");
				}else if(checked == "UN"){
					$(".unionOf").removeClass("invisible");
				}
			}
		}
	)
</script>
</@common.page>