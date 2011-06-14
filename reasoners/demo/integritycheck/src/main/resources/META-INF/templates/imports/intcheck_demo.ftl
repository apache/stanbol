<#macro form>
<form id="sparql" action="/intcheck" method="POST"
 enctype="application/x-www-form-urlencoded"
 accept="application/sparql-results+xml, application/rdf+xml">
<input type="text" class="url" name="databaseName" value="" >Database Name for NGraph</Input
<input type="text" class="url" name="namespace" value="" >Triples namespace
<input type="text" class="url" name="phisicalDBName" value="" >Phisical DB Name
<input type="text" class="url" name="jdbcDriver" value="" >JDBC Driver
<input type="text" class="url" name="protocol" value="" >Protocol
<input type="text" class="url" name="host" value="" >Host
<input type="text" class="url" name="port" value="" >Port
<input type="text" class="url" name="username" value="" >Username
<input type="text" class="url" name="password" value="" >Password
<p><input type="submit" class="submit" value="RDB Reengineering"/></p>
<pre class="prettyprint result" style="max-height: 200px; display: none" disabled="disabled">
</pre>
</form>
</#macro>