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

# Indexer for the [DBLP](http://dblp.uni-trier.de/) dataset.

This tool creates a full cache for DBLP based on the RDF Dump available at
http://dblp.l3s.de/dblp.rdf.gz

## Building

If not yet build by the built process of the entityhub call

    mvn install

to build the jar with all the dependencies used later for indexing.

If the build succeeds go to the /target directory and copy the

    org.apache.stanbol.entityhub.indexing.dblp-*.jar

to the directory you would like to start the indexing.

## Index

### (1) Initialize the configuration

The default configuration is initialized by calling

    java -jar org.apache.stanbol.entityhub.indexing.dblp-*.jar init

This will create a sub-folder with the name indexing in the current directory.
Within this folder all the

* configurations (indexing/config)
* source files (indexing/resources)
* created files (indexing/destination)
* distribution files (indexing/distribution)

will be located.

### (2) Download the Source File

Download the DBLP RDF dump from http://dblp.l3s.de/dblp.rdf.gz to
"indexing/resources/rdfdata" and rename it to "dblp.nt.gz" (because this file
does not use rdf/xml but N-Triples).
You can use the following two commands to accomplish this step

    curl -C - -O http://dblp.l3s.de/dblp.rdf.gz
    mv dblp.rdf.gz indexing/resources/rdfdata/dblp.rdf.gz

### (3) Start the indexing by calling

    java -Xmx1024m -jar org.apache.stanbol.entityhub.indexing.dblp-*.jar index

Note that calling the utility with the option -h will print the help.

Indexing took about 3h on a normal hard disk and about 40min on a SSD (on a
2010 MacBook Pro).

### (4) Using the precomputed Index

After the indexing completes the distribution folder

    /indexing/dist

will contain two files

1. `dblp.solrindex.zip`: This is the ZIP archive with the indexed
   data. This file will be requested by the Apache Stanbol Data File
   Provider after installing the Bundle described above. To install the
   data you need copy this file to the "/sling/datafiles" folder within
   the working directory of your Stanbol Server.

2. `org.apache.stanbol.data.site.dblp-{version}.jar`: This is a Bundle
   that can be installed to any OSGI environment running the Apache Stanbol
   Entityhub (for instance using the Apache Felix web console under
   http://server:port/system/console - with account admin / admin by default).

   When started it will create and configure:

   * a "ReferencedSite" accessible at "http://{host}/{root}/entityhub/site/dblp"
   * a "Cache" used to connect the ReferencedSite with your Data and
   * a "SolrYard" that managed the data indexed by this utility.

In case you install the bundle before copying the "dblp.solrindex.zip" to
"/sling/datafiles" you will need to restart the dblp "SolrYard" instance.


## Using DBLP as Referenced Site of the Entityhub

The necessary configurations needed to use DBLP as referenced site for the
Apache Stanbol Entityhub are provided by the "Apache Stanbol Data: DBLP"
bundle.

See [{stanbol}/data/sites/dblp](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/data/sites/dblp)

The README of this Bundle provides details about the installation process.
During the installation the "dblp.solrindex.zip" created by this utility is
needed.




