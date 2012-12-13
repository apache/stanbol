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

  <h3>Uploading new content to the Contenthub</h3>

  <p>Contenthub lives at the endpoint starting with "contenthub":</p>
  <code>${it.publicBaseUri}contenthub/</code>
  <p>This endpoint automatically forwards to:</p>
  <code>${it.publicBaseUri}contenthub/${it.indexName}/store/</code>
  <p>The endpoint to which Contenthub automatically forwards includes the name of the default index,
  whose name is "contenthub". That is the reason for two consecutive "contenthub"s in the endpoint.
  Lastly, "store" page provides the storage related functionalities of Contenthub such as document submission.</p>

<p>
Tutorials on Stanbol Contenthub can be found in the following links:<br/>
<a href="http://stanbol.apache.org/docs/trunk/components/contenthub/">Contenhub - One Minute Tutorial</a><br/>
<a href="http://stanbol.apache.org/docs/trunk/components/contenthub/contenthub5min">Contenthub - Five Minutes Tutorial</a>
</p>

<br>
<h3>The RESTful API of the Contenthub Store</h3>

<h3>Create a Content Item</h3>

<h4>Create with multipart/form-data</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP POST method to create a content item in Contenthub. This method takes a ContentItem object directly. This means that the values provided for this service will be parsed by the multipart mime serialization of Content Items. (see the following links: <a href="http://incubator.apache.org/stanbol/docs/trunk/components/enhancer/contentitem.html#multipart_mime_serialization">Content Item Multipart Serialization</a> and <a href="http://incubator.apache.org/stanbol/docs/trunk/components/enhancer/enhancerrest.html">Using the multi-part content item RESTful API extensions</a>)</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/{indexName}/store</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>ci:</b> ContentItem to be stored.<br/>
      <b>title:</b> The title for the content item. Titles can be used to present summary of the actual content. For example, search results are presented by showing the titles of resultant content items.<br/>
      <b>chain:</b> name of a particular Chain in which the enhancement engines are ordered according to a specific use case or need
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>Varies based on the <b>Accept</b> header:
      <ul>
        <li><b>text/html:</b> HTTP 303 (SEE OTHER) including the full URI representing the newly created ContentItem in the Contenthub.</li>
        <li><b>otherwise:</b> HTTP 201 (CREATED) including the full URI representing the newly created ContentItem in the Contenthub.</li>
      </ul>
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
In the examples below, specified parameters are processed by the multipart serialization feature and a ContentItem object is created as a result.</br> 
With following curl command, a ContentItem is created using the data specified in the content parameter. 
<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl1" class="curlLine">curl -i -F "content=I live in Paris.;type=text/plain" "http://localhost:8080/contenthub/contenthub/store?title=Paris"<hr/></div>curl -i -F "content=I live in Paris.;type=text/plain" \ 
     "http://localhost:8080/contenthub/contenthub/store?title=Paris"
</pre>
</div>

In the example below, metadata of the ContentItem is set with the <b>metadata</b> parameter. Note that the name of the metadata file is set as the URI of the ContentItem. So, it is suggested that the name of the file would start with <b>urn:</b> prefix by convention to provide valid URI for the ContentItem. In this case, since the ContentItem has already enhancements, it is not re-enhanced through the Enhancer component. 

<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl2" class="curlLine">curl -i -F "metadata=@;/urn:my-content-item;type=application/rdf+xml" -F "content=I live in Paris.;type=text/plain" "http://localhost:8080/contenthub/contenthub/store"<hr/></div>curl -i -F "metadata=@urn:my-content-item;type=application/rdf+xml" \
        -F "content=I live in Paris.;type=text/plain" \
     "http://localhost:8080/contenthub/contenthub/store"
</pre>
</div>

It is also possible to specify a custom URI for the ContentItem as in the example below. A valid URI should start with a scheme name followed by a colon ":" . By convention you can use <b>urn:</b> prefix for your custom URIs.

