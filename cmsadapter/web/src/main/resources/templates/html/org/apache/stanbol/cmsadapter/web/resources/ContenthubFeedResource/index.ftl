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
	<h3>Service Endpoint <a href="${it.publicBaseUri}cmsadapter/contenthubfeed">/cmsadapter/contenthubfeed</a></h3>
	<p>
		This endpoint provides services to submit/delete content repository objects to/from <a href="${it.publicBaseUri}contenthub">Contenthub</a> component.
		The aim is to manage content repository items in the Contenthub so that search/exploration provided by Contenthub functionality can be applicable for content repositories.  
		The services basically delegate the request to a suitable <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/cmsadapter/servicesapi/src/main/java/org/apache/stanbol/cmsadapter/servicesapi/mapping/ContenthubFeeder.java">ContenthubFeeder</a> instance
		based on the connection information. There are default implementations of <i>ContenthubFeeder</i> interface for JCR and CMIS repositories.
	</p>
	
	<p>Following services are available for this endpoint:<br>
		<ul>
			<li><a href="#Submit_content_items">Submit content items</a></li>
			<li><a href="#Delete_content_items">Delete content items</a></li>
		</ul>
		Interaction with the content repository will be obtained through the <b>Session Key</b> property. To be able to use the services below a session key
		should be through the <b><a href="${it.publicBaseUri}cmsadapter/session">/cmsadapter/session</a></b> resource.  
		<fieldset>
			<legend>Connection parameters</legend>
			<table>
				<tbody>
					<tr>
						<th>Session key</th>
						<td><input type="text" id="sessionKey" value=""></td>
					</tr>
				</tbody>
			</table>
		</fieldset>
	</p>
	<p>
		Following parameters are used to select objects from content repository. Submitted documents are identified through their identifiers
		in the content repository.
		<ul>
			<li>If the <b>ID</b> parameter is set, then a single object having the specified value as its ID will be submitted to Contenthub. </li>
			<li>If only <b>path</b> parameter is set, then a single object having the specified value as its path, first its ID will be obtained
				from the repository and then the content repository object will be submitted to Contenthub based on its ID. It is also possible to
				use <b>recursive</b> paramater together with the <i>path</i> parameter. If this parameter set as <b>true</b>, all content repository 
				objects under the specified path are processed.</li>
		</ul>
		<fieldset>
			<legend>Content object selectors</legend>
			<table>
				<tbody>
					<tr>
						<th>ID</th>
						<td><input type="text" id="contentID" value=""></td>
					</tr>
					<tr>
						<th>Path</th>
						<td><input type="text" id="contentPath" value=""></td>
					</tr>
					<tr>
						<th>Recursive</th>
						<td><input type="checkbox" id="recursiveCheck" value=""></td>
					</tr>
				</tbody>
			</table>
		</fieldset>
	</p>
	<p>
		The following index will be used to store content items or delete contents items from. 	
		<fieldset>
		    <legend>Select an index</legend>
		    <div id="indexDiv"><#--this div will be populated by ajax--></div>
		</fieldset>
	</p>
	
	<a name="Submit_content_items" id="Submit_content_items"></a>
	<h4>Submit content items</h4>
	<p>
		This service enables submission of content repository objects to Contenthub.
	</p>
	<p>
		Following <b>Content Properties</b> parameter indicates the possible properties of content repository object holding the actual
		content. Please note that this property is not used for all cases. For example, content of CMIS documents are obtained via CMIS api directly,
		or if a procesed JCR node has <b>nt:resource</b> node type, the content is obtained from the <b>jcr:data</b> property. Currently, <i>Content Properties</i>
		parameter is only used while processing a JCR node which is neither <b>nt:file</b> or <b>nt:resource</b>.  
		<fieldset>
			<legend>Content properties</legend>
			<table>
				<tbody>
					<tr>
						<th>Content properties</th>
						<td><input type="text" id="contentProperties" value="content,skos:definition"></td>
					</tr>
				</tbody>
			</table>
		</fieldset>
	</p>
	<a id="submitContentItemsLink" href="">Submit content item(s) to Contenthub</a>
	<div id="submitContentItemsResult" style="display: none; ">
		</p><pre id="submitContentItemsResultText"></pre>
	</div>
	
	<a name="Delete_content_items" id="Delete_content_items"></a>
	<h4>Delete content items</h4>
	<p>
		This service enables deletion of content items from Contenthub.
	</p>

	<a id="deleteContentItemsLink" href="">Delete content item(s) from Contenthub</a>
	<div id="deleteContentItemsResult" style="display: none; ">
		<pre id="deleteContentItemsResultText"></pre>
	</div>
