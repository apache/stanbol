<br>
<h3>The RESTful API of the Contenthub Index / LDPath</h3>

<h3>Submit an LDPathSemanticIndex</h3>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP POST method which saves an LDPathSemanticIndex into the persistent store of Contenthub.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/index/ldpath</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>name:</b> The name identifying the index<br>
      <b>description:</b> Description of the index<br>
      <b>program:</b> LDPath program that will be used as a source to create the semantic index. Index fields and Solr specific configurations regarding those index fields are given in this parameter.<br>
      <b>indexContent:</b> If this configuration is true plain text content of the ContentItem is also indexed to be used in the full text search
      <b>batchSize:</b> Maximum number of changes to be returned<br>
      <b>indexingSourceName:</b> Name of the IndexingSource instance to be checked for updates<br>
      <b>indexingSourceCheckPeriod:</b> Time to check changes in the Contenthub Store in second units<br>
      <b>solrCheckTime:</b> Maximum time in seconds to wait for the availability of the Solr Server<br>
      <b>ranking:</b> To be able to use other SemanticIndex implementations rather than this, Service Ranking property of other implementations should be set higher than of this one
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTTP OK(200) or BAD REQUEST(400)</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>
      IndexManagementException<br>
      InterruptedException<br>
      IndexException
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curlLDPath1" class="curlLine">curl -i -X POST -d "name=myindex&description=It indexes person and place entities of a content&program=@prefix dbp-ont: &lt;http://dbpedia.org/ontology/&gt;; person_entities = .[rdf:type is dbp-ont:Person]:: xsd:anyURI (termVectors=\"true\"); place_entities = .[rdf:type is dbp-ont:Place]:: xsd:anyURI (termVectors=\"true\");" ${it.publicBaseUri}contenthub/index/ldpath<hr/></div>curl -i -X POST -d "name=myindex&\
                    description=It indexes person and place entities of a content&\
                    program=@prefix dbp-ont: &lt;http://dbpedia.org/ontology/&gt;; \
                    person_entities = .[rdf:type is dbp-ont:Person]:: xsd:anyURI (termVectors="true"); \
                    place_entities = .[rdf:type is dbp-ont:Place]:: xsd:anyURI (termVectors="true");" \
     ${it.publicBaseUri}contenthub/ldpath/program
</pre>
</div>

<hr>

<script>
  function selectText(element) {
      var doc = document;
      var text = doc.getElementById(element);    
      if (doc.body.createTextRange) { // ms
          var range = doc.body.createTextRange();
          range.moveToElementText(text);
          range.select();
      } else if (window.getSelection) {
          var selection = window.getSelection();
          var range = doc.createRange();
          range.selectNodeContents(text);
          selection.removeAllRanges();
          selection.addRange(range);
      }
  }

  function getLine(div){
      $(div).children('pre').children('.curlLine').toggle();
      selectText($(div).children('pre').children('.curlLine').attr('id'));
  }
</script>