Indexer for the DBLP dataset (see http://dblp.uni-trier.de/)

This Tool creates a full cache for DBLP based on the RDF Dump available at
http://dblp.l3s.de/dblp.rdf.gz

Building:
========
If not yet build by the built process of the entityhub call
   mvn install
in this directory.

To create the runable jar that contains all the dependencies call
   mvn assembly:assembly
   
If everything completes successfully, than there should be two jar files within
the target directory.
The one called 
   org.apache.stanbol.entityhub.indexing.dblp-0.1*-jar-with-dependencies.jar
is the one to be used for indexing.

Creating the index:
==================

(1) download the dump from http://dblp.l3s.de/dblp.rdf.gz
(2) rename the dump file to "dblp.nt.gz" to allow the RdfIndexer to correctly 
    set the RDF format to NTRIPLES!
(3) The Indexer will need a SolrServer. So you need to prepare the Solr Index
    to store the data.
    A default configuration is provided within the "/solrConf" directory. This
    can be used to configure a SorlServer or a new Core to an existing SolrServer.
    You can also copy the "dblp" folder within the "/solrConf" directory to an
    other location and than parse the absolute path as SolrServer location to the
    Tool. In that case an EmbeddedSolrServer will be used for indexing. 
(4) call the tool with the -h option to print the help screen
    java -jar ./target/org.apache.stanbol.entityhub.indexing.dblp-*-jar-with-dependencies.jar -h

Indexing took about 3h on my Computer. Indexing time heavily depends on the
used hard disc.