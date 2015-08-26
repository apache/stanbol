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

<#if !it.executionNodes??>
  <p><em>There seams to be a problem with the Enhancement Chain <b>${it.chain.name}</b>. 
   To fix this an Administrator needs to install, configure and enable enhancement 
   chains and -engines by using the <a href="${it.consoleBaseUri}">OSGi console</a>.</em></p>
<#elseif it.executionNodes?size == 0>
  <p><em>There is no active engines for Enhancement Chain <b>${it.chain.name}</b>. 
   Administrators can install, configure and enable enhancement chains and 
   -engines by using the <a href="${it.consoleBaseUri}">OSGi console</a>.</em></p>
<#else>
  <#assign executionNodes = it.executionNodes>
  <div class="enginelisting">
  <#if it.chainAvailable>
    <div class="collapsed">
  <#else>
    <div>
  </#if>
  <p class="collapseheader">Enhancement Chain: 
    <#if it.chainAvailable>
      <span class="active">
    <#else>
      <span class="inactive">
    </#if>
    <strong>${it.chain.name}</strong></span> 
    <#if it.activeNodes?size &lt; it.executionNodes?size>
      <strong>${it.activeNodes?size}/</strong><#else> all </#if><strong>${it.executionNodes?size}</strong>
    engines available 
      <span style="float: right; margin-right: 25px;">
        &lt; List of <a href="#">Enhancement Chains</a> &gt;
      </span>
    </p>
    <div class="collapsable">
    <ul>
      <#list executionNodes as node>
        <li>
        <#if node.engineActive>
          <span class="active">
        <#elseif node.optional>
          <span class="optional">
        <#else>
          <span class="inactive">
        </#if>
          <b>${node.engineName}</b> 
          <small>(
          <#if node.optional> optional <#else> required </#if>, 
          <#if node.engineActive>
            ${node.engine.class.simpleName})</small></li>
          <#else>
            currently not available)</small>
          </span>
          </li>
        </#if>
      </#list>
    </ul>
    <p class="note">You can enable, disable and deploy new engines using the
      <a href="/system/console/components">OSGi console</a>.</p>
    </div>
    </div>
  </div>
  
<script>
$(".enginelisting p").click(function () {
  $(this).parents("div").toggleClass("collapsed");
})
.find("a").click(function(e){
    e.stopPropagation();
    //link to all active Enhancement Chains
    window.location = "${it.publicBaseUri}enhancer/chain";
    return false;
});     
</script>
</#if>
<#if it.chainAvailable>
  <p>Paste some text below and submit the form to let the Enhancement Chain ${it.chain.name} enhance it:</p>
  <form id="enginesInput" method="POST" accept-charset="utf-8">
    <p><textarea rows="15" name="content"></textarea></p>
    <p class="submitButtons">Output format:
      <select name="format">
        <option value="application/ld+json">JSON-LD</option>
        <option value="application/rdf+xml">RDF/XML</option>
        <option value="application/rdf+json">RDF/JSON</option>
        <option value="text/turtle">Turtle</option>
        <option value="text/rdf+nt">N-TRIPLES</option>
      </select> <input class="submit" type="submit" value="Run engines">
    </p>
  </form>
<script language="javascript">
function registerFormHandler() {
   $("#enginesInput input.submit", this).click(function(e) {
     // disable regular form click
     e.preventDefault();
     
     var data = {
       content: $("#enginesInput textarea[name=content]").val(),
       ajax: true,
       format:  $("#enginesInput select[name=format]").val()
     };
     var base = window.location.href.replace(/\/$/, "");
     
     $("#enginesOuputWaiter").show();
     
     // submit the form query using Ajax
     $.ajax({
       type: "POST",
       url: base,
       data: data,
       dataType: "html",
       cache: false,
       success: function(result) {
         $("#enginesOuputWaiter").hide();
         $("#enginesOuput").html(result);
       },
       error: function(result) {
         $("#enginesOuputWaiter").hide();
         $("#enginesOuput").text('Invalid query.');
       }
     });
   });
 }
 $(document).ready(registerFormHandler);
</script>
  <div id="enginesOuputWaiter" style="display: none">
    <p>Stanbol is analysing your content...</p>
    <p><img alt="Waiting..." src="${it.staticRootUrl}/home/images/ajax-loader.gif" /></p>
  </div>
  <p id="enginesOuput"></p>
</#if>
