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

# Default Indexing Tool for RDF

This tool provides a default configuration for creating a SOLr index of RDF 
files (e.g. a SKOS export of a thesaurus or a set of foaf files)

## Building

If not yet built during the build process of the entityhub call

    mvn install

to build the jar with all the dependencies used later for indexing.

If the build succeeds go to the /target directory and copy the

    org.apache.stanbol.entityhub.indexing.genericrdf-*.jar

to the directory you would like to start the indexing.

## Indexing

### (1) Initialize the configuration

The default configuration is initialized by calling

    java -jar org.apache.stanbol.entityhub.indexing.genericrdf-*.jar init

This will create a sub-folder "indexing" in the current directory.
Within this folder all the

* configurations (indexing/config)
* source files (indexing/resources)
* created files (indexing/destination)
* distribution files (indexing/distribution)

will be located.

### (2) Adapt the configuration

The configuration is located within the

    indexing/config

directory.

The indexer supports two indexing modes

1. Iterate over the data and lookup the scores for entities (default). 
For this mode the "entityDataIterable" and an "entityScoreProvider" MUST BE 
configured. If no entity scores are available, a default entityScoreProvider 
provides no entity scores. This mode is typically used to index all entities of 
a dataset.
2. Iterate over the entity IDs and Scores and lookup the data. For this Mode an 
"entityIdIterator" and an "entityDataProvider" MUST BE configured. This mode is 
typically used if only a small sub-set of a large dataset is indexed. This might
be the case if Entity-Scores are available and users want only to index the e.g.
10000 most important Entities or if a dataset contains Entities of many different
types but one wants only include entities of a specific type (e.g. Species in
DBpedia).


The configuration of the mentioned components is contained in the main indexing 
configuration file explained below.

#### Main indexing configuration (indexing.properties)

This file contains the main configuration for the indexing process.

