<#macro page title hasrestapi>
<html>
  <head>
	<script type="text/javascript">
		document.write("<base href=\"");
		var whref =window.location.href; 
		var base =  whref.substring(0, whref.indexOf("ontologies"));
		document.write(base);
		document.write("\"/>");
	</script>
    <title>FISE - ${title?html}</title>
    
    <meta http-equiv="Content-type" content="text/html; charset=utf-8" />

    <link rel="stylesheet" href="static/style/persistencestore.css" /> 
    <link rel="stylesheet" href="static/scripts/prettify/prettify.css" />
    <link rel="icon" type="image/png" href="static/images/favicon.png" />

    <script type="text/javascript" src="static/scripts/prettify/prettify.js"></script>
    <script type="text/javascript" src="static/scripts/jquery-1.4.2.js"></script>
    <script type="text/javascript" src="static/scripts/jit.js"></script>
    <script type="text/javascript" src="static/scripts/paging.js"></script>
    <script type="text/javascript" src="static/scripts/requestResponse.js"></script>
	
  </head>
  <body>
    <div class="home"><a href="/ontologies"><img src="static/images/fise_logo_cropped.png" alt="FISE Home" /></a></div>
    <div class="header">
      <h1>The RESTful Persistence Store</h1>
		<div class="mainNavigationMenu">
			<ul>
				<li><a href="ontologies">/ontologies</a></li>
				<#if it?exists && it.metadata?exists && it.metadata.ontologyMetaInformation?exists>
	           		<li ><a href="${it.metadata.ontologyMetaInformation.href}">ontology</a></li>
      			</#if>
	      	</ul>
	     </div>
      <div style="clear: both"></div>
    </div>

    <div class="content">
      <h2>${title?html}</h2>
   <#if hasrestapi>
	  <div class="restapitabs">
	  <ul>
	    <li id="tab-webview" class="selected"><a href="javascript:">Web View</a></li>
	    <li id="tab-restapi" ><a href="javascript:">REST API</a></li>
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
	    src="static/images/sw-cube.png"/></a>
      <a href="http://www.iks-project.eu"><img
        height="60px" alt="IKS Project" src="static/images/iks_project_logo.jpg" /></a>
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
