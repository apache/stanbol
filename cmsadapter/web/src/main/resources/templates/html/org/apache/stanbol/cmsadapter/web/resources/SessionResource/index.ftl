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
<@common.page title="CMS Adapter" hasrestapi=true> 

<div class="panel" id="webview">
	<h3>Service Endpoint <a href="${it.publicBaseUri}cmsadapter/session">/cmsadapter/session</a></h3>
	<p>
		This endpoint provides services to generate a <b>session key</b> to be used in later usages of CMS Adapter
		to access content repository. 
	</p>
</div>

<div class="panel" id="restapi" style="display: none;">
	<h3>Service Endpoint <a href="${it.publicBaseUri}cmsadapter/session">/cmsadapter/session</a></h3>
	<p>Following services are available for this endpoint:<br>
	<ul>
		<li><a href="#Create_session">Create session</a></li>
	</ul>
		
	<a name="Create_session" id="Create_session"></a>
	<h4>Create session</h4>
	<p><table>
		<tbody>
			<tr>
				<th valign="top">Description</th>
				<td><p>This services enables users to create session for later usage in the CMS Adapter. Created session is obtained by a generated
				session key.</p>
				</td>
			</tr>
			<tr>
				<th valign="top">Request</th>
				<td>GET /cmsadapter/session</td>
			</tr>
			<tr>
				<th valign="top">Parameters</th>
				<td>
					<ul>
						<li>@FormParam repositoryURL: URL of the content repository. For JCR repositories <b>RMI protocol</b>, for CMIS repositories
							<b>AtomPub Binding</b> is used. This parameter should be set according to these connection methods.</li>
						<li>@FormParam workspaceName: For JCR repositories this parameter determines the workspace to be connected. On the other hand
							for CMIS repositories <b>repository ID</b> should be set to this parameter. In case of not setting this parameter,
							for JCR <b>default workspace</b> is selected, for CMIS the <b>first repository</b> obtained through the session object 
							is selected.</li>
						<li>@FormParam username: Username to connect to content repository</li>
						<li>@FormParam password: Password to connect to content repository</li>
						<li>@FormParam connectionType: Connection type; either <b>JCR</b> or <b>CMIS</b></li>
			    	</ul>
			    </td>
			</tr>
			<tr>
				<th valign="top">Produces</th>
				<td>HTTP 201 together with the <b>session key</b> corresponding to created session in case of successful execution.<br>
					HTTP 400 when one of the <code>repositoryURL</code>, <code>username</code>, <code>password</code> or <code>connectionType</code> parameters are not set</td>
			</tr>
			<tr>
				<th valign="top">Example</th>
				<td><pre>curl -X GET -H "Accept: text/plain" "http://localhost:8080/cmsadapter/session?repositoryURL=http://localhost:8083/nuxeo/atom/cmis&username=Administrator&password=Administrator&connectionType=CMIS"</pre></td>
			</tr>
		</tbody>
	</table></p>
</div>

</@common.page>
</#escape>