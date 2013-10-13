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
<#import "/imports/taskDescription.ftl" as taskDescription>
<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Reasoning Service /${it.currentService.path}/${it.currentTask}" hasrestapi=true> 
 	<h3>Service Endpoint <a href="/${it.currentPath}/..">/reasoners/${it.currentService.path}</a>: ${it.serviceDescription.name}</h3>
	<h4 style="font-size: 1em">Subresource <a href="/${it.currentPath}">/${it.currentPath}</a></h4>
 
 <div class="panel" id="webview">
	<p><b>Service:</b> <tt>${it.currentService.class.name}</tt>
	<br/><b>Name:</b> ${it.currentService.path}
	<br/><b>Task:</b> ${it.currentTask}</p>
	<h4>Run as foreground job</h4>
	<form method="GET" accept-charset="utf-8">
	<fieldset>
	<legend>Submit a URL to the service</legend>
	<p><b>URL:</b> <input type="text" name="url" size="80"/> <input type="submit" value="Send"/></p>
	</fieldset>
	</form>
	
	<form method="POST" enctype="multipart/form-data" accept-charset="utf-8">
	<fieldset>
	<legend>Submit a file to the service</legend>
	<p><b>File:</b> <input type="file" name="file"/> <input type="submit" value="Send"/></p>
	</fieldset>
	</form>
	<h4>Run as background job</h4>
	<form method="GET" action="/${it.currentPath}/job" accept-charset="utf-8">
	<fieldset>
	<legend>Submit a URL to the service</legend>
	<p><b>URL:</b> <input type="text" name="url" size="80"/> <input type="submit" value="Send"/></p>
	</fieldset>
	</form>
	
	<form method="POST" action="/${it.currentPath}/job" enctype="multipart/form-data" accept-charset="utf-8">
	<fieldset>
	<legend>Submit a file to the service</legend>
	<p><b>File:</b> <input type="file" name="file"/> <input type="submit" value="Send"/></p>
	</fieldset>
	</form>
 </div>
 
 <div class="panel" id="restapi" style="display: none;">
	 <#if it.currentTask == "classify">
		<@taskDescription.view name="Classify" path="${it.currentPath}" description="This task infer all <tt>rdf:type</tt> statements."/>
	 <#elseif it.currentTask == "check">
		<@taskDescription.view name="Check" path="${it.currentPath}" description="This task checks whether the schema is correctly used."/>
	 <#elseif it.currentTask == "enrich">
		<@taskDescription.view name="Enrich" path="${it.currentPath}" description="This task materializes all inferences."/>
	 </#if>
 </div>
</@common.page>
</#escape>