* the "name" property MUST BE set to the name of the referenced site to be created 
by the indexing process
* the "entityDataIterable" is used to configure the component iterating over the 
RDF data to be indexed. The "source" parameter refers to the directory the RDF 
files to be indexed are searched. The RDF files can be compressed with 'gz', 
'bz2' or 'zip'. It is even supported to load multiple RDF files contained in a 
single ZIP archive.
* the "entityScoreProvider" is used to provide the ranking for entities. A 
typical example is the number of incoming links. Such rankings are typically 
used to weight recommendations and sort result lists. (e.g. by a query for 
"Paris" it is much more likely that a user refers to Paris in France as to one 
of the two Paris in Texas). If no rankings are available you should use the 
"org.apache.stanbol.entityhub.indexing.core.source.NoEntityScoreProvider".
* the "scoreNormalizer" is only useful in case entity scores are available. 
This component is used to normalize rankings or also to filter entities with 
low rankings.
* the "entityProcessor" is used to process (map, convert, filter) information 
of entities before indexing. The mapping configuration is provided in an separate 
file (default "mapping.txt").
* the "entityPostProcessor" is used to process already indexed entities in a 
2nd iteration. This has the advantage, that processors used in the post-processing 
can assume that all raw data are already present within IndexingDestination. 
For this step the IndexingDestination is used for both source and destination. 
See also [STANBOL-591](https://issues.apache.org/jira/browse/STANBOL-591)
* Indexes need to provide the configurations used to store entities. The 
"fieldConfiguration" allows to specify this. Typically it is the same mapping 
file as used for the "entityProcessor" however this is not a requirement.
* the "indexingDestination" property is used to configure the target for the 
indexing. Currently there is only a single implementation that stores the indexed 
data within a SolrYard. The "boosts" parameter can be used to boost (see Solr 
Documentation for details) specific fields (typically labels) for full text 
searches.
* all properties starting with "org.apache.stanbol.entityhub.site." are used for
 the configuration of the referenced site.

Please note also the documentation within the "indexing.properties" file for details.

#### Mapping configuration (mappings.txt)

Mappings are used for three different purposes:

1. During the indexing process by the "entityProcessor" to process the 
information of each entity
2. At runtime by the local Cache to process single Entities that are updated in the cache.
3. At runtime by the Entityhub when importing an Entity from a referenced Site.

The configurations for (1) and (2) are typically identical. For (3) one might 
want to use a different configuration. The default configuration assumes to 
use the same configuration (mappings.txt) for (1) and (2) and no specific 
configuration for (3).

The mappings.txt in its default already include mappings for popular ontologies 
such as Dublin Core, SKOS and FOAF. Domain specific mappings can be added to 
this configuration. 

#### Score Normalizer configuration

The default configuration also provides examples for configurations of the 
different score normalisers. However by default they are not used.

* "minscore.properties": Example of how to configure minimum score for Entities 
to be indexed
* "scorerange.properties": Example of how to normalise the maximum/minimum score
 of Entities to the configured range.

NOTE: 

* To use score normalisation, scores need to be provided for Entities. This means 
an "entityScoreProvider" or an "entityIdIterator" needs to be configured 
(indexing.properties).
* Multiple score normalisers can be used. The call order is determined by the 
configuration of the "scoreNormalizer" property (indexing.properties). 

### (3) Provide the RDF files to be indexed

All sources for the indexing process need to be located within the the

    indexing/resources

directory

By default the RDF files need to be located within

    indexing/resources/rdfdata

however this can be changed via the "source" parameter of the "entityDataIterable" 
or "entityDataProvider" property in the main indexing configuration (indexing.properties).


Supported RDF files are:

* RDF/XML (by using one of "rdf", "owl", "xml" as extension): Note that this 
encoding is not well suited for importing large RDF datasets.
* N-Triples (by using "nt" as extension): This is the preferred format for 
importing (especially large) RDF datasets.
* NTurtle (by using "ttl" as extension)
* N3 (by using "n3" as extension)
* NQuards (by using "nq" as extension): Note that all named graphs will be 
imported into the same index.
* Trig (by using "trig" as extension)

Supported compression formats are:

* "gz" and "bz2" files: One need to use double file extensions to indicate both 
the used compression and RDF file format (e.g. myDump.nt.bz2)
* "zip": For ZIP archives all files within the archive are treated separately. 
That means that even if a ZIP archive contains multiple RDF files, all of them 
will be imported.

### (4) Create the Index

    java -Xmx1024m -jar org.apache.stanbol.entityhub.indexing.genericrdf-*.jar index

Note that calling the utility with the option -h will print the help.


## Use the created index with the Entityhub

After the indexing completes the distribution folder 

    /indexing/dist

will contain two files

1. org.apache.stanbol.data.site.{name}-{version}.jar: This is a Bundle that can 
be installed to any OSGI environment running the Apache Stanbol Entityhub. When 
Started it will create and configure

 * a "ReferencedSite" accessible at "http://{host}/{root}/entityhub/site/{name}"
 * a "Cache" used to connect the ReferencedSite with your Data and
 * a "SolrYard" that managed the data indexed by this utility.

 When installing this bundle the Site will not be yet work, because this Bundle 
 does not contain the indexed data but only the configuration for the Solr Index.

2. {name}.solrindex.zip: This is the ZIP archive with the indexed data. This 
file will be requested by the Apache Stanbol Data File Provider after installing 
the Bundle described above. To install the data you need copy this file to the 
"/sling/datafiles" folder within the working directory of your Stanbol Server.

 If you copy the ZIP archive before installing the bundle, the data will be 
 picked up during the installation of the bundle automatically. If you provide 
 the file afterwards you will also need to restart the SolrYard installed by the 
 Bundle.

{name} denotes to the value you configured for the "name" property within the
"indexing.properties" file.

### A note about blank nodes

If your input data sets contain large numbers of blank nodes, you may find that
you have problems running out of heap space during indexing. This is because Jena
(like many semantic stores) keeps a store of blank nodes in core memory while 
importing. Keeping in mind that EntityHub does not support the use of blank nodes,
there is a means of indexing such data sets nonetheless. You can convert them to
named nodes and then index. There is a convenient tool packaged with Stanbol for
this purpose, called "Urify" (org.apache.stanbol.entityhub.indexing.Urify).
It is available in the runnable JAR file built by this indexer. To use it, put that
JAR on your classpath, and you can execute Urify, giving it a list of files to process.
Use the "-h" or "--help" flag to see options for Urify:

    java -Xmx1024m -cp org.apache.stanbol.entityhub.indexing.genericrdf-*.jar \
    org.apache.stanbol.entityhub.indexing.Urify --help
    
    