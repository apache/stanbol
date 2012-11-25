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
<@common.page title="Topic Classification" hasrestapi=false> 

<div class="panel" id="webview">
<p>Registered models and training datasets used by the Topic Classification
Enhancement Engine.</p>

<table>
  <tr>
    <th>Topic Model</th><th>Enhancement Chains</th><th>Training Set</th>
  </tr>
  <#list it.classifiers as classifier>
    <tr>
    <td>
      <#assign name = classifier.name>
      <a href="${it.publicBaseUri}topic/model/${name}">${name}</a>
      (${classifier.class.simpleName})
    </td>
    <td>
       <ul>
         <#list classifier.chainNames as chainName>
           <li><a href="${it.publicBaseUri}enhancer/chain/${chainName}">${chainName}</a></li>
         </#list>
       </ul>
    </td>
    <td>
      <#if classifier.trainingSet?exists>
        <#assign trainingSet = classifier.trainingSet>
        <a href="${it.publicBaseUri}topic/trainingset/${trainingSet.name}">${trainingSet.name}</a>
        (${trainingSet.class.simpleName})
      </#if>
    </td>
    </tr>
  </#list>
</table>
</div>
</@common.page>
</#escape>