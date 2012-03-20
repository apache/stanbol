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


Apache Stanbol Demo: eHealth
============================

This module provides a demo on how to customize Apache Stanbol to the ehealth domain.

This demo uses the following datasets:

* __[Dailymed](http://dailymed.nlm.nih.gov/dailymed/)__([RDF version](http://www4.wiwiss.fu-berlin.de/dailymed/))): Published by the National Library of Medicine, this dataset provides high quality information about marketed drugs.
* __[SIDER](http://sideeffects.embl.de/)__([RDF version](http://www4.wiwiss.fu-berlin.de/sider)): SIDER contains information on marketed drugs and their adverse effects. The information is extracted from public documents and package inserts.
* __[Diseasome](http://www.nd.edu/~networks/Publication%20Categories/03%20Journal%20Articles/Biology/HumanDisease_PNAS-V104-p8685(14My07).pdf)__([RDF version](http://www4.wiwiss.fu-berlin.de/diseasome)): The human disease network publishes a network of 4,300 disorders and disease genes linked by known disorder-gene associations for exploring all known phenotype and disease gene associations, indicating the common genetic origin of many diseases.
* __[DrugBank](http://www.drugbank.ca/)__([RDF version](http://www4.wiwiss.fu-berlin.de/drugbank)): A repository of almost 5000 FDA-approved small molecule and biotech drugs. It contains detailed information about drugs including chemical, pharmacological and pharmaceutical data; along with comprehensive drug target data such as sequence, structure, and pathway information.

Note that the RDF versions of this dataset used by this dataset is hosted [by the [Freie Universität Berlin](http://www.wiwiss.fu-berlin.de/en/institute/pwo/bizer/)

This demo shows how to

1. import the listed datasets to the Apache Stanbol Entityhub by using the indexing utilities provided by the Stanbol Entityhub. Including the usage examples for 
    * schema mappings during indexing (see "src/main/indexing/config/mappings.txt")
    * customized Apache Solr schemas to control how fields are indexed (see solr core configuration at "src/main/indexing/config/ehealth")
2. "install" the indexed data set - make them available via the Apache Stanbol Entityhub
3. configure the Stanbol Enhancer to extract ehealth related entities
    * based on there labels (fields mapped to rdfs:label)
    * Durgs based on their various IDs (fields mapped to skos:notation)

## Usage

To install the demo you will need to do the following steps

1. execute "__mvn install__": This will create the bundle "org.apache.stanbol.demo.ehealth-*.jar" in the "./target" folder. This bundle includes the configuration for Apache Stanbol Components:
    * Two configurations for the [KeywordLinkingEngine](http://incubator.apache.org/stanbol/docs/trunk/enhancer/engines/keywordlinkingengine.html). One that is configured to extract Entities of the above datasets based on their labels and an other one that is configured to extract Drugs based on their IDs.
    * A special [EnhancementChain](http://incubator.apache.org/stanbol/docs/trunk/enhancer/chains/) for processing ehealt data.
2. execute "__./index.sh__": This shell script automates the steps described in detail by the [Working with local Entities Guide](http://incubator.apache.org/stanbol/docs/trunk/customvocabulary.html). This includes the following steps. 
    * assembly the generic RDF indexing tool ({stanbol}/entityhub/indexing/genericrdf)
    * copy the configuration from "./src/main/indexing/config" to the target directory used for indexing
    * initialize missing configs by calling "java -jar org.apache.stanbol.entityhub.indexing.genericrdf-*-jar-with-dependencies.jar init"
    * download the datasets listed above to "./target/indexing/indexing/resources/rdfdata/"
    * index the datasets
    * copy the results "org.apache.stanbol.data.site.ehealth-1.0.0.jar" and "ehealth.solrindex.zip" to the "./target" folder
3. __Install__ the data to a running Stanbol instance: Both the Stable and the Full launcher can be used as base for this demo. The following steps are required to install the demo
    1. copy "./target/ehealth.solrindex.zip" to "{stanbol-workingdir}/sling/datafils"
    2. install both bundles "./target/org.apache.stanbol.data.site.ehealth-1.0.0.jar" and "./target/org.apache.stanbol.demo.ehealth-*.jar" to your Stanbol instance. Users can use the [Apache Felix Web Console](http://localhost:8080/system/console/bundles)(url: http:{host}:{port}/system/console/bundles) for this task.
    3. wait a minute until Stanbol has installed the data from the "ehealth.solrindex.zip" file


After that the you will be able to 

* use the datasets with the [Stanbol Entityub](http://localhost:8080/entityhub/site/ehealth/)(url: http:{host}:{port}/{alias}/entityhub/site/ehealth)
* extract ehealth related terms by using the [Stanbol Enhancer](http://localhost:8080/enhancer/chain/ehealth) (url: http:{host}:{port}/{alias}/enhancer/chain/ehealth)


## Backround information about this demo

TODO!!