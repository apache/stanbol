## Building Search Component

* Checkout Apache Stanbol
	$ svn co http://svn.apache.org/repos/asf/incubator/stanbol/trunk/
* Build Stanbol [see] (http://svn.apache.org/repos/asf/incubator/stanbol/trunk/README.md)
* Build Stanbol Ontology Manager - Store
	$ cd trunk/ontologymanager/store
	$ mvn install
* Build Stanbol Commons Web Ontology
	$ cd trunk/commons/web/ontology
	$ mvn install
* Go to Search component working copy root (/sandbox/search)
	$ mvn install
	
## Running

* Run full launcher 
	$ cd  launchers/full
	$ java -jar -Xmx1024m -XX:MaxPermSize=128m target/org.apache.stanbol.search.launchers.full-0.9-SNAPSHOT.jar

## Configuration

* Configure EntityHub endpoint from [Apache Felix Web Console](http://localhost:8080/system/console/configMgr/org.apache.stanbol.search.engines.location.LocationSearchEngine)
* (OPTIONAL) Configure ContentHub endpoint from [Apache Felix Web Console](http://localhost:8080/system/console/configMgr/org.apache.stanbol.search.helper.cnn.imp.CNNImporterImp)

## Try

* Go to http://localhost:8080/search/import
* Insert:
	Topic: Tokyo
  Click Import News, wait for the links of the imported documents to appear
* Go to http://localhost:8080/search, Search for Japan, check Tokyo keyword in results.
	
