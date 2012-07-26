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
<h3>Subresource /query</h3>

<table>
<tbody>
	<tr>
		<th>Description</th>
		<td>Allows to parse JSON serialized field queries to the sites endpoint.</td>
	</tr>
	<tr>
		<th>Request</th>
        <td><code>-X POST -H "Content-Type:application/json" --data "@fieldQuery.json" /entityhub/sites/query<code></td>
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

<h4>Example</h4>

<pre>curl -X POST -H "Content-Type:application/json" --data "@fieldQuery.json" ${it.publicBaseUri}entityhub/sites/query</pre>

<p><em>Note</em>: "@fieldQuery.json" links to a local file that contains the parsed
    Fieldquery (see ection "FieldQuery JSON format" for examples).</p>

    