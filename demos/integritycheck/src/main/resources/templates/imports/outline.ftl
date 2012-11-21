<#macro view>
Steps of the demo:
<p>
<ol>
<li>Send the text to the <b>/engines</b> service, getting the list of entities found.</li>
<li>Load the FOAF schema in a <b>/ontonet</b> <i>scope</i>. Add a Demo schema to a <b/>ontonet</b><i>/session</i>, to declare some additional properties and clases:
<ul>
<li><tt>demo:ValidContent</tt></li>
<li><tt>dc:subject</tt></li>
<li><tt>geo:long</tt></li>
<li><tt>geo:lat</tt></li>
<li><tt>dbpedia:populationTotal</tt></li>
<li><tt>dc:subject</tt></li>
</ul>
</li>
<li>Define rule to apply using <b>/recipe</b> and <b>/rules</b>, using classes and properties from the above schemas. Rule validation will result in <tt>-> is(?x,demo:ValidContent</tt>) </li>
<li>Run the classifier (<b>/reasoners/owl2/classify</b>) providing <i>scope</i>, <i>session</i>, <i>recipe</i> and the link at each entity from the <b>/entityhub</b></li>
</ol>
</p>
<p>You can check more details about how to implement this process (REST calls) from the <a target="_blank" href="/static/integritycheck/integritycheck.js">Javascript sourcecode</a></p>
</#macro>