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
<@common.page title="Reasoners: ${it.serviceDescription.name}" hasrestapi=true> 
		
<h3>Service Endpoint <a href="/${it.currentPath}">/reasoners/${it.service.path}</a>: ${it.serviceDescription.name}</h3>
<div class="panel" id="webview">
		<@serviceDescription.view name="${it.serviceDescription.name}" fullpath="${it.currentPath}" path="${it.service.path}" description="${it.serviceDescription.description}"/>
</div>

    <!-- We disable this at the moment -->
    <div class="panel" id="restapi" style="display: none;">
		<@serviceDescription.rest name="${it.serviceDescription.name}" fullpath="${it.currentPath}" path="${it.service.path}" description="${it.serviceDescription.description}"/>
    </div>

</@common.page>
</#escape>