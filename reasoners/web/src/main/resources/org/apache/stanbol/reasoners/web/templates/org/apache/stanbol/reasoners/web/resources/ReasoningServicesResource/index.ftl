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
<#import "/imports/reasonersDescription.ftl" as reasonersDescription>
<#escape x as x?html>
<@common.page title="Reasoners" hasrestapi=false> 
		
 <div class="panel" id="webview">
 <#if it.activeServices?size == 0>
   <p><em>There is no reasoning services. Administrators can install and
   configure new reasoning services using the
    <a href="/system/console/components" target="_blank">OSGi console</a>.</em></p>
 <#else>
 <!-- FIXME class names should be generic, and not bound to a specific functionality (here engines->reasoning services)-->
 <div class="enginelisting">
  <div class="collapsed">
  <p class="collapseheader">There are currently
   <strong>${it.activeServices?size}</strong> active services.</p>
   <div class="collapsable">
    <ul>

     <#list it.activeServices as service>
      <li><b>${service.path}</b>:
        <#list service.supportedTasks as task> 
        	<a href="${it.publicBaseUri}${it.currentPath}/${service.path}/${task}" title="${service.class.name} Task: ${task}">${task}</a> |
        </#list>
        <a href="${it.publicBaseUri}${it.currentPath}/${service.path}/check" title="${service.class.name} Task: check">check</a>
      </li>
     </#list>
    </ul>
    
  <p class="note">Administrators can enable, disable and deploy reasoning services using the
    <a href="/system/console/components" target="_blank">OSGi console</a>.</p>
   </div>
   
  </div> 
 </div>

<script>
$(".enginelisting p").click(function () {
  $(this).parents("div").toggleClass("collapsed");
});    
</script>
 </#if>
	</div>

    <!-- We disable this at the moment -->
    <!--div class="panel" id="restapi" style="display: none;">
          
    </div -->

</@common.page>
</#escape>