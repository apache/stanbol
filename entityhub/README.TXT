Building RICK
=============

System Requirements
--------------------------------------------

You need Java 6 and maven (version as defined in the pom)
You probably need
  export MAVEN_OPTS="-Xmx512M -XX:MaxPermSize=128M"
or similar.

Building the RICK Framework
--------------------------------------------

Checkout the source::

  % svn co https://iks-project.googlecode.com/svn/sandbox/rick/trunk/ iks-rick

Build and run the tests::

  % cd iks-rick
  % mvn clean install

Launch the FISE server::

  % cd launchers/sling/target
  % rm -rf sling                # erase previous sling install if any (optional)
  % java -Xmx512M -jar org.apache.stanbol.entityhub.launchers.sling-*-SNAPSHOT.jar



Configuring RICK
================

Connect your browser to the Apache Felix Admin Console 
(http://localhost:8080/system/console)
   user: admin
   pwd:  admin

Open the Cofiguration Tab (http://localhost:8080/system/console/configMgr)

Configuring Referenced Sites
--------------------------------------------

First configure one or more Referenced Sites by clicking on
   IKS Referenced Site Configuration

The default values can be used to configure dbPedia.org. Other examples can be
found in *-siteConfig.txt files (e.g. musicbrainz-siteConfig.txt)

Configuring Yards
--------------------------------------------

Second configure a Yard (Storage Component) of the RICK framework.
Configuring a Yard is done by using by one of the available Yard implementations.
Currently there are two different Yard Implementations available:
 - ClerezzaYard: Implementation based on a RDF TripleStore
 - SolrYard: Implementation using an external Solr Server

Click at
   IKS RICK YARD: Clerezza Yard Configuration
to configure a Clerezza Yard instance or
   IKS RICK YARD: Solr Yard Configuration
to configure a Solr Yard instance

You need to configure a Yard for the RICK. This Yard will be used to store the
Symbols and EntityMappings defined by the RICK. This Yard is a required
dependency of the RICK and must be configured before the RICK can be used.
The Yard used by the RICK to store its information is called the RickYard.

The default values provided by the Yard Configuration Dialog do contain values
suitable for the RickYard. If you need to configure Yard instances for other
purposes you need to change the ID to a different value.
The suggestion is to use the id of the site followed by Yard (e.g. the yard for
a site with the ID "dbPedia" should be called "dbPediaYard".

Configuring the RICK
--------------------------------------------

As last step one needs to set the configuration of the Rick. To do that click on
   IKS RICK Configuration

Just use the default values. But note that the value of the "Rick Yard" property
MUST BE set to an ID of an active Yard.

After completing this three steps all required components of the Rick framework
should be active meaning that you can start to use the RICK.


OSGI Components of the RICK
===========================

The Components of the RICK are all listed in the component tab of the 
Apache Felix Web Console (http://localhost:8080/system/console/components).
The RICK uses the following components:
 - org.apache.stanbol.entityhub.core.impl.EntityhubConfigurationImpl 
     (Singelton holding the configuration of the Rick)
 - org.apache.stanbol.entityhub.core.impl.EntityhubImpl
     (Singelton implementing the Rick Interface - Java API)
 - org.apache.stanbol.entityhub.core.impl.YardManagerImpl
     (Singelton that keeps track of all the running Yard instances)
 - org.apache.stanbol.entityhub.jersey.JerseyEndpoint
     (Singelton that starts the RESTful Service Implementation - RESTful API)
 - org.apache.stanbol.entityhub.site.referencedSite
     (Multiple instances - one for each Site referenced by the Rick)
 - org.apache.stanbol.entityhub.yard.clerezza.impl.ClerezzaYard
     (Multiple instances - Best suited for small and medium sized caches of
      semantic web data)
 - org.apache.stanbol.entityhub.yard.solr.impl.SolrYard
     (Multiple instances - Best suited for full text queries. Better performance
     for Representations with a limited number of different fields)
 - org.apache.stanbol.entityhub.site.CoolUriDereferencer
     (used and instantiated by ReferencedSite based on there configuration)
 - org.apache.stanbol.entityhub.site.SparqlDereferencer
     (used and instantiated by ReferencedSite based on there configuration)
 - org.apache.stanbol.entityhub.site.SparqlSearcher
     (used and instantiated by ReferencedSite based on there configuration)
 - org.apache.stanbol.entityhub.site.VirtuosoSearcher
     (used and instantiated by ReferencedSite based on there configuration)


RESTfull Interface 
==================

Initial Notes:
--------------------------------------------

This describes the first (alpha) implementations of the RESTfull API for
the RICK.
This API will be changed and extended a lot in future versions.

Currently there is no HTML interface to the Rick

Supported ContentType (for all services unless noted otherwise)
  - application/json
  - application/rdf+xml
  - text/turtle
  - application/x-turtle
  - text/rdf+nt
  - text/rdf+n3
  - application/rdf+json


SITES Service Endpoint "/sites"
-------------------------------------------

This endpoint allows to access all currently active referenced sites

 - GET /sites/referenced
     produces: application/json

This service returns a json array containing the IDs of all referenced sites.
Sites returned by this Method can be accessed via the SITE service endpoint

Example:
Request
    curl  "http://localhost:8080/sites/referenced"
Response
    ["http:\/\/localhost:8080\/site\/dbPedia\/","http:\/\/localhost:8080\/site\/musicbrainz\/"]

-------------------------------------------

 - GET /sites/entity?id={URI}
    parameter:
      id: the URI of the requested Entity 
This service searches all referenced sites for the entity with the parsed URI
and returns the result in the requested entity in the mediaType.
If the requested entity can not be found a 404 is returned.

Example:
  curl  "http://localhost:8080/sites/entity?id=http://dbpedia.org/resource/Paris"

-------------------------------------------
  
 - GET /sites/find?name={query}&lang={lang}&limit={limit}&offset={offset}
 - POST -d "name={query}&lang={lang}&limit={limit}&offset={offset}" /sites/find
    parameter:
        name: the name of the entity (supports wildcards e.g. "Frankf*")
        lang: optionally the language of the parsed name can be defined
        limit: optionally the maximum number of results
        offset: optionally the offset of first result

This service can be used to search all referenced sites for entities with the
parsed name. Both a POST and a GET version are available

POST Example:
  curl -X POST -d "name=Bischofsh*&lang=de&limit=10&offset=0" http://localhost:8080/sites/find


SITE Service Endpoint "/site/{siteID}"
-------------------------------------------   

The SITE endpoint allows to interact with a specific referenced site. All 
available referenced site can be queried by making a GET request to 
"/sites/referenced".
The following Services are provided by the "/site/{siteId}" endpoint

-------------------------------------------

 - GET /site/{site}/entity?id={entityID}
    Parameter:
        site: is the ID configured for the referenced site (e.g. "dbPedia")
        id: is the URI of the requested Entity

Example:
curl -X GET -H "Accept: application/json" http://localhost:8080/site/dbPedia/entity?id=http://dbpedia.org/resource/Paris

--------------------------------------------

 - GET /site/{site}/find?nams={name}&lang={lang}&limit={limit}&offset={offset}
 - POST -d "name={query}&lang={lang}&limit={limit}&offset={offset}" /site/{site}/find
    parameter:
        site: is the ID configured for the referenced site (e.g. "dbPedia")
        name: the name of the entity (supports wildcards e.g. "Frankf*")
        lang: optionally the language of the parsed name can be defined
        limit: optionally the maximum number of results
        offset: optionally the offset of first result


Note that this method suffers form very bad performance on SPARQL Endpoints that
do not support extensions for full text searches.

Example:
curl -X POST -d "name=Frankf*&lang=de&limit=10&offset=0" http://localhost:8080/site/dbPedia/find


RICK Endpoint ("/symbol" and "/mapping")
--------------------------------------------

 - GET /symbol?id={uri}
    Parameter
        id: The uri of the symbol

Service to get Symbols by id

Request
    curl "http://localhost:8080/symbol?id=rick/symbol.2e64fd20-0df8-2d0c-0358-2e421c7d8f22"
Response
{
    "id": "rick\/symbol.2e64fd20-0df8-2d0c-0358-2e421c7d8f22",
    "site": "rick",
    "representation": { ... data removed ... },
    "label": "Wien",
    "stateUri": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/symbolState-proposed",
    "state": "proposed"
}

Service to get a symbol by id

--------------------------------------------

 - GET /symbol/lookup?id={uri}&create={create}
    Parameter:
        id: the id of the entity
        create: if "true" a new symbol is created if necessary and allowed
        
This service looks-up Symbols (Entities managed by the RICK) based on the parsed
URI. The parsed id can be the URI of a Symbol or an Entity of any referenced 
site.
If the parsed "id" is a URI of a Symbol, than the stored information of the Symbol
are returned in the requested media type (Accept header field).
If the parsed "id" is a URI of an already mapped entity, that the existing mapping
is used to get the according Symbol.
If "create" is enabled, and the parsed URI is not already mapped to a Symbol, than
all the currently active referenced sites are searched for an Entity with the
parsed URI. If the configuration of the referenced site allows to create new
symbols, than a the entity is imported in the RICK, a new Symbol and EntityMapping
is created and the newly created Symbol is returned.
In case the entity is not found (this also includes if the entity would be
available via a referenced site, but create=false) a 404 "Not Found" is returned
In case the entity is found on a referenced site, but the creation of a new
Symbol is not allowed a 403 "Forbidden" is returned.


Example 1 (without the create parameter (default value for create=false))
    curl -H "Accept: application/json" "http://localhost:8080/symbol/lookup/?id=http://dbpedia.org/resource/Wien"

This Example looksup the Symbol for the dbPedia Entity "Wien" (The German name 
for Vienna). If a EntityMapping for this Entity is present in the RICK, than this
call returns the Symbol. For a example see the Response of the example of the 
"/symbol?id={symbolId}" above.

A request with the ID of the Symbol would result in the same response
    curl "http://localhost:8080/symbol/lookup/?id=rick/symbol.2e64fd20-0df8-2d0c-0358-2e421c7d8f22"

Example 2 (with create=true)
    curl "http://localhost:8080/symbol/lookup/?id=http://dbpedia.org/resource/Hallein&create=true"

In this case a new Symbol and EntityMapping for the city "Hallein" would be
created if not already present in the RICK.

--------------------------------------------

 - GET /mapping?id={uri}
    Parameter
        id: The uri of the mapping

Service to get a mapping by id

Example:
    curl "http://localhost:8080/mapping?id=rick/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5"
Response:
{
    "id": "rick\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
    "site": "rick",
    "representation": {
        "id": "rick\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedSymbol": [{
            "type": "reference",
            "value": "rick\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6"
        }],
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/hasMappingState": [{
            "type": "reference",
            "value": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed"
        }],
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedEntity": [{
            "type": "reference",
            "value": "http:\/\/dbpedia.org\/resource\/Hallein"
        }]
    },
    "symbol": "rick\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6",
    "entity": "http:\/\/dbpedia.org\/resource\/Hallein",
    "stateUri": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed",
    "state": "proposed"
}

