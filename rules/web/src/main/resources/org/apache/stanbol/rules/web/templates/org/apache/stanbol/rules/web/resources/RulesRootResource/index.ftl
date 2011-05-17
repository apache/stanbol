<#import "/imports/common.ftl" as common>
<#import "/imports/ruleSyntax.ftl" as ruleSyntax>
<#import "/imports/tutorial0.ftl" as tutorial0>
<#import "/imports/tutorial1.ftl" as tutorial1>
<#import "/imports/tutorial2.ftl" as tutorial2>
<#import "/imports/tutorial3.ftl" as tutorial3>
<#import "/imports/tutorial4.ftl" as tutorial4>
<#import "/imports/tutorial5.ftl" as tutorial5>
<#escape x as x?html>
<@common.page title="Apache Stanbol Rules" hasrestapi=false> 

<div id="syntax-title" class="title-point">Rules syntax in BNF
<input id="show-syntax-button" class="show-button" value="show" type="button" onClick="javascript:var interaction = new Interaction(); interaction.show('syntax');">
<input id="hide-syntax-button" class="hide-button" value="hide" type="button" onClick="javascript:var interaction = new Interaction(); interaction.hide('syntax');">
<div id="syntax-body"  class="indent">

<@ruleSyntax.view /> 

</div> 
</div>


<div id="tutorial-title" class="title-point">Rules tutorial
<input id="show-tutorial-button" class="show-button" value="show" type="button" onClick="javascript:var interaction = new Interaction(); interaction.show('tutorial');">
<input id="hide-tutorial-button" class="hide-button" value="hide" type="button" onClick="javascript:var interaction = new Interaction(); interaction.hide('tutorial');">
<div id="tutorial-body" class="indent">

<div id="tutorial0" class="active"> 
<@tutorial0.view />
</div>

<div id="tutorial1" class="inactive"> 
<@tutorial1.view />


</div>

<div id="tutorial2" class="inactive"> 
<@tutorial2.view />
</div>

<div id="tutorial3" class="inactive"> 
<@tutorial3.view />
</div>

<div id="tutorial4" class="inactive"> 
<@tutorial4.view />
</div>

<div id="tutorial5" class="inactive"> 
<@tutorial5.view />
</div>
 
<div class="arrows">
<a id="previous" href="javascript:var interaction = new Interaction(); interaction.previousTutorial()">Previous</a> | <a id="next" href="javascript:var interaction = new Interaction(); interaction.nextTutorial()">Next</a>
</div>

</div>

</div>
</@common.page>
</#escape>