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
                <img src="${it.staticRootUrl}/contenthub/images/edit_icon_16.png" onClick="javascript:editContentItem('${item.localId}', '${item.title?js_string}');" title="Edit this item" />
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

  <div id="editingDiv"> </div>

  <h3>Submit Constraints to Content Item for analysis</h3>
  <fieldset>
    <legend>Give Field:Value for your content</legend>
    <input type="text" id="fieldTitle" value="title" readonly="readonly"/> : <input type="text" id="valueTitle" />
    <div id="constraintsDiv" style="max-height:190px;overflow:auto"></div>
    <br/>
    <label onClick="javascript:addConstraint();">
      <img src="${it.staticRootUrl}/contenthub/images/add_icon_16.png" />  Add a new constraint
    </label>
  </fieldset>
  <br/>
  
  <h3>Submit a new Content Item for analysis</h3>
  <form method="POST" id="contentForm" accept-charset="utf-8" onSubmit = "return setConstraints();">
    <fieldset>
      <input type="hidden" id="constraintsContent" name="constraints" value="" />
      <input type="hidden" name="title" value="" />
      <input type="hidden" id="uriContent" name="uri" value="" />
      <legend>Submit raw text content</legend>
      <p><textarea rows="15" id="contentTextArea" name="content"></textarea></p>
      <p><input type="submit" id="contentSubmit" value="Submit text" /></p>
    </fieldset>
  </form>

  <form method="POST" id="urlForm" accept-charset="utf-8" onSubmit = "return setConstraints();">
    <fieldset>
      <input type="hidden" id="constraintsURL" name="constraints" value="" />
      <input type="hidden" name="title" value="" />
      <input type="hidden" id="uriURL" name="uri" value="" />
      <legend>Submit a remote public resource by URL</legend>
      <p>
        <input name="url" type="text" class="url" />
        <input type="submit" id="urlSubmit" value="Submit URL" />
      </p>
    </fieldset>
  </form>

  <form method="POST" id="fileForm" accept-charset="utf-8"  enctype="multipart/form-data" onSubmit = "return setConstraints();">
    <fieldset>
      <input type="hidden" id="constraintsFile" name="constraints" value="" />
      <input type="hidden" name="title" value="" />
      <input type="hidden" id="uriFile" name="uri" value="" />
      <legend>Upload a local file</legend>
      <p>
        <input id="file" name="file" type="file"/>
        <input type="submit" id="fileSubmit" value="Submit file" />
      </p>
    </fieldset>
  </form>
</div>

