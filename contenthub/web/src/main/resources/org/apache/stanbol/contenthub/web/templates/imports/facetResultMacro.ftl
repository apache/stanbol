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
<#macro facetResultMacro facetField consLink>
	<#assign limit=4 />
	<#if facetField?exists>
		<#if facetField.values?exists && facetField.values?size != 0>
			<#if facetField.name == "stanbolreserved_creationdate">
				<#assign facetName=facetField.name?substring(facetField.name?index_of("_")+1,facetField.name?length)/>
				${facetName}
				<br/>
				<#assign orderedList = facetField.values?sort_by("name") />
				<ul><li>
					<input id="dateFrom" class="facetText" type="text" value="${orderedList[0].name?substring(0,10)}" readonly="true"/> 
					to <input id="dateTo" class="facetText" type="text" value="${orderedList[orderedList?size-1].name?substring(0,10)}" readonly="true"/>
				  <#assign consLinkEscaped = consLink?url("UTF-8")?js_string/>
					<a href="javascript:getResults('${consLinkEscaped}','stanbolreserved_creationdate','','date')"><input type="button" value=">" /></a>
				</li></ul>
			<#else>
				<#if facetField.name?last_index_of("_") &gt; 0>
					<#assign facetName=facetField.name?substring(0,facetField.name?last_index_of("_"))/>
				<#else>
					<#assign facetName=facetField.name />
				</#if>
				${facetName}
				<ul id="${facetName}list">
					<#if facetField.name?ends_with("_l")>
						<li>
							<input id="${facetField.name}TextMin" class="facetText" type="text"/> 
							to <input id="${facetField.name}TextMax" class="facetText" type="text"/>
							<#assign facetNameEscaped = facetField.name?url("UTF-8")?js_string/>
							<#assign consLinkEscaped = consLink?url("UTF-8")?js_string/>
							<a href="javascript:getResults('${consLinkEscaped}','${facetNameEscaped}','','range')"><input type="button" value=">" /></a>
						</li>
					</#if>
					
					<#assign x=0 />
					<#list facetField.values as count>
						<#assign facetNameEscaped = facetField.name?url("UTF-8")?js_string/>
						<#assign countNameEscaped = count.name?url("UTF-8")?js_string/>
						<#assign consLinkEscaped = consLink?url("UTF-8")?js_string/>
						<#if x = limit><#break/></#if>
						<li><a href=javascript:getResults('${consLinkEscaped}','${facetNameEscaped}','${countNameEscaped}','addFacet')>${count.name} ( ${count.count} )</a></li>
						<#assign x=x+1 />
					</#list>
				</ul>						
				<#if facetField.values?size &gt; limit>
					<a id="${facetName?replace(':','_')}" href="">more</a><br>
				</#if>
			</#if>
		</#if>
	<#else>
		<p>No results found<p>
	</#if>
	<script type=text/javascript>
	
	
	
	function init() {
	
		$(document).ready(function(){
			$("#dateFrom").datepicker({ dateFormat: 'yy-mm-dd' });
			$("#dateTo").datepicker({ dateFormat: 'yy-mm-dd' });
		});
	
	   $("#${facetName?replace(':','_')}", this).click(function(e) {
	     // disable regular form click
	     e.preventDefault();
	     if(document.getElementById("${facetName?replace(':','_')}").innerHTML == "more")
	     {
	     	 var a="<#list facetField.values as count><#assign consLinkEscaped = consLink?url("UTF-8")?js_string/><#assign countNameEscaped = count.name?url("UTF-8")?js_string/><#assign facetNameEscaped = facetField.name?url("UTF-8")?js_string/><li><a href=javascript:getResults('${consLinkEscaped}','${facetNameEscaped}','${countNameEscaped}','addFacet')>${count.name} ( ${count.count} )</a></li></#list>";
	       document.getElementById("${facetName}list").innerHTML=a;
	       $(this).attr({ 'innerHTML': 'less' });
  		 }
  		 else
  		 {
  		 	 var a="<#assign x=0 /><#list facetField.values as count><#if x = limit><#break/></#if><li><a href=javascript:getResults('${consLinkEscaped}','${facetNameEscaped}','${countNameEscaped}','addFacet')>${count.name} ( ${count.count} )</a></li><#assign x=x+1 /></#list>";
  		 	 document.getElementById("${facetName}list").innerHTML=a;
  		 	 $(this).attr({ 'innerHTML': 'more' });		 	
  		 }    
     });
	 }
	 
	 $(document).ready(init);
</script>
</#macro>