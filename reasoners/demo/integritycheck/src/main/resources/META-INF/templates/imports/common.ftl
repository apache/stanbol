<#macro page title>
<html>
  <head>

    <title>Integrity Check Demo - ${title?html}</title>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8" />

    <link rel="stylesheet" href="/intcheck/static/style/stanbol.css" title="Stanbol Style" media="screen,projection" /> 
    <link rel="stylesheet" href="/static/enhancer/scripts/prettify/prettify.css" /> 
    <link rel="icon" type="image/png" href="/static/home/images/favicon.png" />
 	
    <script type="text/javascript" src="/intcheck/static/scripts/jquery-1.4.2.js"></script>
    <script type="text/javascript" src="/intcheck/static/scripts/intcheck_demo.js"></script>
    <script type="text/javascript" src="/intcheck/static/scripts/jquery.rdfquery.core-1.0.js"></script>
    <script type="text/javascript" src="/intcheck/static/scripts/tinybox.js"></script>
    <script type="text/javascript" src="/intcheck/static/scripts/jit-yc.js"></script>

  </head>
  <body><div class="wrap">
  		<div class="header">
                    <a href="/"><img alt="Stanbol Logo" src="/intcheck/static/images/apache_stanbol_logo_cropped.png"/></a>
                </div>
    
                    <#if it?exists && it.mainMenuItems?exists>
                    <div class="sitemenu"> 
                            <h2 class="hide">Menu:</h2> 
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
                            <h2><a href="http://stlab.istc.cnr.it/stlab/The_Semantic_Technology_Laboratory_(STLab)">
                                <img src="/intcheck/static/images/stlabLogo.jpg" width="80" height="40"></a>
                            </h2>
                    </div> 
                    </#if>

                     
                <div id="content">
                    <h2>${title?html}</h2>
                <#nested>
                </div>
	
		<!--<div id="footer">
                    <a href="http://www.w3.org/standards/semanticweb/"><img class="swcube"
		    src="/intcheck/static/images/sw-cube.png"/></a>
                        <a href="http://www.iks-project.eu"><img
                        height="60px" alt="IKS Project" src="/intcheck/static/images/iks_project_logo.jpg" /></a>
                    <p><em>The research leading to these results has received funding from the European Community's
	          Seventh Framework Programme (FP7/2007-2013) under grant agreement n° 231527</em></p>
                </div>-->

           <div class="footer">

            <div class="column">
              <a href="http://www.w3.org/standards/semanticweb/"><img class="swcube"
                src="/intcheck/static/images/sw-cube.png"/></a>
              <a href="http://www.iks-project.eu"><img
                height="60px" alt="IKS Project" src="/intcheck/static/images/iks_project_logo.jpg" /></a>
            </div>
            <div class="column right">
              <em>The research leading to these results has received funding from the European Community's
                  Seventh Framework Programme (FP7/2007-2013) under grant agreement n° 231527</em>
            </div>
            <div style="clear: both"></div>
            </div>
        </div>
  </body>
</html>
</#macro>
