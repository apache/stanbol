<#macro view>
Steps of the demo:
<p>
<ol>
<li>Send the text to the <b>/engines</b> service</li>
<li>Load the data about entities in a <b>/ontonet</b> session from the <b>web</b></li>
<li>Define rule to apply for valid content using <b>/recipe</b> and <b>/rules</b></li>
<li>Run the classifier (<b>/reasoners/classify</b>) pointing to the ontonet session and the recipe</li>
</ol>
</p>
<p>You can check more details about how to implement this process (REST calls) from the <a target="_blank" href="/static/integritycheck/integritycheck.js">Javascript sourcecode</a></p>
</#macro>