<#macro page title hasrestapi>
<html>
  <head>

    <title>Apache Stanbol - ${title?html}</title>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8" />

    <link rel="stylesheet" href="/static/style/stanbol.css" />
    <link rel="stylesheet" href="/static/openlayers-2.9/theme/default/style.css" type="text/css" />
    <link rel="stylesheet" href="/static/scripts/prettify/prettify.css" />
    <link rel="icon" type="image/png" href="/static/images/favicon.png" />

    <script type="text/javascript" src="/static/openlayers-2.9/OpenLayers.js"></script>
    <script type="text/javascript" src="/static/scripts/prettify/prettify.js"></script>
    <script type="text/javascript" src="/static/scripts/jquery-1.4.2.js"></script>
    <script type="text/javascript" src="/static/scripts/jit.js"></script>

  </head>

  <body>
    <div class="home"><a href="/"><img src="/static/images/apache_stanbol_logo_cropped.png" alt="Stanbol Home" /></a></div>
    <div class="header">
      <h1>The RESTful Semantic Engine</h1>

      <#if it?exists && it.mainMenuItems?exists>
      <div class="mainNavigationMenu">
      <ul>
        <#list it.mainMenuItems as item>
        <li class="${item.cssClass}"><a href="${item.link}">${item.label}</a></li>
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
        src="/static/images/sw-cube.png"/></a>
      <a href="http://www.iks-project.eu"><img
        height="60px" alt="IKS Project" src="/static/images/iks_project_logo.jpg" /></a>
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
