<#macro view>
<div id="step-2" class="step-2 step">
<div id="step-2-before" class="step-2 before">
<h3 class="step-2">Step 2: /ontonet</h3>
<p class="info">We create a new session on the <b>integritycheck</b> scope, which includes an ontology
that defines types and attributes of dbpedia. It also contains a type <b>dbpedia:ValidContent</b>, whch we use for this demo (<a target="_blank" href="/static/integritycheck/dbpedia_demo.owl">get the ontology</a>).</p>
<p>
  <button type="button" id="step-2-start" class="start">Start</button>
</p>
</div>
<div id="step-2-after" class="step-2 after">
<p class="message"></p>
<p><ul id="step-2-results"></ul></p>
</div>
</div>
</#macro>