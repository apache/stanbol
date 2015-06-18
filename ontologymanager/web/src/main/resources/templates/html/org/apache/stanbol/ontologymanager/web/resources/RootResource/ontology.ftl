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
<@common.page title="Ontology Manager : Ontology Detail" hasrestapi=false>	

  <div class="panel">
    <div>
      Aliases:
      <ul>
      <#list it.aliases as alias>
        <li><a href=${it.publicBaseUri}ontonet/${alias}>${alias}</a></li>
      </#list>
      </ul>

      Direct dependencies:
      <ul>
      <#list it.dependencies?keys as dep>
        <li><a href=${it.publicBaseUri}ontonet/${dep}>${dep}</a>
        <#if it.dependencies[dep] == "NO_MATCH">
          (not loaded)
        </#if>
        </li>
      </#list>
      </ul>
        
      Handles:
      <ul>
      <#list it.scopeHandles as handle>
        <li>${handle}</li>
      </#list>
      <#list it.sessionHandles as handle>
        <li>Session ${handle}</li>
      </#list>
      </ul>
    </div>
    <#--
    <pre>${it.result}</pre>
    -->
  </div>
  
  <ul class="downloadLinks">
<li>
  Output format:
  <select onChange="javascript:setFormat();" id="selectFormat">
    <option value="application/json">JSON-LD</option>
    <option value="application/owl+xml">OWL/XML</option>
    <option value="application/rdf+xml">RDF/XML</option>
    <option value="application/rdf+json">RDF/JSON</option>
    <option value="text/owl-functional">OWL Functional Syntax</option>
    <option value="text/owl-manchester">Manchester OWL Syntax</option>
    <option value="text/rdf+n3">N3</option>
    <option value="text/rdf+nt">N-Triples</option>
    <option value="text/turtle" selected>Turtle</option>
  </select>
  <a id="downloadOntology" href="${it.requestUri}?header_Accept=text%2Fturtle" class="downloadRDF" download="${it.stringForm(it.representedOntologyKey)}.rdf">Download raw ontology</a>
</li>
</ul>

</@common.page>
</#escape>

<script>
function setFormat(){var format=$("#selectFormat").val();$("#downloadOntology").attr("href","${it.requestUri}?header_Accept="+encodeURIComponent(format));var extn;switch(format){case"application/json":case"application/rdf+json":extn=".json";break;case"application/owl+xml":case"text/owl-functional":case"text/owl-manchester":extn=".owl";break;case"application/rdf+xml":case"text/turtle":extn=".rdf";break;case"text/rdf+n3":extn=".n3";break;case"text/rdf+nt":default:extn=".nt"}$("#downloadOntology").attr("download","${it.stringForm(it.representedOntologyKey)}"+extn)}
</script>