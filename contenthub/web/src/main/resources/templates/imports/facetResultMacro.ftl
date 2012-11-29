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
<#escape x as x?html>
<#macro facetResultMacro facetResult>
  <#assign limit=4 />
  <#assign facetField=facetResult.facetField />
  <#assign facetHtmlName=facetResult.htmlName />
  <#assign facetID=facetHtmlName?replace(':','_') />
  
  <#if facetField?exists>
    <#if facetField.values?exists && facetField.values?size != 0>
      <#assign facetNameEscaped = facetField.name?url("UTF-8")?js_string/>
      <#if facetField.name == "stanbolreserved_creationdate">
        ${facetHtmlName}
        <br/>
        <#assign orderedList = facetField.values?sort_by("name") />
        <p>
          <input id="dateFrom" class="facetText" type="text" value="${orderedList[0].name?substring(0,10)}" readonly="true"/> 
          to <input id="dateTo" class="facetText" type="text" value="${orderedList[orderedList?size-1].name?substring(0,10)}" readonly="true"/>
          <a href="javascript:getResults('stanbolreserved_creationdate','','date')"><input type="button" value=">" /></a>
        </p>
      <#else>
        ${facetHtmlName}
        <#assign type = facetResult.type />
        <#if type=="int" || type=="float" || type=="long" || type=="double">
          <p>
          <input id="${facetHtmlName}TextMin" class="facetText" type="text"/> 
            to <input id="${facetHtmlName}TextMax" class="facetText" type="text"/>
          <a href="javascript:getResults('${facetNameEscaped}','','range')"><input type="button" value=">" /></a>
          </p>
        </#if>
        <ul id="${facetHtmlName}list"> 
          <#assign x=0 />
          <#list facetField.values as count>
            <#assign countNameEscaped = count.name?url("UTF-8")?js_string/>
            <#if x = limit><#break/></#if>
            <li><a href="javascript:getResults('${facetNameEscaped}','${countNameEscaped}','addFacet')">${count.name} ( ${count.count} )</a></li>
            <#assign x=x+1 />
          </#list>
        </ul>       
        <#if facetField.values?size &gt; limit>
          <a id="${facetID}" href="">more</a><br>
        </#if>
      </#if>
    </#if>
  <#else>
    <p>No results found<p>
  </#if>
  <hr />
  <script type=text/javascript>
  
      function init() {
          $("#dateFrom").datepicker({ dateFormat: 'yy-mm-dd' });
          $("#dateTo").datepicker({ dateFormat: 'yy-mm-dd' });
      
          $("#${facetID}", this).click(function(e) {
              // disable regular form click
              e.preventDefault();
              if(document.getElementById("${facetID}").innerHTML == "more") {
                  var a="<#list facetField.values as count><#assign countNameEscaped = count.name?url("UTF-8")?js_string/><#assign facetNameEscaped = facetField.name?url("UTF-8")?js_string/><li><a href=javascript:getResults('${facetNameEscaped}','${countNameEscaped}','addFacet')>${count.name} ( ${count.count} )</a></li></#list>";
                  document.getElementById("${facetHtmlName}list").innerHTML=a;
                  document.getElementById("${facetID}").innerHTML = "less";
              }
              else {
                  var a="<#assign x=0 /><#list facetField.values as count><#if x = limit><#break/></#if><li><a href=javascript:getResults('${facetNameEscaped}','${countNameEscaped}','addFacet')>${count.name} ( ${count.count} )</a></li><#assign x=x+1 /></#list>";
                  document.getElementById("${facetHtmlName}list").innerHTML=a;
                  document.getElementById("${facetID}").innerHTML = "more";     
              }    
          });
      }
  
      $(document).ready(init);
  </script>
</#macro>
</#escape>