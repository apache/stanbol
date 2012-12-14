Enhancement engine that uses the Zemanta API. You need a Zemanta API key to run this engine.

Running
-------

- build ("mvn install") and deploy the Clerezza bundle org.apache.clerezza.rdf.jena.parser

- build the jar ("mvn install")

- import the jar into the OSGi runtime

- In the OSGi web console, set the property "org.apache.stanbol.enhancer.engines.zemanta.key" with your API key
  (restart the bundle in the OSGi console)

- Watch the console when you add text using commands such as:

  curl -T myText.txt -H Content-Type:text/plain http://localhost:8080/fise/someId

