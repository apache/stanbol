This module builds a runnable Persistence Store jar using the Sling Launchpad Maven plugin,
including the bundles defined at src/main/bundles/list.xml


To start this after building use:

  java -Xmx512M -jar target/eu.iksproject.persistencestore.launchers.fise-0.9-SNAPSHOT.jar

The Persistence Store HTTP endpoint should then be available at 

  http://localhost:8080/persistencestore/ontologies


The OSGi state is stored in the ./sling folder.

The logs are found at sling/logs/error.log and can be configured from the
OSGi console

