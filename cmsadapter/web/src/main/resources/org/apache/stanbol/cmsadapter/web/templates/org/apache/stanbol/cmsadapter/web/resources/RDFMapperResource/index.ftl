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
	<h3>Service Endpoint <a href="${it.publicBaseUri}cmsadapter/map">/cmsadapter/map</a></h3>
	<p>This endpoint provides a bidirectional mapping between external RDF data and JCR/CMIS 
	   	content repositories based on <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/servicesapi/src/main/java/org/apache/stanbol/cmsadapter/servicesapi/mapping/RDFBridge.java">RDFBridge</a>s.
		<b>Default RDF Bridge</b> implementation can be configured through the <b>Apache Stanbol CMS Adapter Default RDF Bridge Configurations</b> 
		entry in the <a href="${it.publicBaseUri}system/console/configMgr">Apache Felix Web Console Configuration Panel</a>. 
	</p>
	
	<p>Following services are available for this endpoint:<br>
		<ul>
			<li><a href="#Map_RDF_to_repository">Map RDF to repository</a></li>
			<li><a href="#Map_repository_to_RDF">Map repository to RDF</a></li>
		</ul>
		Interaction with the content repository is provided through the following properties. They are required all services provided by this endpoint.
		Please note that while connecting JCR repositories <b>RMI protocol</b> is used, on the other hand for CMIS repositories <b>AtomPub Binding</b> 
		is used. So, it is necessary to set <b>repositoryURL</b> parameter based on these connection methods. 
		<fieldset>
			<legend>Connection parameters</legend>
			<table>
				<tbody>
					<tr>
						<th>Repository URL</th>
						<td><input type="text" id="repositoryURL" value=""></td>
					</tr>
					<tr>
						<th>Workspace Name</th>
						<td><input type="text" id="workspaceName" value=""></td>
					</tr>
					<tr>
						<th>Username</th>
						<td><input type="text" id="username" value=""></td>
					</tr>
					<tr>
						<th>Password</th>
						<td><input type="password" id="password" value=""></td>
					</tr>
					<tr>
						<th>Connection type</th>
						<td>
							<select id="connectionType">
							  <option value="JCR">JCR</option>
							  <option value="CMIS">CMIS</option>
							</select>
						</td>
					</tr>
				</tbody>
			</table>
		</fieldset>
	</p>

	<a name="Map_RDF_to_repository" id="Map_RDF_to_repository"></a>
	<h4>Map RDF to repository</h4>
	<p>
		This service allows clients to map specified RDF to the content repository. In the first step the RDF data is annotated according to
		RDF Bridges loaded in the OSGI environment. Additional annotations provide selection of certain resources from RDF data and 
		creation/update of related content repository object. 
	</p>
	<h5>Test</h5>
	<fieldset>
		<legend>Submit raw RDF data</legend>
		<p><textarea rows="15" id="rawRDF"></textarea></p>
		<p><input type="button" value="Submit RDF" onclick="postRawRDF()"/></p>
	</fieldset>
	<div id="rawRDFpostResult" style="display: none; ">
		</p><pre id="rawRDFpostResultText"></pre>
	</div>
	
	<fieldset>
	  	<legend>Submit a remote public RDF by URL</legend>
	  	<p><input id="urlInput" type="text" class="url">
	   	  <input type="button" value="Submit URL" onclick="postRDFFromURL()"></p>
  	</fieldset>
	<div id="postRDFFromURLResult" style="display: none; ">
		</p><pre id="postRDFFromURLResultText"></pre>
	</div>
	
	<form id="localRDFFileForm" method="POST" accept-charset="utf-8" enctype="multipart/form-data">
	  	<fieldset>
			<legend>Upload a local RDF file</legend>
		 	<p><input name="rdfFile" type="file">
		  	   <input type="button" value="Submit file" onclick="postLocalRDFFileMapping()"></p>
	  	</fieldset>
	</form>
		
	<a name="Map_repository_to_RDF" id="Map_repository_to_RDF"></a>
	<h4>Map repository to RDF</h4>
	<p>
		This service allows clients to map content repository to RDF. In the first step, structure of the content repository is converted into an RDF. 
		For this process detailed documentation can be found in javadoc of <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/servicesapi/src/main/java/org/apache/stanbol/cmsadapter/servicesapi/mapping/RDFMapper.java">RDFMapper</a> interface. 
		There are two implementations of this interface for JCR and CMIS protocols respectively, <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/jcr/src/main/java/org/apache/stanbol/cmsadapter/jcr/repository/JCRRDFMapper.java">JCRRDFMapper</a> 
		and <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/cmis/src/main/java/org/apache/stanbol/cmsadapter/cmis/repository/CMISRDFMapper.java">CMISRDFMapper</a>. 
		At the end of first step, generated RDF contains only <b>CMS Vocabulary</b> annotations. Afterwards, additional assertions are added based on RDF 
		Bridges loaded in the OSGI environment.  
	</p>
	<h5>Test</h5>
	<a onclick="javascript:postMapRepositoryToRDF()" href="javascript:void(0);">Map content repository to RDF</a>
	<div id="postMapRepositoryToRDFResult" style="display: none; ">
		<p><a href="#" onclick="$('#postMapRepositoryToRDFResult').hide(); return false;">Hide results</a>
		</p><pre id="postMapRepositoryToRDFResultText"></pre>
	</div>
	
	<p>
		<input id="storeCheck" type="checkbox"/>
		Store generated RDF persistently
	</p>