--------------------------------------------

 - GET /mapping/entity?id={uri}
    Parameter
        id: The uri of the entity

This service allows to retrieve the mapping for a entity. If now mapping for the
parsed uri is defined, the service returns a 404 "Not Found"

Example:
    curl "http://localhost:8080/mapping/entity/?id=http://dbpedia.org/resource/Hallein"

Response:
{
    "id": "rick\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
    "site": "rick",
    "representation": {
        "id": "rick\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedSymbol": [{
            "type": "reference",
            "value": "rick\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6"
        }],
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/hasMappingState": [{
            "type": "reference",
            "value": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed"
        }],
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedEntity": [{
            "type": "reference",
            "value": "http:\/\/dbpedia.org\/resource\/Hallein"
        }]
    },
    "symbol": "rick\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6",
    "entity": "http:\/\/dbpedia.org\/resource\/Hallein",
    "stateUri": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed",
    "state": "proposed"
}

--------------------------------------------

 - GET /mapping/symbol?id={uri}
    Parameter
        id: The uri of the symbol

This service allows to retrieve all mappings defined for a symbol. Note that one
Symbol can be mapped to 1..n Entities. If no Symbol with the parsed URI is defined
by the RICK, than this service returns a 404 "Not Found"

Example
    curl "http://localhost:8080/mapping/symbol?id=rick/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6"
Response:
{"results": [{
    "id": "rick\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
    "site": "rick",
    "representation": {
        "id": "rick\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedSymbol": [{
            "type": "reference",
            "value": "rick\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6"
        }],
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/hasMappingState": [{
            "type": "reference",
            "value": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed"
        }],
        "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedEntity": [{
            "type": "reference",
            "value": "http:\/\/dbpedia.org\/resource\/Hallein"
        }]
    },
    "symbol": "rick\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6",
    "entity": "http:\/\/dbpedia.org\/resource\/Hallein",
    "stateUri": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed",
    "state": "proposed"
}]}
Note that the response contains a Json Object with the key "results" as root that
has a Json Array containing all EntityMappings for the parsed Symbol.
