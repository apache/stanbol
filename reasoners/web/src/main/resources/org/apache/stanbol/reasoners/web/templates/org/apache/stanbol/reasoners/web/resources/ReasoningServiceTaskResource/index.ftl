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
<@common.page title="Reasoning Service /${it.currentService.path}/${it.currentTask}" hasrestapi=false> 
		
 <div class="panel" id="webview">
 	<h3>/${it.currentPath}</h3>
	<p><b>Service type:</b> <tt>${it.currentService.class.name}</tt></p>
	<p><b>Name:</b> ${it.currentService.path}</p>
	<p><b>Task:</b> ${it.currentTask}</p>
	
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
 </div>
  </@common.page>
</#escape>