<#macro page title>
<html>
  <head>

    <title>KReS Platform - ${title?html}</title>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8" />

    <link rel="stylesheet" href="/kres/static/style/kres.css" title="KReS Style" media="screen,projection" /> 
 
 	<link rel="icon" type="image/png" href="/kres/static/images/kresLogoExtended.png" />
 	
    <script type="text/javascript" src="/kres/static/scripts/jquery-1.4.2.js"></script>
    <script type="text/javascript" src="/kres/static/scripts/kres.js"></script>
    <script type="text/javascript" src="/kres/static/scripts/jquery.rdfquery.core-1.0.js"></script>
    <script type="text/javascript" src="/kres/static/scripts/tinybox.js"></script>
    <script type="text/javascript" src="/kres/static/scripts/jit-yc.js"></script>

  </head>
  <body> <div id="wrap">
  		<div id="header">
  			<div id="leftHeader">
  				<h1><a href="/kres"><img alt="KReS Logo" src="/kres/static/images/kresLogoExtended.png" width="110" height="112"></a></h1>
  			</div>
  			<div id="rightHeader">
  				<p id="slogan">The Knowledge Representation and Reasoning System!</p>
  			</div> 
			 
		</div>  
		
		<#if it?exists && it.mainMenuItems?exists>
		<div id="sitemenu"> 
			<h2 class="hide">KReS menu:</h2> 
			<ul> 
				<#list it.mainMenuItems as item>
					<li><a class="${item.cssClass}" href="${item.link}">${item.label}</a>
					<#if item.subMenu?exists>
						<div id="${item.id}" class="hide">
							<ul>
							<#list item.subMenu as subItem>
								<a class="${subItem.cssClass}" href="${subItem.link}">${subItem.label}</a></li>
							</#list>
							</ul>	
						</div>
					</#if>
					</li>
				</#list> 
			</ul> 
			<h2><a href="http://stlab.istc.cnr.it/stlab/The_Semantic_Technology_Laboratory_(STLab)"><img src="/kres/static/images/stlabLogo.jpg" width="80" height="40"></a></h2>
		</div> 
		</#if>
		
	    <div id="content">
	      <h2>${title?html}</h2>
	      <#nested>
	    </div>
	
		<div id="footer">
		
		  <a href="http://www.w3.org/standards/semanticweb/"><img class="swcube"
		    src="/kres/static/images/sw-cube.png"/></a>
	      <a href="http://www.iks-project.eu"><img
	        height="60px" alt="IKS Project" src="/kres/static/images/iks_project_logo.jpg" /></a>
	      <p><em>The research leading to these results has received funding from the European Community's
	          Seventh Framework Programme (FP7/2007-2013) under grant agreement n° 231527</em></p>
	   </div>
	</div>
  </body>
</html>
</#macro>
