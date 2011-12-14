Apache Stanbol (Incubating) 
Reasoners


Building Apache Stanbol Reasoners
=============

Build
---------------------------------------------
Checkout the source::

  % svn co https://svn.apache.org/repos/asf/incubator/stanbol/trunk/reasoners reasoners

Build and run the tests::

  % cd reasoners
  % mvn clean install

Installation
---------------------------------------------
Run Stanbol, for example:

 % java -jar -Xmx1g org.apache.stanbol.launchers.full-0.9.0-incubating-SNAPSHOT.jar
 

You must have the Ontonet and Rules modules already installed (they are if you have followed the above example).
Move to the /reasoners directory, then run

 % mvn install -PinstallBundle -Dsling.url=<the path to your running Felix administration console>

for example

 % mvn install -PinstallBundle http://localhost:8080/system/console

 

Description
=============

* A serviceapi for ReasoningServices, using SCR
* Base OWLApi and Jena abstract services
* Jena RDFS,OWL,OWLMini reasoning services
* HermiT reasoning service

* A common REST endpoint at /reasoners with the following preloaded services:
**    /rdfs
**    /owl
**    /owlmini
**    /owl2

each can be accessed with one of three tasks: check,enrich,classify, for example:

/reasoners/owl/check    (the Jena owl service with task classify)
or
/reasoners/owl2/classify (the hermit service with task classify)

Tasks description:
* check    : is the input consistent? 200 =true, 204 =false
* classify : return only rdf:type inferences
* enrich   : return all inferences

This is how the endpoint behave:

GET (same if POST and Content-type: application/x-www-form-urlencoded)
params:
* url        // Loads the input from url
* target  // (optional) If given, save output in the store (TcManager) and does not return the stream

for example:
$ curl "http://localhost:8080/reasoners/owl2/classify?url=http://xmlns.com/foaf/0.1/"

POST   [Content-type: multipart/form-data]
* file       // Loads from the input stream
* target  // (optional)  If given, save output in the store (TcManager) and does not return the stream

Other parameters can be sent, to support inputs from Ontonet and Rules:
These additional parameters can be sent:
* scope // the ID of an Ontonet scope
* session // The ID of an Ontonet session
* recipe  // The ID of a recipe from the Rules module (only with OWLApi based services)s

Supported output formats:
Supported return formats are all classic RDF types (n3,turtle,rdf+xml) and HTML. For HTML the returned statements are provided in Turtle (Jena) or OWL Manchester syntax (OWLApi), wrapped in the stanbol layout. It would be nice to have all in the latter, which is very much readable (todo).

Recently added
=============

* Support for long-term operations. 

Todo
=============

* Support for return types json and json-ld (need to write jersey writers)
* The front service actually returns only inferred statements. It is useful also to have the complete set of input+inferred statements
* Decouple input preparation from the rest endpoint resource, creating something like an InputProvider SCR api;  each InputProvider is bound to a set of additional parameters. 
This have several benefits:
** Remove of additional optional parameters, bound to specific input sources from the default rest api (ex, session, scope, recipe)
** Remove dependencies to ontonet, rules and other modules which are not needed for standard usage. They could be implemented as InputProvider/s, bound to specific parameters.
** Allow the addition of other input sources (for example 'graph', 'entity' or 'site') 
* Implement a Custom Jena ReasoningService, to use a Jena rules file or a stanbol recipe (when implemented the toJena() functionality in the rules module) from configuration. This could be done as multiple SCR instance, as it is now for entityhub sites, for example.
* Provide a validation report in case of task CHECK (validity check).
* Implement a progress monitor, relying on the jena and owlapi apis, which have this feature, for debugging purpose
* Implement a benchmark endpoint, relying on OWL manchester syntax, to setup benchmark tests in the style of the one made for the enhancer
* Implementing owllink client reasoning service
* Implement additional data preparation steps, for example to implement a "consistent refactoring" task. For example, giving a parameter 'refactor=<recipe-id>' the service could refactor the graph before execute the task.
* Implement off the shelf reasoning services (for example, targeted to resolve only owl:sameAs links)

General issues
=============
The main problem is performance, which decrease while the input data grows, in some cases dramatically. This could be faced (IMHO), in two directions:
* Improve input preparation. In particular, the preparation of input form ontonet scope/session needs to stream the ontologies, in cases of more input (url provided) twice!, and this have some drawback on performance.
* Support long-term operations, to start the process from the REST call and then ping it's process through a dedicated endpoint

Notes (to be known)
=============
Differences between Jena and OWLApi services:
* CHECK have different meaning with respect to the reasoning service implementation



Examples
=============
#
# Basic GET calls to the reasoning services.
# Send a URL and the service will return the inferred triples
#
# Classify the FOAF ontology, getting it from the web using the Jena OWL reasoner, result in turtle
curl -v -H "Accept: application/turtle" "http://localhost:8080/reasoners/owl/classify?url=http://xmlns.com/foaf/0.1/"

# Classify the FOAF ontology, getting it from the web using the Jena OWL reasoner, result in n3
curl -v -H "Accept: text/n3" "http://localhost:8080/reasoners/owl/classify?url=http://xmlns.com/foaf/0.1/"

# Enrich the FOAF ontology, getting it from the web using the Jena RDFS reasoner, result in rdf/xml
curl -v -H "Accept: application/rdf+xml" "http://localhost:8080/reasoners/owl/classify?url=http://xmlns.com/foaf/0.1/"

# Check consistency of the FOAF ontology, getting it from the web using the Jena OWL reasoner, result in turtle
curl -v "http://localhost:8080/reasoners/owl/check?url=http://xmlns.com/foaf/0.1/"

# Check consistency of the FOAF ontology, getting it from the web using the Hermit OWL2 reasoner, result in turtle
curl -v "http://localhost:8080/reasoners/owl2/check?url=http://xmlns.com/foaf/0.1/"

# Trying with an ontology network (large ontology composed by a set of little ontologies connected through owl:import statements)
curl -v "http://localhost:8080/reasoners/owl2/check?url=http://www.cnr.it/ontology/cnr/cnr.owl"
# or
curl -v "http://localhost:8080/reasoners/owl2/enrich?url=http://www.cnr.it/ontology/cnr/cnr.owl"

#
# POST calls (send a file)
#
# Send the foaf.rdf file to a reasoning service and see the output
# (get it with 
curl -H "Accept: application/rdf+xml"  http://xmlns.com/foaf/0.1/ > foaf.rdf 
# )
curl -X POST -H "Content-type: multipart/form-data" -H "Accept: text/turtle" -F file=@foaf.rdf "http://localhost:8080/reasoners/rdfs/enrich"

# Save output in the triple store instead of return
# >> Add the "target" parameter, with the graph identifier
curl "http://localhost:8080/reasoners/owl/classify?url=http://xmlns.com/foaf/0.1/&target=example-foaf-inferred"
# or, posting a file
curl -X POST -H "Content-type: multipart/form-data" -F file=@foaf.rdf -F target=example-rdfs-inferences "http://localhost:8080/reasoners/rdfs/enrich"

