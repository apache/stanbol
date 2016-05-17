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
<#macro form>
<fieldset>
  <legend>Registered Graphs</legend>
  <#-- graph list -->
  <#if it.graphList?size &gt; 0>
    <select id="graphList" onChange='javascript:graphChangeHandler();'>
    	<#list it.graphList as tcInfo>
    		<option value="${tcInfo.graphUri}">${tcInfo.graphUri}</option>
    	</#list>
    </select>
  <#else>
    There is no registered Graph.
  </#if>
</fieldset>

<#if it.graphList?size &gt; 0>
  <fieldset>
    <legend>Details of Selected Graph</legend>
    <#list it.graphList as tcInfo>
    	<ul id="${tcInfo.graphUri}" class="graphDetailInvisible">
    		<li>Graph Name: ${tcInfo.graphName}</li>
    		<li>Graph Description: ${tcInfo.graphDescription}</li>
    	</ul>
    </#list>
  </fieldset>
</#if>

<form id="sparql">
<textarea class="query" rows="11" name="query">
PREFIX fise: &lt;http://fise.iks-project.eu/ontology/&gt;
PREFIX dc:   &lt;http://purl.org/dc/terms/&gt;
SELECT distinct ?enhancement ?content ?engine ?extraction_time
WHERE {
  ?enhancement a fise:Enhancement .
  ?enhancement fise:extracted-from ?content .
  ?enhancement dc:creator ?engine .
  ?enhancement dc:created ?extraction_time .
}
ORDER BY DESC(?extraction_time) LIMIT 5
</textarea>
<p><input type="submit" class="submit" value="Run SPARQL query" /></p>
<pre class="prettyprint result" style="max-height: 200px; display: none" disabled="disabled">
</pre>
</form>
<script language="javascript">
function init() {
  graphChangeHandler();

  $("#sparql input.submit", this).click(function(e) {
    // disable regular form click
    e.preventDefault();
    
    // clean the result area
    $("#sparql textarea.result").text('');
   
    // submit sparql query using Ajax
    $.ajax({
      type: "POST",
      url: "${it.publicBaseUri}sparql",
      data: {graphuri: $("#graphList").val(), query: $("#sparql textarea.query").val()},
      dataType: "html",
      cache: false,
      success: function(result) {
        $("#sparql pre.result").text(result).css("display", "block");
        prettyPrint();
      },
      error: function(result) {
        $("#sparql pre.result").text('Invalid query.').css("display", "block");
      }
    });
  });
  
  // set graph combo box change handler
  $('#graphList').onchange(graphChangeHandler);
}
 
function graphChangeHandler() {
  var selectedGraph = $("#graphList").val();
  if(selectedGraph != undefined) {
    $(".graphDetailInvisible").hide();
    $("#" + selectedGraph).toggle();
  }
}
$(document).ready(init);
</script>
</#macro>