</div>

<div class="panel" id="restapi" style="display: none;">
	<h3>Service Endpoint <a href="${it.publicBaseUri}cmsadapter/contenthubfeed">/cmsadapter/contenthubfeed</a></h3>
	<p>Following services are available for this endpoint: Following services are available for this endpoint: Following services are available for this 
	endpoint: Following services are available for this endpoint: Following services are available for this endpoint: Following services are available 
	for this endpoint:<br>
	<ul>
		<li><a href="#Submit_content_item_rest">Submit content items</a></li>
		<li><a href="#Delete_content_item_rest">Delete content items</a></li>
	</ul>
		
	<a name="Submit_content_item_rest" id="Submit_content_items_rest"></a>
	<h4>Submit content items</h4>
	<p><table>
		<tbody>
			<tr>
				<th valign="top">Description</th>
				<td><p>This service enables submission of content repository objects to Contenthub. Connection to the content repository is 
				established by the provided connection information. This service makes possible to submit content items through either their IDs or 
				paths in the content repository. Enhancements of content items are obtained through <b>Stanbol Enhancer</b> before submitting them to 
				Contenthub.<br><br> 
				
				If <code>id</code> parameter is set, the target object is obtained from the content repository according to its ID. If 
				<code>path</code> parameter is set, first the ID of target object is obtained from the content repository and then the retrieved 
				ID is used in submission of content item. When <code>path</code> parameter is set, it is also possible to process all content 
				repository objects under the specified path by setting <code>recursive</code> parameter as <code>true</code>.<br><br>
     			
     			For some cases, it is necessary to know the property of the content repository object that keeps the actual content e.g while 
     			processing a <i>nt:unstructured</i> typed JCR content repository object. Such custom properties are specified within the 
     			<code>contentProperties</code> parameter.</p>
				</td>
			</tr>
			<tr>
				<th valign="top">Request</th>
				<td>POST /cmsadapter/contenthubfeed</td>
			</tr>
			<tr>
				<th valign="top">Parameters</th>
				<td>
					<ul>
						<li>@FormParam sessionKey: Interaction with the content repository is provided through the <b>Session Key</b> property. To be able to use the services below a session key
						should be through the <b><a href="${it.publicBaseUri}cmsadapter/session">/cmsadapter/session</a></b> resource.</li>
			    		<li>@FormParam id: Content repository ID of the content item to be submitted</li>
			    		<li>@FormParam path: Content repository path of the content item to be submitted</li>
			    		<li>@FormParam recursive: This parameter is used together with <code>path</code> parameter. Its default value is 
			    		<code>false</code>. If it is set as <code>true</code>. All content repository objects under the specified path are processed.</li>
			    		<li>@FormParam indexName: Name of the Solr index managed by Contenthub. Specified index will be used to submit the content items.</li>
			    		<li>@FormParam contentProperties: This parameter indicates the list of properties that are possibly holding the actual
     					content. Possible values are passed as comma separated. Its default value is <b>content, skos:definition</b>.</li>
			    	</ul>
			    </td>
			</tr>
			<tr>
				<th valign="top">Produces</th>
				<td>HTTP 200 in case of successful execution.<br>
					HTTP 400 when both <code>id</code> and <code>path</code> parameters or <code>sessionKey</code> parameter is not set</td>
			</tr>
			<tr>
				<th valign="top">Example</th>
				<td><pre>curl -i -X POST --data "sessionKey=eec8ff46-aaf9-485f-a7b5-452c1d7197d0&path=/contenthubfeedtest&recursive=true" http://localhost:8080/cmsadapter/contenthubfeed</pre></td>
			</tr>
		</tbody>
	</table></p>
	<br>
	<a name="Delete_content_item_rest" id="Delete_content_items_rest"></a>
	<h4>Delete content items</h4>
	<table>
		<tbody>
			<tr>
				<th valign="top">Description</th>
				<td>This service enables deletion of content items from Contenthub. Connection to the content repository is established by the 
				provided connection information. This service makes possible to delete content items through either their IDs or paths in the 
				content repository.<br><br>
     			
     			If <code>id</code> parameter is set, the content item is directly tried to be deleted from Contenthub. If <code>path</code> 
     			parameter is set, the ID of the target object is first obtained from the content repository according to its path. Then retrieved ID 
     			is used to delete related content item from Contenthub.
				</td>
			</tr>
			<tr>
				<th valign="top">Request</th>
				<td>DELETE /cmsadapter/contenthubfeed</td>
			</tr>
			<tr>
				<th valign="top">Parameters</th>
				<td>
					<ul>
						<li>@FormParam sessionKey: Interaction with the content repository is provided through the <b>Session Key</b> property. To be able to use the services below a session key
						should be through the <b><a href="${it.publicBaseUri}cmsadapter/session">/cmsadapter/session</a></b> resource.</li>
			    		<li>@FormParam id: Content repository ID of the content item to be submitted</li>
			    		<li>@FormParam path: Content repository path of the content item to be submitted</li>
			    		<li>@FormParam recursive: This parameter is used together with <code>path</code> parameter. Its default value is 
			    		<code>false</code>. If it is set as <code>true</code>. All content repository objects under the specified path are processed.</li>
			    		<li>@FormParam indexName: Name of the Solr index managed by Contenthub. Specified index will be used to delete the content items
			    		from</li>
			    	</ul>
			    </td>
			</tr>
			<tr>
				<th valign="top">Produces</th>
				<td>HTTP 200 in case of successful execution.<br>
					HTTP 400 when both <code>id</code> and <code>path</code> parameters or <code>sessionKey</code> parameter is not set</td>
			</tr>
			<tr>
				<th valign="top">Example</th>
				<td><pre>curl -i -X DELETE --data "sessionKey=eec8ff46-aaf9-485f-a7b5-452c1d7197d0&path=/contenthubfeedtest&recursive=true" http://localhost:8080/cmsadapter/contenthubfeed</pre></td>
			</tr>
		</tbody>
	</table>
