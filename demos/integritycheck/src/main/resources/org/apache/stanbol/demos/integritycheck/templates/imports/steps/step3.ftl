<#macro view>
<div id="step-3" class="step-3 step">
<div id="step-3-before" class="step-3 before">
<h3 class="step-3">Step 3: /rules, /recipe</h3>
<p>
<!--
Just a nice rule to start from...
-->
  <textarea id="step-3-input"
  >
dbpedia = <http://dbpedia.org/ontology/> . 
category = <http://dbpedia.org/resource/Category:> . 
dc = <http://purl.org/dc/terms/> .
foaf = <http://xmlns.com/foaf/0.1/> .
demo = <http://www.example.org/integritycheck/> .
geo =  <http://www.w3.org/2003/01/geo/wgs84_pos#> .

check[ 
  has(dc:subject, ?x, category:Consumer_electronics_brands) .
  is(dbpedia:Organisation, ?x) 
     -> 
  is(demo:ValidContent, ?x)
]  
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