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

# Entity Co-reference Engine

The Entity co-reference Engine  perform co-reference resolution of Named Entities in a given text. 
The co-references will be noun phrases which refer to those Named Entities by having a minimal set of attributes which match 
contextual information (rdf:type of the entity and spatial and object function giving info) from entity repositories
such as Dbpedia and Yago for that Named Entity.

We have the following text as an example : "Microsoft has posted its 2013 earnings. The software company did better than expected. ... The Redmond-based company will hire 500 new developers this year."
The enhancement engine will link "Microsoft" with "The software company" and "The Redmond-based company".

## Building the DbPedia 3.9 index with yago types

This index will contain the yago rdf:types and several spatial/org membership and functional properties from the DBpedia index.
NOTE: At the moment the index is available only for english.

### (1) Follow the instructions at entityhub/indexing/dbpedia/README.md and build the dbpedia index with the following configuration:

#### (1) Use the RDF dumps (in N-Triple format) from :
	http://downloads.dbpedia.org/3.9/dbpedia_3.9.owl
	http://downloads.dbpedia.org/3.9/en/labels_en.nt.bz2
	http://downloads.dbpedia.org/3.9/en/instance_types_en.nt.bz2
	http://downloads.dbpedia.org/3.9/en/mappingbased_properties_en.nt.bz2
	http://downloads.dbpedia.org/3.9/links/yago_types.nt.bz2
	
#### (2) The mappings.txt file must contain the following entries:
	
	rdfs:label | d=entityhub:text
	rdf:type | d=entityhub:ref
	dbp-ont:birthPlace | d=entityhub:ref
	dbp-ont:region | d=entityhub:ref
	dbp-ont:foundationPlace | d=entityhub:ref
	dbp-ont:locationCity | d=entityhub:ref
	dbp-ont:location | d=entityhub:ref
	dbp-ont:hometown | d=entityhub:ref
	dbp-ont:country | d=entityhub:ref

### (2) Run the script /dbpedia_yag_classes/build_yago_dbpedia_labels.sh which will create the dbpedia_yago_classes_labels.nt.bz2 archive
which contains the labels of the yago types.

### (3) Follow the instructions at entityhub/indexing/genericrdf/README.md and rebuild the dbpedia index in order to include the
aforementioned yago types labels. After you init the indexer go through the following steps:

#### (1) Copy the dbpedia_yago_classes_labels.nt.bz2 to the indexing\resources\rdfdata folder.

#### (2) Change the indexing/config/indexing.properties to include the following attributes:
	
	name=dbpedia
	description=DBpedia.org
	
#### (3) The indexing/config/mappings.txt file must olny contain the rdfs:label attribute

#### (4) Copy the contents of the indexing/destination folder from the results of point ### (1) to the /indexing/destination folder
of the generic rdf indexing at point ### (3).

The results of all these steps will be the dbpedia.solrindex.zip archive which should be used as described in entityhub/indexing/dbpedia/README.md.

## Configuring the Engine
TODO

## Running the Entity co-reference engine in Stanbol.

In order to run the engine you need to add it to a chain that also contains the following engine types:
- a language detection engine
- a sentence detection engine (like opennlp-sentence)
- a token detection engine (like opennlp-token)
- a NER detection engine (like opennlp-ner)
- a noun phrase detection engine (like pos-chunker)
