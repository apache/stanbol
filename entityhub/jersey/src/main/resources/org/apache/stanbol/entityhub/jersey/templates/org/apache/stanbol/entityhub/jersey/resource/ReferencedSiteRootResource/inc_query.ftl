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
<h4>Subresource /query</h4>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>Allows to parse JSON serialized field queries to the sites endpoint.</td>
	</tr>
	<tr>
		<th>Request</th>
        <td><code>-X POST -H "Content-Type:application/json" --data "@fieldQuery.json" /entityhub/site/{siteId}/query<code></td>
	</tr>
	<tr>
		<th>Parameter</th>
		<td>query: the JSON serialised FieldQuery (see section "FieldQuery JSON format" 
           below)</td>
	</tr>
	<tr>
		<th>Produces</th>
        <td>The results of the query serialised in the format as specified by the
        Accept header</td>
	</tr>
</tbody>
</table>

<h5>Example</h5>

<pre>curl -X POST -H "Content-Type:application/json" --data "@fieldQuery.json" ${it.publicBaseUri}entityhub/site/dbpedia/query</pre>

<p><em>Note</em>: "@fieldQuery.json" links to a local file that contains the parsed
    Fieldquery (see ection "FieldQuery JSON format" for examples).</p>

<h4>FieldQuery JSON format:</h4>

<p> The <a href="http://svn.apache.org/repos/asf/incubator/stanbol/trunk/entityhub/generic/servicesapi/src/main/java/org/apache/stanbol/entityhub/servicesapi/query/FieldQuery.java">
FieldQuery</a> is part of the java API defined in the  
<code>org.apache.stanbol.entityhub.servicesapi</code> bundle<p>

<h4>Main Elements</h4> 
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
<h5>Example:</h5>
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

<h4>FieldQuery Constraints:</h4>

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

<h4>Reference Constraint: </h4>

<p>Additional key:</p>
<ul>
    <li><code>value</code>: the value (usually an URI) (required) </li>
</ul>

<h5>Example:</h5>

<p>Search for instances of the type Place as defined in the dbpedia ontology</p>

<code><pre>
{ 
    "type": "reference", 
    "field": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type", 
    "value": "http:\/\/dbpedia.org\/ontology\/Place", 
} 
</pre></code>

<h4>Value Constraint</h4>

<p>Additional keys:</p>
<ul>
    <li><code>value</code>: the value (required)</li>
    <li><code>dataTypes</code>: json array with the data types of the value 
        (by default the dataType is defined by the type of the parsed value)</li>
</ul> 

<h5>Example:</h5>

<p>Search for entities with the rdfs:label "Paris". (Note: one could also use a
TextConstraint for this</p>

<code><pre>
{ 
    "type": "value", 
    "field": "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label", 
    "value": "Paris", 
} 
</pre></code>

<h4>Text Constraint</h4>

<p>Additional key:</p>
<ul>
    <li><code>text</code>: the text to search (required)</li>
    <li><code>languages</code>: json array with the languages to search 
        (default is all languages) </li>
    <li><code>patternType</code>: one of "wildcard", "regex" or "none" 
        (default is "none") </li>
    <li><code>caseSensitive</code>: boolean (default is "false")</li>
</ul>

<h5>Example:</h5>

<p>Searches for entities with an german rdfs:label starting with "Frankf"</p>

<code><pre>
{ 
   "type": "text", 
   "languages": ["de"], 
   "patternType": "wildcard", 
   "text": "Frankf*", 
   "field": "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label" 
}, 
</pre></code>

<h4>Range Constraint: </h4>

<p>Additional key:</p>
<ul>
    <li><code>lowerBound</code>: The lower bound of the range 
        (one of lower and upper bound MUST BE defined) </li>
    <li><code>upperBound</code>: The upper bound of the range 
        (one of lower and upper bound MUST BE defined) </li>
    <li><code>inclusive</code>: used for both upper and lower bound 
        (default is "false") </li>
</ul>

<h5>Example:</h5>

<p>Searches for entities with a population over 1 million. Note that the data type
is automatically detected based on the parsed value (integer in that case)</p>

<code><pre>
{ 
    "type": "range", 
    "field": "http:\/\/dbpedia.org\/ontology\/populationTotal", 
    "lowerBound": 1000000, 
    "inclusive": true, 
}
</pre></code>