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
<#macro page title hasrestapi>
<html>
  <head>

    <title>${title?html} - Apache Stanbol</title>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8" />

    <!-- to be moved in the dedicated fragment -->
    <link rel="stylesheet" href="${it.staticRootUrl}/home/style/stanbol.css" />
    <link rel="icon" type="image/png" href="${it.staticRootUrl}/home/images/favicon.png" />

    <#list it.registeredLinkResources as link>
    <link rel="${link.rel}" href="${it.staticRootUrl}/${link.fragmentName}/${link.relativePath}" />
    </#list>

    <#list it.registeredScriptResources as script>
    <script type="${script.type}" src="${it.staticRootUrl}/${script.fragmentName}/${script.relativePath}"></script>
    </#list>

  </head>

  <body>
    <div class="home"><a href="${it.rootUrl}"><img src="${it.staticRootUrl}/home/images/apache_stanbol_logo_cropped.png" alt="Stanbol Home" /></a></div>
    <div class="header">
      <h1>The RESTful Semantic Engine</h1>

      <#if it?exists && it.mainMenuItems?exists>
      <div class="mainNavigationMenu">
      <ul>
        <#list it.mainMenuItems as item>
        <li class="${item.cssClass}"><a href="${it.publicBaseUri}${item.link}">${item.label}</a></li>
        </#list>
      </ul>
      </div>
      </#if>
      <div style="clear: both"></div>
    </div>

    <div class="content">
      <h2>${title?html}</h2>
      <#if hasrestapi>
      <div class="restapitabs">
      <ul>
        <li id="tab-webview" class="selected"><a href="#">Web View</a></li>
        <li id="tab-restapi" ><a href="#">REST API</a></li>
      <ul>
      </div>
<script>
$(".restapitabs a").click(function () {
  $(this).parents("ul").find("li").removeClass("selected");
  $(this).parents("li").addClass("selected");
  $(".panel").hide();
  var panelId = $(this).parents("li")[0].id.split("-")[1];
  $("#" + panelId).fadeIn();
});
</script>
      </#if>
      <div style="clear: both"></div>
      <#nested>
    </div>

    <div class="footer">

    <div class="column">
      <a href="http://www.w3.org/standards/semanticweb/"><img class="swcube"
        src="${it.staticRootUrl}/home/images/sw-cube.png"/></a>
      <a href="http://www.iks-project.eu"><img
        height="60px" alt="IKS Project" src="${it.staticRootUrl}/home/images/iks_project_logo.jpg" /></a>
    </div>
    <div class="column right">
      <em>The research leading to these results has received funding from the European Community's
          Seventh Framework Programme (FP7/2007-2013) under grant agreement n° 231527</em>
    </div>
    <div style="clear: both"></div>
    </div>
  </body>
</html>
</#macro>
