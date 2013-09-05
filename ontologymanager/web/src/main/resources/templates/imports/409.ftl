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
<@common.page title="Ontology Manager : conflict detected" hasrestapi=false> 
	
    <div class="panel">
      <#assign ontology = it.representedOntologyKey>
      An ontology with ID
      <ul><li>
        <a href="${it.publicBaseUri}ontonet/${it.stringForm(ontology)}">${ontology}</a>
      </li></ul>
      is already stored in Stanbol.
      
      <p>
        <u>Note</u>: the ID of the submitted ontology was guessed over a 
        limited number of triples. If you know this is not the full 
        ontology ID, or wish to overwrite the stored ontology anyhow, 
        please try again by setting POST parameter <tt>force=true</tt>.
      </p>
      <p><i>HTTP Status : 409 Conflict</i></p>
      
    </div>
    
  </@common.page>
</#escape>