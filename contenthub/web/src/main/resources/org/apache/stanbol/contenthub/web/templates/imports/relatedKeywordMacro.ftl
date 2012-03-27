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
<#macro relatedKeywordMacro relatedKeywordId relatedKeywordList source>
  <#assign limit = 4/>
  <#assign normalizedSourceName = source?replace("*","_")?replace(" ", "_")?replace("'", "_")>
  Related ${source} Keywords
  <ul id="${relatedKeywordId}${normalizedSourceName}list" class="spadded">
    <#assign x = 0/>
    <#list relatedKeywordList as related>
      <#assign relatedName = related.keyword?replace(' ','_')>
      <#if x == limit><#break/></#if>
      <li><a href=javascript:getResults(null,"${relatedName}","explore")>${relatedName?replace('_',' ')}</a></li>
      <#assign x = x + 1>
    </#list>
  </ul>
  <#if relatedKeywordList?size &gt; limit>
    <a id="${relatedKeywordId}${normalizedSourceName}button" href="">more</a><br>
  </#if>
  <br/>
    
  <script language="javascript">
      function moreLessButtonHandler() {
          $("#${relatedKeywordId}${normalizedSourceName}button", this).click(function(e){
              // disable regular form click
              e.preventDefault();
              if(document.getElementById("${relatedKeywordId}${normalizedSourceName}button").innerHTML == "more") {
                  var a="<#list relatedKeywordList as related><#assign relatedName = related.keyword?replace(' ','_')><li><a href=javascript:getResults(null,'${relatedName}','explore')>${relatedName?replace('_', ' ')}</a></li></#list>";
                  document.getElementById("${relatedKeywordId}${normalizedSourceName}list").innerHTML=a;
                  $(this).attr({ 'innerHTML': 'less' });
              } else{
                  var a="<#assign x=0><#list relatedKeywordList as related><#assign relatedName = related.keyword?replace('_',' ')><#if x == limit><#break/></#if><li><a href=javascript:getResults(null,'${relatedName}','explore')>${relatedName?replace('_', ' ')}</a></li><#assign x=x+1 /></#list>";
                  document.getElementById("${relatedKeywordId}${normalizedSourceName}list").innerHTML=a;
                  $(this).attr({ 'innerHTML': 'more' });       
              }    
          });
       }     
       
       $(document).ready(moreLessButtonHandler);
  </script>
</#macro>