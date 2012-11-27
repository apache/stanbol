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

<h3>The RESTful API of the Contenthub Search</h3>

<h3>Featured Search</h3>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to make a featured search over Contenthub.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/{indexName}/search/featured</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
        <b>queryTerm:</b> A keyword a statement or a set of keywords which can be regarded as the query term.<br>
        <b>solrQuery:</b> Solr query string. This is the string format which is accepted by a Solr server. For example, <code> q="john doe"&fl=score</code> is a valid value for this parameter. If this parameter exists, search is performed based on this solrQuery and any queryTerms are neglected.<br>
        <b>constraints:</b> Solr query string. This is the string format which is accepted by a Solr server. For example, <code> q="john doe"&fl=score</code> is a valid value for this parameter. If this parameter exists, search is performed based on this solrQuery and any queryTerms are neglected.<br>
        <b>ontologyURI:</b> URI of the ontology in which related keywords will be searched by RelatedKeywordSearchManager#getRelatedKeywordsFromOntology(String, String)<br>
        <b>offset:</b> The offset of the document from which the resultant documents will start as the search result.<br>
        <b>limit:</b> Maximum number of resultant documents to be returned as the search result. offset and limit parameters can be used to make a pagination mechanism for search results.<br>
        <b>fromStore:</b> Special parameter for HTML view only.
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTML view or JSON representation of the search results or HTTP BAD REQUEST(400)</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>
      IllegalArgumentException<br>
      SearchException<br>
      InstantiationException<br>
      IllegalAccessException<br>
      SolrServerException<br>
      IOException
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i "http://localhost:8080/contenthub/contenthub/search/featured?queryTerm=paris&limit=3"</pre>

<hr>

<!--
<h3>Related Search</h3>

Related Search provide three sub-endpoints

<h4>/related endpoint</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to retrieve related keywords from all resources defined within Contenthub.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/search/related</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
        <b>keyword:</b> The keyword whose related keywords will be retrieved.<br>
        <b>graphURI:</b> URI of the ontology to be used during the step in which related keywords are searched in ontology resources. If this parameter is <code> null</code>, then no related keywords are returned from ontology resources.
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>JSON string which is constructed by SearchResultWriter. SearchResult returned by RelatedKeywordSearchManager#getRelatedKeywordsFromAllSources(String, String) only contains related keywords (no resultant documents or facet fields are returned within the SearchResult).</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>SearchException</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X GET http://localhost:8080/contenthub/search/related?keyword=Paris -H "Accept:application/json"</pre>
<hr>


<h4>related/wordnet sub-endpoint</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to retrieve related keywords from Wordnet. If a Wordnet database is not installed into Contenthub, this method cannot find any related keywords.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/search/related/wordnet</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>keyword:</b> The keyword whose related keywords will be retrieved from Wordnet.</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>JSON string which is constructed by SearchResultWriter. SearchResult returned by RelatedKeywordSearchManager#getRelatedKeywordsFromWordnet(String) contains only related keywords from Wordnet. (No resultant documents or facet fields are returned within the SearchResult).</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>SearchException</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X GET http://localhost:8080/contenthub/search/related/wordnet?keyword=Paris -H "Accept:application/json"</pre>

<hr>


<h4>related/ontology sub-endpoint</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to retrieve related keywords from ontology resources. Given the ontology URI, this method looks for subsumption/hierarchy relations among the concepts to come up with related keywords.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/search/related/ontology</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>keyword:</b> The keyword whose related keywords will be retrieved from ontology resources.<br>
      <b>graphURI:</b> URI of the ontology in which related keywords will be searched. The ontology should be available in the Contenthub system.
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>JSON string which is constructed by SearchResultWriter. SearchResult returned by RelatedKeywordSearchManager#getRelatedKeywordsFromOntology(String, String) contains only related keywords from ontology resources. (No resultant documents or facet fields are returned within the SearchResult).</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>SearchException</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl1" class="curlLine">curl -i -X GET http://localhost:8080/contenthub/search/related/ontology?keyword=Paris&graphURI=testOntology -H "Accept:application/json"<hr/></div>curl -i -X GET http://localhost:8080/contenthub/search/related/ontology?keyword=Paris&graphURI=testOntology \
     -H "Accept:application/json"
</pre>
</div>

<hr>


<h4>related/referencedsite sub-endpoint</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to retrieve related keywords from the referenced sites.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/search/related/referencedsite</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>keyword:</b> The keyword whose related keywords will be retrieved from referenced sites.</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>JSON string which is constructed by SearchResultWriter. SearchResult returned by RelatedKeywordSearchManager#getRelatedKeywordsFromReferencedSites(String) contains only related keywords from referenced sites. (No resultant documents or facet fields are returned within the SearchResult).</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>SearchException</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X GET http://localhost:8080/contenthub/search/related/referencedsite?keyword=Paris -H "Accept:application/json"</pre>

<hr>
-->
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