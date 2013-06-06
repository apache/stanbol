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
<#import "/imports/common.ftl" as common>
<#escape x as x?html>
<@common.page title="Welcome to Apache Stanbol!" hasrestapi=false> 

<p>Apache Stanbol is an Open Source HTTP service meant to help Content
Management System developers to semi-automatically enhance unstructured
content (text, image, ...) with semantic annotations to be able to link
documents with related entities and topics.</p>

<p>Please go to <a href="http://stanbol.apache.org">the
official website</a> to learn more on the project, read the
documentation and join the mailing list.</p>

<p>Here are the main HTTP entry points. Each resource comes with a web
view that documents the matching RESTful API for applications:</p>
<dl>

  <#list it.navigationLinks as link>
  <#if link.htmlDescription??>
    <dt><a href="${it.publicBaseUri}${link.path}">${link.label}</a><dt>
    <dd><#noescape>${link.htmlDescription}</#noescape></dd>
  </#if>
  </#list>

  <dt><a href="${it.consoleBaseUri}">/system/console</a><dt>
  <dd>
    <p>This is the OSGi administration console (for administrators and developers). The initial
       username / password is set to <em>admin / admin</em>.</p>
    <p>Use the console to add new bundles and activate, de-activate and configure components.</p>
    <p>The console can also be used to perform hot-(re)deployment of any OSGi bundles.
       For instance to re-deploy a new version of this web interface, go to the <tt>$STANBOL_HOME/enhancer/jersey</tt>
       source folder and run the following command:</p>
<pre>
mvn install -o -DskipTests -PinstallBundle \
    -Dsling.url=${it.consoleBaseUri}
</pre>
  </dd>

</dl>
</@common.page>
</#escape>
