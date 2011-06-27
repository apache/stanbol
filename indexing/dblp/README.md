# Indexer for the [DBLP](http://dblp.uni-trier.de/) dataset.

This Tool creates a full cache for DBLP based on the RDF Dump available at
http://dblp.l3s.de/dblp.rdf.gz

## Building:

If not yet build by the built process of the entityhub call

    mvn install

in this directory and than

    mvn -o assembly:single
    
to build the jar with all the dependencies used later for indexing.

If the build succeeds go to the /target directory and copy the

    org.apache.stanbol.entityhub.indexing.dblp-*-jar-with-dependencies.jar

to the directory you would like to start the indexing.

## Index:

### (1) Initialise the configuration

The default configuration is initialised by calling

    java -jar org.apache.stanbol.entityhub.indexing.dblp-*-jar-with-dependencies.jar init

This will create a sub-folder with the name indexing in the current directory.
Within this folder all the

* configurations (indexing/config)
* source files (indexing/resources)
* created files (indexing/destination)
* distribution files (indexing/distribution)

will be located.

### (2) Download the Source File:

Download the DBLP RDF dump from http://dblp.l3s.de/dblp.rdf.gz to
"indexing/resources/rdfdata" and rename it to "dblp.nt.gz" (because this file
does not use rdf/xml but N-Triples).
You can use the following two commands to accomplish this step

    curl -C - -O http://dblp.l3s.de/dblp.rdf.gz
    mv dblp.rdf.gz indexing/resources/rdfdata/dblp.rdf.gz

### (3) Start the indexing by calling

    java -Xmx1024m -jar org.apache.stanbol.entityhub.indexing.dblp-*-jar-with-dependencies.jar index

Note that calling the utility with the option -h will print the help.

Indexing took about 3h on a normal hard disk and about 40min on a SSD (on a
2010 MacBook Pro).

### (4) Using the precomputed Index:

After the indexing completes the distribution folder will contain two files

1. dblp.solrindex.ref: This contains the configuration for the SolrIndex. It does
not contain the data and is intended to be used to provide configurations without
the need to also include the precomputed index. When loading this file to
Apache Stanbol (typically via the Apache Sling Installer Framework) the 
Stanbol DataFileProvder service will ask for the binary data.

2. dblp.solrindex.zip: This is the ZIP archive with the precomputed data.
Typically you will need to copy this file to the data directory of the
Apache Stanbol DataFileProvider (defaults to "sling/datafiles").

## Using DBLP as Referenced Site of the Entityhub

The necessary configurations needed to use DBLP as referenced site for the
Apache Stanbol Entityhub are provided by the "Apache Stanbol Data: DBLP"
bundle.

See [{stanbol}/data/sites/dblp](http://svn.apache.org/repos/asf/incubator/stanbol/trunk/data/sites/dblp)

The README of this Bundle provides details about the installation process.
During the installation the "dblp.solrindex.zip" created by this utility is
needed.




