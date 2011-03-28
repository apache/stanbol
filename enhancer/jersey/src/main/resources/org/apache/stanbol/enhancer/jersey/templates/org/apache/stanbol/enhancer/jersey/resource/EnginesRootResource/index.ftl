<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Enhancement Engines" hasrestapi=true>


<div class="panel" id="webview">
<#if it.activeEngines?size == 0>
  <p><em>There is no active engines. Administrators can install,
   configure and enable new engines by using the
   <a href="/system/console">OSGi console</a>.</em></p>
<#else>
  <div class="enginelisting">
  <div class="collapsed">
  <p class="collapseheader">There are currently
   <strong>${it.activeEngines?size}</strong> active engines.</p>
  <div class="collapsable">
  <ul>
    <#list it.activeEngines as engine>
    <li>${engine.class.name}</li>
    </#list>
  </ul>
  <p class="note">You can enable, disable and deploy new engines using the
    <a href="/system/console/components">OSGi console</a>.</p>
  </div>
  </div>
  </div>
  
<script>
$(".enginelisting p").click(function () {
  $(this).parents("div").toggleClass("collapsed");
});    
</script>

  <p>Paste some text below and submit the form to let the active engines enhance it:</p>
  <form id="enginesInput" method="POST" accept-charset="utf-8">
    <p><textarea rows="15" name="content"></textarea></p>
    <p class="submitButtons">Output format:
      <select name="format">
      	<option value="application/json">JSON-LD</option>
        <option value="application/rdf+xml">RDF/XML</option>
        <option value="application/rdf+json">RDF/JSON</option>
        <option value="text/turtle">Turtle</option>
        <option value="text/rdf+nt">N-TRIPLES</option>
      </select> <input class="submit" type="submit" value="Run engines">
    </p>
  </form>
<script language="javascript"><!--
function registerFormHandler() {
   $("#enginesInput input.submit", this).click(function(e) {
     // disable regular form click
     e.preventDefault();
     
     var data = {
       content: $("#enginesInput textarea[name=content]").val(),
       ajax: true,
       format:  $("#enginesInput select[name=format]").val()
     };
     
     $("#enginesOuputWaiter").show();
     
     // submit the form query using Ajax
     $.ajax({
       type: "POST",
       url: "/engines",
       data: data,
       dataType: "html",
       cache: false,
       success: function(result) {
         $("#enginesOuputWaiter").hide();
         $("#enginesOuput").html(result);
       },
       error: function(result) {
         $("#enginesOuputWaiter").hide();
         $("#enginesOuput").text('Invalid query.');
       }
     });
   });
 }
 $(document).ready(registerFormHandler);
--></script>
  <div id="enginesOuputWaiter" style="display: none">
    <p>the Stanbol enhancer is analysing your content...</p>
    <p><img alt="Waiting..." src="/static/images/ajax-loader.gif" /></p>
  </div>
  <p id="enginesOuput"></p>
</#if>
</div>

<div class="panel" id="restapi" style="display: none;">
<h3>Stateless REST analysis</h3>

<p>This stateless interface allows the caller to submit content to the Stanbol enhancer engines and
get the resulting enhancements formatted as RDF at once without storing anything on the
server-side.</p>

<p>The content to analyze should be sent in a POST request with the mimetype specified in
the <code>Content-type</code> header. The response will hold the RDF enhancement serialized
in the format specified in the <code>Accept</code> header:</p>
   
<pre>
curl -X POST -H "Accept: text/turtle" -H "Content-type: text/plain" \
     --data "The Stanbol enhancer can detect famous cities such as Paris." \
     ${it.publicBaseUri}engines/
</pre> 

<p>The list of mimetypes accepted as inputs depends on the deployed engines. By default only
 <code>text/plain</code> content will be analyzed</p>
 
<p>Stanbol enhancer is able to serialize the response in the following RDF formats:</p>
<ul>
<li><code>application/json</code> (JSON-LD)</li>
<li><code>application/rdf+xml</code> (RDF/XML)</li>
<li><code>application/rdf+json</code> (RDF/JSON)</li>
<li><code>text/turtle</code> (Turtle)</li>
<li><code>text/rdf+nt</code> (N-TRIPLES)</li>
</ul> 

<p>By default the URI of the content item being enhanced is a local, non
de-referencable URI automatically built out of a hash digest of the binary
content. Sometimes it might be helpful to provide the URI of the content-item
to be used in the enhancements RDF graph. This can be achieved by passing a
<code>uri</code> request parameter as follows:</p>

<pre>
curl -X POST -H "Accept: text/turtle" -H "Content-type: text/plain" \
     --data "The Stanbol enhancer can detect famous cities such as Paris." \
     "${it.publicBaseUri}engines/?uri=urn:fise-example-content-item"
</pre> 

</div>


</@common.page>
</#escape>
