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
<#escape x as x?html>
<@common.page title="Contenthub" hasrestapi=true>
  <div class="panel" id="webview">
  <a href="${it.publicBaseUri}contenthub/store">Store</a> | <b>Index</b><a href="${it.publicBaseUri}contenthub/index/ldpath">/ldpath</a>
    <h3>Submitted Indexes: </h3>
    <#if it.semanticIndexes?size &gt; 0>
      <#list it.semanticIndexes as index>
        <div class="index">
          <div class="collapsed">
            <p class="collapseheader"><b>${index.name}</b></p>
            <div class="collapsable">
              <ul>
                <li>Description: ${index.description}</li>
                <li>
                  State: 
                  <#if index.state?contains("INDEXING") >
                  <span style="color:#FDD017">
                  </#if>
                  <#if index.state?contains("ACTIVE") >
                  <span style="color:#006600">
                  </#if>
                  <#if index.state?contains("UNINIT") >
                  <span style="color:#808080">
                  </#if>
                  ${index.state}
                  </span>
                </li>
                <li>Revision: ${index.revision}</li>
                <li>RESTful Endpoints: <br/>
                  <ul>
                    <#list index.endpoints?keys as endpoint>
                      <li>
                        ${endpoint}:
                        
                        <a href="${it.publicBaseUri + index.endpoints[endpoint]}<#if endpoint?lower_case = "solr">/select?q=*:*</#if>">
                          ${it.publicBaseUri + index.endpoints[endpoint]}<br/>
                        </a>
                      </li>
                    </#list>
                  </ul>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </#list>
    <#else>
      There is no submitted index.
    </#if>
  </div>
  <div class="panel" id="restapi" style="display: none;">
    <#include "/imports/doc/indexRestApi.ftl">
  </div>
  <script>
    $(".index p").click(function () {
      $(this).parents("div").toggleClass("collapsed");
    })
  </script>
</@common.page>
</#escape>