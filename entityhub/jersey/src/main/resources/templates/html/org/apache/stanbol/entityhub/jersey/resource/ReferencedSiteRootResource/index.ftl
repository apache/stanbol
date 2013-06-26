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
<#import "site_common.ftl" as common>
<#escape x as x?html>
<@common.page> 

<p>List of subresources:</p>
<ul>
	<li><a href="entity">/entityhub/site/{siteId}/entity</a></li>
	<li><a href="find">/entityhub/site/{siteId}/find</a></li>
	<li><a href="query">/entityhub/site/{siteId}/query</a></li>
    <li><a href="ldpath">/entityhub/site/{siteId}/ldpath</a></li>
    <li><a href="reconcile">/entityhub/site/{siteId}/reconcile</a>:
       Implements the <a href="http://code.google.com/p/google-refine/">
       Google Refine</a> Reconciliation API
    </li>
    
</ul>
<hr>
<#include "inc_metadata.ftl">
<hr>
<#include "inc_entity.ftl">
<hr>
<#include "inc_find.ftl">
<hr>
<#include "inc_query.ftl">
<hr>
<#include "inc_ldpath.ftl">

</@common.page>
</#escape>