<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl2_1" class="curlLine">curl -i -F "metadata=@metadata_file;type=application/rdf+xml" -F "content=I live in London.;type=text/plain" "http://localhost:8080/contenthub/contenthub/store?uri=urn:my-content-item3"<hr/></div>curl -i -F "metadata=@metadata_file;type=application/rdf+xml" \
        -F "content=I live in London.;type=text/plain" \
     "http://localhost:8080/contenthub/contenthub/store?uri=urn:my-content-item3"
</pre>
</div>

The following one is an example representing the submission of a ContentItem to another index. The submitted the content will be enhanced by the Enhancement Engines included in the speficied chain.
<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl3" class="curlLine">curl -i -F "content=I live in Berlin.;type=text/plain" "http://localhost:8080/contenthub/myindex/store?uri=urn:my-content-item3&chain=dbpedia-proper-noun"<hr/></div>curl -i -F "content=I live in Berlin.;type=text/plain" \
     "http://localhost:8080/contenthub/myindex/store?uri=urn:my-content-item3&chain=dbpedia-proper-noun"
</pre>
</div>

<hr>


<h4>Create with raw content</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP POST method to create a content item in Contenthub. This is the very basic method to create the content item. The payload of the POST method should include the raw data of the content item to be created. This method stores the content in the default Solr index ("contenthub").</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/{indexName}/store</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>data:</b> Raw data of the content item<br>
      <b>uri:</b>  URI for the content item. If not supplied, Contenthub automatically assigns a unique ID (uri) to the content item.<br/>
      <b>title:</b> The title for the content item. Titles can be used to present summary of the actual content. For example, search results are presented by showing the titles of resultant content items.<br/>
      <b>chain:</b> name of a particular Chain in which the enhancement engines are ordered according to a specific use case or need
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTTP 201 (CREATED) including the full URI representing the newly created ContentItem in the Contenthub.</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl3" class="curlLine">curl -i -X POST -H "Content-Type:text/plain" --data @content_file "http://localhost:8080/contenthub/contenthub/store?uri=urn:my-content-item4&title=contentitem4"<hr/></div>curl -i -X POST -H "Content-Type:text/plain" \
     --data @contentfile \
     "http://localhost:8080/contenthub/contenthub/store?uri=urn:my-content-item4&title=contentitem4"
</pre>
</div>

<hr>

