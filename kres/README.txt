This module builds a runnable Stanbol  jar using the Sling Launchpad Maven plugin,
including Knowledge Representation and Reasoning modules. 

The bundles included are defined at src/main/bundles/list.xml

That Sling plugin is not released as I write this, so you need to build it
(source at http://svn.apache.org/repos/asf/sling/trunk/maven/maven-launchpad-plugin)

To start this after building use:

  java -Xmx512M -jar target/org.apache.stanbol.launchers.kres-0.9-SNAPSHOT.jar

The main Stanbol HTTP endpoint should then be available at 

  http://localhost:8080

Configure any required parameter at

  http://localhost:8080/system/console/

The OSGi state is stored in the ./sling folder.

The logs are found at sling/logs/error.log and can be configured from the
OSGi console.
