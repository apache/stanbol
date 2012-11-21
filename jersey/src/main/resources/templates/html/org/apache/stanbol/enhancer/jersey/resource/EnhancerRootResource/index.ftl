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
<@common.page title="Apache Stanbol Enhancer" hasrestapi=true>


<div class="panel" id="webview">
  <#include "/imports/enhancerweb.ftl">
</div>

<div class="panel" id="restapi" style="display: none;">

    <#-- 1. Documentation of the normal RESTful API -->  
    <#include "/imports/doc/enhancerbase.ftl">
  
    <#-- 2. Documentation of Enhancer Configuration RESTful API -->  
    <h3>Enhancer Configuration</h3>
    
    <p> GET requests with an accept header of any supported RDF serialisation 
    such as
    <code><pre>
        curl -H "Accept: application/rdf+xml" ${it.publicBaseUri}enhancer
    </pre></code>
    will return information about the available enhancement chains and engines.</p>
    <h4>Example:</h4>
    <p><a href="#" onclick="getChainConfig(); return false;">clicking here</a> to
    get the metadata for the currently active enhancement chains and engines</p>
    <script language="javascript">
      function getChainConfig() {
         var base = window.location.href.replace(/\/$/, "");
         
         $("#enhancerConfigResult").show();     
         
         // submit the form query using Ajax
         $.ajax({
           type: "GET",
           url: base,
           data: "",
           dataType: "text",
           beforeSend: function(req) {
             req.setRequestHeader("Accept", "application/rdf+xml");
           },
           cache: false,
           success: function(result) {
             $("#enhancerConfiguration").text(result);
           },
           error: function(result) {
             $("#enhancerConfiguration").text('Error while loading chain config.');
           }
         });
       }
    </script>
    <div id="enhancerConfigResult" style="display: none">
    <p><a href="#" onclick="$('#enhancerConfigResult').hide(); return false;">Hide results</a>
    <pre id="enhancerConfiguration">... waiting for results ...</pre>
    </div>
    
    <p>In addition there is aleo a <a href="${it.publicBaseUri}enhancer/sparql">
    SPARQL Endpoint</a> that allows to query the configuration</p>

    <#-- 3. Documentation of the multipart ContentItem RESTful API -->  
    <#include "/imports/doc/multipartcontentitem.ftl">
    
    <#-- 5. Documentation of the ExecutionPlan RESTful API -->  
    <#include "/imports/doc/executionplan.ftl">
    
</div>


</@common.page>
</#escape>
