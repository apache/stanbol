# Indexing utility for the [geonames.org](http://www.geonames.org) dataset.

Up to now this tool is not yet ported to ne new Indexing infrastructure defined
by the org.apache.stanbol.entityhub.indexing.core module.

Please follow [STANBOL-187](https://issues.apache.org/jira/browse/STANBOL-187)
for updates


## Building and Indexing

Built the utility:
   
    mvn install
    mvn assembly:assembly

To print the help of the utility call

   java -jar target/org.apache.stanbol.entityhub.indexing.geonames-.*-jar-with-dependencies.jar -h
   
You will need an external SolrServer and configure it with the Solr Core
configuration as provided by the SolrYard module "org.apache.stanbol.entityhub.yard.solr".

## Creating a Entityhub Solr Archive

This step is required to create the Archive with the Solr Index as required
after the Installation of geonames.org Referenced Site (see the 
"org.apache.stanbol.data.sites.geonames" module for details)

The Entityhub uses special solr archive for the initialisation of local solr
indexes. As soon as this indexer is moved to the new Indexing Infrastructure
(see [STANBOL-187](https://issues.apache.org/jira/browse/STANBOL-187) ) the
required files will be automatically created.

Until that this needs to be done manually by creating a ZIP archive of the
data and the configuration of the SolrIndex used for the indexing.
The archive needs to be renamed to "geonames.solrindex.zip".

