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

# Apache Stanbol Ontology Network Manager

Ontology Network Manager is the set of Apache Stanbol components responsible for managing the knowledge base and presenting it on-demand as a network of interconnected OWL ontologies for OWL-aware applications such as Description Logics reasoners.

## Components

   - 'ontonet'     - allows to construct subsets of the knowledge base 
                     managed by Stanbol into OWL/OWL2 ontology networks
   - 'registry'    - manages ontology libraries for bootstrapping the network
                     using both external and internal ontologies
   - 'store'       - create, read, update and modify operations on single
                     ontologies stored in Stanbol
   - 'web'         - the RESTful Web Service interface for OntoNet

## Building Ontology Network Manager

To build Stanbol OntoNet you need a JDK 1.6 and Maven 2.2.1 or 3 installed. You will most probably need to increase the available memory for Maven:

    $ export MAVEN_OPTS="-Xmx512M -XX:MaxPermSize=128M" (Unix systems)
    
The sources can be checked out from::

  % svn co https://svn.apache.org/repos/asf/incubator/stanbol/trunk stanbol (full Apache Stanbol sources)
  % svn co https://svn.apache.org/repos/asf/incubator/stanbol/trunk/ontologymanager ontologymanager (ontology manager only)

Build and run the tests::

  % cd ontologymanager
  % mvn clean install
  
## Running Ontology Network Manager

The Ontology Network Manager is packaged with two Apache Stanbol launchers provided with the distribution, i.e. the 'full' and 'kres' launchers.

You will either need to run one of these launchers (after moving to their locations):

 % java -jar -Xmx1g org.apache.stanbol.launchers.full-0.9.0-incubating-SNAPSHOT.jar
 
 % java -jar -Xmx1g org.apache.stanbol.launchers.kres-0.9.0-incubating-SNAPSHOT.jar

or run another Stanbol launcher and install the Ontology Network Manager modules:

 % cd ontologymanager
 % mvn install -PinstallBundle -Dsling.url=<path to your running Felix administration console>

for example

 % mvn install -PinstallBundle -Dsling.url=http://localhost:8080/system/console
 
If instead you have the precompiled ontologymanager OSGi bundles, you can install them straight away via the Felix administration Web console.

## Useful links

  - Documentation will be published and mailing lists info on [the official
    Stanbol page](http://incubator.apache.org/stanbol/)

  - Please report bugs on the [Apache issue tracker for Stanbol](
    https://issues.apache.org/jira/browse/STANBOL)

