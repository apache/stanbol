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
        <span class="active">
      <#elseif it.chainExecution.failed>
        <span class="inactive"">
      <#else>
         <span>
      </#if>
      <strong>${it.chainExecution.chainName}</strong> 
      ${it.getExecutionStatusText(it.chainExecution)} </span>
      in <strong>${it.getExecutionDurationText(it.chainExecution)}</strong>.
    </p>
    <div class="collapsable">
    <ul>
      <#list it.engineExecutions as node>
        <li><#if node.offsetText??>${it.getExecutionOffsetText(node)}<#else>${it.getExecutionStartTime(node)}</#if>:
        <#if node.completed>
          <span class="active">
        <#elseif node.failed && node.executionNode.optional>
          <span class="optional">
        <#elseif node.failed && !node.executionNode.optional>
          <span class="inactive">
        <#else>
           <span>
        </#if>
          <b>${it.getExecutionStatusText(node)}</b></span> 
          in ${it.getExecutionDurationText(node)} :
          <b>${node.executionNode.engineName}</b>
          <small>(
          <#if node.executionNode.optional> optional <#else> required </#if>, 
          start: ${it.getExecutionStartTime(node)}, completion: ${it.getExecutionCompletionTime(node)})
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
