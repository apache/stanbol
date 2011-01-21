Jersey front-end to the FISE engine
===================================

Goals for this sub-project:

- RESTful web services API to FISE for machines.

- Human-friendly HTML interface to quickly test the service and document the API.


Building from source
--------------------

Checkount and build both iks-autotagging and FISE:

  $ svn checkout http://iks-project.googlecode.com/svn/sandbox/iks-autotagging/trunk iks-autotagging
  $ cd iks-autotagging
  $ mvn clean install -DskipTests
  $ cd ..

  $ svn checkout http://iks-project.googlecode.com/svn/sandbox/fise/trunk fise
  $ cd fise
  $ mvn clean install -DskipTests


Deployment
----------

Go to `launchers/sling/target` and run::

  rm -rf sling && java -jar org.apache.stanbol.fise.launchers.sling-*-SNAPSHOT.jar

Once deployed (check the logs) you can either use the HTML interface:

  http://localhost:8080

To submit data to engines using a simple form:

  http://localhost:8080/engines

If that page gives errors such as class not found, no web provider, stop and restart the jersey-server bundle.
There's a bug in Jersey 1.2 that causes those if the core bundle starts before the server bundle.

You can setup an apache virtualhost to blend a fise instance on you domain name
using a ``NameVirtualHost *:80`` instruction in you global apache configuration
and the following virtualhost parameters::

  <VirtualHost *:80>

  ServerName fise.example.com

  CustomLog logs/fise.example.com.access.log combined
  ErrorLog logs/fise.example.com.error.log

  ProxyPass/  http://localhost:8080/
  ProxyPassReverse/ http://localhost:8080/
  ProxyPreserveHostOn

  </VirtualHost>


Stateless operation
-------------------

You can use the HTTP (somewhat RESTful) API directly with cURL for instance:

  $ curl -X POST -H "Content-type: text/plain" --data "Paris is a beautiful city." http://localhost:8080/engines/

You can force a specific RDF serialization scheme by setting the "Accept" HTTP header:

  $ curl -X POST -H "Content-type: text/plain" -H "Accept: application/rdf+xml" --data "Paris is a beautiful city." http://localhost:8080/engines/

Or to upload the content of a file:

  $ curl -X POST -H "Content-type: text/plain" -H "Accept: text/rdf+nt" --data @/path/to/my_local_file.txt http://localhost:8080/engines/


Asynchronous and stateful operation
-----------------------------------

TODO: implement and document me!

