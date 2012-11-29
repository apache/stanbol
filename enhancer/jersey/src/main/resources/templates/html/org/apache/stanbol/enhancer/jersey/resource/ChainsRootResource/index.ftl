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
<@common.page title="Enhancement Chains" hasrestapi=true>


<div class="panel" id="webview">
<p> Enhancement Chains define a set of Enhancement Engines as well as the 
execution oder of those used to enhance content parsed to the Stanbol Enhancer.<p>
Currently the following Chains are available:
<ul>
  <#list it.chains as chain>
    <#assign name = chain.name >
    <li> <a href="${it.publicBaseUri}enhancer/chain/${name}">${name}</a>
    (<#if it.isDefault(name)><b>default</b>,</#if>
    id: ${it.getServiceId(name)}, ranking: ${it.getServiceRanking(name)},
    impl: ${chain.class.simpleName}
    )<#if it.getServicePid(name)??>: 
    <a href="${it.consoleBaseUri}/configMgr/${it.getServicePid(name)}">configure</a></#if>
  </#list>
</ul>
<p>Enhancement Request for the <a href="${it.publicBaseUri}enhancer">
/enhancer</a> and <a href="${it.publicBaseUri}engines">
/engines</a> endpoints are processed by using the default chain - the engine
in the above list marked as <b>default</b>. </p><p>
The default Chain is defined as (1) the Chain with the name "default" 
and the highest <code>service.ranking</code> or (2) if no Chain has the name 
"default" is active than the Chain with the highest <code>service.ranking</code> 
(regardless of the name).<p>

<p class="note">
You can configure Chains by using the the <a href="${it.consoleBaseUri}/configMgr">
Configuration Tab</a> of the OSGi console.</p>

</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Enhancement Chains Metadata</h3>

<p>This stateless interface allows the caller to query all available
Enhancement Chains</p>
<p> GET requests with an accept header of any supported RDF serialisation 
such as
<code><pre>
    curl -H "Accept: application/rdf+xml" ${it.publicBaseUri}enhancer/chain
</pre></code>
will return information about the available enhancement chains.</p>
<h4>Example:</h4>
<p><a href="#" onclick="getChainConfig(); return false;">clicking here</a> to
get the metadata for the currently active enhancement chains</p>
<script language="javascript">
  function getChainConfig() {
     var base = window.location.href.replace(/\/$/, "");
     
     $("#chainMetadataResult").show();     
     
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
         $("#chainMetadata").text(result);
       },
       error: function(result) {
         $("#chainMetadata").text('Error while loading chain config.');
       }
     });
   }
</script>
<div id="chainMetadataResult" style="display: none">
<p><a href="#" onclick="$('#chainMetadataResult').hide(); return false;">Hide results</a>
<pre id="chainMetadata">... waiting for results ...</pre>
</div>


</div>


</@common.page>
</#escape>
