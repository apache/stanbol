  <h3>Uploading new content to the Contenthub</h3>

<#--  
  <p>Contenthub lives at the endpoint starting with "contenthub":</p>
  <code>${it.publicBaseUri}contenthub/</code>
  <p>This endpoint automatically forwards to:</p>
  <code>${it.publicBaseUri}contenthub/${it.indexName}/store/</code>

  <p>The endpoint to which Contenthub automatically forwards includes the name of the default index,
  whose name is "contenthub". That is the reason for two consecutive "contenthub"s in the endpoint.
  Lastly, "store" page provides the storage related functionalities of Contenthub such as document submission.</p>

  <p>You can upload content to the Contenthub for analysis with or without providing the content
   id at your option:</p>
  <ul>
    <li><code>POST</code> content to <code>${it.publicBaseUri}contenthub/${it.indexName}/store/<strong>content-id</strong></code>
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
    curl -i -X POST -H "Content-Type:text/plain" -T $file ${it.publicBaseUri}contenthub/${it.indexName}/store/$(basename $file);
  done
  </pre> 

  Alternatively you can let the Stanbol Contenthub automatically build an id base on the SHA1
  hash of the content by posting it at the root of the Contenthub.
  <ul>
    <li><code>POST</code> content to <code>${it.publicBaseUri}contenthub/${it.indexName}/store</code>
     with <code>Content-Type: text/plain</code>.</li>
  </ul>
  
  <p>For instance:</p>
<pre>
curl -i -X POST -H "Content-Type:text/plain" \
     --data "The Stanbol enhancer can detect famous cities such as Paris." \
     ${it.publicBaseUri}contenthub/${it.indexName}/store
    
HTTP/1.1 201 Created
Location: ${it.publicBaseUri}contenthub/${it.indexName}/store/content/{<code><strong>content-id</strong><code>}
Content-Length: 0
Server: Jetty(6.1.x)
</pre>

<h3>Fetching back the original content item and the related enhancements from the Contenthub</h3>

<p>Once the content is created in the Contenthub, you can fetch back either the original content, a HTML summary view or
the extracted RDF metadata by dereferencing the URL:</p>

<pre>
curl -i <strong>-H "Accept: text/plain"</strong> ${it.publicBaseUri}contenthub/${it.indexName}/store/content/{<code><strong>content-id</strong><code>}

HTTP/1.1 307 TEMPORARY_REDIRECT
Location: ${it.publicBaseUri}contenthub/${it.indexName}/store/raw/{<code><strong>content-id</strong><code>}
Content-Length: 0
Server: Jetty(6.1.x)
</pre>
-->
<p>
Tutorials on Stanbol Contenthub can be found in the following links:<br/>
<a href="http://incubator.apache.org/stanbol/docs/trunk/contenthub/">Contenhub - One Minute Tutorial</a><br/>
<a href="http://incubator.apache.org/stanbol/docs/trunk/contenthub/contenthub5min">Contenthub - Five Minutes Tutorial</a>
</p>

<h4>The RESTful API of the Contenthub</h4>

<#-- Will be written by Meric -->
