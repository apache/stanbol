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
<#import "/imports/ontonetDescription.ftl" as ontonetDescription>

<#escape x as x?html>
  <@common.page title="Apache Stanbol OntoNet session manager" hasrestapi=false>
		
    <div class="panel" id="webview">
      <p>This is the start page of the ontology session manager.</p>
    </div>
    
    <hr>
    <#include "/imports/inc_sessionmgr.ftl">

  </@common.page>
</#escape>