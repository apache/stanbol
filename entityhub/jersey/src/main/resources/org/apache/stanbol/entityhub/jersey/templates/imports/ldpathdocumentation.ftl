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
<h3>LD Path Language:</h3>

<p><strong>NOTE:</strong> This only provides a short overview. A much more
complete <a href="http://code.google.com/p/ldpath/wiki/PathLanguage">
documentation</a> is also available on the 
<a href="http://code.google.com/p/ldpath/">LDPath project</a> home.</p>

<h3>Namespace Definitions</h3>

<p>Define shortcut names for URI prefixes, like in SPARQL or N3.</p>

<pre><code>@prefix PREFIX : &lt;URI&gt;
</code></pre>

<p>where PREFIX is a shortcut name for the uri URI.</p>

<p><em>Example:</em></p>

<pre><code>@prefix foaf : &lt;http://xmlns.com/foaf/0.1/&gt;;
</code></pre>

<h3>Field Definitions</h3>

<p>Define fields in the search index to map to path definitions.</p>

<pre><code>FIELDNAME = PATH :: FIELDTYPE FIELDCONF
</code></pre>

<p>where PATH is an RDF path and FIELDTYPE is one of the available field types. 
FIELDCONF is an optional field configuration.</p>

<p><em>Example:</em></p>

<pre><code>title = foaf:name :: xsd:string ;
</code></pre>

<h3>Path Selectors</h3>

<p>The path language supports a number of path selectors that start at the current 
"context node" and return a collection of nodes when applied. The following is 
a short overview over the different selectors, detailed documentation follows 
below:</p>

<ul>
<li><p>Property Selections (URI or prefix:local): select the values of a property</p>

<pre><code>title = foaf:name :: xsd:string ;
title = &lt;http://xmlns.com/foaf/0.1/name&gt; :: xsd:string;
</code></pre></li>
<li><p>Reverse Property Selections (^URI or ^prefix:local)</p>

<pre><code>childs = ^skos:parent :: xsd:string ;
</code></pre></li>
<li><p>Wildcard Selections (*): select the values of all properties</p>

<pre><code>all = * :: xsd:string ;
</code></pre></li>
<li><p>Self Selector (.): select the current context node</p></li>
<li><p>Path Traversal (/): follow a path of selectors recursively</p>

<pre><code>friend = foaf:knows/foaf:name :: xsd:string;
</code></pre></li>
<li><p>Unions (|): join the results of two selections in one collection</p>

<pre><code>friend = foaf:knows/foaf:name | foaf:knows/rdfs:label :: xsd:string;
</code></pre></li>
<li><p>Intersections (&amp;): build the intersection of the results of two selections</p>

<pre><code>topic_interests = foaf:interest &amp; foaf:topic</em>interest :: xsd:anyURI;
</code></pre></li>
<li><p>Recursive Selections ((PATH)+)</p>
<li><p>Tests ([...]): filter the collection based on test criteria</p>

<ul><li>Path Existence Test (PATH): only resources where a subpath yields some value</li>
<li><p>Language Test (@language): only literals of a certain language</p>

<pre><code>title = rdfs:label[@de] | rdfs:label[@none] :: xsd:string ;
</code></pre></li>
<li><p>Type Test (^^xsd:type): only literals of a certain type</p>

<pre><code>decimals = *[^^xsd:decimal] :: xsd:decimal
</code></pre></li>
<li><p>Path Value Test (is): only resources with a subpath yielding a given value</p>

<pre><code>food = foaf:interest[rdf:type is ex:Food] :: xsd:anyURI;
</code></pre></li>
<li><p>Test Conjunction and Disjunction</p>

<pre><code>foodstuff = foaf:interest[rdf:type is ex:Food | rdf:type is ex:Drink] :: xsd:anyURI ;
fluidfood = foaf:interest[rdf:type is ex:Food &amp; rdf:type is ex:Drink] :: xsd:anyURI ;
</code></pre></li>
<li><p>Combinations of Tests</p>

<pre><code>foodstuff = foaf:interest[rdf:type is ex:Food]/rdfs:label[@es] :: xsd:string ;
</code></pre></li>
</ul>
<li><p>Functions (f(...)): apply a function on the values of the selections passed as argument</p>

<pre><code>friends = foaf:knows/fn:concat(foaf:given," ",foaf:surname) :: xsd:string ;
friends = foaf:knows/(fn:concat(foaf:given," ",foaf:surname) | foaf:name) :: xsd:string ;
label = fn:first(skos:prefLabel[@de], skos:prefLabel) :: xsd:string ;
content = fn:removeTags(ex:hasHtmlContent) :: xsd:string ;
title = fn:xpath("//head/title/text()", ex:hasHtmlContent) :: xsd:string ;

//'fn:content' retrieve the human-readable content (e.g. in HTML) t
// hat is associated with a resource,
content = fn:content(foaf:homepage) :: xsd:string ;</p></li></ul></li>
</code></pre></li>
</ul>