</div>

<div class="panel" id="restapi" style="display: none;">
	<h3>Service Endpoint <a href="${it.publicBaseUri}cmsadapter/map">/cmsadapter/map</a></h3>
	<p>Following services are available for this endpoint:<br>
	<ul>
		<li><a href="#Map_RDF_to_repository">Map RDF to repository</a></li>
		<li><a href="#Map_repository_to_RDF">Map repository to RDF</a></li>
	</ul>
		
	<a name="Map_RDF_to_repository" id="Map_RDF_to_repository"></a>
	<h4>Map RDF to repository</h4>
	<table>
		<tbody>
			<tr>
				<th valign="top">Description</th>
				<td>Allows clients to map specified RDF to the content repository. In the first step the RDF data is annotated according to
					RDF Bridges loaded in the OSGI environment. Additional annotations provide selection of certain resources from RDF data and 
					creation/update of related content repository object. Either a raw RDF can be given in <code>serializedGraph</code> parameter
					or URL of an external RDF data can given in <code>url</code> parameter. However, <code>serializedGraph</code> has a higher
					priority. 
				</td>
			</tr>
			<tr>
				<th valign="top">Request</th>
				<td>POST /cmsadapter/map/rdf</td>
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
			    		<li>@FormParam serializedGraph: External RDF in <b>application/rdf+xml</b> format</li>
			    		<li>@FormParam url: URL of the external RDF data.</li>
			    	</ul>
			    </td>
			</tr>
			<tr>
				<th valign="top">Produces</th>
				<td>HTTP 200 in case of successful execution.</td>
			</tr>
			<tr>
				<th valign="top">Example</th>
				<td><pre>curl -i -X POST -d "repositoryURL=rmi://localhost:1099/crx&workspaceName=test&username=admin&password=admin&connectionType=JCR&url=http://www.externalrdf.data" http://localhost:8080/cmsadapter/map/rdf</pre></td>
			</tr>
		</tbody>
	</table>
	<br><hr>
	<table>
		<tbody>
			<tr>
				<th valign="top">Description</th>
				<td>This is service does the same job with the previous one except that this service provides users to submit an RDF file from his local
					file system. So it takes connection parameters as query parameters in the service URL.
				</td>
			</tr>
			<tr>
				<th valign="top">Request</th>
				<td>POST /cmsadapter/map/rdf?repositoryURL={repositoryURL}&workspaceName={workspaceName}&username={username}&password={password}"&connectionType={connectionType}</td>
			</tr>
			<tr>
				<th valign="top">Parameters</th>
				<td>
					<ul>
						<li>@QueryParam repositoryURL: URL of the content repository</li>
			            <li>@QueryParam workspaceName: For JCR repositories this parameter determines the workspace to be connected. On the other hand
			            	for CMIS repositories <b>repository ID</b> should be set to this parameter. In case of not setting this parameter,
			            	for JCR <b>default workspace</b> is selected, for CMIS the <b>first repository</b> obtained through the session object 
			            	is selected.</li>
			    		<li>@QueryParam username: Username to connect to content repository</li>
			    		<li>@QueryParam password: Password to connect to content repository</li>
			    		<li>@QueryParam connectionType: Connection type; either <b>JCR</b> or <b>CMIS</b></li>
			    		<li>@FormDataParam rdfFile: Local RDF file to be submitted</li>
			    		<li>@FormDataParam rdfFileInfo: Information about submitted RDF file</li>
			    	</ul>
			    </td>
			</tr>
			<tr>
				<th valign="top">Produces</th>
				<td>HTTP 200 together with entry page of <b>/cmsadapter/map</b> endpoint, in case of successful execution.</td>
			</tr>
			<tr>
				<th valign="top">Example</th>
				<td><pre>curl -i -X POST -F "rdfFile=@localRDFFile" "http://localhost:8080/cmsadapter/map/rdf?repositoryURL=http://localhost:8083/nuxeo/atom/cmis&workspaceName=test&username=admin&password=admin&connectionType=CMIS"</pre></td>
			</tr>
		</tbody>
	</table>
	
	<a name="Map_repository_to_RDF" id="Map_repository_to_RDF"></a>
	<h4>Map repository to RDF</h4>
	<table>
		<tbody>
			<tr>
				<th valign="top">Description</th>
				<td>
					This service allows clients to map content repository to RDF. In the first step, structure of the content repository is converted into an RDF. 
					For this process detailed documentation can be found in javadoc of <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/servicesapi/src/main/java/org/apache/stanbol/cmsadapter/servicesapi/mapping/RDFMapper.java">RDFMapper</a> interface. 
					There are two implementations of this interface for JCR and CMIS protocols respectively, <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/jcr/src/main/java/org/apache/stanbol/cmsadapter/jcr/repository/JCRRDFMapper.java">JCRRDFMapper</a> 
					and <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/cmis/src/main/java/org/apache/stanbol/cmsadapter/cmis/repository/CMISRDFMapper.java">CMISRDFMapper</a>. 
					At the end of first step, generated RDF contains only <b>CMS Vocabulary</b> annotations. Afterwards, additional assertions are added based on RDF 
					Bridges loaded in the OSGI environment.  
				</td>
			</tr>
			<tr>
				<th valign="top">Request</th>
				<td>POST /cmsadapter/map/cms</td>
			</tr>
			<tr>
				<th valign="top">Parameters</th>
				<td>
					<ul>
						<li>@FormParam repositoryURL: URL of the content repository</li>
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
				<td>Mapped RDF from content repository in <b>application/rdf+xml</b> format</td>.
			</tr>
			<tr>
				<th valign="top">Example</th>
				<td><pre>curl -i -X POST -d "repositoryURL=rmi://localhost:1099/crx&workspaceName=test&username=admin&password=admin&connectionType=JCR" http://localhost:8080/cmsadapter/map/cms</pre></td>
			</tr>
		</tbody>
	</table>
