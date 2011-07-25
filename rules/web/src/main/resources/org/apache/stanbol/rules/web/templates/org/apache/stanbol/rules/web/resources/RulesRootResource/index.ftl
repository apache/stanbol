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
curl -X POST -H "Accept: application/rdf+xml" -F input=@<span style="color:red;">%rdfGraph%</span> -F recipe=<span style="color:red;">%refactoringRecipe%</span>
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
<h3> Getting rules from the rule store ("/rule") </h3>
Request type: GET<br><br>
Accepted MIME types:
<ul>
<li>application/rdf+xml</li>
<li>text/owl-manchester</li>
<li>application/rdf+json</li>
<li>application/turtle</li>
<li>application/owl-functional</li>
</ul>

Parameters:

<ul>
<li> <b>uri (Mandatory).</b> The URI which identifies the rule in the rule store. The
 parameter has to be passed as a path parameter</li> 
</ul>
Possible outputs:
<ul>
  <li> 200 The rule is retrieved (import declarations point to KReS Services) </li>
  <li> 404 The rule does not exists in the manager </li>
  <li> 500 Some error occurred </li>
</ul>

Request example which returns the RDF representation of a rule identified
by the URI <http://iks-project.eu/rules/rule1> in its RDF/XML serialization.
<pre style="margin-bottom:40px;">
$ curl -X GET -H "Accept: application/rdf+xml" \
  http://localhost:8080/rule/http://iks-project.eu/rules/rule1
</pre>

<h3> Adding rules to the rule store ("/rule") </h3>
Request type: POST<br><br>

Parameters:

<ul>
<li> <b>recipe (Mandatory).</b> The recipe's identifier. It is the recipe to which the
 rules should be added</li>
 <li> <b>rule (Mandatory).</b> The rule's identifier. It can be any unique string, e.g. a URI</li> 
 <li> <b>kres-syntax (Mandatory).</b> The rule content expressed with the Stanbol Rules
 syntax</li>
 <li> <b>description.</b> A textual description of the behaviour of the rule</li>
</ul>
Possible outputs:
<ul>
  <li> 200 The rule has been added </li>
  <li> 204 The rule has not been added </li>
  <li> 400 The rule and recipe are not specified </li>
  <li> 404 Recipe or rule not found </li>
  <li> 409 The rule has not been added </li>
  <li> 500 Some error occurred </li>
</ul>

Request example which shows how can be used the service in order to
create a rule:
<pre style="margin-bottom:40px;">
$ curl -X POST -d recipe="http://iks-project.eu/recipies/recipeA" \
      -d rule="http://iks-project.eu/rule/uncleRule" \
      -d kres-syntax= \
                  "has(<http://www.foo.org/myont.owl#hasFather>, ?x, ?z) . \
                  has(<http://www.foo.org/myont.owl#hasBrother>, ?z, ?y) \
                    -> \
                  has(<http://www.foo.org/myont.owl#hasUncle>, ?x, ?y)" \
      -d description="The rule which allows to infer hasUncle relations." \
      http://localhost:8080/rule \
</pre>

<h3> Removing rules from the rule store ("/rule") </h3>
Request type: DELETE<br><br>

Parameters:

<ul>
 <li> <b>rule (Mandatory).</b> The rule's identifier. It can be any unique string, e.g. a URI</li> 
 <li> <b>recipe.</b> The recipe's identifier. It is the recipe from which
 the rules should be removed. When the parameter is provided, the rule is
 removed only from the recipe, but it is still availavle in the rule base.
 Otherwise, if the parameter is not provided, the rule is completely deleted
 from the rule store</li>
</ul>
Possible outputs:
<ul>
  <li> 200 The rule has been deleted </li>
  <li> 204 The rule has not been deleted </li>
  <li> 404 Recipe or rule not found </li>
  <li> 409 The recipe has not been deleted </li>
  <li> 500 Some error occurred </li>
</ul>

Request example which shows how to delete a rule from a recipe leaving it
into the rule store:
<pre style="margin-bottom:40px;">
$ curl -X DELETE -G -d recipe="http://iks-project.eu/recipies/recipeA" \
      -d rule="http://iks-project.eu/rule/uncleRule" \
      http://localhost:8080/rule
</pre>

<h3> Getting recipes from the rule store ("/recipe") </h3>
Request type: GET<br><br>

Accepted MIME types:
<ul>
<li>application/rdf+xml</li>
<li>text/owl-manchester</li>
<li>application/rdf+json</li>
<li>application/turtle</li>
<li>application/owl-functional</li>
</ul>

Parameters:

<ul>
 <li> <b>uri (Mandatory).</b> The recipe's identifier that is basically an URI</li>