<div class="panel" id="restapi" style="display: none;">
  <h3>Uploading new content to the Contenthub</h3>

  <p>Contenthub lives at the endpoint starting with "contenthub":</p>
  <code>${it.publicBaseUri}contenthub/</code>
  <p>This endpoint automatically forwards to:</p>
  <code>${it.publicBaseUri}contenthub/${it.indexName}/store/</code>
  <p>The endpoint to which Contenthub automatically forwards includes the name of the default index,
  whose name is "contenthub". That is the reason for two consecutive "contenthub"s in the endpoint.
  Lastly, "store" page provides the storage related functionalities of Contenthub such as document submission.</p>

  <p>You can upload content to the Contenthub for analysis with or without providing the content
   id at your option:</p>
  <ul>
    <li><code>POST</code> content to <code>${it.publicBaseUri}contenthub/${it.indexName}/store/<strong>content-id</strong></code>
     with <code>Content-Type: text/plain</code>.</li>
    <li><code>GET</code> content with its enhancements from the same URL.</li>
  </ul>
  
  <p><code><strong>content-id</strong></code> can be any valid URI and
   will be used to fetch your item back later. <code><strong>content-id</strong></code>s are unique within Contenthub.</p>

  <p>On a unix-ish box you can use run the following command from
   the top-level source directory to populate the Stanbol Contenthub service with
   sample content items:</p>

  <pre>
  for file in enhancer/data/text-examples/*.txt;
  do
    curl -i -X POST -H "Content-Type:text/plain" -T $file ${it.publicBaseUri}contenthub/${it.indexName}/store/$(basename $file);
  done
  </pre> 

  Alternatively you can let the Stanbol Contenthub automatically build an id base on the SHA1
  hash of the content by posting it at the root of the Contenthub.
  <ul>
    <li><code>POST</code> content to <code>${it.publicBaseUri}contenthub/${it.indexName}/store</code>
     with <code>Content-Type: text/plain</code>.</li>
  </ul>
  
  <p>For instance:</p>
<pre>
curl -i -X POST -H "Content-Type:text/plain" \
     --data "The Stanbol enhancer can detect famous cities such as Paris." \
     ${it.publicBaseUri}contenthub/${it.indexName}/store
    
HTTP/1.1 201 Created
Location: ${it.publicBaseUri}contenthub/${it.indexName}/store/content/{<code><strong>content-id</strong><code>}
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<h3>Fetching back the original content item and the related enhancements from the Contenthub</h3>

<p>Once the content is created in the Contenthub, you can fetch back either the original content, a HTML summary view or
the extracted RDF metadata by dereferencing the URL:</p>

<pre>
curl -i <strong>-H "Accept: text/plain"</strong> ${it.publicBaseUri}contenthub/${it.indexName}/store/content/{<code><strong>content-id</strong><code>}

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/${it.indexName}/store/raw/{<code><strong>content-id</strong><code>}
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<p>
Tutorials on Stanbol Contenthub can be found in the following links:<br/>
<a href="http://incubator.apache.org/stanbol/docs/trunk/contenthub/">Contenhub - One Minute Tutorial</a><br/>
<a href="http://incubator.apache.org/stanbol/docs/trunk/contenthub/contenthub5min">Contenthub - Five Minutes Tutorial</a>
</p>

<h4>The RESTful API of the Contenthub</h4>

<#-- Will be written by Meric -->

</div>

<script language="javascript">

    var counter = 0;
 
    function init() {
        $.get("${it.publicBaseUri}contenthub/ldpath", function(indexes) {
            innerStr = "<select id='indexSelect' onChange='javascript:redirectIndex();'>" + "<option value='contenthub'>contenthub</option>"
            for(var index in indexes) {
                innerStr += "<option value=" + index + ">" + index + "</option>";
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
    
    function setConstraints() {
        var titleStr = document.getElementById("valueTitle").value;
        var fileStr = document.getElementById("file").value;
        if((!fileStr || fileStr == "") && (!titleStr || titleStr == "")) {
            // control for the title input... it must exist
            alert('You should enter title for your content');
            return false;
        }
        
        var i;
        var result = JSON.parse("{}");
        for(i=0; i<=counter; i++) {
            if (document.getElementById("textDiv" + i)) {
                var field = jQuery.trim(document.getElementsByName("fieldText"+i)[0].value);
                var value = jQuery.trim(document.getElementsByName("valueText"+i)[0].value);
                if(!field || !value) {
                    continue;
                }
                if(result[field] == null) {
                    result[field] = new Array();
                }
                var values = value.split(",");
                for(j=0; j<values.length; j++) {
                    result[field].push(jQuery.trim(values[j]));
                }
            }
        }
    
        var constraints = document.getElementsByName('constraints');
        var title = document.getElementsByName('title');
        for (var i in constraints) {
            constraints[i].value = JSON.stringify(result);
            title[i].value =  document.getElementById('valueTitle').value;
        }
        return true;
    }
  
    function addConstraint() {
        var newCons = document.createElement('div');
        newCons.setAttribute('id','textDiv' + counter);
        var fieldName = "fieldText"+counter;
        var valueName = "valueText"+counter;
        var url = "javascript:removeConstraint(" + counter + ");";
    
        newCons.innerHTML = "<br/><input type='text' name=" + fieldName + " />" 
                 + " : "
                 + "<input type='text' name=" + valueName + " />"
                 + "  <img src='${it.staticRootUrl}/contenthub/images/delete_icon_16.png' title='Remove' onClick=" + url + " />";
           
        document.getElementById("constraintsDiv").appendChild(newCons);
        document.getElementsByName(fieldName)[0].focus();
        counter++;
    }
  
    function removeConstraint(divNo) {
        var constraintsDiv = document.getElementById('constraintsDiv');
        constraintsDiv.removeChild(document.getElementById('textDiv'+divNo));
    }

    function startEditing(set) {
        if(set) {
            $("#contentForm").attr("action", "/contenthub/${it.indexName}/store/update");
            $("#contentSubmit").attr("value", "Update text");
            $("#urlForm").attr("action", "/contenthub/${it.indexName}/store/update");
            $("#urlSubmit").attr("value", "Update URL");
            $("#fileForm").attr("action", "/contenthub/${it.indexName}/store/update");
            $("#fileSubmit").attr("value", "Update file");
        }
        else {
            $("#contentForm").attr("action", "");
            $("#contentSubmit").attr("value", "Submit text");
            $("#urlForm").attr("action", "");
            $("#urlSubmit").attr("value", "Submit URL");
            $("#fileForm").attr("action", "");
            $("#fileSubmit").attr("value", "Submit file");
        }
    }
  
    function cancelEditing() {
        var uris = document.getElementsByName('uri');
        for (var i in uris) {
            uris[i].value = "";
        }
        document.getElementById("editingDiv").innerHTML = "";
        startEditing(false);
    }

    function editContentItem(vlocalid, vtitle) {
        var lurl = "${it.publicBaseUri}contenthub/${it.indexName}/store/edit/" + vlocalid;
        document.getElementById("constraintsDiv").innerHTML = "";
        counter=0;
        $.ajax({
            url: lurl,
            type: "GET",
            async: true,
            cache: false,
            success: function(jsonCons) {
                var contentItem = JSON.parse(jsonCons);
                if(contentItem != null) {
                    startEditing(true);
                    //fill the text content item related components in the user interface
                    // TODO: use more mimeType
                    if(contentItem["mimeType"] == "text/plain") {
                        document.getElementById("contentTextArea").value = contentItem["content"];
                    } else {
                        document.getElementById("contentTextArea").value = "";
                    }
                    var uris = document.getElementsByName('uri');
                    for (var i in uris) {
                        uris[i].value = contentItem["uri"];
                    }
                    document.getElementById("editingDiv").innerHTML =   '<img src="${it.staticRootUrl}/contenthub/images/delete_icon_16.png" title="Cancel Editing" onClick="javascript:cancelEditing()" />'
                                        + " You are editing Content Item with title: <b>" + contentItem["title"] + "</b>";
                    document.getElementById("valueTitle").value = contentItem["title"];
              
                    //delete already consumed values from json representation so that they will not be added to the constraints
                    delete contentItem["content"];
                    delete contentItem["uri"];
                    delete contentItem["mimeType"];
                    delete contentItem["title"];
              
                    for(var p in contentItem) {
                        if(contentItem.hasOwnProperty(p)) {
                            var fieldHtmlName = getHtmlName(p.toString());
                            addConstraint();
                            var createdConstraintIndex = counter - 1;
                            document.getElementsByName("fieldText"+createdConstraintIndex)[0].value = fieldHtmlName;
                            document.getElementsByName("valueText"+createdConstraintIndex)[0].value = contentItem[p].substring(1, contentItem[p].length-1);
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
        if(vlocalid == document.getElementById("uriContent").value) {
            cancelEditing();
        }
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
