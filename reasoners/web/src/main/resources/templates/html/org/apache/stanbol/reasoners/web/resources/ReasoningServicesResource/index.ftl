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
<#import "/imports/reasonersDescription.ftl" as reasonersDescription>
<#import "/imports/serviceDescription.ftl" as serviceDescription>
<#escape x as x?html>
<@common.page title="Reasoners" hasrestapi=true> 
		
 <div class="panel" id="webview">
 <p>The Stanbol Reasoners provides a set of services that take advantage of automatic inference engines.</p>
 <h3>Active services</h3>
 <p>There are currently <strong>${it.activeServices?size}</strong> active services.</p>
 <p>Each reasoning service can be accessed to perform one of three tasks: classification, consistency check, get all inferences</p>          
  <ul>
  <#list it.servicesDescription as service>
    <@serviceDescription.li name="${service.name}" fullpath="${it.currentPath}/${service.path}" path="${service.path}" description="${service.description}"/>
    <ul>
    <#-- TODO: generate the task list dinamically -->
      <li><a href="${it.publicBaseUri}${it.currentPath}/${service.path}/classify" title="${service.name} Task: classify">classify</a></li>
      <li><a href="${it.publicBaseUri}${it.currentPath}/${service.path}/check" title="${service.name} Task: check">check</a></li>
      <li><a href="${it.publicBaseUri}${it.currentPath}/${service.path}/enrich" title="${service.name} Task: classify">enrich</a></li>
    </ul>	
  </#list>
 </ul>
 <#if it.activeServices?size == 0>
   <p><em>There is no reasoning service. Administrators can install and
   configure new reasoning services using the
    <a href="/system/console/components" target="_blank">OSGi console</a>.</em></p>
 <#else>
<#--
  This is not much informative for the moment   
  <p class="note">Administrators can enable, disable and deploy reasoning services using the
    <a href="/system/console/components" target="_blank">OSGi console</a>.</p>
-->
 </#if>
 </div>
 <!-- We disable this at the moment -->
 <div class="panel" id="restapi" style="display: none;">
    <p>This section lists how to use the REST api of all active services:</p>
    <h3>Service Endpoints</h3>
    <ul>
    <#list it.servicesDescription as service>
      <li><a href="#${service.path}">${service.name}</a></li>
	</#list>
    </ul>
    <#list it.servicesDescription as service>
    <hr/>
      <a name="${service.path}"></a>
      <h4 style="font-size:1em">Service Endpoint <a href="${it.currentPath}/${service.path}">/reasoners/${service.path}</a>: ${service.name}</h4>
	  <@serviceDescription.view name="${service.name}" fullpath="${it.currentPath}/${service.path}" path="${service.path}" description="${service.description}"/>
	</#list>
 </div>
</@common.page>
</#escape>