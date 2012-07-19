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
  <@common.page title="Apache Stanbol OntoNet scope manager" hasrestapi=true>
	
	<a href="${it.publicBaseUri}ontonet/ontology">Scope Manager</a>
	
    <div class="panel" id="webview">
      <#assign scopes = it.scopes>
      <p>This is the start page of the ontology scope manager.</p>

      <fieldset>
        <legend>New Scope</legend>
        <p>
          <b>ID:</b> <input type="text" name="sid" id="sid" size="40" value=""/> 
          <input type="submit" value="Create" onClick="javascript:createScope(document.getElementById('sid').value)"/>
        </p>
      </fieldset>
      
      <h3>Registered Scopes</h3>      
      <div class="storeContents" id="allScopes">
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
		    <#list it.scopes as scope>
		      <tr>
			    <td>
			    <#--
                  <img src="${it.staticRootUrl}/ontonet/images/edit_icon_16.png" title="(not available yet) Edit this item" />
                -->
                  <img src="${it.staticRootUrl}/ontonet/images/delete_icon_16.png" title="Delete this item" onClick="javascript:deleteScope('${scope.ID}')"/>
                </td>
                <td><a href="${it.publicBaseUri}ontonet/ontology/${scope.ID}" title="${scope.ID}">${scope.ID}</a></td>
                <td>${scope.locked?string("locked", "modifiable")}</td>
                <td></td>
                <td>${scope.coreSpace.listManagedOntologies()?size + scope.customSpace.listManagedOntologies()?size}</td>
		      </tr>
		    </#list>
		  </div>
	    </table>
        </div>
      </div> <!-- allScopes -->
      
    </div> <!-- web view -->
    
    <div class="panel" id="restapi" style="display: none;">

    <h3>Service Endpoints</h3>
    <#include "/imports/inc_scopemgr.ftl">
    <#include "/imports/inc_scope.ftl">

    </div>

  <!-- Scripts -->
  <script language="JavaScript">
  
    // On document load
    $(function(){
    
      // Set button disabled
      $("input[type=submit]").attr("disabled", "disabled");
 
      // Append a change event listener to you inputs
      $('input').change(function() {     
            // Validate your form here:
            var validated = true;
            if(isBlank($('#sid').val())) validated = false;
 
            //I f form is validated enable form
            if(validated) $("input[type=submit]").removeAttr("disabled");
            else $("input[type=submit]").attr("disabled", "disabled");                           
      });
 
      // Trigger change function once to check if the form is validated on page load
      $('input:first').trigger('change');
    });
    
    function deleteScope(sid) {
      var lurl = "${it.publicBaseUri}ontonet/ontology/" + sid;
      $.ajax({
        url: lurl,
        type: "DELETE",
        async: true,
        cache: false,
        success: function(data, textStatus, xhr) {
          $("#allScopes").load("${it.publicBaseUri}ontonet/ontology #allScopes>div");
        },
        error: function() {
          alert(result.status + ' ' + result.statusText);
        }
      });
    }

    function createScope(sid) {
      var lurl = "${it.publicBaseUri}ontonet/ontology/" + sid;
      $.ajax({
        url: lurl,
        type: "PUT",
        async: true,
        cache: false,
        success: function(data, textStatus, xhr) {
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
          switch(xhr.status) {
          case 201:
            var loc = xhr.getResponseHeader("Location");
            window.location.href = loc;
            break;
          }
        }
      });
    }
    
    function isBlank(str) {
      return (!str || /^\s*$/.test(str));
    }
    
  </script>

  </@common.page>
</#escape>