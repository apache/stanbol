<#macro form>
<form id="sparql" action="/sparql" method="GET"
 enctype="application/x-www-form-urlencoded"
 accept="application/sparql-results+xml, application/rdf+xml">
<textarea class="query" rows="11" name="query">
PREFIX fise: &lt;http://fise.iks-project.eu/ontology/&gt;
PREFIX dc:   &lt;http://purl.org/dc/terms/&gt;
SELECT distinct ?enhancement ?content ?engine ?extraction_time
WHERE {
  ?enhancement a fise:Enhancement .
  ?enhancement fise:extracted-from ?content .
  ?enhancement dc:creator ?engine .
  ?enhancement dc:created ?extraction_time .
}
ORDER BY DESC(?extraction_time) LIMIT 5
</textarea>
<p><input type="submit" class="submit" value="Run SPARQL query" /></p>
<pre class="prettyprint result" style="max-height: 200px; display: none" disabled="disabled">
</pre>
</form>
<script language="javascript"><!--
function registersSparqlHandler() {
   $("#sparql input.submit", this).click(function(e) {
     // disable regular form click
     e.preventDefault();
     
     // clean the result area
     $("#sparql textarea.result").text('');
     
     // submit sparql query using Ajax
     $.ajax({
       type: "POST",
       url: "/sparql",
       data: {query: $("#sparql textarea.query").val()},
       dataType: "html",
       cache: false,
       success: function(result) {
         $("#sparql pre.result").text(result).css("display", "block");
         prettyPrint();
       },
       error: function(result) {
         $("#sparql pre.result").text('Invalid query.').css("display", "block");
       }
     });
   });
 }
 $(document).ready(registersSparqlHandler);
--></script>
</#macro>