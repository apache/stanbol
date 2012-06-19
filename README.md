<!--
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

# Building Flow endpoint

    $ cd flow/ ; mvn clean install;
    $ cd launchers/fullflow ; mvn clean install;

# Starting Stanbol 

    $ cd flow/fullflow/target ; java -jar org.apache.stanbol.launchers.fullflow-0.9.0-incubating.jar;

# Testing Flow :

## Pool flow feature 

By default a file endpoint will fetch all files in /tmp/chaininput folder, process it with the langId engine and then write the xml-rdf result in /tmp/chainoutput folder.

## On demand flow feature 

The flow endpoint offer sub-ressource that allow you to directly call some predefined particular flow.

Template for this request is : 

    $ curl -X POST -H "Accept: text/turtle" -H "Content-type: text/plain" --data "Here comes a little test with Paris as content and also Berlin but why not detect city as Boston and some well know people like Bob Marley." http://localhost:8080/flow/{{flowName}}

Where {{flowName}} can have the values : 

   - default : default chain : all registred engines sorted by weight
   - tika : only call tika engine, no visible result (as the engine don't create enhancements)
   - tika2 : tika then langID then NER then save the result in /tmp/chainoutput folder
   - tika-default : tika then default flow

Flow/Route definition for default can be find here : https://svn.apache.org/repos/asf/incubator/stanbol/branches/cameltrial/flow/weightedgraphflow/src/main/java/org/apache/stanbol/flow/weightedgraphflow/WeightedChain.java

Others Flow/routes definition resides here : https://svn.apache.org/repos/asf/incubator/stanbol/branches/cameltrial/flow/filepoolgraphflow/src/main/java/org/apache/stanbol/flow/filepoolgraphflow/FileRoute.java

