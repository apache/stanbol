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
		
    <div class="panel" id="webview">
      <#assign sessions = it.sessions>
      <p>This is the start page of the Session Manager.</p>
      
      <div class="storeContents">
      	<table id="allSessions">
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
                  <img src="${it.staticRootUrl}/contenthub/images/edit_icon_16.png" title="(not available yet) Edit this item" />
                  <img src="${it.staticRootUrl}/contenthub/images/delete_icon_16.png" title="(not available yet) Delete this item" />
                -->
                </td>
                <td><a href="${it.publicBaseUri}ontonet/session/${session.ID}" title="${session.ID}">${session.ID}</a></td>
                <td>${session.locked?string("locked", "modifiable")}</td>
                <td></td>
                <td>${session.listManagedOntologies()?size}</td>
		      </tr>
		    </#list>
		  </div>
	    </table> <!-- allSessions -->
      </div>
    </div> <!-- webview -->
    
    <div class="panel" id="restapi" style="display: none;">
      <h3>Service Endpoints</h3>
      <#include "/imports/inc_sessionmgr.ftl">
    </div> <!-- restapi -->

  </@common.page>
</#escape>