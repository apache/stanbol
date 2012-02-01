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
<#macro view>

<h3>Enhancement Process Metadata</h3>
<div id="executionmetadata" class="enginelisting">
  <script>
    $("#executionmetadata p").click(function () {
      $(this).parents("div").toggleClass("collapsed");
    });    
  </script>
  <#if !it.chainExecution??>
    <p> No metadata available <small>
    (This indicates that the used EnhancementJobManager
    does not support this feature)</small><p>
  <#else>
    <#if it.chainExecution.failed>
      <div>
    <#else>
    <div class="collapsed">
    </#if>
    <p class="collapseheader"> Execution of Chain 
      <#if it.chainExecution.completed>
        <span style="color:#006600">
      <#elseif it.chainExecution.failed>
        <span style="color:#660000">
      <#else>
         <span>
      </#if>
      <strong>${it.chainExecution.chainName}</strong> 
      ${it.chainExecution.statusText} </span>
      in <strong>${it.chainExecution.durationText}</strong>.
    </p>
    <div class="collapsable">
    <ul>
      <#list it.engineExecutions as node>
        <li><#if node.offsetText??>${node.offsetText}<#else>${node.startTime}</#if>:
        <#if node.completed>
          <span style="color:#006600">
        <#elseif node.failed && node.executionNode.optional>
          <span style="color:#666666">
        <#elseif node.failed && !node.executionNode.optional>
          <span style="color:#660000">
        <#else>
           <span>
        </#if>
          <b>${node.statusText}</b></span> 
          in ${node.durationText} :
          <b>${node.executionNode.engineName}</b>
          <small>(
          <#if node.executionNode.optional> optional <#else> required </#if>, 
          start: ${node.startTime}, completion: ${node.completionTime})
          </small>
          </span>
          </li>
      </#list>
    </ul>
    </div>
  </div>
  </#if>
</div>


</#macro>