</div>

</@common.page>
</#escape>

<script language="javascript">
	function init() {
        $.get("${it.publicBaseUri}contenthub/ldpath", function(indexes) {
            innerStr = "<select id='indexSelect'>" + "<option value='contenthub'>contenthub</option>";
            for(var index in indexes) {
                innerStr += "<option value=" + index + ">" + index + "</option>";
            }
            innerStr += "</select>";
            $("#indexDiv").html(innerStr);
        });
    }
    
    $(document).ready(init);

//click handlers
$(function() {
	$("#submitContentItemsLink").click(function(e) {
		e.preventDefault();
		submitContentItemsToContenthub();
	});
});

$(function() {
	$("#deleteContentItemsLink").click(function(e) {
		e.preventDefault();
		deleteContentItemsFromContenthub();
	});
});
	

function submitContentItemsToContenthub() {
	var data = new Object();
	data.sessionKey = $("#sessionKey").val();
	data.id = $("#contentID").val();
	data.path = $("#contentPath").val();
	data.recursive = $("#recursiveCheck").is(':checked') ? "true" : "false";
	data.indexName = $("#indexSelect").val();
	data.contentProperties = $("#contentProperties").val();
	$("#submitContentItemsResultText").text("Content items are being submitted...");
	$("#submitContentItemsResult").show();
	$.ajax({
	  	type: 'POST',
	  	url: '${it.publicBaseUri}cmsadapter/contenthubfeed',
	  	data: data,
   		success: function(data, textStatus, jqXHR) {
     		$("#submitContentItemsResultText").text(jqXHR.statusText);
     		$("#submitContentItemsResult").show();
   		},
   		error: function(jqXHR, textStatus, errorThrown) {
     		$("#submitContentItemsResultText").text(jqXHR.statusText + " - " + errorThrown + " - " + jqXHR.responseText);
     		$("#submitContentItemsResult").show();
   		}
	});
}

function deleteContentItemsFromContenthub(){
	var data = new Object();
	data.sessionKey = $("#sessionKey").val();
	data.id = $("#contentID").val();
	data.path = $("#contentPath").val();
	data.recursive = $("#recursiveCheck").is(':checked') ? "true" : "false";
	data.indexName = $("#indexSelect").val();
	$("#deleteContentItemsResultText").text("Content items are being deleted...");
	$("#deleteContentItemsResult").show();
	$.ajax({
	  	type: 'DELETE',
	  	url: '${it.publicBaseUri}cmsadapter/contenthubfeed',
	  	data: data,
   		success: function(data, textStatus, jqXHR) {
     		$("#deleteContentItemsResultText").text(jqXHR.statusText);
     		$("#deleteContentItemsResult").show();
   		},
   		error: function(jqXHR, textStatus, errorThrown) {
     		$("#deleteContentItemsResultText").text(jqXHR.statusText + " - " + errorThrown + " - " + jqXHR.responseText);
     		$("#deleteContentItemsResult").show();
   		}
	});
}
</script>