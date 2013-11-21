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

# Indexer for the Freebase knowledge base (see http://freebase.com/)

This tool creates local indexes of Freebase to be used with the Stanbol Entityhub.

## Building the tool

If not yet build by the built process of the entityhub call

    mvn install

to build the jar with all the dependencies used later for indexing.

If the build succeeds go to the /target directory and copy the

    org.apache.stanbol.entityhub.indexing.freebase-*.jar

to the directory you would like to start the indexing.

## Index:

### (1) Initialize the configuration

The configuration can be initialized with the defaults by calling

    java -jar org.apache.stanbol.entityhub.indexing.freebase-*.jar init

This will create a sub-folder with the name indexing in the current directory.
Within this folder all the

* configurations (indexing/config)
* source files (indexing/resources)
* created files (indexing/destination)
* distribution files (indexing/dist)

will be located.

The indexing itself can be started by

    java -jar -Xmx32g org.apache.stanbol.entityhub.indexing.freebase-*.jar index

but before doing this please note the points (2) ... (5)

NOTEs:

* that the huge amount of memory assigned to the indexing tool (32GByte) will only
be needed during the final optimization of the created Solr Index. During indexing the
tool will be well below 4GByte. Meaning that the machine used to index will not need
more than ~4GByte RAM available as long as enough swap space is available for the
Solr Index optimization. Note also that even if the final optimization fails the
created index will be still valid. However it will be some GByte bigger and also its
query time performance will be a bit less. 
* Import speed of the dump to Jena TDB will greatly depend on the available RAM. If
Jena TDB runs out of memory for its memory-mapped files, the import will greatly
decrease to the IO performance of the hard disk/SSD. To give an Example: On an 
SSD (~4k IO operations/sec) with 4GByte RAM the import took about 1.5 days 
(see the 'indexingsource.properties' in section (5) for how to reduce the number
of triples to be imported from the Freebase Dump). Having enough RAM can reduce
this time to < 10%.

### (2) Download the Freebase Dump Files:

Freebase provided full RDF dumps at 

    https://developers.google.com/freebase/data

you will need to download the dump and store it to the 'indexing/resources/rdfdata'
folder.

### (3) Entity Scores

The Entityhub Indexing tool supports the use of index time boosts. Those
boosts can be set based on the number of referenced an Entity has within the
freebase knowledge base by using one of the following scripts:

1. [fbrankings.sh](fbranlings.sh): intended for freebase dumps that do use 
namespace prefix mappings.
2. [fbrankings-uri.sh](fbrankings-uri.sh): intended for freebase dumps that
do use full qualified URIs.calling 


NOTE: Ubuntu requires a different syntax for grep e.g. of the `fbrankings.sh`
instead of

    grep "^ns:m\..*\t.*\tns:m\."

you will need to use

    grep $'^ns:m\..*\t.*\tns:m\.'

The resulting $INCOMING_FILE needs to be copied to 'indexing/resource/incoming_lings.txt'.
The file will have a size of about 1.5GByte

### (4) Dealing with corrupted RDF

As of March 2013 some statements within the Freebase RDF dump where corrupted.
In such cases you will encounter RiotExceptions (Jena RDF parser exceptions) 
while importing the Dump to Jena TDB. 

Luckily Andy Seaborne has created an Perl script that is able to correct
all those issues. You can download this script from

    http://people.apache.org/~andy/Freebase20121223/

and use it to process the dump like 

    gunzip -c ${FB_DUMP} | fixit | gzip > ${FB_DUMP_fixed}

NOTE that the script for (3) EntityScores will no longer work on the 
fixed version as 'fixit' replaces '\t' with ' '. So if you want to
run the EntityScore script on the fixed Dump you will need to adapt
the 'grep' part of the script accordingly.


### (5) Configuration

The tool comes with a default configuration that will:

