# Data files for the default DBPedia.org Site shipped with the Stanbol distributions

This source repository only holds the pom.xml file and folder structure to build
the org.apache.stanbol.data.site.dbpedia.default artifact to be included in the standard distributions of stanbol.

To avoid loading subversion repository with large binary files this artifact has to be build and deployed manually to retrieve the precomputed dbpedia index from the web.

## Download the precomputed local cache for DBPedia.org

This bundle needs to include a small local index of DBPedia.org that includes the 43k entities with the most incoming Wiki links.

This index is not in the subversion but is downloaded from `http://www.salzburgresearch.at/~rwesten/stanbol/dbpedia_43k.solrindex.zip` by the maven build
and stored at`./src/main/resources/org/apache/stanbol/data/site/dbpedia/default/index/dbpedia_43k.solrindex.zip`.
