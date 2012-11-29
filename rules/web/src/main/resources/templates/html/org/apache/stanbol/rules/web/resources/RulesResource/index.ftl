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
	
	<!-- REFACTOR -->
<div id="step13" style="margin-top:60px;">
<h3>Hands-on about refactoring RDF graphs</h3>

<!-- REFACTOR TUTORIAL-->

<div id="showcodeofrefactoring" style="margin-top:10px;">

<p><a href=http://localhost:8080/rules>Stanbol refactor</a> coverts RDF graphs given a set of mapping rules i.e. transformation patterns. Stanbol rule syntax is described in detail in the <a href=http://localhost:8080/rules>Stanbol rules</a> documentation page. </p>

<p>Below you can see an example of transformation patterns defined for the GRS patterns</p>

<div class="showcode">

<input id="input-rdf-graph" type="file" value="upload"/>
RDF graph in input to which the refactoring should be applied. <br/><br/>
<input type="button" onClick="$('#recipe').show()" value="view"/> 
<input type="button" onClick="$('#recipe').hide()" value="hide"/> 
Transformation patterns defined as Stanbol Rules 
<div id="recipe" class="indent" style="display:none;">
<textarea id="recipecode" style="height:400px;">
</textarea>
<script type="text/javascript">
var recipeTmp = recipe.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
$('#recipecode').val(recipeTmp);
</script>
<input type="button" style="float:right;" value="save" onClick="javascript:saveRecipe()">
</div>
<p>Click on the button below and view results!</p>
<input type="button" value="refactor" onClick="javascript:var tutorial=new Tutorial(); tutorial.refactor();">
<div id="refactoringoutput" class="indent" style="display:none"></div>
<br>
<p> See how to access the refactor service through cURL command as well as Javascript.</p>
<input type="button" onClick="$('#refactoringcurl').show()" value="view"/> 
<input type="button" onClick="$('#refactoringcurl').hide()" value="hide"/> 
cURL command for refactor 
<div id="refactoringcurl" class="indent" style="display:none">
<pre>
curl -X POST -H "Accept: application/rdf+xml" -F input=@<span style="color:red;">%rdfGraph%</span> -F recipe=<span style="color:red;">%refactoringRecipe%</span> http://localhost:8080/refactor/apply
</pre>
</div>
<br>
<input type="button" onClick="$('#refactoringjavascript').show()" value="view"/> 
<input type="button" onClick="$('#refactoringjavascript').hide()" value="hide"/> 
Example of access to Stanbol refactor from a Javascript application 
<div id="refactoringjavascript" class="indent" style="display:none">
<pre>
var message = "-----------------------------7da24f2e50046" +
	"'Content-Disposition: form-data; name=\\"recipe\\";'" +
	"<span style="color:red;">here goes the recipe</span>" +
	"-----------------------------7da24f2e50046" +
	"'Content-Disposition: form-data; name=\"input\";" +
	"'filename=\"temp.txt\"'" + 
	"'Content-type: application/rdf+xml'" + 
	"<span style="color:red;">here goes the RDF graph</span>" +
	"-----------------------------7da24f2e50046";

$.ajax({
	type: "POST",
	url: "http://localhost:8080/refactor/apply",
	data: message,
	contentType: 'multipart/form-data;boundary=---------------------------7da24f2e50046',
	success: function(result) {
		...
	},
	error: function(result) {
		...
	}
});
</pre>
</div> 
</div>
<!-- input type="button" value="next step" onClick="javascript:$('#step14').show();" / -->
</div>
<div id="step14">

<!-- REFACTOR HANDSON (ex2)  -->

</div>

<!-- FINE BLOCCO DI OUTPUT DELL'ENTITY HUB PER IL FETCHING  -->

<!-- INIZIO BLOCCO DI OUTPUT DEL REFACTOR  -->

<!-- FINE BLOCCO DI OUTPUT DEL REFACTOR  -->


</div>
	
	<div id="rules-syntax" class="title-point" style="margin-top:60px;">
		<h3>Rules syntax in BNF</h3>
		<div id="syntax-body"><@ruleSyntax.view /></div>
	</div>

</div> <!-- end webview -->

