IKS FISE Persistence Store Jena Implementation
==================
This bundle implements a persistence store that is defined through eu.iksproject.stores.persistencestore.IPersistenceStore interface.
Although current implementation uses Jena, the persistence store is not bound to any specific implementation.   


after 
>mvn clean install -DskipTests=true
you can upload the jars in target folder to felix

Run
===
see README.txt of eu.iksproject.persistencestore.launchers.lite
 
Configuration
=============
(Optional)eu.iksproject.fise.stores.persistencestore.reasonerURL.name : URL for an OWLlink server to do reasoning tasks
