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

# Indexing utility for the [geonames.org](http://www.geonames.org) dataset.

With [STANBOL-835](https://issues.apache.org/jira/browse/STANBOL-835) this tool was fully ported to the Entityhub Indexing Tool. Please also consider the documentation of that tool as this will only cover geonames.org details.


## Building and Indexing

Built the utility:
   
    mvn install
    mvn assembly:single

after this completes you will find the runable jar at

    target/org.apache.stanbol.entityhub.indexing.geonames-*-jar-with-dependencies.jar

It is strongly recommended to copy this file in an dedicated folder used for indexing. Within this folder you need than to call

    java -jar org.apache.stanbol.entityhub.indexing.geonames-*-jar-with-dependencies.jar init

this will initialize the indexing directory based on the default configuration included in the tool.

## Configuration of the Tool

This chapter only covers genomes specific stuff. Users that are new to the Entityhub Indexing Tool should also have a look at the documentation provided with the genericrdf indexing tool.

### Geonames IndexingSource

The geonamames.org indexing tool provides an own indexing source that operates on the database dump file provided by geonames.org. Users that want to index all Geonames.org entities will want to use the [allCountries](http://download.geonames.org/export/dump/allCountries.zip) archive as source. However Geonames.org also provide country specific as well as files only containing Cities with a population higher than x.

Users that do want to use several files as indexing source should create an own folder in the resources directory (the "./indexing/resources" folder) and add all sums they want to index to that folder. If the indexing source configuration points to that folder all files within that folder will be indexed.

The following example shows an configuration of the indexing source within the indexing.properties file that assumes that the "dump" folder created. The "dump" folder can contain as many genomes archives as needed (e.g. [DE.zip](http://download.geonames.org/export/dump/DE.zip), [AT](http://download.geonames.org/export/dump/AT.zip), [CH](http://download.geonames.org/export/dump/CH.zip) and [cities15000.zip](http://download.geonames.org/export/dump/cities15000.zip).

    entityDataIterable=org.apache.stanbol.entityhub.indexing.geonames.GeonamesIndexingSource,source:allCountries.zip

### Support for alternate labels

Alternate labels are provided by the [alternateNames.zip](http://download.geonames.org/export/dump/alternateNames.zip). That means that those labels are not available form the Geonames IndexingSource. Because of that those labels are added during the Entity processing step by the AlternateLabelProcessor.

To use this EntityProcessor users need to add it the the list of EntityProcessors as configured in the indexing.properties file. It is activated by the default configuration of the tool.

by default the AlternateLabelProcessor assumes the [alternateNames.zip](http://download.geonames.org/export/dump/alternateNames.zip) to be present in the Resource Directory (./indexing/resources)

### Support for Hierarchy

Geonames.org defines different two sources of hierarchies: (1) via the administrative regions and (2) the [hierarchy.zip](http://download.geonames.org/export/dump/hierarchy.zip). For details please see the [Geonames Dump Readme file](http://download.geonames.org/export/dump/readme.txt).

As this information are not part of the geonames.org main table those information are not provided by the Geonames IndexingSource but added by the HierarchyProcessor. This processor consumes the following data:

* [hierarchy.zip](http://download.geonames.org/export/dump/hierarchy.zip)
* [countryInfo.txt](http://download.geonames.org/export/dump/ countryInfo.txt)
* [admin1CodesASCII.txt](http://download.geonames.org/export/dump/admin1CodesASCII.txt)
* [admin2Codes.txt](http://download.geonames.org/export/dump/admin2Codes.txt)

By default all those files are expected in the Resource directory (./indexing/resources). File names and location can be adapted by the configuration provided in the indexing.properties file.

## Indexing

To start the indexing process make sure that all the required files are in the Resource Folder (./indexing/resources). After that you need to call the tool with

java -Xmx4g -server -jar org.apache.stanbol.entityhub.indexing.geonames-*-jar-with-dependencies.jar index

The 4GByte of memory are required because hierarchy and alternate labels are loaded in-memory for the indexing process. If you do not use those EntityProcessors the memory footprint should be less than 500MByte.

## Advanded Options

* __LDPath:__ The GeonamesIndexingSource does not support LDPath. Because of that the LdpathSourceProcessor can not be used. Users that want to use LDPath programs with a path length > 1 can however use the LdPathPostProcessor. This will use the IndexingDestination (SolrYard) as both source and target in the Post-Processing phase of the Indexing Tool. The indexing.properties files contains some examples for that. Users might also want to see the examples of the genericrdf indexing tool.
* __FieldMappings:__ The default configurations does use some simple mapping rules. The UTF-8 labels of genomes are copied to rdfs:label and the genomes:parentFeature relation is used to store links to all parent features (transitive closure over the hierarchy).
