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
<@common.page title="Content Hub" hasrestapi=true> 

<div class="panel" id="webview">

<div class="searchbox" align="right">
  <table><td>
    <tr><input type="text" id="searchKeywords" name="searchKeywords" onkeydown="if (event.keyCode == 13) document.getElementById('searchButton').click()" /><input id="searchButton" type="button" value="Search" onclick="performSearch()" /></tr>
    <tr><div><a href="contenthub/search" />Search Page</a></div></tr>
  </td></table>
</div>

<#--
<em><strong>Disclaimer</strong>: this endpoint is a proof of concept /
<strong>experimental</strong> feature. It does not actually store the content
on the disk, just in memory.</em>
-->

<h3>Recently uploaded Content Items</h3>

<div id="storeContents" class="storeContents">
<div>
<table id="recentlyEnhancedTable">
	<div>
	  <tr>
	  	<th></th>
	    <th>Title</th>
	    <th>Media type</th>
	    <th>Enhancements <#--TODO: fix image path  <img src="${it.staticRootUrl}/contenthub/images/rdf.png" alt="Format: RDF"/> --></th>
	  </tr>
	  <#list it.recentlyEnhancedItems as item>
	  <tr>
		<td>
			<img src="${it.staticRootUrl}/contenthub/images/edit_icon_16.png" onClick="javascript:editContentItem('${item.localId}');" title="Edit this item" />
			<img src="${it.staticRootUrl}/contenthub/images/delete_icon_16.png" onClick="javascript:deleteContentItem('${item.localId}');" title="Delete this item" />
		</td>
	    <td><a href="${item.uri}" title="${item.uri}">${item.title}</a></td>
	    <td>${item.mimetype}</td>
	    <td><a href="${it.publicBaseUri}contenthub/metadata/${item.localId}">${item.enhancements}</a></td>
	  </tr>
	  </#list>
	</ul>
	</div>
</table>
<ul class="previousNext">
  <#if it.moreRecentItemsUri?exists>
    <li class="moreRecent"><a href="${it.moreRecentItemsUri}">More recent items</a></li>
  </#if>
  <#if it.olderItemsUri?exists>
    <li class="older"><a href="${it.olderItemsUri}">Older items</a></li>
  </#if>
</ul>
</div>
</div>


<div id="editingDiv"> </div>

<h3>Submit Constraints to Content Item for analysis</h3>

<fieldset>
	<legend>Give Field:Value for your content</legend>
	<div id="constraintsDiv" style="max-height:190px;overflow:auto">
		<div id="textDiv1">
			<br><input type="text" name="fieldText1" value="${it.titleFieldName}" readonly="readonly" /> : <input type="text" name="valueText1" />
		</div>
	</div>
	
	<br><label onClick="javascript:addConstraint(null);"><img src="${it.staticRootUrl}/contenthub/images/add_icon_16.png" />  Add a new constraint</label>
</fieldset>

<h3>Submit a new Content Item for analysis</h3>

<form method="POST" accept-charset="utf-8" onSubmit = "return setConstraints();">
  <fieldset>
	  <input type="hidden" id="constraintsContent" name="constraints" value="" />
	  <input type="hidden" id="idContent" name="contentId" value="" />
	  <legend>Submit raw text content</legend>
	  <p><textarea rows="15" id="contentTextArea" name="content"></textarea></p>
	  <p><input type="submit" value="Submit text" /></p>
  </fieldset>
</form>

<form method="POST" accept-charset="utf-8" onSubmit = "return setConstraints();">
  <fieldset>
	  <input type="hidden" id="constraintsURL" name="constraints" value="" />
	  <input type="hidden" id="idURL" name="contentId" value="" />
	  <legend>Submit a remote public resource by URL</legend>
	  <p>
	  	<input name="url" type="text" class="url" />
	  	<input type="submit" value="Submit URL" />
	  </p>
  </fieldset>
</form>

