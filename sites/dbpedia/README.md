# Data files for the DBPedia.org Site shipped (by default) with the Stanbol distributions

This source repository only holds the pom.xml file and folder structure to build
the org.apache.stanbol.data.site.dbpedia artifact that includes a small
local solr index including the 43k dbpedia entities with the most incomming wiki
links on wikipedia.

This included index can be seen as preview data of bigger indexes with more
Entities and will be replaced by copying a bigger index into the 

    /sling/datafiles
    
directory within you Stanbol installation.

Such bigger indexes can be created by using the dbpedia indexing tool 
({stanbol-trunk}/entityhub/indexing/dbpedia) or downloaded via the web e.g.
from the [IKS demo server](http://dev.iks-project.eu/downloads/stanbol-indices/);

## Notes about the included DBPedia.org index

This bundle needs to include a small local index of DBPedia.org that includes the 43k entities with the most incoming Wiki links.

This index is not in the subversion but is downloaded from `http://www.salzburgresearch.at/~rwesten/stanbol/dbpedia_43k.solrindex.zip` by the maven build
and stored at`./src/main/resources/org/apache/stanbol/data/site/dbpedia/default/index/dbpedia_43k.solrindex.zip`.
