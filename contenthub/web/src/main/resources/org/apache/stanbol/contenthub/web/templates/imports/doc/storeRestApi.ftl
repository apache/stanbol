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
<br>
<h3>The RESTful API of the Contenthub Store</h3>

<h3>Create Content Item</h3>

<h4>Create ContentItem using multipart serialization</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP POST method to create a content item in Contenthub. This method takes a 
    	<a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/enhancer/generic/servicesapi/src/main/java/org/apache/stanbol/enhancer/servicesapi/ContentItem.java">ContentItem</a> object
      	directly. This means that the values provided for this service will be parsed by the multipart mime
	    serialization of Content Items. (see the following links: <a href="http://incubator.apache.org/stanbol/docs/trunk/components/enhancer/contentitem.html#multipart_mime_serialization">Content Item Multipart Serialization</a> and 
      	<a href="http://incubator.apache.org/stanbol/docs/trunk/components/enhancer/enhancerrest.html">Using the multi-part content item RESTful API extensions</a>)
    </td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/store</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>ci:</b> ContentItem to be stored</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>id of the newly created contentitem</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>
      URISyntaxException<br>
      StoreException
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
With following curl command, a ContentItem is created using the data specified in the <b>content</b> part.
Also metadata of the ContentItem is set with the <b>metadata</b> part. Note that the name of the metadata file is set as the URI
of the ContentItem.
<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl0" class="curlLine">curl -F "metadata=@/home/user/Desktop/urn:content-item-sha1-90a5f5ceaee67fb26d877df9bc56ed973f9cf058;type=application/rdf+xml" -F "content=I live in Paris.;type=text/plain" "http://localhost:8080/contenthub/store"<hr/></div>curl -F "metadata=@/home/user/Desktop/paris.txt;type=application/rdf+xml" \ 
     -F "content=I live in Paris.;type=text/plain" \
     "http://localhost:8080/contenthub/store"
</pre>
</div>

<hr>


<h4>Create ContentItem with raw content</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP POST method to create a content item in Contenthub. This is the very basic method to create the content item. The payload of the POST method should include the raw data of the content item to be created.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/store</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>data:</b> Raw data of the content item</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>Redirects to "contenthub/store/content/uri" which shows the content item in the HTML view</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>
      URISyntaxException<br>
      EngineException<br>
      StoreException
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl1" class="curlLine">curl -i -X POST -H "Content-Type:text/plain" --data @/home/user/Documents/test.txt http://localhost:8080/contenthub/store<hr/></div>curl -i -X POST -H "Content-Type:text/plain" \
     --data @/home/user/Documents/test.txt \
     http://localhost:8080/contenthub/store
</pre>
</div>

<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl2" class="curlLine">curl -i -X POST -H "Content-Type:text/plain" --data "I live in Paris." http://localhost:8080/contenthub/store<hr/></div>curl -i -X POST -H "Content-Type:text/plain" \ 
     --data "I live in Paris." \
     http://localhost:8080/contenthub/store
</pre>
</div>

A custom URI can also be specified for the the Content Item as a path parameter as in the example below:

<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl4" class="curlLine">curl -i -X POST -H "Content-Type:text/plain" --data "I live in Paris." http://localhost:8080/contenthub/store/urn:content-item-exampleid1234<hr/></div>curl -i -X POST -H "Content-Type:text/plain" \
     --data "I live in Paris." \
     http://localhost:8080/contenthub/store/urn:content-item-exampleid1234
</pre>
</div>

<hr>


<h4>Create ContentItem with form elements</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Creates a content item in Contenthub based on the given parameters. The content is given either by raw text content or it is 
    	fetched from a url.
	</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/store</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>content:</b> Actual content in text format. If this parameter is supplied, url is ommitted.<br>
      <b>url:</b> URL where the actual content resides. If this parameter is supplied (and content is <code>null</code>, then the content is retrieved from this url.<br>
      <b>constraints:</b> Constraints in JSON format. Constraints are used to add supplementary metadata to the content item. For example, author of the content item may be supplied as {author:"John Doe"}. All constraints are expected to be passed as field value pairs in the JSON object. During the execution of this method, they are transformed into an RDF graph and that graph is added as an additional content part of the content item.<br>
      <b>title:</b> The title for the content item. Titles can be used to present summary of the actual content. For example, search results are presented by showing the titles of resultant content items.<br>
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>Redirects to "contenthub/store/content/{uri}" which shows the content item in the HTML view.</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>
      URISyntaxException<br>
      EngineException<br>
      MalformedURLException<br>
      IOException<br>
      StoreException
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>

<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl5" class="curlLine">curl -i -X POST --data "content=Paris is the capital of France&constraints={author:"John\ Doe"}&title=Paris" http://localhost:8080/contenthub/store<hr/></div>curl -i -X POST --data \
     "content=Paris is the capital of France&constraints={author:"John\ Doe"}&title=Paris" \
     http://localhost:8080/contenthub/store
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
    <td>DELETE /contenthub/store/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> URI of the content item to be deleted.</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTTP OK</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>StoreException</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X DELETE http://localhost:8080/contenthub/store/urn:content-item-exampleid1234</pre>

<hr>


<h3>Subresources</h3>

<h4>Subresource /content</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Cool URI handler for the uploaded resource. Based on the Accept header this service redirects the incoming request to different endpoints in the following way:
    	<ul>
    		<li>If the Accept header contains the "text/html" value it is the request is redirected to the <b>page</b> endpoint so that an HTML document is drawn.</li>
      		<li>If the Accept header one of the RDF serialization formats defined {@link SupportedFormat} annotation, the request is redirected to the <b>metadata</b> endpoint.</li>
      		<li>If the previous two conditions are not satisfied the request is redirected to the <b>raw</b> endpoint.</li>
      	</ul>
    </td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/store/content/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> The URI of the resource in the Stanbol Contenthub</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>Depending on requested media type, return a redirection to either a browser view, the RDF metadata or the raw binary content</td>
  </tr>
</tbody>
</table>

<h4>Example</h4>
<pre>curl -i http://localhost:8080/contenthub/store/content/uri-234231</pre>

<hr>

<h4>Subresource /metadata</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to retrieve or download the metadata i.e enhancements of the content item. If the
		Accept header is compatible with the <b>text/html</b> value the metadata is serialized and included in
		the response using the specified format type, otherwise the metadata is returned as a multipart object.
	</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/store/metadata/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>uri:</b> The URI of the resource in the Stanbol Contenthub store<br>
      <b>format: </b>Rdf serialization format of metadata.
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>text/html or application/octet-stream, according to the accept header</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>
      IOException<br/>
      StoreException
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -H "Accept:text/html" http://localhost:8080/contenthub/store/metadata/sha1-5d85e7c63cc48c01</pre>
<pre>curl -i -H "Accept:application/octet-stream" http://localhost:8080/contenthub/store/metadata/5d85e7c63cc48c0985?format=application%2Fjson</pre>
<hr>


<h4>Subresource /raw</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to retrieve or download the raw content item. If the Accept header is compatible with
      	the <b>text/html</b> value the raw content of the ContentItem included in the response with the
      	<b>text/plain</b> type, otherwise the content is returned as a multipart object.
  	</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/store/raw/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> The URI of the resource in the Stanbol Contenthub store</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>text/html or application/octet-stream, according to the accept header</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>
      IOException<br/>
      StoreException
    </td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -H "Accept:text/html" http://localhost:8080/contenthub/store/raw/testuri2343124</pre>
<pre>curl -i -H "Accept:application/octet-stream" http://localhost:8080/contenthub/store/raw/testuri2343124</pre>

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