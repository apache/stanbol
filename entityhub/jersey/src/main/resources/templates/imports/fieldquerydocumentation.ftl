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
        <h3 id="fqDocTitle" class="docuTitle">
            FieldQuery Documentation:</h3>
        <script>
            $("#fqDocTitle").click(function () {
              $("#fqDocTitle").parent().toggleClass("collapsed");
            }); 
        </script>
        <div class= "docuCollapsable">

<p> The <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/entityhub/generic/servicesapi/src/main/java/org/apache/stanbol/entityhub/servicesapi/query/FieldQuery.java">
FieldQuery</a> is part of the java API defined in the  
<code>org.apache.stanbol.entityhub.servicesapi</code> bundle<p>

<h3>Main Elements</h3> 
<ul>
    <li><b><code>"selected"</code></b>: 
        json array with the name of the fields selected by this query </li>
    <li><b><code>"offset"</code></b>: 
        the offset of the first result returned by this query </li>
    <li><b><code>"limit"</code></b>: 
        the maximum number of results returned </li>
    <li><b><code>"constraints"</code></b>: 
        json array holding all the constraints of the query </li>
    <li><b><code>"ldpath"</code></b>: 
        <a href="http://code.google.com/p/ldpath/wiki/PathLanguage">LDpath program </a>
        that is executed for all results of the query. More powerful alternative 
        to the <code>"selected"</code> parameter to define returned information 
        for query results.</li>
</ul>
<h4>Examples:</h4>
<p>Simple Field Query that selects rdfs:label and rdf:type with no offset 
that returns at max three results. Constraints are skipped</p>
<code><pre>
{
    "selected": [ 
        "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label", 
        "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type"], 
    "offset": "0", 
    "limit": "3", 
    "constraints": [...]
}
</pre></code>
<p>The following example uses an LDPath program to select the rdfs:type and 
the rdfs:labels as schema:name. The offset is set to 5 and a maximum of 5 results
are returned. This is similar the 2nd page if the number of items is set to 5. <p>
<code><pre>
{
    "ldpath": "schema:name = rdfs:label;rdf:type;", 
    "offset": "5", 
    "limit": "5", 
    "constraints": [...]
}
</pre></code>

<h3>FieldQuery Constraints:</h3>

<p>Constraints are always applied to a field. Currently the implementation is
limited to a single constraint/field. This is an limitation of the implementation
and not a theoretical one.</p>

<p>While there are five different Constraint types the following attributes
are required by all types.</p>
<ul>
    <li><b><code>field</code></b>: the field to apply the constraint.</li>
    <li><b><code>type</code></b>: the type of the constraint. 
        One of <code>"reference"</code>, <code>"value"</code>, 
        <code>"text"</code>, <code>"range"</code> or <code>"similarity"</code>
    </li>
</ul>

<p> In addition the following optional attributes are supported by all constraints </p>
<ul>
    <li><b><code>boost</code></b>: Allows to define a boost for a constraint. If
    supported boosts will influence the ranking of query results. The boost value
    MUST BE a number <code>&gt;= 0</code>. The default is <code>1</code>.</li>
</ul>



<p>There are 4 different constraint types.</p>
<ol>
 <li><em><a href="#value-constraint">ValueConstraint</a>:</em> 
     Checks if the value of the field is equals to the parsed
     value and data type</li>
 <li><em><a href="#reference-constraint">ReferenceConstraint</a>:</em> 
     A special form of the ValueConstraint that defaults the
     data type to references (links to other entities)</li>
 <li><em><a href="#text-constraint">TextConstraint</a>:</em> 
     Checks if the value of the field is equals to the parsed
     value, language. It supports also wildcard and regex searches.</li>
 <li><em><a href="#range-constraint">RangeConstraint</a>:</em> 
     Checks if the value of the field is within the parsed range</li>
 <li><em><a href="#similarity-constraint">SimilarityConstraint</a>:</em> 
     Checks if the value of the field is within the parsed range</li>
</ol>
 

<h3 id="reference-constraint">Reference Constraint: </h3>

<p>Additional key:</p>
<ul>
    <li><b><code>value</code></b>(required): the URI value(s). For a single value a
    string can be used. Multiple values need to be parsed as JSON array</li>
    <li><b><code>mode</code></b>: If multiple values are parsed this can be used
    to specify if query results must have "<code>any</code>" or "<code>all</code>"
    parsed values (default: "<code>any</code>")
</ul>

<h4>Example:</h4>

<p>Search for instances of the type Place as defined in the dbpedia ontology</p>

<code><pre>
{ 
    "type": "reference", 
    "field": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type", 
    "value": "http:\/\/dbpedia.org\/ontology\/Place", 
} 
</pre></code>

<p id="reference-constraint-example2">
Search Entities that link to all of the following Entities. NOTE that the
field "<code>http://stanbol.apache.org/ontology/entityhub/query#references</code>
is special as it will cause a search in any outgoing relation. See the section
<a href="#special-fields">special fields</a> for details</p>

