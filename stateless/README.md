Apache Stanbol Stateless Launcher
============

This defines a Stanbol Launcher configuration for the Stateless Content Enhancement Usage Scenarion. It includes the _Stanbol Enhancer_ and the _Stanbol Entityhub_ component.


To start this after building use:

    java -Xmx512M -jar target/org.apache.stanbol.launchers.stateless-*-SNAPSHOT.jar

The Stanbol Enhancer HTTP endpoint should then be available at 

    http://localhost:8080

So that you can POST content using, for example:

    curl -H "Content-Type: text/plain" \
        -T {content}.txt \
        http://localhost:8080/engines

Configure any required parameter for the enhancement engines, at

    http://localhost:8080/system/console/

The OSGi state is stored in the ./stanbol folder.

The logs are found at stanbol/logs/error.log and can be configured from the
OSGi console.