* import ~400 million of the 1300+ million RDF triples of the Freebase RDF dump
to Jena TDB. 
* index all 35+ million Freebase topics
* index all labels, alias and comments in all languages
* index some additional properties for persons, places and organizations
* provide geo:lat, geo:long, geo:alt values
* provide owl:sameAs links to DBPedia and Musicbrainz
* provide foaf:thumbnail and foaf:depiction URLs for Entities
* the resulting Solr Index will weight ~55GByte on disc
* to load the index to stanbol both the 'o.a.stanbol.commons.solr.extras.smartcn' 
and 'o.a.stanbol.commons.solr.extras.kuromoji' bundles are required
* the index does support Solr MLT queries on the full text field '_text' as
well as rdfs:comment.


__Configuration Files__

This section provide information on the configuration files in the
'indexing/config' folders.

* indexing.properties: Main configuration for the indexing process. It
defines the used components and there configurations. Unless for users 
that want to add/remove additional components (e.g. EntityProcessors)
there is usually no need to make any changes to this file.

* indexingsource.properties: This is the configuration for the Jena TDB
indexing source. This configuration is important as it is used to define
the subset of RDF triples that will get imported from the massive Freebase
RDF dump file containing > 1.300 million RDF triples. Reducing the number 
of imported triples can considerable reduce the indexing time. By default two
'import-filter' are configured: 

    1. Property based filter that uses prefixes based on property URIs for 
filtering. The configuration is defined in the 'propertyfilter.config' file.
The comments in this file for more information.
    2. Language filter that allows to filter Literals based on their language.
By default all languages are imported. Users that are only interested in
e.g. English and German Literals can set 'if-literal-language=en,de'. Note
that '*,!zh,!jp' would import all languages other than Chinese and Japanese.

* mapping.ldpath: This is used to use [LDPath](http://marmotta.incubator.apache.org/ldpath/introduction.html)
for transforming information provided by Freebase. NOTE that with the currently
used LDPath version full URIs need to be used for Freebase properties as the parser
does not support '{ns}:{localname}' for '{localname}'s that do contain '.'.

* mapping.txt: This defines the properties included in the generated index. In
addition it is used for data type transformation and copying fields. While those
things could be also done using LDPath it is more efficient to use the
this configuration for those things.

* entityTypes.properties: This allows to index only Entities of specific types.
By default only Freebase topics are indexed (as those are similar to what 
Entities are in Apahce Stanbol). However this can also be used to index only
specific types (e.g. Persons, Organizations and Places).

* fieldboosts.properties: Contains index time boosts for specific fields. 
By default labels are boosted agains alternate labels and comments.

* namespaceprefix.mappings: defines extra namespace prefixes used within the
indexing configuration. Stanbol default mappings are anyway present. If
the host has internet connectivity also mappings from [prefix.cc](http:prefix.cc)
will be loaded. It is important that this file maps the prefix 'ns' to the 
Freebase namespace. 

* minincoming.properties: The minimum number of incoming links an entity must
have within the Freebase knowledge base to be indexed. Higher values will
reduce the number of indexed Entities. '1' will include all Entities.

* iditerator.properties: Ensures that the 'indexing/resource/incoming_lings.txt'
file is correctly processed. No need to change this file.

* scorerange.properties: This file ensures that ranking of Entities (based
on the number of incoming links) are correctly mapped in the [0..1] range.
No need to change this file

* freebase/**: this is the Solr Core configuration used for indexing.


## Use the created index with the Entityhub

After the indexing completes the distribution folder

    /indexing/dist

will contain two files

1. `freebase.solrindex.zip`: This is the ZIP archive with the indexed
   data. This file will be requested by the Apache Stanbol Data File
   Provider after installing the Bundle described above. To install the
   data you need copy this file to the "/stanbol/datafiles" folder within
   the working directory of your Stanbol Server.
      
      
2. `org.apache.stanbol.data.site.freebase-{version}.jar`: 

   This is a Bundle that can be installed to any OSGI environment running the Apache Stanbol
   Entityhub. This can be done by using the Apache Felix Webconsole or by copying
   the bundle to the '{stanbol-working-dir}/stanbol/fileinstall' folder.

