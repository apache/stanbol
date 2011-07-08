<#import "/imports/common.ftl" as common>
<#import "/imports/ruleSyntax.ftl" as ruleSyntax>
<#import "/imports/tutorial0.ftl" as tutorial0>
<#import "/imports/tutorial1.ftl" as tutorial1>
<#import "/imports/tutorial2.ftl" as tutorial2>
<#import "/imports/tutorial3.ftl" as tutorial3>
<#import "/imports/tutorial4.ftl" as tutorial4>
<#import "/imports/tutorial5.ftl" as tutorial5>
<#escape x as x?html>
<@common.page title="Apache Stanbol Rules" hasrestapi=true>

 <div class="panel" id="webview">
          
	<div id="rules-tutorial" class="title-point">
		<h3>Rules tutorial</h3>
		<div id="tutorial-body">
			<div id="tutorial0" class="active"><@tutorial0.view /></div>

			<div id="tutorial1" class="inactive"><@tutorial1.view /></div>

			<div id="tutorial2" class="inactive"><@tutorial2.view /></div>

			<div id="tutorial3" class="inactive"><@tutorial3.view /></div>

			<div id="tutorial4" class="inactive"><@tutorial4.view /></div>

			<div id="tutorial5" class="inactive"><@tutorial5.view /></div>
 
		</div> <!-- end tutorial-body -->
		
		<div class="arrows">
			<a id="previous" href="javascript:var interaction = new Interaction(); interaction.previousTutorial()">Previous</a> | <a id="next" href="javascript:var interaction = new Interaction(); interaction.nextTutorial()">Next</a>
		</div>

	</div> <!-- end rules-tutorial -->
	
	<div id="rules-syntax" class="title-point">
		<h3>Rules syntax in BNF</h3>
		<div id="syntax-body"><@ruleSyntax.view /></div>
	</div>

</div> <!-- end webview -->

<div class="panel" id="restapi" style="display: none;"/>

</@common.page>
</#escape>