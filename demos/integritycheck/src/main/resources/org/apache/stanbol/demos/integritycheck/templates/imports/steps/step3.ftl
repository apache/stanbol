<#macro view>
<div id="step-3" class="step-3 step">
<div id="step-3-before" class="step-3 before">
<h3 class="step-3">Step 3: /rules, /recipe</h3>
<p>
<!--
TODO: Find a nice rule to start from...
Why this rule does not return valid contents?

dbpedia = <http://dbpedia.org/ontology/> . 
ruleIntegrity[has(dbpedia:product, ?x, ?product) . 
is(dbpedia:Organisation, ?x) -> is(dbpedia:ValidContent, ?x)]  

-->
  <textarea id="step-3-input"
  >dbpedia = <http://dbpedia.org/ontology/> . 
ruleIntegrity[ 
is(dbpedia:Organisation, ?x) -> is(dbpedia:ValidContent, ?x)]  
  </textarea>
  <button type="button" id="step-3-start" class="start">Start</button>
</p>
</div>
<div id="step-3-after" class="step-3 after">
<p class="message"></p>
<p><ul id="step-3-results"></ul></p>
</div>
</div>
</#macro>