<code><pre>
{ 
    "type": "reference", 
    "field": "http:\/\/stanbol.apache.org\/ontology\/entityhub\/query#references", 
    "value": [
            "http:\/\/dbpedia.org\/resource\/Category:Capitals_in_Europe",
            "http:\/\/dbpedia.org\/resource\/Category:Host_cities_of_the_Summer_Olympic_Games",
            "http:\/\/dbpedia.org\/ontology\/City"
       ],
    "mode": "all"
} 
</pre></code>

<h3 id="value-constraint">Value Constraint</h3>

<p>Value Constraints are very similar to Reference Constraints however they can
be used to check values of fields for any data type.<br>
If no data type is defined the data type will be guessed based on the provided
JSON type of the value. For details please see the table below.</p>

<p>Additional keys:</p>
<ul>
    <li><b><code>value</code></b>(required): the value(s). For multiple values
        a JSON array must be used.</li>
    <li><b><code>datatype</code></b>: the data type of the value as a string. 
        Multiple data types can also be parsed by using a JSON array.
        Note that if no datatype is define, the default is guessed based on the 
        type of the parsed value. <br>
        Especially note that string values are mapped to "xsd:string" and not 
        "entityhub:text" as used for natural language texts within the entityhub.
        Users that want to query for natural language text values should use
        TextConstraints instead.</li>
    <li><b><code>mode</code></b>: If multiple values are parsed this can be used
        to specify if query results must have "<code>any</code>" or "<code>all</code>"
        parsed values (default: "<code>any</code>"). For an usage example see the
        <a href="#reference-constraint-example2"> 2nd reference constraint example</a>
</ul> 

<h4>Example:</h4>

<p>Search for all entities with an altitude of 34 meter. Note that a String is
parsed as value, but the datatype is explicitly set to 'xsd:integer'</p>

<code><pre>
{
   "selected": [
       "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label"],
   "offset": "0",
   "limit": "3",
   "constraints": [{
        "type": "value",
        "value": "34",
        "field": "http:\/\/www.w3.org\/2003\/01\/geo\/wgs84_pos#alt",
        "datatype": "xsd:int"
    }]
}
</pre></code>

<p> The same can be achieved by parsing numerical 34 and not specifying the
datatype. In this case "xsd:interger" would be guessed based on the provided
value. Note however that this would not work for "xsd:long".</p>

<code><pre>
   {
    "type": "value",
    "value": 34,
    "field": "http:\/\/www.w3.org\/2003\/01\/geo\/wgs84_pos#alt",
    }
</pre></code>

<p>Expected Results on DBPedia.org for this query include Berlin and Baghdad 
</p>

<h3 id="text-constraint">Text Constraint</h3>

<p>Additional key:</p>
<ul>
    <li><b><code>text</code></b>(required): the text to search. Multiple values
        can be parsed by using a JSON array. Note that multiple values are
        considerd optional. (e.g. parsing "Barack Obama" returns Entities that 
        contain both "Barack" and "Obama" while parsing ["Barack","Obama"] 
        will also return documents with any of the two words; Also combinations
        like ["Barack Obama","USA","United States"] are allowed)
    </li>
    <li><code>language</code>: the language of the searched text as string.
        Multiple languages can be parsed as JSON array. Parsing "" as language
        will include values with missing language information. If no language is
        defined values in any language will be used.</li>
    <li><code>patternType</code>: one of "wildcard", "regex" or "none" 
        (default is "none") </li>
    <li><code>caseSensitive</code>: boolean (default is "false")</li>
    <li><code>proximityRanking</code>: boolean (default is undefined). This tells
        Sites that the proximity of parsed texts should be used for ranking. The
        default is undefined and may depend on the actual Site executing the
        query</li>
</ul>

<h4>Example:</h4>

<p>
(1) Searches for entities with an german rdfs:label starting with "Frankf"<br>
(2) Searches for entities that contain "Frankfurt" OR "Main" OR "Airport" in
any language
Typically the "Frankfurt am Main Airport" should be ranked first because it
contains all the optional terms.
</p>

<code><pre>
{ 
   "type": "text", 
   "language": "de", 
   "patternType": "wildcard", 
   "text": "Frankf*", 
   "field": "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label" 
}

{ 
   "type": "text", 
   "text": ["Frankfurt","Main","Airport"] 
   "field": "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label" 
}, 
</pre></code>
<p>Expected Results on DBPedia.org for (1) include "Frankfurt am Main", 
"Eintracht Frankfurt" and "Frankfort, Kentucky" and for (2) 
the Airport of Frankfurt am Main, Frankfurt as well as Airport. 
</p>

<h3 id="range-constraint">Range Constraint:</h3>

<p>Additional key:</p>
<ul>
    <li><code>lowerBound</code>: The lower bound of the range 
        (one of lower and upper bound MUST BE defined) </li>
    <li><code>upperBound</code>: The upper bound of the range 
        (one of lower and upper bound MUST BE defined) </li>
    <li><code>inclusive</code>: used for both upper and lower bound 
        (default is "false") </li>