<h4>Create with form elements</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Creates a content item in Contenthub. This method requires the content to be text-based</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/{indexName}/store</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>uri:</b> Optional uri for the content item to be created.<br>
      <b>content:</b> Actual content in text format. If this parameter is supplied, url is ommitted.<br>
      <b>url:</b> URL where the actual content resides. If this parameter is supplied (and content is <code>null</code>, then the content is retrieved from this url.<br>
      <b>title:</b> The title for the content item. Titles can be used to present summary of the actual content. For example, search results are presented by showing the titles of resultant content items.<br/>
      <b>chain:</b> name of a particular Chain in which the enhancement engines are ordered according to a specific use case or need
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTTP 303 (SEE OTHER) including the full URI representing the newly created ContentItem in the Contenthub.</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>

<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl5" class="curlLine">curl -i -X POST --data "content=Ankara is the capital of Turkey&title=Ankara" http://localhost:8080/contenthub/contenthub/store<hr/></div>curl -i -X POST --data "content=Ankara is the capital of Turkey&title=Ankara" \ 
     "http://localhost:8080/contenthub/contenthub/store"
</pre>
</div>

<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl6" class="curlLine">curl -i -X POST --data "url=http://en.wikipedia.org/wiki/Istanbul&title=Istanbul" "http://localhost:8080/contenthub/contenthub/store"<hr/></div>curl -i -X POST --data "url=http://en.wikipedia.org/wiki/Istanbul&title=Istanbul" \
     "http://localhost:8080/contenthub/contenthub/store"
</pre>
</div>
<hr>


<h3>Delete a Content Item</h3>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP DELETE method to delete a content item from Contenhub.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>DELETE /contenthub/{indexName}/store, /contenthub/{indexName}/store/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> URI of the content item to be deleted.</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTTP 200 (OK)</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X DELETE "http://localhost:8080/contenthub/contenthub/store/urn:my-content-item4"</pre>
<pre>curl -i -X DELETE "http://localhost:8080/contenthub/contenthub/store?uri=urn:my-content-item4"</pre>
<hr>


<h3>Cool URI handler</h3>

<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>A redirection to either a browser view, the RDF metadata or the raw binary content</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/{indexName}/store/content/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> The URI of the resource in the Stanbol Contenthub store</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>
      <ul>
        <li>
	      HTTP 307 (TEMPORARY REDIRECT) with a URI varying based on the Accept header<br/>
	      <ul>
	        <li><b>text/html:</b> HTTP 307 (TEMPORARY REDIRECT) to the browser view of the specified ContentItem.</li>
	        <li><b>application/rdf+xml, text/turtle, application/x-turtle, text/rdf+nt, text/rdf+n3 or application/rdf+json:</b> HTTP 307 (TEMPORARY REDIRECT) to the metadata of the specified ContentItem.</li>
	        <li><b>otherwise: </b> HTTP 307 (TEMPORARY REDIRECT) to the raw content of the specified ContentItem.</li>
	      </ul>
	    </li>
	    <li>HTTP 400 (BAD REQUEST) if there is a missing parameter in the request.</li>
        <li>HTTP 404 (NOT FOUND) if there is no ContentItem corresponding to the specified URI.</li>
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
To get the metadata of the content item the following command can be run: 
<pre>curl -i -X GET -H "Accept: application/rdf+json" "http://localhost:8080/contenthub/contenthub/store/content/urn:my-content-item3"</pre>

<hr>

<h3>Downlading metadata or raw content of ContentItems</h3>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Raw data or metadata of the content item can be downloaded.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/{indexName}/store/download/{type}/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>type: </b>It can be <code> "metadata"</code> or <code> "raw"</code>. Based on the type, related parts of the content item will be prepared for download.<br>
      <b>uri: </b>URI of the resource in the Stanbol Contenthub store.<br>
      <b>format: </b>Rdf serialization format of metadata.
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>
      <ul>
        <li>HTTP 200 (OK) with a file containing metadata or raw data of the content item</li>
        <li>HTTP 400 (BAD REQUEST) if there is a missing parameter in the request.</li>
        <li>HTTP 404 (NOT FOUND) if there is no ContentItem corresponding to the specified URI.</li>
      </ul>
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X GET "http://localhost:8080/contenthub/contenthub/store/download/metadata/urn:my-content-item3?format=application%2Fjson"</pre>
<pre>curl -i -X GET "http://localhost:8080/contenthub/contenthub/store/download/raw/urn:my-content-item3"</pre>

<hr>


<h3Viewing metadata of the ContentItem</h3>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Retrieves the metadata of the content item. Generally, metadata contains the enhancements of the content item.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/{indexName}/store/metadata/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> The URI of the resource in the Stanbol Contenthub store</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>
      <ul>
        <li>HTTP 200 (OK) with RDF representation of the metadata of the content item.</li>
        <li>HTTP 400 (BAD REQUEST) if there is a missing parameter in the request.</li>
        <li>HTTP 404 (NOT FOUND) if there is no ContentItem corresponding to the specified URI.</li>
      </ul>
   </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X GET "http://localhost:8080/contenthub/contenthub/store/metadata/urn:my-content-item3"</pre>
<hr>


<h3Viewing the raw content of the ContentItem</h3>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Retrieve the raw content item</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/{indexName}/store/raw/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> The URI of the resource in the Stanbol Contenthub store</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>
      <ul>
        <li>HTTP 200 (OK) with the raw data of the content item</li>
        <li>HTTP 400 (BAD REQUEST) if there is a missing parameter in the request.</li>
        <li>HTTP 404 (NOT FOUND) if there is no ContentItem corresponding to the specified URI.</li>
      </ul>
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X GET "http://localhost:8080/contenthub/contenthub/store/raw/urn:my-content-item3"</pre>

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