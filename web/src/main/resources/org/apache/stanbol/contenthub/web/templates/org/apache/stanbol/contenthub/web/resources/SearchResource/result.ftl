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
<#import "/imports/suggestedKeyword.ftl" as suggestedKeyword>
<#import "/imports/facetResultMacro.ftl" as facetResultMacro>
<#import "/imports/relatedKeywordMacro.ftl" as relatedKeywordMacro>
<#-- limit for the more less button -->
<#assign limit=4>
<div id="text">
</div>
<div id="result" class="result">
<a href="">Back to Search</a></br>
	<#assign con=it.templateData.context>
	<!--General Divs for layout  -->
	<div class="keywords">
		<#list con.queryKeyWords?sort_by("scoreString")?reverse as qk>
			<h3  class="keywordItem keywordClickable" id="kw_${qk.keyword?replace("*","_")?replace(" ", "_")?replace("'", "_")}">${qk.keyword}</h3>
			<div id="allSuggestions">
				<#if qk.relatedKeywords?exists && qk.relatedKeywords?size != 0>
					<#list qk.relatedKeywords?keys as mapKey>
						<#assign listOfKey = qk.relatedKeywords[mapKey]>
						<#if listOfKey?size &gt; 0>
							<@relatedKeywordMacro.relatedKeywordMacro relatedKeywordList = listOfKey source = mapKey/>
						</#if>
					</#list>
				</#if>
				<#-- this division includes the results coming from entityHub -->
				<div id="entityHubSuggestionSubDiv"></div>
				<#if (!qk.relatedKeywords?exists || qk.relatedKeywords?size == 0) && (!it.suggestions?exists || it.suggestions?size == 0)>
					<div id="noRelatedKeywordDivision">No related keyword</div>
				</#if>
			</div>
		</#list>
	</div>
	
	<div class="resources">
		<fieldset>
			<#list con.queryKeyWords?sort_by("score") as qk>
				<@keywordTab.keywordTab kw=qk/>
			</#list>
		</fieldset>
	</div>
	
	<div class="chosenfacets">
		<#if it.templateData.constraints != "{}">
			<fieldset>
				<div id="chosenFacets"></div>
				<div id="chosenFacetsHidden" class="invisible">'${it.templateData.constraints?js_string}'</div>			
			</fieldset>
		</#if>
	</div>
	<br/>
	<div class="facets" id="facets">

		<#if it.facets?exists && it.facets?size != 0>
			<fieldset>
				<br/>
				<#list it.facets as facet>
					<@facetResultMacro.facetResultMacro facetField=facet consLink=it.templateData.constraints/>
				</#list>
			</fieldset>
		</#if>
	</div>
</div>