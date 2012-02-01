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
<@common.page title="Enhancement Engines" hasrestapi=true>


<div class="panel" id="webview">

<p> This is the list of all active Enhancement Engines.
<ul>
  <#list it.engines as engine>
    <#assign name = engine.name >
    <li> <a href="${it.publicBaseUri}enhancer/engine/${name}">${name}</a>
    (id: ${it.getServiceId(name)}, ranking: ${it.getServiceRanking(name)}, 
    impl: ${engine.class.simpleName}
    )<#if it.getServicePid(name)??>: 
    <a href="/system/console/configMgr/${it.getServicePid(name)}">configure</a></#if>
  </#list>
</ul>
<p>
EnhancementEngines are used to define <a href="${it.publicBaseUri}enhancer/chain">
Enhancement Chains</a> that can than be used to enhance content parsed to the
Stanbol Enhancer.</p>
<p class="note"> You can configure Chains by using the the
<a href="/system/console/configMgr">Configuration Tab</a> of the OSGi console.</p>
</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Enhancement Engines RESTful API</h3>

<p>This stateless interface allows the caller to query all available
Enhancement Engines</p>


</div>


</@common.page>
</#escape>
