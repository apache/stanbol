<#-- 
  <h3>Uploading new content to the Contenthub</h3>

 
  <p>Contenthub lives at the endpoint starting with "contenthub":</p>
  <code>${it.publicBaseUri}contenthub/</code>
  <p>This endpoint automatically forwards to:</p>
  <code>${it.publicBaseUri}contenthub/store/</code>

  <p>The endpoint to which Contenthub automatically forwards includes the name of the default index,
  whose name is "contenthub". That is the reason for two consecutive "contenthub"s in the endpoint.
  Lastly, "store" page provides the storage related functionalities of Contenthub such as document submission.</p>

  <p>You can upload content to the Contenthub for analysis with or without providing the content
   id at your option:</p>
  <ul>
    <li><code>POST</code> content to <code>${it.publicBaseUri}contenthub/store/<strong>content-id</strong></code>
     with <code>Content-Type: text/plain</code>.</li>
    <li><code>GET</code> content with its enhancements from the same URL.</li>
  </ul>
  
  <p><code><strong>content-id</strong></code> can be any valid URI and
   will be used to fetch your item back later. <code><strong>content-id</strong></code>s are unique within Contenthub.</p>

  <p>On a unix-ish box you can use run the following command from
   the top-level source directory to populate the Stanbol Contenthub service with
   sample content items:</p>

  <pre>
  for file in enhancer/data/text-examples/*.txt;
  do
    curl -i -X POST -H "Content-Type:text/plain" -T $file ${it.publicBaseUri}contenthub/store/$(basename $file);
  done
  </pre> 

  Alternatively you can let the Stanbol Contenthub automatically build an id base on the SHA1
  hash of the content by posting it at the root of the Contenthub.
  <ul>
    <li><code>POST</code> content to <code>${it.publicBaseUri}contenthub/store</code>
     with <code>Content-Type: text/plain</code>.</li>
  </ul>
  
  <p>For instance:</p>
<pre>
curl -i -X POST -H "Content-Type:text/plain" \
     --data "The Stanbol enhancer can detect famous cities such as Paris." \
     ${it.publicBaseUri}contenthub/store
    
HTTP/1.1 201 Created
Location: ${it.publicBaseUri}contenthub/store/content/{<code><strong>content-id</strong><code>}
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<h3>Fetching back the original content item and the related enhancements from the Contenthub</h3>

<p>Once the content is created in the Contenthub, you can fetch back either the original content, a HTML summary view or
the extracted RDF metadata by dereferencing the URL:</p>

<pre>
curl -i <strong>-H "Accept: text/plain"</strong> ${it.publicBaseUri}contenthub/store/content/{<code><strong>content-id</strong><code>}

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/store/raw/{<code><strong>content-id</strong><code>}
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<p>
Tutorials on Stanbol Contenthub can be found in the following links:<br/>
<a href="http://incubator.apache.org/stanbol/docs/trunk/contenthub/">Contenhub - One Minute Tutorial</a><br/>
<a href="http://incubator.apache.org/stanbol/docs/trunk/contenthub5min">Contenthub - Five Minutes Tutorial</a>
</p>
-->
<br>
<h3>The RESTful API of the Contenthub Store</h3>

<h3>Create a Content Item</h3>

<h4>Create with raw content</h4>  
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

<hr>

<h4>Create with raw content and user specified uri</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP POST method to create a content item in Contenthub. </td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/store/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>data:</b> Raw data of the content item<br>
      <b>uri:</b>  URI for the content item. If not supplied, Contenthub automatically assigns a unique ID (uri) to the content item.
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>Redirects to "contenthub/store/content/uri" which shows the content item in the HTML view.</td>
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
<div id="curl3" class="curlLine">curl -i -X POST -H "Content-Type:text/plain" --data @/home/user/Documents/test.txt http://localhost:8080/contenthub/store/example1234<hr/></div>curl -i -X POST -H "Content-Type:text/plain" \
     --data @/home/user/Documents/test.txt \
     http://localhost:8080/contenthub/store/example1234
</pre>
</div>

<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl4" class="curlLine">curl -i -X POST -H "Content-Type:text/plain" --data "I live in Paris." http://localhost:8080/contenthub/store/example1234<hr/></div>curl -i -X POST -H "Content-Type:text/plain" \
     --data "I live in Paris." \
     http://localhost:8080/contenthub/store/example1234
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
    <td>POST /contenthub/store</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>content:</b> Actual content in text format. If this parameter is supplied, url is ommitted.<br>
      <b>url:</b> URL where the actual content resides. If this parameter is supplied (and content is <code>null</code>, then the content is retrieved from this url.<br>
      <b>constraints:</b> Constraints in JSON format. Constraints are used to add supplementary metadata to the content item. For example, author of the content item may be supplied as {author: "John Doe"}. Then, this constraint is added to the Solr and will be indexed if the corresponding Solr schema includes the author field. Solr indexed can be created/adjusted through LDPath programs.<br>
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
<div id="curl5" class="curlLine">curl -i -X POST --data "content=Paris is the capital of France&constraints={type:city}&title=Paris" http://localhost:8080/contenthub/store<hr/></div>curl -i -X POST --data \
     "content=Paris is the capital of France&constraints={type:city}&title=Paris" \
     http://localhost:8080/contenthub/store
</pre>
</div>

<hr>


<h4>Create with file</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP POST method to create a content item from file. File is read and loaded as the actual content.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/store</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>file:</b> File which contains the content for the content item.<br>
      <b>disposition:</b> Additional information about the file parameter<br>
      <b>jsonCons:</b> Constraints in JSON format. Constraints are used to add supplementary metadata to the content item. For example, author of the content item may be supplied as {author: "John Doe"}. Then, this constraint is added to the Solr and will be indexed if the corresponding Solr schema includes the author field. Solr indexed can be created/adjusted through LDPath programs.<br>
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
<div id="curl6" class="curlLine">curl -i -F "file=@/home/mrc/Desktop/test.txt" -F "constraints={type:city}" -F "title=Paris" http://localhost:8080/contenthub/store<hr/></div>curl -i -F "file=@/home/mrc/Desktop/test.txt" \
        -F "constraints={type:city}" \ 
        -F "title=Paris" \
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
<pre>curl -i -X DELETE http://localhost:8080/contenthub/store/example-uri-1234</pre>

<hr>


<h3>Subresources</h3>

<h4>Subresource /content</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Cool URI handler for the uploaded resource.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/store/content/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> The URI of the resource in the Stanbol Contenthub store</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>Depending on requested media type, it can produce HTML, RDF or raw binary content</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i http://localhost:8080/contenthub/store/content/uri-234231</pre>

<hr>


<h4>Subresource /download</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Raw data or metadata of the content item can be downloaded.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/store/download/{type}/{uri}</td>
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
    <td>200 with a file containing metadata or raw data of the content item</td>
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
<pre>curl http://localhost:8080/contenthub/store/download/metadata/5d85e7c63cc48c0985?format=application%2Fjson</pre>
<pre>curl http://localhost:8080/contenthub/store/download/raw/5d85e7c63cc48c01b8d4?format=application%2Frdf%2Bxml</pre>

<hr>


<h4>Subresource /metadata</h4>  
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Retrieves the metadata of the content item. Generally, metadata contains the enhancements of the content item.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/store/metadata/{uri}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>uri:</b> The URI of the resource in the Stanbol Contenthub store</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>200 with RDF representation of the metadata of the content item.</td>
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
<pre>curl http://localhost:8080/contenthub/store/metadata/sha1-5d85e7c63cc48c01</pre>
<hr>


<h4>Subresource /raw</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>Retrieve the raw content item</td>
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
    <td>200 with the raw data of the content item</td>
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
<pre>curl http://localhost:8080/contenthub/store/raw/testuri2343124</pre>

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