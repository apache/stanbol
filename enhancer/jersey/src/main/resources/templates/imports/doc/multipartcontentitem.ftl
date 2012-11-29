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

<h3>MultiPart ContentItem support</h3>

<p>This extension adds support for MultiPart ContentItems to the RESTful API
of the Stanbol Enhancer. (see also 
<a href="https://issues.apache.org/jira/browse/STANBOL-481">STANBOL-481</a>)</p>
<#-- TODO: replace with a link to the Documentation as soon as available on the
Stanbol homepage. -->

<ul>
<li><code>outputContent=[mediaType]</code>: Allows to specify the Mimetypes
of content included within the Response of the Stanbol Enhancer. This parameter
supports wild cards (e.g. '*' ... all, 'text/*'' ... all text versions, 
'text/plain' ... only the plain text version). This parameter can be used
multiple times.<br>
Responses to requests with this parameter will be encoded as 
<code>multipart/from-data</code>. If the "Accept" header of the request is not
compatible to <code>multipart/from-data</code> it is assumed as a #
<code>400 BAD_REQUEST</code>. The selected content variants will 
be included in a content part with the name "content" and the Mimetype 
<code>multipart/alternate</code>.</li>

<li><code>omitParsed=[true/false]</code>: Makes only sense in combination with 
the <code>outputContent</code> parameter. This allows to exclude all
content included in the request from the response. A typical combination is
<code>outputContent=*/*&omitParsed=true</code>. The default value of this
parameter is <code>false</code></li>

<li><code>outputContentPart=[uri/'*']</code>: This parameter allows to
explicitly include content parts with a specific URI in the response. Currently
this only supports ContentParts that are stored as RDF graphs.<br>
See the developer documentation for ContentItems for more information about
ContentParts.<br>
Responses to requests with this parameter will be encoded as 
<code>multipart/from-data</code>. If the "Accept" header of the request is not
compatible to <code>multipart/from-data</code> it is assumed as a #
<code>400 BAD_REQUEST</code>. The selected content parts will be included as
MIME parts. The URI of the part will be used as name. Such parts will be added
after the "metadata" and the "content" (if present).</li>

<li><code>omitMetadata=[true/false]</code>: This allows to enable/disable the
inclusion of the metadata in the response. The default is <code>false</code>.<br>
Typically <code>omitMetadata=true</code> is used when users want to use the
Stanbol Enhancer just to get one or more ContentParts as an response. Note that
Requests that use an <code>Accept: {mimeType}</code> header AND 
<code>omitMetadata=true</code> will directly return the content verison of 
<code>{mimeType}</code> and NOT wrap the result as 
<code>multipart/from-data</code></li>

<li><code>rdfFormat=[rdfMimeType]</code>: This allows for requests that result
in <code>multipart/from-data</code> encoded responses to specify the used RDF
serialization format. Supported formats and defaults are the same as for
normal Enhancer Requests. 
</ul>

<p><code>multipart/from-data</code> can also be used as <code>Content-Type</code>
for requests to parsed multiple content variants or pre-existing metadata
(such as user tags). See the documentation provided by 
<a href="https://issues.apache.org/jira/browse/STANBOL-481">STANBOL-481</a>
for details on how to represent content items as Multipart MIME.</p>
</p>
<#-- TODO: replace with a link to the Documentation as soon as available on the
Stanbol homepage. -->

<h4>Examples</h4>

<p>The following examples show some typical usages of the MultiPart ContentItem
RESTful API. For better readability the values of the query parameters are
not URLEncoded.</p>

<p>Return Metadata and transformed Content versions</p>
<pre>
curl -v -X POST -H "Accept: multipart/from-data" \
    -H "Content-type: text/html; charset=UTF-8"  \
    --data "&lt;html&gt;&lt;body&gt;&lt;p&gt;The Stanbol enhancer \
           can detect famous cities such as Paris and people such \
           as Bob Marley..&lt;/p&gt;&lt;/body&gt;&lt;/html&gt;" \
    "${it.serviceUrl}?outputContent=*/*&omitParsed=true&rdfFormat=application/rdf%2Bxml"
</pre> 
<p>This will result in an Response with the mime type 
<code>"Content-Type: multipart/from-data; charset=UTF-8; boundary=contentItem"</code>
and the Metadata as well as the plain text version of the parsed HTML document
as content.</p>
<pre>
    --contentItem
    Content-Disposition: form-data; name="metadata"; filename="urn:content-item-sha1-76e44d4b51c626bbed38ce88370be88702de9341"
    Content-Type: application/rdf+xml; charset=UTF-8;
    Content-Transfer-Encoding: 8bit

    &lt;rdf:RDF
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    [..the metadata formatted as RDF+XML..]
    &lt;/rdf:RDF&gt;

    --contentItem
    Content-Disposition: form-data; name="content"
    Content-Type: multipart/alternate; boundary=contentParts; charset=UTF-8
    Content-Transfer-Encoding: 8bit

    --contentParts
    Content-Disposition: form-data; name="urn:metaxa:plain-text:2daba9dc-21f6-7ea1-70dd-a2b0d5c6cd08"
    Content-Type: text/plain; charset=UTF-8
    Content-Transfer-Encoding: 8bit

    John Smith was born in London.
    --contentParts--

    --contentItem--
</pre> 

<p>This request will directly return the text/plain version</p>
<pre>
curl -v -X POST -H "Accept: text/plain" \
    -H "Content-type: text/html; charset=UTF-8" \
    --data "&lt;html&gt;&lt;body&gt;&lt;p&gt;The Stanbol enhancer \
           can detect famous cities such as Paris and people such \
           as Bob Marley.&lt;/p&gt;&lt;/body&gt;&lt;/html&gt;" \
    "${it.serviceUrl}?omitMetadata=true"
</pre> 
<p>The response will be of type <code>text/plain</code> and return the string
<code>"John Smith was born in London."</code>.