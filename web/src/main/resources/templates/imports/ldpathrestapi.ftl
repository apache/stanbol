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

<h3>The RESTful API of the Contenthub LDPath</h3>

<h4>Retrieve All Programs</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method which returns all LDPath programs residing in Contenthub. LDPath programs are uniquely dentified by their names. Returning JSON string presents each LDPath program in string format aligned with its name.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/ldpath</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>JSON string of <code>name:program</code> pairs.</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i http://localhost:8080/contenthub/ldpath</pre>

<hr>


<h4>Submit Program</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP POST method which saves an LDPath program into the persistent store of Contenthub.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>POST /contenthub/ldpath/program</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td>
      <b>programName:</b> Unique name to identify the LDPath program<br>
      <b>program:</b> The LDPath program.
    </td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTTP OK(200) or BAD REQUEST(400)</td>
  </tr>
  <tr>
    <th>Throws</th>
    <td>LDPathException</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<div class="preParent">
  <img onClick="javascript:getLine(this.parentNode);" class="copyImg" src="${it.staticRootUrl}/contenthub/images/copy_icon_16.png" title="Get command in a single line" />
<pre>
<div id="curl1" class="curlLine">curl -i -X POST -d "name=universities&program= @prefix dbp-ont : &lt;http://dbpedia.org/ontology/&gt;; city = dbp-ont:city / rdfs:label :: xsd:string; country = dbp-ont:country / rdfs:label :: xsd:string; president = dbp-ont:president / rdfs:label :: xsd:string; numberOfStudent = dbp-ont:numberOfStudents :: xsd:int;" "http://localhost:8080/contenthub/ldpath/program"<hr/></div>curl -i -X POST -d "name=universities&program= \
                    @prefix dbp-ont : &lt;http://dbpedia.org/ontology/&gt;; \
                    city = dbp-ont:city / rdfs:label :: xsd:string; \
                    country = dbp-ont:country / rdfs:label :: xsd:string; \
                    president = dbp-ont:president / rdfs:label :: xsd:string; \
                    numberOfStudent = dbp-ont:numberOfStudents :: xsd:int;" \
                    "http://localhost:8080/contenthub/ldpath/program"
</pre>
</div>
<hr>


<h4>Get Program By Name</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to retrieve an LDPath program, given its name.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/ldpath/program</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>name:</b> The name of the LDPath program to be retrieved.</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>LDPath program in String format or HTTP NOT FOUND(404)</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i "http://localhost:8080/contenthub/ldpath/program?name=universities"</pre>

<hr>


<h4>Delete Program</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP DELETE method to delete an LDPath program.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>DELETE /contenthub/ldpath/program/{name}</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>name:</b> The name of the LDPath program.</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTTP OK(200)</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i -X DELETE http://localhost:8080/contenthub/ldpath/program/universities</pre>

<hr>


<h4>Check existance of LdPath Program</h4>
<table>
<tbody>
  <tr>
    <th>Description</th>
    <td>HTTP GET method to check whether an LDPath program exists in Contenthub or not.</td>
  </tr>
  <tr>
    <th>Request</th>
    <td>GET /contenthub/ldpath/exists</td>
  </tr>
  <tr>
    <th>Parameter</th>
    <td><b>name:</b> The name of the LDPath program.</td>
  </tr>
  <tr>
    <th>Produces</th>
    <td>HTTP OK(200) or HTTP NOT FOUND(404)</td>
  </tr>
</tbody>
</table>
<h4>Example</h4>
<pre>curl -i http://localhost:8080/contenthub/ldpath/exists?name=universities</pre>

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