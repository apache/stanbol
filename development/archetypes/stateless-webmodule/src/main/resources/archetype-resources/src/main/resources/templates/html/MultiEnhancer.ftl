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
<@namespace ont="http://example.org/service-description#" />
<@namespace ehub="http://stanbol.apache.org/ontology/entityhub/entityhub#" />
<@namespace cc="http://creativecommons.org/ns#" />
<@namespace dct="http://purl.org/dc/terms/" />

<html>
  <head>
    <title>Multi-Enhancer Example Application - Apache Stanbol</title>
    <link type="text/css" rel="stylesheet" href="styles/multi-enhancer.css" />
  </head>

  <body>
    <h1>Enhance a file</h1>
    
    <form method="POST" action="<@ldpath path="."/>" accept-charset="utf-8"  
          enctype="multipart/form-data">
        <label for="file">File to enhance</label><input type="file" name="file" 
                size="90"/><br/>
        <label for="chain">Chain to use</label><input type="text" name="chain" 
                size="30"/><br/>
        <input type="submit" value="Get enhancements" />
        <input type="submit" onclick="this.form.action = '<@ldpath path="."/>?header_Accept=application%2Frdf%2Bxml'" 
              value="Get enhancement as rdf/xml" />
        <input type="submit" onclick="this.form.action = '<@ldpath path="."/>?header_Accept=text%2Fturtle'"
              value="Get enhancement as turtle" />
    </form>
    <#include "/html/includes/footer.ftl">
  </body>
</html>

