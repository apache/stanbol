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
<b>Store</b> | <a href="${it.publicBaseUri}contenthub/index">Index</a>
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
            <th>Enhancements</th>
          </tr>
          <#list it.recentlyEnhancedItems as item>
            <tr>
              <td>
                <img src="${it.staticRootUrl}/contenthub/images/delete_icon_16.png" onClick="javascript:deleteContentItem('${item.localId}');" title="Delete this item" />
              </td>
              <td><a href="${item.dereferencableURI}" title="${item.dereferencableURI}"><#if item.title??>${item.title}<#else>${item.localId}</#if></a></td>
              <td>${item.mimetype}</td>
              <td><a href="${it.publicBaseUri}contenthub/store/metadata/${item.localId}">${item.enhancementCount}</a></td>
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
      <legend>Submit raw text content</legend>
      <p><textarea rows="15" id="contentTextArea" name="content"></textarea></p>
      <p><input type="submit" id="contentSubmit" value="Submit text" /></p>
    </fieldset>
  </form>

  <form method="POST" id="urlForm" accept-charset="utf-8" onSubmit = "return setConstraints();">
    <fieldset>
      <input type="hidden" id="constraintsURL" name="constraints" value="" />
      <input type="hidden" name="title" value="" />
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
      <legend>Upload a local file</legend>
      <p>
        <input id="file" name="file" type="file"/>
        <input type="submit" id="fileSubmit" value="Submit file" />
      </p>
    </fieldset>
  </form>
  
  <div class="waitingDiv">
    <p>Stanbol is storing your content...</p>
    <p><img alt="Waiting..." src="http://localhost:8080/static/home/images/ajax-loader.gif"></p>
  </div>
</div>

<div class="panel" id="restapi" style="display: none;">
  <#include "/imports/storeRestApi.ftl">
</div>

<script language="javascript">

    var counter = 0;
    
    function setConstraints() { 
        var titleStr = document.getElementById("valueTitle").value;
        var fileStr = document.getElementById("file").value;
        if((!fileStr || fileStr == "") && (!titleStr || titleStr == "")) {
            // control for the title input... it must exist
            alert('You should enter title for your content');
            return false;
        }
        $(".waitingDiv").show();
        
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
  
    function deleteContentItem(vlocalid) {
        var lurl = "${it.publicBaseUri}contenthub/store/" + vlocalid;
        $.ajax({
            url: lurl,
            type: "DELETE",
            async: true,
            cache: false,
            success: function() {
                $("#storeContents").load("${it.publicBaseUri}contenthub/store #storeContents>div");
            },
            error: function(result) {
                alert(result.status + ' ' + result.statusText);
            }
        });
    }
  
</script>
</@common.page>
</#escape>
