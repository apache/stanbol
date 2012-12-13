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
<html>
    <head>
        <title>Welcome to: <@ldpath path="rdfs:label[@en]"/></title>
    </head>

    <body>
        <h1>A properly located template! <@ldpath path="rdfs:label[@en]"/></h1>

        <p>
            Comment: <@ldpath path="rdfs:comment[@en]"/>
        </p>

        <ul>
            <@ldpath path="fn:sort(rdf:type)">
                <#if evalLDPath("rdfs:label[@en] :: xsd:string")??>
                    <li><@ldpath path="rdfs:label[@en] :: xsd:string"/></li>
                </#if>
            </@ldpath>
        </ul>
        <#include "/html/included.ftl">
    </body>

</html>