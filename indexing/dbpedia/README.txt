Indexer for the DBpedia dataset (see http://dbpedia.org/)

This Tool creates a full cache for DBPedia based on the RDF Dump available via
the download section of the dbpedia.org web page.

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
   org.apache.stanbol.entityhub.indexing.dbpedia-0.1*-jar-with-dependencies.jar
is the one to be used for indexing.

Creating the index:
==================

(1) download the all the RDF files you need from the download section of the 
    dbpedia.org web page. Make sure you download all the files needed to have
    all data available used by the configured mappings. All files need to be
    in the directory parsed as second parameter to the tool.
(2) To enable ranking for DBpedia resources you need to also to calculate the
    incoming links for wikipedia sites using [1]. The generated file needs to
    be parsed to the tool by using the -i parameter.
    In case you use this feature also note the -ri parameter that can be used to
    define the minimum required number of incomming links so that an entity gets
    included in the index. (setting it to 2 will result in about 50% of all the
    entities to be indexed)
(3) The Indexer will need a SolrServer. So you need to prepare the Solr Index
    to store the data.
    A default configuration is provided within the "/solrConf" directory. This
    can be used to configure a SorlServer or a new Core to an existing SolrServer.
    You can parse the absolute path. In that case an EmbeddedSolrServer will be 
    used for indexing. 
    NOTE that the "/solrConf" directory only represents a Core and not a full
    SolrServer configuration. You need to have a valid "solr.xml" in the parent
    Directory of dbPedaia. See the Solr documentation for details how to
    configure Cores.    
(4) call the tool with the -h option to print the help screen
    java -jar ./target/org.apache.stanbol.entityhub.indexing.dblp-*-jar-with-dependencies.jar -h
    The help screen should provide you with all the information needed for indexing

Indexing will take a lot of time. Indexing time heavily depends on the IO
operations/sec of the used hard disc.

[1] https://gist.github.com/360315: 
    NOTE: There are two "head". The first restricts to 10e6 lines and the
    second prints only the first ten lines. When calculating the page rank for
    all entities one need to change this and pipe the results into a file.
    Also NOTE  that the Link to download the file with the incomming links 
    should be adopted to the version of the dumps you use for indexing. 
    e.g. http://downloads.dbpedia.org/3.6/en/page_links_en.nt.bz2 for 
    Version 3.6 of the dump