<form method="POST" accept-charset="utf-8"  enctype="multipart/form-data" onSubmit = "return setConstraints();">
  <fieldset>
	  <input type="hidden" id="constraintsFile" name="constraints" value="" />
	  <input type="hidden" id="idFile" name="contentId" value="" />
	  <legend>Upload a local file</legend>
	  <p>
	  	<input name="file" type="file"/>
	  	<input type="submit" value="Submit file" />
	  </p>
  </fieldset>
</form>
</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Uploading new content to the Content Hub</h3>

  <p>You can upload content to the Content Hub for analysis with or without providing the content
   id at your option:</p>
  <ol>
    <li><code>PUT</code> content to <code>${it.publicBaseUri}contenthub/content/<strong>content-id</strong></code>
     with <code>Content-Type: text/plain</code>.</li>
    <li><code>GET</code> enhancements from the same URL with
     header <code>Accept: application/rdf+xml</code>.</li>
  </ol>
  
  <p><code><strong>content-id</strong></code> can be any valid path and
   will be used to fetch your item back later.</p>

  <p>On a unix-ish box you can use run the following command from
   the top-level source directory to populate the Stanbol enhancer service with
    sample content items:</p>

<pre>
for file in enhancer/data/text-examples/*.txt;
do
  curl -i -X PUT -H "Content-Type:text/plain" -T $file ${it.publicBaseUri}contenthub/content/$(basename $file) ;
done
</pre> 

  Alternatively you can let the Stanbol enhancer automatically build an id base on the SHA1
  hash of the content by posting it at the root of the Content Hub.
  <ol>
    <li><code>POST</code> content to <code>${it.publicBaseUri}contenthub/</code>
     with <code>Content-Type: text/plain</code>.</li>
    <li><code>GET</code> enhancements from the URL in the response along with
       header <code>Accept: application/rdf+xml</code>.</li>
  </ol>
  
  <p>For instance:</p>
<pre>
curl -i -X POST -H "Content-Type:text/plain" \
     --data "The Stanbol enhancer can detect famous cities such as Paris." \
     ${it.publicBaseUri}contenthub
    
HTTP/1.1 201 Created
Location: ${it.publicBaseUri}contenthub/content/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d
Content-Length: 0
Server: Jetty(6.1.x)
</pre>


<h3>Fetching back the original content item and the related enhancements from the Content Hub</h3>

<p>Once the content is created in the Content Hub, you can fetch back either the original content, a HTML summary view or
the extracted RDF metadata by dereferencing the URL using the <code>Accept</code> header
as selection switch:</p>

<pre>
curl -i <strong>-H "Accept: text/plain"</strong> ${it.publicBaseUri}contenthub/content/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/<strong>raw</strong>/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<pre>
curl -i <strong>-H "Accept: text/html"</strong> ${it.publicBaseUri}contenthub/content/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/<strong>page</strong>/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<pre>
curl -i <strong>-H "Accept: application/rdf+xml"</strong> ${it.publicBaseUri}contenthub/content/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/<strong>metadata</strong>/sha1-84854eb6802a601ca2349ba28cc55f0b930ac96d
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

</div>
<script language="javascript">

	var counter = 1;
	
	function setConstraints(){
		var i;
		var result = JSON.parse("{}");
		for(i=1; i<=counter; i++){
			if (document.getElementById("textDiv" + i)) {
				var field = jQuery.trim(document.getElementsByName("fieldText"+i)[0].value);
				var value = jQuery.trim(document.getElementsByName("valueText"+i)[0].value);
				if(i == 1 && !value) {
					// control for the title input... it must exist
					alert('You should enter title for your content');
					return false;
				}
				if(!field || !value) {
					continue;
				}
				
				if(result[field] == null) {
					result[field] = new Array();
				}
				var values = value.split(",");
				for(j=0; j<values.length; j++){
					result[field].push(jQuery.trim(values[j]));
				}
			}
		}
		var constraints = document.getElementsByName('constraints');
		for (var i in constraints) {
			constraints[i].value = JSON.stringify(result);
		}
		return true;
	}
	
	function addConstraint(vfn){
		counter++;
		var newCons = document.createElement('div');
		newCons.setAttribute('id','textDiv' + counter);
		var fieldName = "fieldText"+counter;
		var valueName = "valueText"+counter;
		var url = "javascript:removeConstraint(" + counter + ");";
		if(vfn == '${it.titleFieldName}') {
			newCons.innerHTML = "<br/><input type='text' name=" + fieldName + " readonly=\"readonly\" />" 
		 					+ " : "
		 					+ "<input type='text' name=" + valueName + " />";
		}
		else {
			newCons.innerHTML = "<br/><input type='text' name=" + fieldName + " />" 
		 					+ " : "
		 					+ "<input type='text' name=" + valueName + " />"
		 					+ "  <img src='${it.staticRootUrl}/contenthub/images/delete_icon_16.png' title='Remove' onClick=" + url + " />";
		}		 		
		document.getElementById("constraintsDiv").appendChild(newCons);
		document.getElementsByName(fieldName)[0].focus();
	}
	
	function removeConstraint(divNo){
		var constraintsDiv = document.getElementById('constraintsDiv');
		constraintsDiv.removeChild(document.getElementById('textDiv'+divNo));
	}

	function cancelEditing(){
		var contentids = document.getElementsByName('contentId');
		for (var i in contentids) {
			contentids[i].value = "";
		}
		document.getElementById("editingDiv").innerHTML = "";
	}

	function editContentItem(vlocalid) {
		var lurl = "${it.publicBaseUri}contenthub/update/" + vlocalid;
		document.getElementById("constraintsDiv").innerHTML = "";
		counter=0;
		$.ajax({
			url: lurl,
			type: "GET",
			async: true,
			cache: false,
			success: function(jsonCons) {
			
				var contentItem = JSON.parse(jsonCons);
				var count=1;
				if(contentItem != null) {
					// TODO: use more mimeType
					if(contentItem["mimeType"] == "text/plain"){
						document.getElementById("contentTextArea").value = contentItem["content"];
					} else {
						document.getElementById("contentTextArea").value = "";
					}
					var contentids = document.getElementsByName('contentId');
					for (var i in contentids) {
						contentids[i].value = contentItem["id"];
					}
					document.getElementById("editingDiv").innerHTML = 	'<img src="${it.staticRootUrl}/contenthub/images/delete_icon_16.png" title="Cancel Editing" onClick="javascript:cancelEditing()" />'
																		+ " You are editing Content Item " + contentItem["id"];
					delete contentItem["content"];
					delete contentItem["id"];
					delete contentItem["mimeType"];
					
					for(var p in contentItem) {
						if(contentItem.hasOwnProperty(p)) {
							var fieldName = p.toString();
							if(fieldName.indexOf("_") != -1) {
								var lastindex = fieldName.lastIndexOf("_");
								fieldName = fieldName.substring(0, lastindex);
							}
							addConstraint(fieldName);
							document.getElementsByName("fieldText"+counter)[0].value = fieldName;
							document.getElementsByName("valueText"+counter)[0].value = contentItem[p].substring(1, contentItem[p].length-1);
						}
					}
				}	
			},
			error: function(content) {
				alert(result.status + ' ' + result.statusText);
			}
		});
	}	
	
	function deleteContentItem(vlocalid) {
		var lurl = "${it.publicBaseUri}contenthub/content/" + vlocalid;
		$.ajax({
			url: lurl,
			type: "DELETE",
			async: true,
			cache: false,
			success: function() {
				$("#storeContents").load("${it.publicBaseUri}contenthub #storeContents>div");
			},
			error: function() {
				alert(result.status + ' ' + result.statusText);
			}
		});
	}
	
	function performSearch() {
		var lurl = "${it.publicBaseUri}contenthub/search?kw=" + $("#searchKeywords").val();
		window.location.replace(lurl);
	}
	
</script>
</@common.page>
</#escape>