<div class="panel" id="restapi" style="display: none;">
<h3 id="how-to-create-a-recipe">How to create a recipe</h3>
<ul>
<li>Service: <strong>/rules/recipe/</strong></li>
<li>Method: PUT</li>
<li>Parameters:<ul>
<li>recipe (Path parameter): the ID of the recipe as a path parameter(MANDATORY)</li>
<li>description: the textual description of the recipe (OPTIONAL)</li>
</ul>
</li>
</ul>
<p>Example:</p>
<div class="codehilite"><pre>curl -G -X PUT -d <span class="nv">description</span><span class="o">=</span><span class="s2">&quot;A test recipe.&quot;</span> <span class="se">\</span>
http://localhost:8080/rules/recipe/recipeTestA
</pre></div>


<h3 id="how-to-add-rules-to-a-recipe">How to add rules to a recipe</h3>
<ul>
<li>Service: <strong>/rules/recipe/</strong></li>
<li>Method: POST</li>
<li>Parameters:<ul>
<li>recipe (Path parameter): the ID of the recipe as a path parameter (MANDATORY)</li>
<li>rules: the rules in Stanbol syntax (MANDATORY)</li>
<li>description: the textual description of the rules (OPTIONAL)</li>
</ul>
</li>
</ul>
<p>Example:</p>
<div class="codehilite"><pre>curl -X POST -H <span class="s2">&quot;Content-type: multipart/form-data&quot;</span> <span class="se">\</span>
-F <span class="nv">rules</span><span class="o">=</span>@myRules -F <span class="nv">description</span><span class="o">=</span><span class="s2">&quot;My rules in the recipe.&quot;</span> <span class="se">\</span>
http://localhost:8080/rules/recipe/recipeTestA
</pre></div>


<h3 id="how-to-get-a-recipe-or-a-recipe-from-the-store">How to get a recipe or a recipe from the store</h3>
<ul>
<li>Service: <strong>/rules/recipe/</strong></li>
<li>Method: GET</li>
<li>Parameters:<ul>
<li>recipe (Path parameter): the ID of the recipe as a path parameter(MANDATORY)</li>
<li>rule: the ID of the rule (OPTIONAL). If it is null than the whole recipe is returned. Otherwise it is returned the single rule identified by the parameter value</li>
</ul>
</li>
<li>Accepts:<ul>
<li>application/rdf+xml</li>
<li>text/html</li>
<li>text/plain</li>
<li>application/owl+xml</li>
<li>text/owl-functional</li>
<li>text/owl-manchester</li>
<li>application/rdf+json,</li>
<li>text/turle</li>
</ul>
</li>
</ul>
<p>Example:</p>
<div class="codehilite"><pre>curl -G -X GET -H <span class="s2">&quot;Accept: text/turtle&quot;</span> <span class="se">\ </span>
-d <span class="nv">rule</span><span class="o">=</span>recipeTestA_rule1 <span class="se">\</span>
http://localhost:8080/rules/recipe/recipeTestA
</pre></div>


<h3 id="how-to-delete-a-recipe-or-a-recipe-from-the-store">How to delete a recipe or a recipe from the store</h3>
<ul>
<li>Service: <strong>/rules/recipe/</strong></li>
<li>Method: DELETE</li>
<li>Parameters:<ul>
<li>recipe (Path parameter): the ID of the recipe as a path parameter(MANDATORY)</li>
<li>rule: the ID of the rule (OPTIONAL). If it is null than the whole recipe is deleted. Otherwise it is deleted the single rule identified by the parameter value</li>
</ul>
</li>
</ul>
<p>Example:  <br />
</p>
<div class="codehilite"><pre>curl -X DELETE <span class="se">\</span>
-d <span class="nv">rule</span><span class="o">=</span>recipeTestA_rule1 <span class="se">\</span>
http://localhost:8080/rules/recipe/recipeTestA
</pre></div>


