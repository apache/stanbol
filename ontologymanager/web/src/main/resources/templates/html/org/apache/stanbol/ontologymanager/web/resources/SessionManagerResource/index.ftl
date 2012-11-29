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
<#import "/imports/ontonetDescription.ftl" as ontonetDescription>

<#escape x as x?html>
  <@common.page title="Apache Stanbol OntoNet session manager" hasrestapi=true>
	
	<a href="${it.publicBaseUri}ontonet/session">Session Manager</a>
	
    <div class="panel" id="webview">
      <#assign sessions = it.sessions>
      <p>This is the start page of the Session Manager.</p>
      
      <fieldset>
        <legend>New Session</legend>
        <p>
          <b>ID:</b> <input type="text" name="sid" id="sid" size="40" value=""/> 
          <input type="submit" value="Create" onClick="javascript:createSession(document.getElementById('sid').value)"/>
        </p>
      </fieldset>
      
      <h3>Registered Sessions</h3>
      <div class="storeContents" id="allSessions">
        <div>
          <table>
		  <div>
		    <tr>
		  	  <th></th>
		      <th>Name</th>
              <th>Status</th>
		      <th>Comment <#--TODO: fix image path  <img src="${it.staticRootUrl}/contenthub/images/rdf.png" alt="Format: RDF"/> --></th>
			  <th>&#35;Ontologies</th>
		    </tr>
		    <#list sessions as session>
		      <tr>
			    <td>
			    <#--
                  <img src="${it.staticRootUrl}/ontonet/images/edit_icon_16.png" title="(not available yet) Edit this item" />
                -->
                  <img src="${it.staticRootUrl}/ontonet/images/delete_icon_16.png" title="Delete this item" onClick="javascript:deleteSession('${session.ID}')"/>
                </td>
                <td><a href="${it.publicBaseUri}ontonet/session/${session.ID}" title="${session.ID}">${session.ID}</a></td>
                <td>${session.locked?string("locked", "modifiable")}</td>
                <td></td>
                <td>${session.listManagedOntologies()?size}</td>
		      </tr>
		    </#list>
		  </div>
	    </table>
        </div>
      </div> <!-- allSessions -->
      
    </div> <!-- webview -->
    
    <div class="panel" id="restapi" style="display: none;">
      <h3>Service Endpoints</h3>
      <#include "/imports/inc_sessionmgr.ftl">
    </div> <!-- restapi -->

  <script language="JavaScript">

    function createSession(sid) {
      var lurl = "${it.publicBaseUri}ontonet/session" + (isBlank(sid)?"":("/"+sid));
      var method = isBlank(sid)?"POST":"PUT";
      $.ajax({
        url: lurl,
        type: method,
        async: true,
        cache: false,
        success: function(data, textStatus, xhr) {
          // window.location.href = data.redirectToUrl;
          console.log(xhr.status);
          console.log(xhr.getResponseHeader("Location"));
        },
        error: function(xhr, textStatus, errorThrown) {
          switch(xhr.status) {
          case 409:
            alert('Conflict: ID \"' + sid + '\" already taken.');
            break;
          default:
            alert(xhr.status + ' ' + xhr.statusText);
          }
        },
        complete: function(xhr, textStatus) {
          if (xhr.status == 201) {
            var loc = xhr.getResponseHeader("Location");
            window.location.href = loc;
          }
        }
      });
    }
    
    function deleteSession(sid) {
      var lurl = "${it.publicBaseUri}ontonet/session/" + sid;
      $.ajax({
        url: lurl,
        type: "DELETE",
        async: true,
        cache: false,
        success: function(data, textStatus, xhr) {
          $("#allSessions").load("${it.publicBaseUri}ontonet/session #allSessions>div");
        },
        error: function() {
          alert(result.status + ' ' + result.statusText);
        }
      });
    }
    
    function isBlank(str) {
      return (!str || /^\s*$/.test(str));
    }
    
    
  </script>

  </@common.page>
</#escape>