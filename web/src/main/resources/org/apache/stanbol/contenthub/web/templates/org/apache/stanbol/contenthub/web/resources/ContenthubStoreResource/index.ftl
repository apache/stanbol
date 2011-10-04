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

<#--
<em><strong>Disclaimer</strong>: this endpoint is a proof of concept /
<strong>experimental</strong> feature. It does not actually store the content
on the disk, just in memory.</em>
-->

<h3>Recently uploaded Content Items</h3>

<div class="storeContents">
<table id="recentlyEnhancedTable">
	<div>
	  <tr>
	  	<th></th>
	    <th>Local ID</th>
	    <th>Media type</th>
	    <th>Enhancements <#--TODO: fix image path  <img src="${it.staticRootUrl}/contenthub/images/rdf.png" alt="Format: RDF"/> --></th>
	  </tr>
	  <#list it.recentlyEnhancedItems as item>
	  <tr>
		<td><img src="${it.staticRootUrl}/contenthub/images/delete_icon_16.png" onClick="javascript:deleteContentItem('${item.localId}');" title="Delete this item"></td>
	    <td><a href="${item.uri}" title="${item.uri}">${item.localId}</a></td>
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

<h3>Submit Constraints to Content Item for analysis</h3>

<fieldset>
	<legend>Give Field:Value for your content</legend>
	<div id="constraintsDiv">
		<div id="textDiv1">
			<br><input type="text" name="fieldText1"> : <input type="text" name="valueText1">
		</div>
	</div>
	
	<br><label onClick="javascript:addConstraint();"><img src="${it.staticRootUrl}/contenthub/images/add_icon_16.png" />  Add a new constraint</label>
</fieldset>

<h3>Submit a new Content Item for analysis</h3>



<form method="POST" accept-charset="utf-8">
  <fieldset>
  <input type="hidden" id="hidden1" name="constraints" value="">
  <legend>Submit raw text content</legend>
  <p><textarea rows="15" name="content"></textarea></p>
  <p><input type="submit" onClick="javascript:setConstraints(1);" value="Submit text"></p>
  </fieldset>
</form>

<form method="POST" accept-charset="utf-8">
  <fieldset>
  <input type="hidden" id="hidden2" name="constraints" value="">
  <legend>Submit a remote public resource by URL</legend>
  <p><input name="url" type="text" class="url" />
     <input type="submit" onClick="javascript:setConstraints(2);" value="Submit URL"></p>
  </fieldset>
</form>

<form method="POST" accept-charset="utf-8"  enctype="multipart/form-data">
  <fieldset>
  <input type="hidden" id="hidden3" name="constraints" value="">
  <legend>Upload a local file</legend>
  <p><input name="file" type="file"/>
     <input type="submit" onClick="javascript:setConstraints(3);" value="Submit file"></p>
  </fieldset>
</form>


</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Uploading new content to the Content Hub</h3>

  <p>You can upload content to the Content Hub for analysis with or without providing the content
   id at your option:<p>
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
as selection switch:<p>

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
	
	function setConstraints(hiddenNo){
	
		var i=1;
		var result="{";
		for(i=0 ; i<=counter ; i++){
			if (document.getElementById("textDiv" + i)) {
				var field = "\""+document.getElementsByName("fieldText"+i)[0].value+"\"";
				var value = ("[\""+document.getElementsByName("valueText"+i)[0].value+"\"]").replace(/,/g,'\",\"');
				result += field + ":" + value + ",";
			}
		}
		result = result.substring(0, result.length-1) + "}";
		document.getElementById("hidden"+hiddenNo).value = result;
	}
	
	function addConstraint(){
		counter++;
		var newCons = document.createElement('div');
		newCons.setAttribute('id','textDiv' + counter);
		var fieldName = "fieldText"+counter;
		var valueName = "valueText"+counter;
		var url = "javascript:removeConstraint(" + counter + ");";
		newCons.innerHTML = "<br><input type='text' name=" + fieldName + ">" 
		 					+ " : "
		 					+ "<input type='text' name=" + valueName + ">"
		 					+ "  <img src='${it.staticRootUrl}/contenthub/images/delete_icon_16.png' title='Remove' onClick=" + url + ">";
		 		
		document.getElementById("constraintsDiv").appendChild(newCons);
		
		document.getElementsByName(fieldName)[0].focus();
		
	}
	
	function removeConstraint(divNo){
		var constraintsDiv = document.getElementById('constraintsDiv');
		constraintsDiv.removeChild(document.getElementById('textDiv'+divNo));
	}
	
	function deleteContentItem(vlocalid) {
		var lurl = "${it.publicBaseUri}contenthub/content/" + vlocalid;
		$.ajax({
			url: lurl,
			type: "DELETE",
			async: true,
			cache: false,
			success: function(ind) {
				var startIndex = ind.indexOf('<table id="recentlyEnhancedTable">');
				var endIndex = ind.indexOf('</table>',startIndex)+8;
				$("#recentlyEnhancedTable").replaceWith(ind.substring(startIndex,endIndex));
			},
			error: function(ind) {
				alert(result.status + ' ' + result.statusText);
			}
		});
	}
	
</script>
</@common.page>
</#escape>
