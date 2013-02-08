Apache Stanbol Knowledge Representation and ReaSoning (KRES) Launcher
=====================================================================

The Knowledge Representation and ReaSoning (KRES) launcher includes the following Stanbol Components:

* Ontology Manager: Allows to manage Ontologies, define Scopes used for Reasoning Sessions
* Reasoning: Allows to use Scopes for OWL-DL reasoning
* Rules: Supports the management and execution of Rules.


To start this after building use:

    java -Xmx1024M -jar target/org.apache.stanbol.launchers.kres-*-SNAPSHOT.jar

The Stanbol Enhancer HTTP endpoint should then be available at 

    http://localhost:8080


__TODO:__ add simple usage example

Configure any required parameter for the enhancement engines, at

    http://localhost:8080/system/console/

The OSGi state is stored in the ./stanbol folder.

The logs are found at stanbol/logs/error.log and can be configured from the
OSGi console.
