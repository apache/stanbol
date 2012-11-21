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
  <@common.page title="${it.session.ID} : an Apache Stanbol OntoNet session" hasrestapi=false>
	
	<a href="${it.publicBaseUri}ontonet/session">Session Manager</a> &gt; Session <tt>${it.session.ID}</tt>
	
    <div class="panel" id="webview">
  
  <br/>
  <!-- FIXME class names should be generic, and not bound to a specific functionality (here engines->reasoning services)-->
  <div class="enginelisting">
    <div class="collapsed">
      <p class="collapseheader"><b>Load an ontology</b></p>
      <div class="collapsable">
      <br/>

    <form method="POST" enctype="multipart/form-data" accept-charset="utf-8">
    <fieldset>
      <legend>Manage stored ontology</legend>
      <p><b>Ontology ID:</b> 
        <select name="stored">  
        <option value="null">&lt;please select an ontology&gt;</option>
        <#list it.manageableOntologies as manageable>
        <option value="${manageable}">${manageable}</option>
        </#list>
        </select>
        <input type="submit" value="Manage"/>
      </p>
    </fieldset>
    </form>
      
    <form id="loadfromfile" method="POST" enctype="multipart/form-data" accept-charset="utf-8">
    <fieldset>
      <legend>From a local file</legend>
      <p><b>File:</b> <input type="file" name="file"/> 
        Input format:
        <select name="format">
          <option value="auto">Auto</option>
          <option value="application/rdf+xml">RDF/XML</option>
          <option value="application/rdf+json">RDF/JSON</option>
          <option value="text/turtle">Turtle</option>
          <option value="text/rdf+nt">N-TRIPLE</option>
          <option value="text/rdf+n3">N3</option>
          <!--
          <option value="application/owl+xml">OWL/XML</option>
          <option value="text/owl-manchester">Manchester OWL</option>
          <option value="text/owl-functional">OWL Functional</option>
          -->
        </select>
        <input type="submit" value="Send"/>
      </p>
    </fieldset>
    </form>
  
    <form method="POST" enctype="multipart/form-data" accept-charset="utf-8">
    <fieldset>
      <legend>From a URL</legend>
      <p>
        <b>URL:</b> <input type="text" name="url" size="80" value="http://"/> 
        <input type="submit" value="Fetch"/>
      </p>
    </fieldset>
    </form>
  
  Note: OWL import targets will be included. Ontology loading is set to fail on missing imports.

     </div>
    </div> 
  </div>

  <script>
    $(".collapseheader").click(function () {
      $(this).parents("div").toggleClass("collapsed");
    });    
  </script>
  
  <h3>Managed ontologies</h3>
  <#assign ontologies = it.ontologies>
  <div class="storeContents" id="managed">
    <table id="onts">
      <div>
        <tr>
          <th></th>
          <th>Name</th>
        </tr>
        <#list ontologies as ontology>
          <tr>
            <td><img src="${it.staticRootUrl}/ontonet/images/unload_icon_16.png" title="Unmanage this ontology" onClick="javascript:unload('${ontology}')"/></td>
            <td><a href="${it.publicBaseUri}ontonet/session/${it.session.ID}/${ontology}">${ontology}</a></td>
          </tr>
        </#list>
      </div>
    </table> <!-- onts -->
  </div> <!-- managed -->
  
  <form method="POST" enctype="multipart/form-data" accept-charset="utf-8">
  <input type="hidden" name="sessionid" value="${it.session.ID}"/>
  <h3>Appendable Scopes</h3>
  <div class="storeContents">
    <table id="appSc">
      <div>
        <tr>
          <th width="1%">Appended</th><th>Name</th>
        </tr>
        <#assign scs = it.appendedScopes>
        <#list it.allScopes as sc>
          <tr>
            <td><input type="checkbox" name="scope" value="${sc.ID}"${scs?seq_contains(sc.ID)?string(" checked", "")}/></td>
            <td><a href="/ontonet/ontology/${sc.ID}">${sc.ID}</a></td>
          </tr>
        </#list>
      </div>
    </table> <!-- appSc -->
    <input type="submit" value="Update"${(it.allScopes?size==0)?string(" disabled=\"disabled\"", "")}/>
  </div>
  </form>
  
  </div> <!-- web view -->
  
  <script language="JavaScript">
    
    function unload(ontologyid) {
      var lurl = "${it.publicBaseUri}ontonet/session/${it.session.ID}" + "/" + ontologyid;
      $.ajax({
        url: lurl,
        type: "DELETE",
        async: true,
        cache: false,
        success: function(data, textStatus, xhr) {
          $("#managed").load("${it.publicBaseUri}ontonet/session/${it.session.ID} #managed>table");
        },
        error: function() {
          alert(result.status + ' ' + result.statusText);
        }
      });
    }
    
  </script>

  </@common.page>
</#escape>