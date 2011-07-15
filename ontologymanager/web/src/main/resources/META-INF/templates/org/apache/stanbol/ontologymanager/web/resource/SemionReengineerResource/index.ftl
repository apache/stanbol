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
<@common.page title="KReS Semion Reengineer">

  <p>The Semion Reengineer helps users to transform structured non-RDF data sources into RDF ones.
  Currently supported data sources are:</p>
  <ul class="kressList">
  <li> Relational Databases
  <li> Document
  </ul>
  <p>Next developing will support:</p>
  <ul class="kressList">
  <li> RSS
  <li> iCalendar
  </ul>
	
<p>Tranform a data source</p>
<select name="data-source-type" onchange="showReengineer(this.selectedIndex)">
<option value="empty">
<option value="rdb">Relational DB
<option value="xml">XML
</select><br/>

<div id="data-source-form">
</div>

</@common.page>
</#escape>
