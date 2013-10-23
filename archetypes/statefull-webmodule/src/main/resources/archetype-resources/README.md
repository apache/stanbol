Example Apache Stanbol Component
===========

This is an example Apache Stanbol component.

To compile the engine run

    mvn install

To deploy the engine to a stanbol instance running on localhost port 8080 run

    mvn org.apache.sling:maven-sling-plugin:install


After installing a new menu item pointing you to /${artifactId} will appear.

The example service allows to look up resources using the site-manager. The 
service can be accessed via browser as HTML or as RDF for machine clients.
