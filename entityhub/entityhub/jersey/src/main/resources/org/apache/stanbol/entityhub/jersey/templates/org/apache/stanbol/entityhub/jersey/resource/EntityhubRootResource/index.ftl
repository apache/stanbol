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
<@common.page title="Entityhub" hasrestapi=true> 

<!-- <div class="panel" id="webview">
<p>This is the start page of the entity hub.</p>

</div> -->

<!-- div class="panel" id="restapi" style="display: none;" -->
  <h3>Service Endpoints</h3>

  <p>The Entityhub provide two main service. First it allows to manage a network
  of site used to consume Entity Information from and second it allows to manage
  locally used Entities.</p>
  <p>The RESTful API of the Entityhub ist structured as follows.</p>

  <h4>Entity Network (<code>"/entityhub/site*"</code>):</h4>

  <ul>
    <li>Referenced Site Manager @ <a href="${it.publicBaseUri}entityhub/sites">/entityhub/sites</a>:
    Manages the network of Referenced Sites and allows to retrieve and search
    Entities in all Sites of the Entity Network. </li>
    <li>Referenced Site @ <code>/entityhub/site/{siteName}</code>: A single
    Site of the Referenced Site Manager allows to retrieve all active Sites.
    Referenced Sites provide the same Interface as the
    Referenced Site Manager.<br>
    Currently active Referenced Sites:<ul id="referencedSiteList">
    </ul>
    <script>
        $.get("${it.publicBaseUri}entityhub/sites/referenced", function(data){
            var res = "";
            for(i=0; i<data.length; i++){
                res += "<li><a href='" + data[i] + "'>" + data[i] + "</a></li>";
            }
            $("#referencedSiteList").html(res);
        });      
    </script>
    </li>
  </ul>

  <h4>Entityhub (<code>"/entityhub"</code>):</h4>

  <ul>
    <li>Local Entities @<a href="${it.publicBaseUri}entityhub/entity">/entityhub/entity</a>:
      Full CRUD operations on Entities managed by the Entityhub
    </li>
    <li>Entity Mappings @ <a href="${it.publicBaseUri}entityhub/mapping">/entityhub/mapping</a>:
      Lookup mappings from local Entities to Entities managed by a
      <a href="${it.publicBaseUri}entityhub/sites">Referenced Site</a>
    </li>
    <li>Local Search @<a href="${it.publicBaseUri}entityhub/find">/entityhub/find</a>:
        Find locally managed Entities by label based search.
    </li>
    <li>Local Query @<a href="${it.publicBaseUri}entityhub/query">/entityhub/query</a>:
        Find locally managed Entities by parsing queries
    </li>
    <li>Entity Lookup @<a href="${it.publicBaseUri}entityhub/lookup">/entityhub/lookup</a>:
       Lookup Entities by id. This supports also to lookup Entities managed by
       <a href="${it.publicBaseUri}entityhub/sites">Referenced Sites</a> and
       the import of found Entities to the Entityhub.
    </li>
    <li>LDPath @<a href="${it.publicBaseUri}entityhub/ldpath">/entityhub/ldpath</a>:
       Allows to execute LDPath programs on locally managed Entities.
    </li>
    <li>Reconciliation @<a href="${it.publicBaseUri}entityhub/reconcile">/entityhub/reconcile</a>:
       Implements the <a href="http://code.google.com/p/google-refine/">
       Google Refine</a> Reconciliation API
    </li>
  </ul>
  <hr>
  <#include "inc_entity.ftl">
  <hr>
  <#include "inc_lookup.ftl">
  <hr>
  <#include "inc_find.ftl">
  <hr>
  <#include "inc_query.ftl">
  <hr>
  <#include "inc_ldpath.ftl">
<!-- /div -->

</@common.page>
</#escape>