<h3 id="how-to-find-a-recipe-in-the-store">How to find a recipe in the store</h3>
<ul>
<li>Service: <strong>/rules/find/recipes</strong></li>
<li>Method: GET</li>
<li>Parameters:<ul>
<li>description: some word describing the recipe. This parameter is used as search field.</li>
</ul>
</li>
<li>Accepts:<ul>
<li>application/rdf+xml</li>
<li>text/html</li>
<li>text/plain</li>
<li>application/owl+xml</li>
<li>text/owl-functional</li>
<li>text/owl-manchester</li>
<li>application/rdf+json,</li>
<li>text/turle</li>
</ul>
</li>
</ul>
<p>Example:  <br />
</p>
<div class="codehilite"><pre>curl -G -X GET <span class="se">\</span>
-d <span class="nv">description</span><span class="o">=</span><span class="s2">&quot;test recipe&quot;</span> <span class="se">\</span>
http://localhost:8080/rules/find/recipes
</pre></div>


<h3 id="how-to-find-a-rule-in-the-store">How to find a rule in the store</h3>
<ul>
<li>Service: <strong>/rules/find/rules</strong></li>
<li>Method: GET</li>
<li>Parameters:<ul>
<li>description: some word describing the rule. This parameter is used as search field.</li>
</ul>
</li>
<li>Accepts:<ul>
<li>application/rdf+xml</li>
<li>text/html</li>
<li>text/plain</li>
<li>application/owl+xml</li>
<li>text/owl-functional</li>
<li>text/owl-manchester</li>
<li>application/rdf+json,</li>
<li>text/turle</li>
</ul>
</li>
</ul>
<p>Example:  <br />
</p>
<div class="codehilite"><pre>curl -G -X GET <span class="se">\</span>
-d <span class="nv">description</span><span class="o">=</span><span class="s2">&quot;My rules&quot;</span> <span class="se">\</span>
http://localhost:8080/rules/find/rules
</pre></div>

<h3 id="refactor-engine-refactor">Refactor Engine ("/refactor"):</h3>
<ul>
<li>The Refactor Engine <strong>@/refactor</strong> performs a refactoring applying an existing recipe in the rule store to the provided RDF graph. </li>
</ul>
<p>The request should be done as it follows:</p>
<ul>
<li>Method: GET</li>
<li>Parameters:<ul>
<li>input-graph: the ID of RDF graph in the triplestore provided as input</li>
<li>output-graph: the ID of RDF graph in the triplestore in which we want to store the result.</li>
<li>recipe: the ID of the recipe in the rule store</li>
</ul>
</li>
</ul>
<p>Example:</p>
<div class="codehilite"><pre>curl -G -X GET <span class="se">\</span>
-d input-graph<span class="o">=</span>stored_graph -d <span class="nv">recipe</span><span class="o">=</span>myTestRecipeA -d output-graph<span class="o">=</span>result_graph <span class="se">\</span>
http://localhost:8080/refactor
</pre></div>


<h3 id="refactor-engine-refactorapply">Refactor Engine ("/refactor/apply"):</h3>
<ul>
<li>Refactor Engine <strong>@/refactor/apply</strong> performs a refactoring applying an recipe as string to the provided RDF graph as input source.</li>
</ul>
<p>The request should be done as it follows:</p>
<ul>
<li>Method: POST</li>
<li>Parameters:<ul>
<li>recipe: the ID of the recipe (MANDATORY)</li>
<li>input: the RDF graph to which the refactoring has to be applied. The graph has to be provided as a binary file (MANDATORY)</li>
</ul>
</li>
<li>Accepts:<ul>
<li>application/rdf+xml</li>
<li>text/html</li>
<li>text/plain</li>
<li>application/owl+xml</li>
<li>text/owl-functional</li>
<li>text/owl-manchester</li>
<li>application/rdf+json,</li>
<li>text/turle</li>
</ul>
</li>
</ul>
<p>Example:</p>
<div class="codehilite"><pre>curl -X POST -H <span class="s2">&quot;Content-type: multipart/form-data&quot;</span> <span class="se">\</span>
-H <span class="s2">&quot;Accept: application/rdf+json&quot;</span> <span class="se">\</span>
-F <span class="nv">recipe</span><span class="o">=</span>recipeTestA -F <span class="nv">input</span><span class="o">=</span>@graph.rdf <span class="se">\</span>
http://localhost:8080/refactor/apply
</pre></div>

</div>
</@common.page>
</#escape>