Named Entity Recognizer based on OpenNLP

Running
-------

- Create the jar ("mvn install")

- Import the jar in Karaf.

- From the Karaf web console, set the property "org.apache.stanbol.enhancer.engines.opennlp.models.path"
  (on the NER bundle screen) to the absolute path of the "data" directory included in the sources.

- Watch the console when you add text using commands such as:

  curl -T myText.txt -H Content-Type:text/plain http://localhost:8181/enhancer/someId

TODO
----

1. Use more sensible URIs in RDF assertions.

2. Recognize not only person and places, names but also organizations, etc.

3. Work in other languages than English.
