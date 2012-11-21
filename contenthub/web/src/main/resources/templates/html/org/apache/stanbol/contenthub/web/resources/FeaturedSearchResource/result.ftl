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
<#import "/imports/facetResultMacro.ftl" as facetResultMacro>
<#import "/imports/relatedKeywordMacro.ftl" as relatedKeywordMacro>
<#escape x as x?html>
<#-- limit for the more less button -->
<#assign limit=4>
<div id="text"></div>

<div id="result" class="result">
  <a href="${it.publicBaseUri}contenthub/${it.indexName}/search/featured">Back to Search</a></br>
  <div class="leftContainer">
    <div class="keywords">
      <#list it.searchResults.relatedKeywords?keys as queryTermToken>
        <#assign queryTerm = queryTermToken?replace("*","_")?replace(" ", "_")?replace("'", "_")>
        <#assign kwId = "kw_" + queryTerm>
        <h3 class="keywordItem keywordClickable" id="${kwId}">${queryTermToken}</h3>
        <div id="allSuggestions_${queryTerm}">
          <#assign rkwsForSingleToken = it.searchResults.relatedKeywords[queryTermToken]>
          <#list rkwsForSingleToken?keys as rkwSource>
            <#assign rkwList = rkwsForSingleToken[rkwSource]>
            <#if rkwList?size &gt; 0>
              <@relatedKeywordMacro.relatedKeywordMacro relatedKeywordId = kwId relatedKeywordList = rkwList source = rkwSource/>
            </#if>
          </#list>
        </div>
      </#list>
    </div>
    
    <#-- chosen facets division --> 
    <div class="chosenfacets">
      <#if it.chosenFacets?exists && it.chosenFacets != "{}">
        <fieldset>
          <div id="chosenFacets"></div>
          <input type="hidden" id="chosenFacetsHidden" value="${it.chosenFacets}"/>
        </fieldset>
      </#if>
    </div>
  
    <#-- facets constructed for the retrieved current search results -->
    <div class="facets" id="facets">
      <#if it.searchResults.facets?exists && it.searchResults.facets?size != 0>
        <fieldset>
          <#list it.searchResults.facets as facet>
            <#if facet.facetField?exists>
                <@facetResultMacro.facetResultMacro facetResult=facet/>
            </#if>
          </#list>
        </fieldset>
      </#if>
    </div>
    
  </div>

  <#-- search result division --> 
  <div class="resources">
    <fieldset>
      <legend><h3>Results for ${it.queryTerm}:</h3></legend>
      <div>
        <ul class="spadded">
          <#if it.searchResults.documents?size == 0>
          Your search did not match any documents
          <#else>
            <#list it.documents as docRes>
              <div class="bordered-bottom">
                <li class="lined"><a href="${it.publicBaseUri}contenthub/${it.indexName}/store/page/${docRes.localId}">${docRes.title}</a></li>
              </div>  
            </#list>
          </#if>
        </ul>
      </div>
    </fieldset>
    <ul class="previousNext">
      <#if it.moreRecentItems?exists>
        <li class="moreRecent"><a id="previousLink" href="javascript:getResults(null, null, 'first', ${it.offset - it.pageSize}, ${it.pageSize})">Previous</a></li>
      </#if>
      <#if it.olderItems?exists>
        <li class="older"><a id="nextLink" href="javascript:getResults(null, null, 'first', ${it.offset + it.pageSize}, ${it.pageSize})">Next</a></li>
      </#if>
    </ul>
  </div>
  
</div>
</#escape>