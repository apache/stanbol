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
    <li><code>"selected"</code>: 
        json array with the name of the fields selected by this query </li>
    <li><code>"offset"</code>: 
        the offset of the first result returned by this query </li>
    <li><code>"limit"</code>: 
        the maximum number of results returned </li>
    <li><code>"constraints"</code>: 
        json array holding all the constraints of the query </li>
</ul>
<h4>Example:</h4>
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

<h3>FieldQuery Constraints:</h3>

<p>Constraints are always applied to a field. Currently the implementation is
limited to a single constraint/field. This is an limitation of the implementation
and not a theoretical one.</p>
<p>There are 4 different constraint types.</p>
<ol>
 <li><em>ValueConstraint:</em> Checks if the value of the field is equals to the parsed
    value and data type</li>
 <li><em>TextConstraint:</em> Checks if the value of the field is equals to the parsed
    value, language. It supports also wildcard and regex searches.</li>
 <li><em>RangeConstraint:</em> Checks if the value of the field is within the parsed range</li>
 <li><em>ReferenceConstraint:</em> A special form of the ValueConstraint that defaults the
    data type to references (links to other entities)</li>
</ol>
 
<p>Keys required by all Constraint types:</p>
<ul>
    <li><code>field</code>: the field to apply the constraint</li>
    <li><code>type</code>: the type of the constraint. 
        One of <code>"reference"</code>, <code>"value"</code>, 
        <code>"text"</code> or <code>"range"</code></li>
</ul>

<h3>Reference Constraint: </h3>

<p>Additional key:</p>
<ul>
    <li><code>value</code>: the value (usually an URI) (required) </li>
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

<h3>Value Constraint</h3>

<p>Value Constraints are very similar to Reference Constraints however they can
be used to check values of fields for any data type.<br>
If no data type is defined the data type will be guessed based on the provided
JSON type of the value. For details please see the table below.</p>

<p>Additional keys:</p>
<ul>
    <li><code>value</code>: the value (required)</li>
    <li><code>datatype</code>: the data type of the value as a string. Multiple
        data types can also be parsed by using a JSON array.
        Note that if no datatype is define, the default is guessed based on the 
        type of the parsed value. <br>
        Especially note that string values are mapped to "xsd:string" and not 
        "entityhub:text" as used for natural language texts within the entityhub.
        However users are encouraged anyway to use Text Constraints for filtering
        based on natural languages values.</li>
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

<h3>Text Constraint</h3>

<p>Additional key:</p>
<ul>
    <li><code>text</code>: the text to search (required). If multiple values
        are parsed, that those values are connected by using OR.<br>
        Parsing "Barack Obama" returns Entities that contain "Barack Obama" as
        value for the field. Parsing ["Barack","Obama"] will return all Entities
        that contain any of the two words. Most Sites however will boost results
        that contain both values over such that only contain a single one. 
    </li>
    <li><code>language</code>: the language of the searched text as string.
        Multiple languages can be parsed as JSON array. Parsing "" as language
        will include values with missing language information. If no language is
        defined values in any language will be used.</li>
    <li><code>patternType</code>: one of "wildcard", "regex" or "none" 
        (default is "none") </li>
    <li><code>caseSensitive</code>: boolean (default is "false")</li>
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

<h3>Range Constraint:</h3>

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
        </div>
    </div>
</div>  
