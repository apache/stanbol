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
<#setting url_escaping_charset='ISO-8859-1'>
<#import "/imports/keyword_result_tab.ftl" as keywordTab>
<div id="text">
</div>
<div id="result" class="result">
<a href="">Back to Search</a></br>
	<#assign con=it.templateData.context>
	<!--General Divs for layout  -->
	<div class="keywords">
		<#list con.queryKeyWords?sort_by("scoreString")?reverse as qk>
			<h3  class="keywordItem keywordClickable" id="kw_${qk.keyword?replace("*","_")?replace(" ", "_")?replace("'", "_")}">${qk.scoreString}:${qk.keyword}</h3>
			<div>
			<#if qk.relatedKeywords?exists && qk.relatedKeywords?size != 0>
				<fieldset>
					<legend>Related Keywords</legend>
					<ul class="spadded"> 
					<#list qk.relatedKeywords?sort_by("scoreString")?reverse as kw>
						<li class="keywordItem" id="kw_${kw.keyword?replace("*","_")?replace(" ", "_")?replace("'", "_")}"><a class="keywordClickable" href="">${kw.scoreString}:${kw.keyword}</a></li>
					</#list>
					<ul>
				</fieldset>
			
			<#else>
				<p><i>No related keywords</i></p>
			</#if>
			</div>
		</#list>
	</div>
	
	<div class="resources">
		<#list con.queryKeyWords?sort_by("score") as qk>
			<@keywordTab.keywordTab kw=qk/>
			<#list qk.relatedKeywords?sort_by("score") as kw1>
				<@keywordTab.keywordTab kw=kw1/>
			</#list>
		</#list>
	</div>
	
	<div id="facets">

		<#if it.facets?exists && it.facets?size != 0>
			<fieldset>
				<div id="chosenFacets"></div>
				<div id="chosenFacetsHidden" class="invisible">
					'${it.templateData.constraints}'
				</div>
				<br/>
				<#list it.facets as facet>
					${facet.name?substring(0,facet.name?last_index_of("_"))}
					<#if facet.values?exists && facet.values?size != 0>
						<ul>
							<#list facet.values as value>
								<#assign consLink = it.templateData.constraints>
								<li><a href=javascript:getResults('${consLink?url}','${facet.name?url}','${value.name?url}')>${value.name} (${value.count})</a></li>
								
							</#list>
						</ul>
					</#if>
				</#list>
			</fieldset>
		</#if>
	</div>
</div>