</div>

</@common.page>
</#escape>

<script language="javascript">
function postRawRDF() {
	var data = new Object();
	data.repositoryURL = $("#repositoryURL").val();
	data.workspaceName = $("#workspaceName").val();
	data.username = $("#username").val();
	data.password = $("#password").val();
	data.connectionType = $("#connectionType").val();
	data.serializedGraph = $("#rawRDF").val();
	$.ajax({
	  	type: 'POST',
	  	url: '${it.publicBaseUri}cmsadapter/map/rdf',
	  	data: data,
   		success: function(data, textStatus, jqXHR) {
     		$("#rawRDFpostResultText").text(jqXHR.responseText);
     		$("#rawRDFpostResult").show();
   		},
   		error: function(jqXHR, textStatus, errorThrown) {
     		$("#rawRDFpostResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
     		$("#rawRDFpostResult").show();
   		}
	});
}

function postRDFFromURL() {
	var data = new Object();
	data.repositoryURL = $("#repositoryURL").val();
	data.workspaceName = $("#workspaceName").val();
	data.username = $("#username").val();
	data.password = $("#password").val();
	data.connectionType = $("#connectionType").val();
	data.url = $("#urlInput").val();
	$.ajax({
	  	type: 'POST',
	  	url: '${it.publicBaseUri}cmsadapter/map/rdf',
	  	data: data,
   		success: function(data, textStatus, jqXHR) {
     		$("#postRDFFromURLResultText").text(jqXHR.responseText);
     		$("#postRDFFromURLResult").show();
   		},
   		error: function(jqXHR, textStatus, errorThrown) {
     		$("#postRDFFromURLResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
     		$("#postRDFFromURLResult").show();
   		}
	});
}

function postLocalRDFFileMapping() {
	var url = "?repositoryURL=" + $("#repositoryURL").val() + 
				"&workspaceName=" + $("#workspaceName").val() + 
				"&username=" + $("#username").val() +
				"&password=" + $("#password").val() +
				"&connectionType=" + $("#connectionType").val();
	url = "${it.publicBaseUri}cmsadapter/map/rdf" + url;
	$('#localRDFFileForm').attr('action', url);
	$('#localRDFFileForm').submit();
}

function postMapRepositoryToRDF(){
	var data = new Object();
	data.repositoryURL = $("#repositoryURL").val();
	data.workspaceName = $("#workspaceName").val();
	data.username = $("#username").val();
	data.password = $("#password").val();
	data.connectionType = $("#connectionType").val();
	data.store = $("#storeCheck").is(':checked') ? "true" : "false";
	$.ajax({
	  	type: 'POST',
	  	url: '${it.publicBaseUri}cmsadapter/map/cms',
	  	data: data,
   		success: function(data, textStatus, jqXHR) {
     		$("#postMapRepositoryToRDFResultText").text(jqXHR.responseText);
     		$("#postMapRepositoryToRDFResult").show();
   		},
   		error: function(jqXHR, textStatus, errorThrown) {
     		$("#postMapRepositoryToRDFResultText").text(jqXHR.statusText + " - " + jqXHR.responseText);
     		$("#postMapRepositoryToRDFResult").show();
   		}
	});	
}
</script>
