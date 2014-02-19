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
* __[Diseasome](http://www.nd.edu/~networks/Publication%20Categories/03%20Journal%20Articles/Biology/HumanDisease_PNAS-V104-p8685%2814My07%29.pdf)__([RDF version](http://www4.wiwiss.fu-berlin.de/diseasome)): The human disease network publishes a network of 4,300 disorders and disease genes linked by known disorder-gene associations for exploring all known phenotype and disease gene associations, indicating the common genetic origin of many diseases.
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
    1. copy "./target/ehealth.solrindex.zip" to "{stanbol-workingdir}/sling/datafiles" (NOTE: for the 0.10.* versions it is "{stanbol-workingdir}/stanbol/datafiles")
    2. install both bundles "./target/org.apache.stanbol.data.site.ehealth-1.0.0.jar" and "./target/org.apache.stanbol.demo.ehealth-*.jar" to your Stanbol instance. Users can use the [Apache Felix Web Console](http://localhost:8080/system/console/bundles)(url: http:{host}:{port}/system/console/bundles) for this task.
    3. wait a minute until Stanbol has installed the data from the "ehealth.solrindex.zip" file


After that the you will be able to 

* use the datasets with the [Stanbol Entityub](http://localhost:8080/entityhub/site/ehealth/)(url: http:{host}:{port}/{alias}/entityhub/site/ehealth)
* extract ehealth related terms by using the [Stanbol Enhancer](http://localhost:8080/enhancer/chain/ehealth) (url: http:{host}:{port}/{alias}/enhancer/chain/ehealth)

---

__NOTE__: The remaining part of this document provides detailed information about this demo and provides information on how to customize it further to specific needs. Users that want only use this demo will not need to read this part.

---

## Indexing

The configuration used for indexing can be found at

    ./src/main/indexing/config

It contains of the following parts:

* __indexing.properties__: Core configuration for the Indexing tools TODO: link to docu
* __mappings.txt__: Configures the indexed fields, data types and property mappings. TODO: link to dock
* __fieldboost.properties__: configuration for the field boosts. TODO: link to dock
* __ehealth/__: the SolrCore configuration used for indexing. This is used in this example to customize how Solr indexes labels and ID. See the following section for details.

### Customizing the Solr Schema used for indexing

The default SolrCore configuration used by the Apache Entityhub is contained in the SolrYard module and can be found [here](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/entityhub/yard/solr/src/main/resources/solr/core/default.solrindex.zip). This configuration will be used if no customized configuration is present in "{indexing-root}/indexing/config/{name}" where {name} refers to the value of the property "name" in the "indexing.properties".

Users that want/need to customize the SolrCore configuration should start with the [default configuration](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/entityhub/yard/solr/src/main/resources/solr/core/default.solrindex.zip) extract this zip file to "{incexing-root}/indexing/config" and than rename the folder to the "name" configured in the "indexing.properties". After that you can start to customize the configuration of the SolrCore used for the configuration.

THis demo uses this procedure to define two special Solr field types for indexing labels and IDs (see ./src/main/indexing/config/ehealth/conf/schema.xml).

    :::xml
    <!-- intended to be used for labels of drugs -->
    <fieldType name="label" class="solr.TextField" positionIncrementGap="100" omitNorms="false">
      <analyzer>
        <tokenizer class="solr.WhitespaceTokenizerFactory"/>
        <filter class="solr.ASCIIFoldingFilterFactory"/>
        <filter class="solr.WordDelimiterFilterFactory" 
            catenateWords="1" catenateNumbers="1" catenateAll="1" 
            generateWordParts="1" generateNumberParts="0"
            splitOnCaseChange="0" splitOnNumerics="0" stemEnglishPossessive="0" 
            preserveOriginal="0" />
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
    </fieldType>

    <!-- Field Type used for searching Drugs based on their variouse IDs -->
    <fieldType name="code_field" class="solr.TextField" positionIncrementGap="100" omitNorms="false">
      <analyzer>
        <tokenizer class="solr.WhitespaceTokenizerFactory"/>
        <filter class="solr.ASCIIFoldingFilterFactory"/>
        <filter class="solr.WordDelimiterFilterFactory" 
            catenateWords="1" catenateNumbers="1" catenateAll="1" 
            generateWordParts="1" generateNumberParts="0"
            splitOnCaseChange="0" splitOnNumerics="0" stemEnglishPossessive="0" 
            preserveOriginal="0" />
      </analyzer>
    </fieldType>

For more information on the tokenizers and filters used by this configuration please see [Analyzers, Tokenizers, and Token Filters](http://wiki.apache.org/solr/AnalyzersTokenizersTokenFilters) documentation.

Such field types are than applied to specific properties with the following configurations

   <!-- fields that store codes -->
   <field name="@/skos:notation/" type="code_field" indexed="true" stored="true" multiValued="true"/>
   <field name="@/drugbank:ahfsCode/" type="code_field" indexed="true" stored="true" multiValued="true"/>
   <field name="@/drugbank:atcCode/" type="code_field" indexed="true" stored="true" multiValued="true"/>
   [...] 
   <!-- String fields (e.g. chemical formulars)-->
   <field name="@/drugbank:smilesStringCanonical/" type="string" indexed="true" stored="true" multiValued="true"/>
   <field name="@/drugbank:smilesStringIsomeric/" type="string" indexed="true" stored="true" multiValued="true"/>
   [...] 

The defined field names must include the prefixes used by the Apache Entityhub to represent RDF types. In this case '@' refers to a plain literal without a defined language and '/' is used as separator between the prefix, property and postfix.

### Customized Mappings for the used Datasets

Such mappings are configured by the "mappings.txt" file in the "{indexing-root}/indexing/config" directory. 

NOTE that for this demo the "mapping.txt" file is located at "./src/main/indexing/conifg/mapping.txt" and copied by the "./indexing.sh" script to the "./target/indexing/indexing/config" folder. Users that want to modify the mappings should edit the mappings.txt file under "./src"!.

While this demo defines a lot of mappings a lot of them could be omitted, because they do just validate data types. In the following some of those data types mappings are shown.

    diseasome:geneId | d=xsd:anyURI
    drugbank:creationDate | d=xsd:dateTime
    drugbank:patientInformationInsert | d=xsd:anyURI

Data type mappings are only needed if the dataset does not correctly specify the XSD datatype for literal values. Typically this happens for numbers that are stored as plain literals.

Important are field mappings such as the following mappings for SKOS preferred labels.

    drugbank:genericName > skos:prefLabel
    diseasome:name > skos:prefLabel
    dailymed:fullName > skos:prefLabel

This specific set of mappings allow to search for entities of the three different datasets by using one and the same property. This is extremely useful for finding those entities form text parsed to the enhancer, because one needs only to configure a single KeywordExtractionEngine instance to cover them all.

A similar configuration is used for the various IDs specified for drugs. Those are all mapped to the "skos:notation" field. This allows to easily identify them regardless of the ID known by the User or mentioned in an text. Here are those mappings.

    drugbank:ahfsCode | d=xsd:string > skos:notation
    drugbank:atcCode | d=xsd:string > skos:notation
    drugbank:dpdDrugIdNumber | d=xsd:string > skos:notation
    drugbank:pdbHomologyId | d=xsd:string > skos:notation
    drugbank:inchiKey | d=xsd:string > skos:notation
    drugbank:primaryAccessionNo | d=xsd:string > skos:notation
    drugbank:secondaryAccessionNumber | d=xsd:string > skos:notation

Note also the wildcard mappings for the used namespaces

    dailymed:*
    drugbank:*
    diseasome:*
    sider:*
    
that ensures that all properties of those namespaces get indexed. This also ensures that even if a mapping like 

    drugbank:genericName > skos:prefLabel

is defined also

    drugbank:genericName

will be present in the indexed dataset. Without those wildcard mappings one would need to explicitly define both

    drugbank:genericName > skos:prefLabel
    drugbank:genericName

to get the same result.

### LDPath mappings

While the default mapping language supports a lot of use cases for mapping, converting and filtering of properties it is by far not as capable as [LDpath](http://code.google.com/p/ldpath/). Because of that the indexing tools has also support for using LDPath to process entities by using the "LdpathProcessor". 

A typical configuration of this processor (in the "indexing.properties" file) would look like

    org.apache.stanbol.entityhub.indexing.core.processor.LdpathProcessor,ldpath:ldpath-mapping.txt,append:true;

This configuration says that the LDPath program is read from a file with the name "ldpath-mapping.txt" within the same directory and that the results of the transformation are appended to the indexed entity. If append is deactivated that the data of the parsed entity will be replaced by the results of the LDPath statement.

A typical usage example of the LdpathProcessor processor are type specific mappings such as

    skos:prefLabel = .[rdf:type is diseasome:genes]/rdfs:label;

This specifies that only for entities of the type "diseasome:genes" the rdfs:label is mapped to skos:prefLabel. 


__NOTEs__:

* The LdpathProcessor has only access to the local properties of the currently indexed entity. LDPath statements that refer other information such as paths with a lengths > 1 or inverse properties will not work
* Processors can be chained by defining multiple Processor instances in the configuration and separating them with ';'. This allows to use multiple LdpathProcessor instances and/or to chain LdpathProcessor(s) with others such as the "FiledMapperProcessor". Processors are executed as defined within the configuration of the "entityProcessor" property. 
* When using the FiledMapperProcessor on results of the LdpathProcessor make sure that the fields defined in the LDpath statements are indexed by the FiledMapperProcessor. Otherwise such values will NOT be indexed!


### Indexing Datasets separately

This demo indexes all four datasets in a single step. However this is not required. With a simple trick it is possible to index different datasets with different indexing configurations to the same target. This section describes how this could be achieved and why users might want to do this.

This demo uses Solr as target for the indexing process. Theoretically there might be several possibility, but currently this is the only available IndexingDestination implementation. The SolrIdnex used to store the data is located at "{indexing-root}/indexing/destination/indexes/default/{name}. If this directory does not alread exist it is initialized by the indexing tool based on the SolrCore configuration in "{indexing-root}/indexing/config/{name}" or the default SolrCore configuration of not present. However if it already exists than this core is used and the data of the current indexing process are added to the existing SolrCore.

Because of that is is possible to subsequently add information of different datasets to the same SolrIndex. However users need to know that if the different dataset contain the same entity (resource with the same URI) the information of the second dataset will replace those of the first. Nonetheless this would allow in the given demo to create separate configurations (e.g. mappings) for all four datasets while still ensuring the indexed data are contained in the same SolrIndex.

This might be useful in situations where the same property (e.g. rdfs:label) is used by the different datasets in different ways. Because than one could create a mapping for dataset1 that maps rdfs:label > skos:prefLabel and for dataset2 an mapping that ensures that rdfs:label > skos:altLabel.

Workflows like that can be easily implemented by shell scrips or by setting soft links in the file system.

###  Entity Filters

Often users will only be interested in specific Entities of a dataset (e.g. only in Drugs but not in drug interactions, genes, side effects …). In such cases Entity Filters can be used to specify what entities should be indexed and what entities can be safely ignored.

This can be achieved by using the "FieldValueFilter" actually a special implementation of an EntityProcessor. It is included by default within the "indexing.properties" configuration, but it is deactivated by the default configuration within the "entityTypes.properties". Detailed information on how to correctly configure this filter are provided within the "entityTypes.properties" file. To give an example the following configuration would just index drugs (of all datasets), diseases and organizations. All other entities such as  sider:side_effects and dailymed:ingredients would be skipped.

    field=rdf:type
    values= drugbank:drugs; ailymed:drugs; sider:drugs; tcm:Medicine; diseasome:diseases; dailymed:organization

FieldValueFilter supports only a single field/value combination and entities are selected if they do match at least a single of the defined values. Users that need to filter for several fields and/or multiple values can configure multiple instances. This is achieved by adding the "FieldValueFilter" multiple times as entityProcessor in the "indexing.properties" file but with different config parameters. Here is an example of such an configuration

     entityProcessor=org.apache.stanbol.entityhub.indexing.core.processor.FieldValueFilter,config:filter1;org.apache.stanbol.entityhub.indexing.core.processor.FieldValueFilter,config:filter2;org.apache.stanbol.entityhub.indexing.core.processor.FiledMapperProcessor

Make shure that the "{indexing-root}/indexing/config" contains both a "filter1.properties" and "filter2.properties" file with the according filter rules. Only Entities that pass both filters will be indexed.

## Querying and traversing the ehealth dataset

This section assumes that this demo is running on a Apache Stanbol server (version 0.9.0-incubating or later). Readers that do not run their own server or have not yet installed this demo are encouraged to do so. If you do not want to do that you can also use the [Stambol test server](http://dev.iks-project.eu:8081) hosted by the IKS project. However all the links used by this demo will point to "http://localhost:8080". So you will need to edit the used commands.

### Traversing owl:sameAs

Sider, Drugbank and Dailymed are interlinked with each other but do define a lot of different sets of properties. The following example shows how to collect information about a drug based on following "owl:sameAs" relations defined in-between Dailymed, Sider and DrugBank. 

    name = dailymed:name;
    activeIngredient = dailymed:activeIngredient/rdfs:label;
    indication = dailymed:indication;
    dosage = dailymed:dosage;
    adverseReaction = dailymed:adverseReaction;
    warning = dailymed:boxedWarning;
    contraindication = dailymed:contraindication;
    
    sideEffect = (owl:sameAs)+/sider:sideEffect/rdfs:label;
          
    genericName = (owl:sameAs)+/drugbank:genericName;
    inchiKey = (owl:sameAs)+/drugbank:inchiKey;
    indication = (owl:sameAs)+/drugbank:indication;
    foodInteraction = (owl:sameAs)+/drugbank:foodInteraction;
    toxicity = (owl:sameAs)+/drugbank:toxicity;
    pharmacology = (owl:sameAs)+/drugbank:pharmacology;

Here [LDpath](http://code.google.com/p/ldpath/) is used to collect the interesting information. "(owl:sameAs)+" is used to build the transitive closure over the "owl:sameAs" properties. This LDpath program ensures that the context is an entity if the type "dailymed:drugs".

LDPath statements like that can be used with the

* [ehealth/ldpath](http://localhost:8080/entityhub/site/ehealth/ldpath) endpoint to request the information for a single drug
* [ehealth/find](http://localhost:8080/entityhub/site/ehealth/find) endpoint to search for "dailymed:name" (make sure the language field is empty if you use the UI)
* [ehealth/query](http://localhost:8080/entityhub/site/ehealth/query) endpoint to make any kind of field queries.

