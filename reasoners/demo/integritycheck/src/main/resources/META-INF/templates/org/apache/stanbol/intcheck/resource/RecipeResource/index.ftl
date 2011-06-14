<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Recipes and Rules">

<div class="contentTag">
<div class="menuLeft">Recipes stored <span id="addRecipe" class="hide"><a href="javascript:var recipe = new Recipe(); recipe.displayAddBox()" alt="add recipe"><img src="/intcheck/static/images/add.gif"</a><span></div>
<div class="menuRightText"><a id="action" href="javascript:listRecipes()">view</a></div>
<div id="recipeList"></div>
</div>

</@common.page>
</#escape>
