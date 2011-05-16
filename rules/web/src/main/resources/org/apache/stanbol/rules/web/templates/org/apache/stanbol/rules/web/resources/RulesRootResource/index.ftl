<#import "/imports/common.ftl" as common>
<#import "/imports/ruleSyntax.ftl" as ruleSyntax>
<#import "/imports/tutorial0.ftl" as tutorial0>
<#import "/imports/tutorial1.ftl" as tutorial1>
<#escape x as x?html>
<@common.page title="Apache Stanbol Rules" hasrestapi=false> 

<div id="syntax-title" class="title-point">Rules syntax in BNF
<input id="show-syntax-button" class="show-button" value="show" type="button" onClick="javascript:var interaction = new Interaction(); interaction.show('syntax');">
<input id="hide-syntax-button" class="hide-button" value="hide" type="button" onClick="javascript:var interaction = new Interaction(); interaction.hide('syntax');">
<div id="syntax-body">

<@ruleSyntax.view /> 

</div> 
</div>


<div id="tutorial-title" class="title-point">Rules tutorial
<input id="show-tutorial-button" class="show-button" value="show" type="button" onClick="javascript:var interaction = new Interaction(); interaction.show('tutorial');">
<input id="hide-tutorial-button" class="hide-button" value="hide" type="button" onClick="javascript:var interaction = new Interaction(); interaction.hide('tutorial');">
<div id="tutorial-body">

<p id="tutorial0" class="active"> 
<@tutorial0.view />
</p>

<p id="tutorial1" class="inactive"> 
<@tutorial1.view />
</p> 


</div>
<div class="arrows">
<a id="previous" href="javascript:var interaction = new Interaction(); interaction.previousTutorial()">Previous</a> | <a id="next" href="javascript:var interaction = new Interaction(); interaction.nextTutorial()">Next</a>
</div>
</div>
</@common.page>
</#escape>