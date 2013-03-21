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

# Indexer for the DBpedia dataset (see http://dbpedia.org/)

This tool creates local indexes of DBpedia to be used with the Stanbol Entityhub.

## Building:

If not yet build by the built process of the entityhub call

    mvn install

to build the jar with all the dependencies used later for indexing.

If the build succeeds go to the /target directory and copy the

    org.apache.stanbol.entityhub.indexing.dbpedia-*.jar

to the directory you would like to start the indexing.

## Index:

### (1) Initialize the configuration

The configuration can be initialized with the defaults by calling

    java -jar org.apache.stanbol.entityhub.indexing.dbpedia-*.jar init

This will create a sub-folder with the name indexing in the current directory.
Within this folder all the

* configurations (indexing/config)
* source files (indexing/resources)
* created files (indexing/destination)
* distribution files (indexing/distribution)

will be located.

The indexing itself can be started by

    java -jar org.apache.stanbol.entityhub.indexing.dbpedia-*.jar index

but before doing this please note the points (2), (3) and (4)

### (2) Download the dbPedia Dump Files:

All RDF dumps need to be copied to the directory:

    indexing/resources/rdfdata

The files do not need to be decompressed. The raw ".nt.bz2" files from
DBpedia can downloaded to that folder directly.

At the time of writing, version 3.6 is the latest release. All available
archives are referenced on this page:

<http://wiki.dbpedia.org/Downloads36>

The RDF dump of DBpedia.org is splitted up in a number of different files.
The actual files needed depend on the configuration of the mappings
(indexing/config/mappings.txt). Generally one need to make sure that
all the RDF dumps with the source data for the specified mappings
are available.  A best is to use the previews of the dumps to check if
the data of a dump is required or not. Here is the list of typically
interesting dump files:

* <http://downloads.dbpedia.org/3.6/dbpedia_3.6.owl.bz2>
* <http://downloads.dbpedia.org/3.6/en/instance_types_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/labels_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/short_abstracts_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/long_abstracts_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/geo_coordinates_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/persondata_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/article_categories_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/category_labels_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/skos_categories_en.nt.bz2>
* <http://downloads.dbpedia.org/3.6/en/redirects_en.nt.bz2>

During the first part of the indexing (a.k.a. the initialisation step)
all the RDF files within the "indexing/resources/rdfdata" directory
will be imported to an Jena TDB RDF triple store. The imported data are
stored under:

    indexing/resources/tdb

and can be reused for subsequent indexing processes.

To avoid (re)importing of already imported resources one need to remove such
RDF files from the "indexing/resources/rdfdata" or - typically the better
option - rename the "rdfdata" folder after the initial run.

It is also safe to:

* cancel the indexing process after the initialization has completed
  (as soon as the log says that the indexing has started).

* load additional RDF dumps by putting additional RDF files to the
  "rdfdata" directory. This files will be added to the others on the next
  start of the indexing tool.

### (3) Entity Scores

The DBpedia.org indexer uses the incoming links from other wikipages to
calculate the rank of entities. Entities with more incoming links get an
higher rank. A RDF dump containing all outgoing wiki links is available
on DBpedia (page_links_en.nt.bz2). This file need to be processed with the
following command to get an file containing an ordered list of incoming
count and the local name of the entity.

    curl http://downloads.dbpedia.org/3.6/en/page_links_en.nt.bz2 \
        | bzcat \
        | sed -e 's/.*<http\:\/\/dbpedia\.org\/resource\/\([^>]*\)> ./\1/' \
        | sort \
        | uniq -c  \
        | sort -nr > incoming_links.txt

Note: replace "3.6" by the latest release version of DBpedia in the above
command line to get up to date data.

Depending on the machine and the download speed for the source file the
execution of this command will take several hours.

Important NOTES:

* Links to Categories use wrong URLs in the current version (3.6) of the
page_links_en.nt.bz2 dump.
All categories start with "CAT:{categoryName}" but the correct local name
would be "Category:{categoryName}". because of this categories would not be
indexed.

