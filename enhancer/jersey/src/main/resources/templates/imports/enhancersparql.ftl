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
<form id="sparql" action="${it.rootUrl}enhancer/sparql" method="GET"
 enctype="application/x-www-form-urlencoded"
 accept="application/sparql-results+xml, application/rdf+xml">
<textarea class="query" rows="11" name="query">
PREFIX enhancer: &lt;http://stanbol.apache.org/ontology/enhancer/enhancer#&gt;
PREFIX rdfs:     &lt;http://www.w3.org/2000/01/rdf-schema#&gt;
SELECT distinct ?name ?chain
WHERE {
  ?chain a enhancer:EnhancementChain .
  ?chain rdfs:label ?name .
}
ORDER BY ASC(?name)
</textarea>
<p><input type="submit" class="submit" value="Run SPARQL query" /></p>
<pre class="prettyprint result" style="max-height: 200px; display: none" disabled="disabled">
</pre>
</form>
<script language="javascript">
function registersSparqlHandler() {
   $("#sparql input.submit", this).click(function(e) {
     // disable regular form click
     e.preventDefault();
     
     // clean the result area
     $("#sparql textarea.result").text('');
     
     // submit sparql query using Ajax
     $.ajax({
       type: "POST",
       url: "${it.rootUrl}enhancer/sparql",
       data: {query: $("#sparql textarea.query").val()},
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
 }
 $(document).ready(registersSparqlHandler);
</script>
</#macro>