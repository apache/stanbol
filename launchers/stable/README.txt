This module builds a runnable Stanbol jar using the Sling Launchpad Maven plugin,
including the bundles defined at src/main/bundles/list.xml.

This launcher includes only Stanbol components that were agreed on being stable
components.

To start this after building use:

  java -Xmx512M -jar target/org.apache.stanbol.launchers.stable-0.9.0-incubating-SNAPSHOT.jar

The Stanbol Enhancer HTTP endpoint should then be available at 

  http://localhost:8080

So that you can POST content using, for example:

  curl -H "Content-Type: text/plain" -T data/text-examples/obama-signing.txt http://localhost:8080/engines

Configure any required parameter for the enhancement engines, at

  http://localhost:8080/system/console/

The OSGi state is stored in the ./sling folder.

The logs are found at sling/logs/error.log and can be configured from the
OSGi console.
