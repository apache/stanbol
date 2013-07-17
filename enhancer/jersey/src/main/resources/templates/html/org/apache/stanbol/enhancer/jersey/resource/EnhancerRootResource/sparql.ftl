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
<#import "/imports/enhancersparql.ftl" as sparql>
<#escape x as x?html>
<@common.page title="SPARQL Endpoint for the Stanbol Enhancer Configuration" hasrestapi=false>

  <p>This allows to query the configuration of the Stanbol Enhancer for 
    active Enhancement Engines and EnhancementChains.</p>
  
  <p><a href="http://en.wikipedia.org/wiki/Sparql">SPARQL</a> is the
    standard query language the most commonly used to provide interactive
    access to semantic knowledge bases.</p>
    
  <p>A SPARQL endpoint is a standardized HTTP access to perform SPARQL queries.
    Developers of REST clients will find all the necessary documentation in the
    official <a href="http://www.w3.org/TR/rdf-sparql-protocol/#query-bindings-http">W3C
    page for the RDF SPARQL protocol</a>.</p>
    
  <p>The Stanbol enhancer SPARQL endpoint gives access to the Configuration
    of the Stanbol Enhancer. It does NOT allow to auery enhancement results.
    Users that want to query/search for ContentItems based on extracted
    knowledge should use instead:<ul>
    <li> the <b><a href="${it.rootUrl}contenthub">Contenthub</a></b>: Supports
        semantic search based on configurable semantic indexes.</li>
    <li> <b><a href="${it.rootUrl}sparql">Sparql Endpoint</a></b>: Supports
        SPARQL querys over the RDF graph containing the metadata of all enhanced
        ContentItmes. </li>
    </ul></p>
 
  <@sparql.form/>

</@common.page>
</#escape>