It is strongly suggested to:

 * first check if still Category: is used as prefix (e.g. by checking if
   <http://dbpedia.org/page/Category:Political_culture> is still valid)

 * second if that is the case replace all occurrences of "CAT:" to
   "Category:"

    sed -i 's/CAT:/Category:/g' incoming_links.txt

The resulting file MUST BE copied to

    indexing/resources/incoming_links.txt

There is also the possibility do download a precomputed file from:

TODO: add download location

### (4) Configuration of the Index

The configurations are contained within the "indexing/config" folder:

* indexing.properties: Main configuration for the indexing process. It
defines the used components and there configurations. Usually no need to
make any changes.
* mapping.txt: Define the fields, data type requirements and languages to be
indexed. Note: It is also important that the dumps containing the RDF
data are available.
* dbpedia/conf/schema.xml: Defines the schema used by Solr to store the data.
This can be used to configure e.g. if values are stored (available for
retrieval) or only indexed. See the comments within the file for details
* fieldBoosts.properties: Can be used to set boost factors for fields.
* minincoming.properties: Can be used to define the minimum number of
incoming links (to an Wiki page from other Wiki pages) so that an entity
is indexed. Higher values will cause less entities to be indexed. A
value of 0 will result in all entities to be indexed.
* scoreRange.properties: Can be use to set the upper bound for entities score.
The entities with the most incoming links will get this score. Entities
with no incoming links would get a score of zero.

### (5) Using the precomputed Index:


## Use the created index with the Entityhub

After the indexing completes the distribution folder

    /indexing/dist

will contain two files

1. `dbpedia.solrindex.zip`: This is the ZIP archive with the indexed
   data. This file will be requested by the Apache Stanbol Data File
   Provider after installing the Bundle described above. To install the
   data you need copy this file to the "/sling/datafiles" folder within
   the working directory of your Stanbol Server.
   
   If you are currently using the default data for DBPedia.org (as included in
   the Stanbol launchers) copying this file to the datafiles directory will
   automatically trigger the replacement of the default data. The new data will
   be available as soon as the copying of the data and the initialisation of the
   index completes. In the meantime queries will still use the old data.
   
   To update from one "dbpedia.solrindex.zip" to an other you need first to delete
   the existing dbpedia.solrindex.zip from the datafiles folder.
   Secondly you need to stop the "org.apache.stanbol.data.site.dbpedia-{version}"
   bundle. This should trigger the deletion of the current dbpedia index.
   After the deletion you need to reactivate the bundle and copy the new 
   "dbpedia.solrindex.zip" into the "datafiles" folder. You can already copy the
   new "dbpedia.solrindex.zip" to the datafile folder while the bundle is
   deactivated.
   
   *TODO:* updating from one dbpedia.solrindex.zip to an other one is currently
   to complex! This could be simplified if we set "Synchronized=true" also for
   DBPedia.org, or by providing a management user interface for the
   ManagedSolrServer (e.g. Tab within the Apache Felix Web Console).
   
   
2. `org.apache.stanbol.data.site.dbpedia-{version}.jar`: 

   NOTE: The installation of this bundle is no longer required, because all
   current Stanbol launchers do already include this bundle. If you are using
   an launcher configuration that does not include this bundle the steps as
   described below are still required.
   
   This is a Bundle
   that can be installed to any OSGI environment running the Apache Stanbol
   Entityhub (for instance using the Apache Felix web console under
   http://server:port/system/console - with account admin / admin by default).

   When started it will create and configure:

   * a "ReferencedSite" accessible at "http://{host}/{root}/entityhub/site/dbpedia"
   * a "Cache" used to connect the ReferencedSite with your Data and
   * a "SolrYard" that managed the data indexed by this utility.

In case you install the bundle before copying the "dbpedia.solrindex.zip" to
"/sling/datafiles" you will need to restart the dbpedia "SolrYard" instance.


## The used Default configuration:

This describes the default configuration as initialized during the first start
of the indexing tool.

The default configuration stores creates an index with the following features:

### Languages:

By default English, German, France and Italian and all literals without any
language information are indexed. Please also note that one needs to provide
also the RDF dumps for those languages.

### Labels and Descriptions:

DBpedia.org uses "rdfs:label" for labels. Short description are stored within
"rdfs:comment" and a longer version in "dbp-ont:abstract".
For both labels and descriptions generic language analyzer are used for indexing.
Also Term Vectors are stored so that "More Like This" queries can be used on
such fields.
Abstracts are only indexed and not stored in the index. This means that values
can be searched but not retrieved.

### Entity types:

The types of the entities (Person, Organisation, Places, ...) are stored in
"rdf:type". Values are URLs as defined mainly by the DBpedia.org ontology.

### Spatial Information:

The geo locations are indexed within "geo:lat", "geo:long" and "geo:alt". The
mappings ensure that lat/long values are doubles and the altitude are integers.

### Categories:

DBpedia contains also categories. Entities are linked to categories by the
"skos:subject" and/or the "dcterms:subject" property. During the import all
values defined by "dcterms:subject" are copied to "skos:subject".
Categories itself are hierarchical. Parent categories can be used by following
"skos:broader" relations.
e.g.

    Berlin -> skos:subject
        -> Category:City-states -> skos:broader
            -> Category:Cities -> skos:broader
                -> Category:Populated_places -> skos:broader
                    -> Category:Human_habitats ...

All properties defined by SKOS (http://www.w3.org/TR/skos-reference/) are
indexed and stored.

### DBpedia Ontology:

All properties of the DBpedia.org Ontology are indexed and stored in the index.
see http://wiki.dbpedia.org/Ontology

### DBpedia Properties:

Properties are field/values directly taken from the information boxes on the
right side of Wikipedia pages. Fieldnames may depend on the language and also
the data type of the values may be different from entity to entity.
Because of this such entities are not indexed by the default configuration.
It is possible to include some/all such properties by changing the mappings.txt.
Note that in such cases it is also required do include the RDF dump containing
this data.

### Person related Properties:

DBpedia uses FOAF (http://www.foaf-project.org/) to provide additional information
for persons. Some properties such as foaf:homepage are also used for entities of
other types. All properties defined by FOAF are indexed and stored.

### Dublin Core (DC) Metadata:

DC Elements and DC Terms metadata are indexed and stored.
All DC Element properties are mapped to there DC Terms counterpart.
