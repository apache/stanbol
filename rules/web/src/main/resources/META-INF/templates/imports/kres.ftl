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
<#macro form>
<form id="sparql" action="/kres" method="POST"
 enctype="application/x-www-form-urlencoded"
 accept="application/sparql-results+xml, application/rdf+xml">
<input type="text" class="url" name="databaseName" value="" >Database Name for NGraph</Input
<input type="text" class="url" name="namespace" value="" >Triples namespace
<input type="text" class="url" name="phisicalDBName" value="" >Phisical DB Name
<input type="text" class="url" name="jdbcDriver" value="" >JDBC Driver
<input type="text" class="url" name="protocol" value="" >Protocol
<input type="text" class="url" name="host" value="" >Host
<input type="text" class="url" name="port" value="" >Port
<input type="text" class="url" name="username" value="" >Username
<input type="text" class="url" name="password" value="" >Password
<p><input type="submit" class="submit" value="RDB Reengineering"/></p>
<pre class="prettyprint result" style="max-height: 200px; display: none" disabled="disabled">
</pre>
</form>
</#macro>