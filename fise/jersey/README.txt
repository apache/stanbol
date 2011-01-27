Jersey front-end to the FISE engine
===================================

Goals for this sub-project:

- RESTful web services API to Stanbol Enhancer for machines.

- Human-friendly HTML interface to quickly test the service and document the API.


Building from source
--------------------

Checkount and build Stanbol Enhancer and Stanbol Entityhub:

  $ svn checkout http://svn.apache.org/repos/asf/incubator/stanbol/trunk/entityhub stanbol-entityhub
  $ cd stanbol-entityhub
  $ mvn clean install
  $ cd ..

  $ svn checkout http://svn.apache.org/repos/asf/incubator/stanbol/trunk/enhancer stanbol-enhancer
  $ cd stanbol-enhancer
  $ mvn clean install



Deployment
----------

Go to 
  - 'launchers/lite/target' to run the default configuration that comes with
      a set of engines that is considered as stable
  - 'launchers/full/target' to run the configuration that contains all availanle
      engines.Stanbol

  rm -rf sling && java -jar org.apache.stanbol.enhancer.launchers.sling-*-SNAPSHOT.jar

Once deployed (check the logs) you can either use the HTML interface:

  http://localhost:8080

To submit data to engines using a simple form:

  http://localhost:8080/engines

If that page gives errors such as class not found, no web provider, stop and restart the jersey-server bundle.
There's a bug in Jersey 1.2 that causes those if the core bundle starts before the server bundle.

You can setup an apache virtualhost to blend a Stanbol ehnacer instance on you 
domain name using a ``NameVirtualHost *:80`` instruction in you global apache 
configuration and the following virtualhost parameters::

  <VirtualHost *:80>

  ServerName stanbol-enhancer.example.com

  CustomLog logs/stanbol-enhancer.example.com.access.log combined
  ErrorLog logs/stanbol-enhancer.example.com.error.log

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

