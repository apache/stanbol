Apache Stanbol Semantic Indexing Launcher
=========================================

This launcher includes the Stanbol Semantic Indexing Launcher. This includes:

* Stanbol Enhancer: For extracting Knowledge from parsed Content
* Stanbol Entityhub: For managing domain Vocabularies of Entities users want to extract from parsed Content
* Stanbol Entityhub: For semantic indexing and semantic search over processed Content.


To start this after building use:

    java -Xmx1024M -jar target/org.apache.stanbol.launchers.semindex-*-SNAPSHOT.jar

The Stanbol Enhancer HTTP endpoint should then be available at 

    http://localhost:8080

So that you can POST content using, for example:

__TODO:__ add typical usage scenario (curl commands)


Configure any required parameter for the enhancement engines, at

    http://localhost:8080/system/console/

The OSGi state is stored in the ./stanbol folder.

The logs are found at stanbol/logs/error.log and can be configured from the
OSGi console.
