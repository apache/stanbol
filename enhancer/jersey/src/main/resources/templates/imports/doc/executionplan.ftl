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

<h3>Execution Plan</h3>

<p>The Executionpaln formally describes how ContentItems parst to the
Stanbol Enhancer are processes by an Enhancement Chain. Such information are
also included in enhancement results as part of the ExectionMetadata (see
also the <code>executionmetadata=true/false</code> parameter)</p>

<p>Users that need to retrieve the ExecutionPlan used by an enhancement endpoint
can do this by sending a GET request with an accept header of any supported 
RDF serialisation to "{enhancement-endpoint}/ep":</p>

<code><pre>
    curl -H "Accept: application/rdf+xml" ${it.serviceUrl}/ep
</pre></code>

<h4>Example:</h4>
<p><a href="#" onclick="getExecutionPlan(); return false;">clicking here</a> to
get the metadata for the currently active enhancement chains</p>
<script language="javascript">
  function getExecutionPlan() {
     var base = "${it.serviceUrl}/ep";
     
     $("#executionPlanResult").show();     
     
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
         $("#executionPlanData").text(result);
       },
       error: function(result) {
         $("#executionPlanData").text('Error while loading chain config.');
       }
     });
   }
</script>
<div id="executionPlanResult" style="display: none">
<p><a href="#" onclick="$('#executionPlanResult').hide(); return false;">Hide results</a>
<pre id="executionPlanData">... waiting for results ...</pre>
</div>
