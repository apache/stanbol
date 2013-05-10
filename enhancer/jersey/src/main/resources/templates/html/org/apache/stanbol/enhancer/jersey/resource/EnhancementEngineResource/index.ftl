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
<@common.page title="Enhancement Engine ${it.name}" hasrestapi=true>

<div class="panel" id="webview">
<p> Enhancement Engine Details: <ul>
<li> name: ${it.name}
<li> class: ${it.engine.class}
<li> ordering: ${it.ordering}
<li> service.id : ${it.id}
<li> service.ranking: ${it.ranking}
</ul>

<#if it.pid??>
<p class="note">You can <a href="${it.consoleBaseUri}/configMgr/${it.pid}">
configure this engine</a> by using the the Configuration Tab of the OSGi console.</p>
</#if>

<#--
<p> This is the list of all active Enhancement Engines active for 
the name ${it.name}:
<ul>
  <#list it.engines as engine>
    <li> <a href="${it.publicBaseUri}enhancer/engine/${engine.name}">${engine.name}</a>
      (impl: ${engine.class.simpleName})
  </#list>
</ul>
<p>
-->
</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Enhancement Engines RESTful API</h3>

<p>This stateless interface allows the caller to query all available
Enhancement Engines</p>

</div>

</@common.page>
</#escape>
