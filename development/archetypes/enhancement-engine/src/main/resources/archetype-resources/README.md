Example Apache Stanbol Enhancement Engine
======

This provides an example engine. The engine will add an ennnoation with a 
rdfs:comment property to a sentence containing the number of charactes in
the annotated document (e.g. "A text of 6 charaters" ) .

To compile the engine run

    mvn install

To deploy the engine to a stanbol instance running on localhost port 8080 run

    mvn org.apache.sling:maven-sling-plugin:install

To see you engine in action you may use 
http://localhost:8080/enhancer/chain/all-active to enhance an arbitrary text.