</ul>

<h4>Example:</h4>

<p>The following Query combines two range constraints and a reference constraint
to search for cities with more than one million inhabitants that are more than
1000 meter above sea level.</p>
<p>Note that the range for the population needs to parse the datatype "xsd:long"
because otherwise the parsed value would be converted the "xsd:integer".</p>

<code><pre>
{
   "selected": [
       "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label",
       "http:\/\/dbpedia.org\/ontology\/populationTotal",
       "http:\/\/www.w3.org\/2003\/01\/geo\/wgs84_pos#alt"],
   "offset": "0",
   "limit": "3",
   "constraints": [{ 
        "type": "range", 
        "field": "http:\/\/dbpedia.org\/ontology\/populationTotal", 
        "lowerBound": 1000000,
        "inclusive": true,
        "datatype": "xsd:long"
    },{ 
        "type": "range", 
        "field": "http:\/\/www.w3.org\/2003\/01\/geo\/wgs84_pos#alt", 
        "lowerBound": 1000,
        "inclusive": true,
    },{ 
        "type": "reference", 
        "field": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type", 
        "value": "http:\/\/dbpedia.org\/ontology\/City", 
    }]
}
</pre></code>

<p>Expected Results on DBPedia.org include Mexico City, Bogota and Quito. 
</p>

<p> The following query searches for persons born in 1946 </p>

<code><pre>
{
    "selected": [
        "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label",
        "http:\/\/dbpedia.org\/ontology\/birthDate",
        "http:\/\/dbpedia.org\/ontology\/deathDate"],
    "offset": "0",
    "limit": "3",
    "constraints": [{ 
        "type": "range", 
        "field": "http:\/\/dbpedia.org\/ontology\/birthDate", 
        "lowerBound": "1946-01-01T00:00:00.000Z",
        "upperBound": "1946-12-31T23:59:59.999Z",
        "inclusive": true,
        "datatype": "xsd:dateTime"
    },{ 
        "type": "reference", 
        "field": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type", 
        "value": "http:\/\/dbpedia.org\/ontology\/Person", 
    }]
}
</pre></code>

<p>Expected Results on DBPedia.org include Bill Clinton, George W. Bush and
Donald Trump.</p>

<h3 id="similarity-constraint">Similarity Constraint: </h3>
<p>This constaint allows to select entities similar to the parsed context. This
constraint is curretly only supported by the Solr based storage of the Entityhub.
It can not be implemented on storages that use SPARQL for search.<br>
NOTE also that only a single Similarity Constraint can be used per Field Query.</p>
<p>Additional key:</p>
<ul>
    <li><b><code>context</code></b>(required): The text used as context to search
    for similar entities. Users can parse values form single words up to
    the text of the current section or an whole document.</li>
    <li><b><code>addFields</code></b>: This allows to parse additional fields
    (properties) used for the similarity search. This fields will be added to
    the value of the "<code>field</code>".
</ul>

<h4>Example:</h4>

<p>This example combines a filter for Entities with the type Place with an
similarity search for "Wolfgang Amadeus Mozart". The field
<code>http://stanbol.apache.org/ontology/entityhub/query#fullText</code> is 
a <a href="#special-fields">special field</a> that allows to search the full
text (all textual and <code>xsd:string</code> values) of an Entity.</p>

<code><pre>
{ 
   "type": "reference", 
   "value": "http:\/\/dbpedia.org\/ontology\/Place", 
   "field": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type",
},
{ 
   "type": "similarity", 
   "context": "Wolfgang Amadeus Mozart", 
   "field": "http:\/\/stanbol.apache.org\/ontology\/entityhub\/query#fullText",
}
</pre></code>

<p>Expected results with the default DBpedia dataset include Salzurg. However
because the default dataset only includes the short rdfs:comment texts results
of similarity searches are very limited. Typically the use of similarity 
searches needs already considered when indexing data sets.</p>

<h3 id="special-fields">Special Fields</h3>

<p>Currently the following special fields are defined</p>
<ul>
<li><b><code>http://stanbol.apache.org/ontology/entityhub/query#fullText</code></b>:
    Allows to search within the all natuaral langauge and <code>xsd:string</code>
    values that are linked with the Entity. This field is especially usefull for 
    <a href="#text-constraint">Text Constraints</a> and 
    <a href="#similarity-constraint">Similarity Constraint</a> searches.<br>
    NOTE that for text queries language constrains may be ignored as the full text
    field MAY NOT be able to support language constraints.</li>
<li><b><code>http://stanbol.apache.org/ontology/entityhub/query#references</code></b>:
    Allows to search far all entities referenced by this Entity. This includes
    other entities and <code>xsd:anyURI</code> values (e.g. foaf:homepage values).
    Because if this <a href="#reference-constraint">Reference Constraints</a>
    applied to this field are queries for the semantic context of an Entity.</li>
</ul>

        </div>
    </div>
</div>  