</ul>
Possible outputs:
<ul>
  <li> 200 The recipe is retrieved </li>
  <li> 404 The recipe does not exists in the manager </li>
  <li> 500 Some error occurred </li>
</ul>

Request example which shows how to get a recipe from the rule store:
<pre style="margin-bottom:40px;">
$ curl -X GET \
      http://localhost:8080/recipe/http://iks-project.eu/recipies/recipeA
</pre>

<h3> Adding recipes to the rule store ("/recipe") </h3>
Request type: POST<br><br>

Parameters:

<ul>
 <li> <b>recipe (Mandatory).</b> The recipe's identifier that is basically an URI</li>
 <li> <b>description.</b> The textual description of the recipe</li>
</ul>
Possible outputs:
<ul>
  <li> 200 The recipe has been added </li>
  <li> 409 The recipe has not been added </li>
  <li> 500 Some error occurred </li>
</ul>

Request example which shows how to create a recipe and add it into the rule
store:
<pre style="margin-bottom:40px;">
$ curl -X POST \
      -d recipe="http://iks-project.eu/recipies/recipeA" \
      -d description="Example of recipe." \
      http://localhost:8080/recipe
</pre>

<h3> Removing recipes from the rule store ("/recipe") </h3>
Request type: DELETE<br><br>

Parameters:

<ul>
 <li> <b>recipe (Mandatory).</b> The recipe's identifier that is basically an URI</li>
</ul>
Possible outputs:
<ul>
  <li> 200 The recipe has been delted </li>
  <li> 409 The recipe has not been deleted </li>
  <li> 500 Some error occurred </li>
</ul>

Request example which shows how to delete a recipe from the rule
store:
<pre style="margin-bottom:40px;">
$ curl -X DELETE -G \
      -d recipe="http://iks-project.eu/recipies/recipeA" \
      http://localhost:8080/recipe
</pre>


<h3> Refactoring RDF graphs ("/refactor") </h3>
Request type: GET<br><br>

Accepted MIME types:
<ul>
<li>application/rdf+xml</li>
<li>text/owl-manchester</li>
<li>application/rdf+json</li>
<li>application/turtle</li>
<li>application/owl-functional</li>
</ul>

Parameters:

<ul>
 <li> <b>recipe (Mandatory).</b> The recipe's identifier that is basically an URI</li>
 <li> <b>input-graph (Mandatory).</b> The ID of the graph to transform</li>
 <li> <b>output-graph (Mandatory).</b> The ID that the transformed graph has to have in
 the IKS triple store</li>
</ul>
Possible outputs:
<ul>
  <li> 200 The refactoring is performed and a new RDF graph is returned </li>
  <li> 404 The recipe does not exists in the manager </li>
  <li> 500 Some error occurred </li>
</ul>

Request example which shows how to perform a refactoring
applying an existing recipe in the rule store:
<pre style="margin-bottom:40px;">
$ curl -X GET -G -H "Accept: application/rdf+xml" \
      -d recipe="http://iks-project.eu/recipies/recipeA" \
      -d input-graph="http://iks-project.eu/graphs/graphIn" \
      -d output-graph="http://iks-project.eu/graphs/graphOut" \
      http://localhost:8080/refactor
</pre>

<h3> Refactoring RDF graphs ("/refactor/apply") </h3>
Request type: POST<br><br>

Accepted MIME types:
<ul>
<li>application/rdf+xml</li>
<li>text/owl-manchester</li>
<li>application/rdf+json</li>
<li>application/turtle</li>
<li>application/owl-functional</li>
</ul>

Parameters:

<ul>
 <li> <b>recipe (Mandatory).</b> The recipe's identifier that is basically an URI</li>
 <li> <b>input-graph (Mandatory).</b> The ID of the graph to transform</li>
 <li> <b>output-graph (Mandatory).</b> The ID that the transformed graph has to have in
 the IKS triple store</li>
</ul>
Possible outputs:
<ul>
  <li> 200 The refactoring is performed and a new RDF graph is returned </li>
  <li> 404 The recipe does not exists in the manager </li>
  <li> 500 Some error occurred </li>
</ul>

Request example which shows how to perform a refactoring
applying an existing recipe in the rule store:
<pre style="margin-bottom:40px;">
$ curl -X POST -H "Content-Type: multipart/form-data" \
      -H "Accept: application/turtle" \
      -F recipe="
           has(<http://www.foo.org/myont.owl#hasFather>, ?x, ?z) . \
           has(<http://www.foo.org/myont.owl#hasBrother>, ?z, ?y) \
              -> \
           has(<http://www.foo.org/myont.owl#hasUncle>, ?x, ?y)" \
      -F input=@graph.rdf
      http://localhost:8080/refactor/apply
</pre>
</div>
</@common.page>
</#escape>