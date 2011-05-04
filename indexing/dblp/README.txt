Indexer for the DBLP dataset (see http://dblp.uni-trier.de/)

This Tool creates a full cache for DBLP based on the RDF Dump available at
http://dblp.l3s.de/dblp.rdf.gz

Building:
========
If not yet build by the built process of the entityhub call
   mvn install
in this directory.

If the build succeeds go to the /target directory and copy the
   org.apache.stanbol.entityhub.indexing.dblp-*-jar-with-dependencies.jar
to the directory you would like to start the indexing.

Index:
==================

(1) Initialise the configuration by calling
java -jar org.apache.stanbol.entityhub.indexing.dblp-*-jar-with-dependencies.jar init

This will create a sub-folder with the name indexing in the current directory.
Within this folder all the
 - configurations (indexing/config)
 - source files (indexing/resources)
 - created files (indexing/destination)
 - distribution files (indexing/distribution)
will be located.

(2) Download the Source File:

Download the DBLP RDF dump from http://dblp.l3s.de/dblp.rdf.gz to
"indexing/resources/rdfData" and rename it to "dblp.nt.gz" (because this file
does not use rdf/xml but N-Triples).
You can use the following two commands to accomplish this step

curl -C - -O http://dblp.l3s.de/dblp.rdf.gz
mv dblp.rdf.gz indexing/resources/rdfData/dblp.rdf.gz

(3) Start the indexing by calling
java -Xmx1024m -jar org.apache.stanbol.entityhub.indexing.dblp-*-jar-with-dependencies.jar index

Note that calling the utility with the option -h will print the help.

Indexing took about 3h on a normal hard disk and about 40min on a SSD (on a
2010 MacBook Pro).