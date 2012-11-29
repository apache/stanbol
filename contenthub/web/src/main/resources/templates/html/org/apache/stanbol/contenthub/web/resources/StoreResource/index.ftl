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
<@common.page title="Contenthub" hasrestapi=true> 

<div class="panel" id="webview">
  <table>
    <tr>
      <td>
        <fieldset>
          <legend>Select an index</legend>
          <div id="indexDiv"><#--this div will be populated by ajax--></div>
        </fieldset>
      </td>
      <td>
        <div class="searchbox" align="right">
          <table>
            <td>
              <tr><input type="text" id="searchKeywords" name="searchKeywords" onkeydown="if (event.keyCode == 13) document.getElementById('searchButton').click()" /><input id="searchButton" type="button" value="Search" onClick="javascript:performSearch();" /></tr>
              <tr><div><a href="${it.publicBaseUri}contenthub/${it.indexName}/search/featured" />Search Page</a></div></tr>
              <br/>
              <tr><div><a href="${it.publicBaseUri}contenthub/ldpath">Submit Index</a></div></tr>
            </td>
          </table>
        </div>
      </td>
    </tr>
  </table>

  <div id="searchResult" class="invisible"></div>

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
                <img src="${it.staticRootUrl}/contenthub/images/delete_icon_16.png" onClick="javascript:deleteContentItem('${item.localId}');" title="Delete this item" />
              </td>
              <td><a href="${item.dereferencableURI}" title="${item.dereferencableURI}"><#if item.title??>${item.title}<#else>${item.localId}</#if></a></td>
              <td>${item.mimetype}</td>
              <td><a href="${it.publicBaseUri}contenthub/${it.indexName}/store/metadata/${item.localId}">${item.enhancementCount}</a></td>
            </tr>
          </#list>
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

  <h3>Submit Content Item </h3>
  <fieldset>
    <legend>Optional parameters</legend>
    <table>
      <tr>
      	<td>
    	  Title : 
    	</td>
    	<td>
    	  <input type="text" id="valueTitle" />
    	</td>
      </tr>
      <tr>
        <td>
          Chain Name : 
    	</td>
    	<td>
    	  <input type="text" id="valueChain" />
    	</td>
      </tr>
    </table>
  </fieldset>
  <br/>
  
  <form method="POST" id="contentForm" accept-charset="utf-8" onSubmit = "return performSubmit(1);">
    <fieldset>
      <input type="hidden" id="title1" name="title" value="" />
      <input type="hidden" id="chain1" name="chain" value="" />
      <legend>Submit raw text content</legend>
      <p><textarea rows="15" id="contentTextArea" name="content"></textarea></p>
      <p><input type="submit" id="contentSubmit" value="Submit text" /></p>
    </fieldset>
  </form>

  <form method="POST" id="urlForm" accept-charset="utf-8" onSubmit = "return performSubmit(2);">
    <fieldset>
      <input type="hidden" id="title2" name="title" value="" />
      <input type="hidden" id="chain2" name="chain" value="" />
      <legend>Submit a remote public resource by URL</legend>
      <p>
        <input name="url" id="url" type="text" class="url" />
        <input type="submit" id="urlSubmit" value="Submit URL" />
      </p>
    </fieldset>
  </form>

  <form method="POST" id="fileForm" accept-charset="utf-8"  enctype="multipart/form-data" onSubmit = "return performSubmit(3);">
    <fieldset>
    <legend>Upload a local file</legend>
      <p>
        <input type="file" id="file" name="content"/>
        <input type="submit" id="fileSubmit" value="Submit file" />
      </p>
    </fieldset>
  </form>
</div>

<div class="panel" id="restapi" style="display: none;">
  <#include "/imports/storerestapi.ftl">
</div>

<script language="javascript">

    var counter = 0;
 
    function init() {
        $.get("${it.publicBaseUri}contenthub/ldpath", function(indexes) {
            innerStr = "<select id='indexSelect' onChange='javascript:redirectIndex();'>" + "<option value='contenthub'>contenthub</option>"
            for(var index in indexes) {
                innerStr += "<option value=\"" + index + "\">" + index + "</option>";
            }
            innerStr += "</select>";
            $("#indexDiv").html(innerStr);
            $("#indexSelect").val("${it.indexName}");
        });
    }
    
    $(document).ready(init);
    
    function redirectIndex() {
        var index = $("#indexSelect").val();
        window.location.replace("${it.publicBaseUri}contenthub/" + index + "/store/");
    }
    
    function performSubmit(submissionMethod) {
    	if(submissionMethod == 1) {
    		var content = document.getElementById("contentTextArea").value;
    		if(!content || content == "") {
    			alert('You should enter non-empty content');
            	return false;
    		}
    		document.getElementById('title1').value = document.getElementById('valueTitle').value;
    		document.getElementById('chain1').value = document.getElementById('valueChain').value;
    	} else if(submissionMethod == 2) {
    	
    		var url = document.getElementById("url").value;
    		if(!url || url == "") {
    			alert('You should enter non-empty URL');
            	return false;
    		}
    		document.getElementById('title2').value = document.getElementById('valueTitle').value;
    		document.getElementById('chain2').value = document.getElementById('valueChain').value;
    	} else if(submissionMethod == 3) {
    		var file = document.getElementById("file").value;
    		if(!file || file == "") {
    			alert('You should specify a file to be submitted');
            	return false;
    		}
    		var title = $.trim($("#valueTitle").val());
    		var chain = $.trim($("#valueChain").val());
    		var actionStr = "";
    		if(title != null && title != ""){
    			actionStr += "?title="+encodeURIComponent(title);
    		}
    		if(chain != null && chain != ""){
    			actionStr += (actionStr != "") ? "&" : "?" 
    			actionStr += "chain="+encodeURIComponent(chain);
    		}
    		document.getElementById('fileForm').action = actionStr;
    	}
        return true;
    }
  
    function deleteContentItem(vlocalid) {
        var lurl = "${it.publicBaseUri}contenthub/${it.indexName}/store/" + vlocalid;
        $.ajax({
            url: lurl,
            type: "DELETE",
            async: true,
            cache: false,
            success: function() {
                $("#storeContents").load("${it.publicBaseUri}contenthub/${it.indexName}/store #storeContents>div");
            },
            error: function() {
                alert(result.status + ' ' + result.statusText);
            }
        });
    }
  
    function performSearch() {
        if($("#searchKeywords").val() == null || $.trim($("#searchKeywords").val()).length == 0) {
            alert("You should enter keyword(s) for search");
            return;
        }
        var lurl = "${it.publicBaseUri}contenthub/${it.indexName}/search/featured?fromStore=\"y\"&queryTerm=" + $("#searchKeywords").val();
        window.location.replace(lurl);
    }
  
    function getHtmlName(name) {
        lastUnderscore = name.lastIndexOf("_");
        if(lastUnderscore >= 0) {
            underScoreExtension = name.substring(lastUnderscore);
            if(underScoreExtension == "_t" || underScoreExtension == "_l" || underScoreExtension == "_d" || underScoreExtension == "_dt") {
                return name.substring(0, lastUnderscore);
            }
        }
        return name;
    }
  
</script>
</@common.page>
</#escape>
