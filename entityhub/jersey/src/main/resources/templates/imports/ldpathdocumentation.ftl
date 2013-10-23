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
<div class="docu"> 
    <div class="collapsed">
        <h3 id="ldpathDocTitle" class="docuTitle">
            LD Path Language Documentation:</h3>
        <script>
            $("#ldpathDocTitle").click(function () {
              $("#ldpathDocTitle").parent().toggleClass("collapsed");
            }); 
        </script>
        <div class="docuCollapsable">

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

<p><em>Predefined Namespaces:</em></p>
<ul>
<li><strong>entityhub</strong>: http://www.iks-project.eu/ontology/rick/model/</li>
<li><strong>entityhub-query</strong>: http://www.iks-project.eu/ontology/rick/query/</li>
<li><strong>xsd</strong>: http://www.w3.org/2001/XMLSchema#</li>
<li><strong>xsi</strong>: http://www.w3.org/2001/XMLSchema-instance#</li>
<li><strong>xml</strong>: http://www.w3.org/XML/1998/namespace#</li>
<li><strong>rdf</strong>: http://www.w3.org/1999/02/22-rdf-syntax-ns#</li>
<li><strong>rdfs</strong>: http://www.w3.org/2000/01/rdf-schema#</li>
<li><strong>owl</strong>: http://www.w3.org/2002/07/owl#</li>
<li><strong>atom</strong>: http://www.w3.org/2005/Atom</li>
<li><strong>cmis</strong>: http://docs.oasis-open.org/ns/cmis/core/200908/</li>
<li><strong>cmis-ra</strong>: http://docs.oasis-open.org/ns/cmis/restatom/200908/</li>
<li><strong>jcr</strong>: http://www.jcp.org/jcr/1.0/</li>
<li><strong>jcr-sv</strong>: http://www.jcp.org/jcr/sv/1.0/</li>
<li><strong>jcr-nt</strong>: http://www.jcp.org/jcr/nt/1.0/</li>
<li><strong>jcr-mix</strong>: http://www.jcp.org/jcr/mix/1.0/</li>
<li><strong>geo</strong>: http://www.w3.org/2003/01/geo/wgs84_pos#</li>
<li><strong>georss</strong>: http://www.georss.org/georss/</li>
<li><strong>gml</strong>: http://www.opengis.net/gml/</li>
<li><strong>dc-elements</strong>: http://purl.org/dc/elements/1.1/</li>
<li><strong>dc</strong>: http://purl.org/dc/terms/</li>
<li><strong>foaf</strong>: http://xmlns.com/foaf/0.1/</li>
<li><strong>vCal</strong>: http://www.w3.org/2002/12/cal#</li>
<li><strong>vCard</strong>: http://www.w3.org/2001/vcard-rdf/3.0#</li>
<li><strong>skos</strong>: http://www.w3.org/2004/02/skos/core#</li>
<li><strong>sioc</strong>: http://rdfs.org/sioc/ns#</li>
<li><strong>sioc-types</strong>: http://rdfs.org/sioc/types#</li>
<li><strong>dc-bio</strong>: http://purl.org/vocab/bio/0.1/</li>
<li><strong>rss</strong>: http://purl.org/rss/1.0/</li>
<li><strong>gr</strong>: http://purl.org/goodrelations/v1#</li>
<li><strong>swrc</strong>: http://swrc.ontoware.org/ontology#</li>
<li><strong>dbp-ont</strong>: http://dbpedia.org/ontology/</li>
<li><strong>dbp-prop</strong>: http://dbpedia.org/property/</li>
<li><strong>geonames</strong>: http://www.geonames.org/ontology#</li>
<li><strong>cc</strong>: http://creativecommons.org/ns#</li>
<li><strong>schema</strong>: http://schema.org/</li>
</ul>


<h3>Field Definitions</h3>

<p>Define fields in the search index to map to path definitions.</p>

<pre><code>[FIELDNAME =] PATH [:: FIELDTYPE] [FIELDCONF]
</code></pre>

<p>where</p> <ul>
<li> <strong>FIELDNAME</strong>: The name of the field used to store the values.
If not provided the first element of the PATH is used as field name. If the PATH
does not provide an unique field name (e.g. if unions or intersections are used)
than FIELDNAME is required. This field supports '&lt;{uri}&gt;', 
'{prefix}:{localname}' as well as '{name}'.
<li> <strong>PATH</strong>: the RDF path (see following sections)
<li> <strong>FIELDTYPE</strong>: The data type for the selected values. Type
conversions are supported (e.g. parsing numbers, dates ... from strings). If not
present the selected values are not converted.
<li> <strong>FIELDCONF</strong>: Allows to provide additional configurations for
the field. Currently not used - and ignored - by the Entityhub.
</ul>

<p><em>Examples:</em></p>

<pre><code>title = foaf:name :: xsd:string ;
schema:name = rdfs:label[@en];
geo:lat; geo:long;
&lt;urn:my.company:label.private&gt; = skos:hiddenLabel;
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
<pre><code>skos:broaderTransitive = (skos:broader | ^skos:narrower)+;
</code></pre></li>

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
relatedPersons = (* | dc:subject/^dc:subject)[rdf:type is dbp-ont:Person]/rdfs:label[@en] :: xsd:string;
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

        </div>
    </div>
</div>  
