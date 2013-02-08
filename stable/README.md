Stanbol Stable Launcher
=======================

This launcher is similar to the [Sateless](../stateless) but includes the DBpedia default data index as well as OpenNLP language models. It also comes with a default configuration providing some Enhancement Chains.

Users that want to get started with Apache Stanbol should prefer this launcher over the stateless.

To start this after building use:

    java -Xmx512M -jar target/org.apache.stanbol.launchers.stable-*-SNAPSHOT.jar

The Stanbol Enhancer HTTP endpoint should then be available at 

    http://localhost:8080

So that you can POST content using, for example:

    curl -H "Content-Type: text/plain" \
        -T ../../enhancer/data/text-examples/obama-signing.txt \
        http://localhost:8080/engines

Configure any required parameter for the enhancement engines, at

    http://localhost:8080/system/console/

The OSGi state is stored in the ./stanbol folder.

The logs are found at stanbol/logs/error.log and can be configured from the
OSGi console.
