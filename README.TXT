<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

Building Apache Stanbol Entityhub
=============

System Requirements
--------------------------------------------

You need Java 6 and maven (version as defined in the pom)
You probably need
  export MAVEN_OPTS="-Xmx512M -XX:MaxPermSize=128M"
or similar.

Building the Apache Stanbol Entityhub Framework
--------------------------------------------

Checkout the source::

  % svn co https://svn.apache.org/repos/asf/incubator/stanbol/trunk/entityhub/ entityhub

Build and run the tests::

  % cd entityhub
  % mvn clean install

Launch the FISE server::

  % cd launchers/sling/target
  % rm -rf sling                # erase previous sling install if any (optional)
  % java -Xmx512M -jar org.apache.stanbol.entityhub.launchers.sling-*-SNAPSHOT.jar



Configuring Apache Stanbol Entityhub
================

Connect your browser to the Apache Felix Admin Console 
(http://localhost:8080/system/console)
   user: admin
   pwd:  admin

Open the Configuration Tab (http://localhost:8080/system/console/configMgr)

Configuring Referenced Sites
--------------------------------------------

First configure one or more Referenced Sites by clicking on
   Apache Stanbol Entityhub Referenced Site Configuration

The default values can be used to configure dbpedia.org. Other examples can be
found in *-siteConfig.txt files (e.g. musicbrainz-siteConfig.txt)

Configuring Yards
--------------------------------------------

Second configure a Yard (Storage Component) of the Apache Stanbol Entityhub framework.
Configuring a Yard is done by using by one of the available Yard implementations.
Currently there are two different Yard Implementations available:
 - ClerezzaYard: Implementation based on a RDF TripleStore
 - SolrYard: Implementation using an external Solr Server

Click at
   IKS Apache Stanbol Entityhub YARD: Clerezza Yard Configuration
to configure a Clerezza Yard instance or
   IKS Apache Stanbol Entityhub YARD: Solr Yard Configuration
to configure a Solr Yard instance

You need to configure a Yard for the Apache Stanbol Entityhub. This Yard will be used to store the
Symbols and EntityMappings defined by the Apache Stanbol Entityhub. This Yard is a required
dependency of the Apache Stanbol Entityhub and must be configured before the Apache Stanbol Entityhub can be used.
The Yard used by the Apache Stanbol Entityhub to store its information is called the RickYard.

The default values provided by the Yard Configuration Dialog do contain values
suitable for the RickYard. If you need to configure Yard instances for other
purposes you need to change the ID to a different value.
The suggestion is to use the id of the site followed by Yard (e.g. the yard for
a site with the ID "dbpedia" should be called "dbpediaYard".

Configuring the Apache Stanbol Entityhub
--------------------------------------------

As last step one needs to set the configuration of the Entityhub. To do that click on
   IKS Apache Stanbol Entityhub Configuration

Just use the default values. But note that the value of the "Entityhub Yard" property
MUST BE set to an ID of an active Yard.

After completing this three steps all required components of the Rick framework
should be active meaning that you can start to use the Apache Stanbol Entityhub.


OSGI Components of the Apache Stanbol Entityhub
===========================

The Components of the Apache Stanbol Entityhub are all listed in the component tab of the 
Apache Felix Web Console (http://localhost:8080/system/console/components).
The Apache Stanbol Entityhub uses the following components:
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


RESTful Service Interface
===========================

Initial Notes:
--------------------------------------------

This describes the alpha release of the RESTfull API of the Apache Stanbol 
Entityhub. That means, that this API will be changed and extended a lot in 
future versions.

Currently there is no HTML interface to the Rick Supported MediaTypes 
(for all services unless noted otherwise)
    application/json (default)
    application/rdf+xml
    text/turtle
    application/x-turtle
    text/rdf+nt
    text/rdf+n3
    application/rdf+json

The Apache Stanbol Entityhub supports three Service Endpoints all under the 
Apache Stanbol Entityhub root node (default "/entityhub"):
    SITES endpoint: This endpoint can be used to access all referenced sites via
        a single URI. It also provides services to retrieve meta data about 
        referenced sites.
    SITE endpoint: This endpoint allows to interact with a specific referenced 
        site. Calls take advantage of local caches (if available)
    Entityhub endpoint: This endpoint allows to work with the 
        Entities managed by the Apache Stanbol Entityhub (called Symbols) and 
        mappings of external Entities to Symbols.

SITES Service Endpoint "/sites"
--------------------------------------------

    /sites/referenced

Request: GET /sites/referenced
Parameter: none
Produces: application/json
Description: This service returns a json array containing the IDs of all 
    referenced sites. Sites returned by this Method can be accessed via the SITE 
    service endpoint
Example:
   curl  "http://localhost:8080/entityhub/sites/referenced"
Response
   ["http:\/\/localhost:8080\/entityhub\/site\/dbpedia\/",
    "http:\/\/localhost:8080\/entityhub\/site\/musicbrainz\/"]


    /sites/entity?id={URI}

Request: GET /sites/entity?id={URI}
Parameter:
    id: the URI of the requested Entity
Description: This service searches all referenced sites for the entity with the 
    parsed URI and returns the result in the requested entity in the mediaType. 
    If the requested entity can not be found a 404 is returned.
Example:
   curl  "http://localhost:8080/entityhub/sites/entity?id=http://dbpedia.org/resource/Paris"

    
    /sites/find?name={query}

Request: 
    GET /sites/find?name={query}&field={field}&lang={lang}&limit={limit}&offset={offset}
    POST -d "name={query}&field={field}&lang={lang}&limit={limit}&offset={offset}" /sites/find
Parameter:
    name: the name of the entity (supports wildcards e.g. "Frankf*")
    field: the name of the field used for the query. One MUST parse the full
           name. Namespace prefixes are not supported yet. (default is rdfs:label)
    lang: optionally the language of the parsed name can be defined
    limit: optionally the maximum number of results
    offset: optionally the offset of first result
Description: This service can be used to search all referenced sites for 
    entities with the parsed name. Both a POST and a GET version are available
Example:
   curl -X POST -d "name=Bischofsh*&lang=de&limit=10&offset=0" http://localhost:8080/entityhub/sites/find


    /sites/query&query={query}

Allows to parse JSON serialzed FieldQueries to the sites endpoint.

Request:
    POST -d "query={query}" /sites/query
Parameter:
    query: the JSON serialized FieldQuery (see section "FieldQuery JSON format" 
           below)
Example:
   curl -X POST -F "query=@fieldQuery.json" http://localhost:8080/entityhub/site/dbpedia/query
Note: that "@fieldQuery.json" links to a local file that contains the parsed
    Fieldquery (see ection "FieldQuery JSON format" for examples)
Note: This method suffers form very bad performance on SPARQL Endpoints that do 
    not support extensions for full text searches. On Virtuoso Endpoints do 
    performance well under normal conditions
Note: Optional selects suffers form very bad performance on any SPRQL Endpoint.
    It is recommended to select only fields that are used for constraints. If
    more data are required it is recommended to dereference found entities after
    recieving initial results of the query.


SITE Service Endpoint "/site/{siteID}"
--------------------------------------------

The SITE endpoint allows to interact with a specific referenced site. 
All available referenced site can be queried by making a GET request to 
"/sites/referenced". The following Services are provided by the "/site/{siteId}" 
endpoint


    /site/{site}/entity?id={entityID}

Request: GET /site/{site}/entity?id={entityID}
Parameter
    site: is the ID configured for the referenced site (e.g. "dbpedia")
    id: is the URI of the requested Entity
Example
   curl -X GET -H "Accept: application/json" http://localhost:8080/entityhub/site/dbpedia/entity?id=http://dbpedia.org/resource/Paris


    /site/{site}/find?name={name}

Request:
    GET /site/{site}/find?name={name}&field={field}&lang={lang}&limit={limit}&offset={offset}
    POST -d "name={query}&field={field}&lang={lang}&limit={limit}&offset={offset}" /site/{site}/find
Parameter:
    site: is the ID configured for the referenced site (e.g. "dbpedia")
    name: the name of the entity (supports wildcards e.g. "Frankf*")
    field: the name of the field used for the query. One MUST parse the full
           name. Namespace prefixes are not supported yet. (default is rdfs:label)
    lang: optionally the language of the parsed name can be defined
    limit: optionally the maximum number of results
    offset: optionally the offset of first result
Note: This method suffers form very bad performance on SPARQL Endpoints that do 
    not support extensions for full text searches. On Virtuoso Endpoints do 
    performance well under normal conditions
Example:
   curl -X POST -d "name=Frankf*&lang=de&limit=10&offset=0" http://localhost:8080/entityhub/site/dbpedia/find


    /site/{site}/query&query={query}

Allows to parse JSON serialzed FieldQueries to the site endpoint.

Request:
    POST -d "query={query}" /site/{site}/query
Parameter:
    site: is the ID configured for the referenced site (e.g. "dbpedia")
    query: the JSON serialized FieldQuery (see section "FieldQuery JSON format" 
           below)
Example:
   curl -X POST -F "query=@fieldQuery.json" http://localhost:8080/entityhub/site/dbpedia/query
Note: that "@fieldQuery.json" links to a local file that contains the parsed
    Fieldquery (see ection "FieldQuery JSON format" for examples)
Note: This method suffers form very bad performance on SPARQL Endpoints that do 
    not support extensions for full text searches. On Virtuoso Endpoints do 
    performance well under normal conditions
Note: Optional selects suffers form very bad performance on any SPRQL Endpoint.
    It is recommended to select only fields that are used for constraints. If
    more data are required it is recommended to dereference found entities after
    recieving initial results of the query.


Entityhub Endpoint ("/symbol" and "/mapping")
--------------------------------------------


    /symbol?id={uri}

Request: GET /symbol?id={uri}
Parameter
    id: The uri of the symbol
Description: Service to get Symbols by id
Request
    curl "http://localhost:8080/entityhub/symbol?id=entityhub/symbol.2e64fd20-0df8-2d0c-0358-2e421c7d8f22"
Response
    {
       "id": "entityhub\/symbol.2e64fd20-0df8-2d0c-0358-2e421c7d8f22",
       "site": "entityhub",
       "representation": { ... data removed ... },
       "label": "Wien",
       "stateUri": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/symbolState-proposed",
       "state": "proposed"
    }


    /symbol/lookup?id={uri}&create={create}

Request: GET /symbol/lookup?id={uri}&create={create}
Parameter:
    id: the id of the entity
    create: if "true" a new symbol is created if necessary and allowed
Description: This service looks-up Symbols (Entities managed by the Apache 
    Stanbol Entityhub) based on the parsed URI. The parsed id can be the URI of 
    a Symbol or an Entity of any referenced site. If the parsed "id" is a URI 
    of a Symbol, than the stored information of the Symbol are returned in the 
    requested media type (Accept header field). If the parsed "id" is a URI of 
    an already mapped entity, that the existing mapping is used to get the 
    according Symbol. If "create" is enabled, and the parsed URI is not already 
    mapped to a Symbol, than all the currently active referenced sites are 
    searched for an Entity with the parsed URI. If the configuration of the 
    referenced site allows to create new symbols, than a the entity is imported 
    in the Apache Stanbol Entityhub, a new Symbol and EntityMapping is created 
    and the newly created Symbol is returned. In case the entity is not found 
    (this also includes if the entity would be available via a referenced site, 
    but create=false) a 404 "Not Found" is returned In case the entity is found 
    on a referenced site, but the creation of a new Symbol is not allowed a 403 
    "Forbidden" is returned.

Example 1: without the create parameter (default value for create=false)

    curl -H "Accept: application/json" "http://localhost:8080/entityhub/symbol/lookup/?id=http://dbpedia.org/resource/Wien"

This Example looksup the Symbol for the dbpedia Entity "Wien" (The German name 
for Vienna). If a EntityMapping for this Entity is present in the Apache Stanbol 
Entityhub, than this call returns the Symbol. For a example see the Response of 
the example of the "/symbol?id={symbolId}" above. A request with the ID of the 
Symbol would result in the same response

    curl "http://localhost:8080/entityhub/symbol/lookup/?id=entityhub/symbol.2e64fd20-0df8-2d0c-0358-2e421c7d8f22"

Example 2: with create=true
    
    curl "http://localhost:8080/entityhub/symbol/lookup/?id=http://dbpedia.org/resource/Paris&create=true"

In this case a new Symbol and EntityMapping for the city "Paris" would be 
created if not already present in the Apache Stanbol Entityhub.


    /symbol/find&name={name}

Finds Symbols (Entities managed by the Entityhub) based on the parsed arguments

Request:
    GET /symbol/find?name={name}&field={field}&lang={lang}&limit={limit}&offset={offset}
    POST -d "name={query}&field={field}&lang={lang}&limit={limit}&offset={offset}" /symbol/find
Parameter:
    name: the name of the entity (supports wildcards e.g. "Frankf*")
    field: the name of the field used for the query. One MUST parse the full
           name. Namespace prefixes are not supported yet. The default is the 
           symbol name (http://www.iks-project.eu/ontology/rick/model/name)
    lang: optionally the language of the parsed name can be defined
    limit: optionally the maximum number of results
    offset: optionally the offset of first result
Example:
   curl -X POST -d "name=Frankf*&lang=de&limit=10&offset=0" http://localhost:8080/entityhub/symbol/find


    /symbol/query&query={query}

Allows to parse JSON serialzed FieldQueries to the Symbol endpoint.

Request:
    POST -d "query={query}" /symbol/query
Parameter:
    query: the JSON serialized FieldQuery (see section "FieldQuery JSON format" 
           below)
Example:
   curl -X POST -F "query=@fieldQuery.json" http://localhost:8080/entityhub/symbol/query
Note that "@fieldQuery.json" links to a local file that contains the parsed
Fieldquery (see ection "FieldQuery JSON format" for examples)
    
    
    /mapping?id={uri}

Request: GET /mapping?id={uri}
Parameter
    id: The uri of the mapping
Description: Service to get a mapping by id
Example:
    curl "http://localhost:8080/entityhub/mapping?id=entityhub/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5"
Response:
    {
       "id": "entityhub\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
       "site": "entityhub",
       "representation": {
           "id": "entityhub\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
           "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedSymbol": [{
               "type": "reference",
               "value": "entityhub\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6"
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
       "symbol": "entityhub\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6",
       "entity": "http:\/\/dbpedia.org\/resource\/Hallein",
       "stateUri": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed",
       "state": "proposed"
    }


    /mapping/entity?id={uri}

Request: GET /mapping/entity?id={uri}
Parameter
    id: The uri of the entity
Description: This service allows to retrieve the mapping for a entity. If no 
    mapping for the parsed uri is defined, the service returns a 404 "Not Found"
Example:
    curl "http://localhost:8080/entityhub/mapping/entity/?id=http://dbpedia.org/resource/Hallein"
Response:
    {
       "id": "entityhub\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
       "site": "entityhub",
       "representation": {
           "id": "entityhub\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
           "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedSymbol": [{
               "type": "reference",
               "value": "entityhub\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6"
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
       "symbol": "entityhub\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6",
       "entity": "http:\/\/dbpedia.org\/resource\/Hallein",
       "stateUri": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed",
       "state": "proposed"
    }


    /mapping/symbol?id={uri}

Request: GET /mapping/symbol?id={uri}
Parameter
    id: The uri of the symbol
Description: This service allows to retrieve all mappings defined for a symbol. 
    Note that one Symbol can be mapped to 1..n Entities. If no Symbol with the 
    parsed URI is defined by the Apache Stanbol Entityhub, than this service 
    returns a 404 "Not Found"

Example
    curl "http://localhost:8080/entityhub/mapping/symbol?id=entityhub/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6"
Response:
    {"results": [{
       "id": "entityhub\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
       "site": "entityhub",
       "representation": {
           "id": "entityhub\/mapping.1cf4f424-4232-5ff1-16f9-e4aaef2a95a5",
           "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappedSymbol": [{
               "type": "reference",
               "value": "entityhub\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6"
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
       "symbol": "entityhub\/symbol.c21aef73-3149-0e4c-3290-d792e236c2d6",
       "entity": "http:\/\/dbpedia.org\/resource\/Hallein",
       "stateUri": "http:\/\/www.iks-project.eu\/ontology\/rick\/model\/mappingState-proposed",
       "state": "proposed"
    }]}

Note that the response contains a Json Object with the key "results" as root 
that has a Json Array containing all EntityMappings for the parsed Symbol.


FieldQuery JSON format
----------------------

The FieldQuery is part of the java API defined in the bundle 
    org.apache.stanbol.entityhub.servicesapi
see http://svn.apache.org/repos/asf/incubator/stanbol/trunk/entityhub/generic/servicesapi/src/main/java/org/apache/stanbol/entityhub/servicesapi/query/FieldQuery.java

To enable to parse FieldQueries also via the RESTful interface of the Entityhub
a JSON serilisazion for queries defined by this this interface is used.
This section describes this interface and provides some examples

Note that the FieldQuery as used for the Query is included in the Response.

Root Element Keys: 

    "selected": json array with the name of the fields selected by this query 
    "offset": the offset of the first result returned by this query 
    "limit": the maximum number of results returned 
    "constraints": json array holding all the constraints of the query 

Example:
{
    "selected": [ 
        "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label", 
        "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type"], 
    "offset": "0", 
    "limit": "3", 
    "constraints": [...]
}

Constraints:

Constraints are always applied to a field. Currently the implementation is
limited to a single constraint/field. This is an limitation of the implementation
and not a theoretical one.
There are 3+1 different constraint types.
The three main types are
 - ValueConstraint: Checks if the value of the field is equals to the parsed
    value and data type
 - TextConstraint: Checks if the value of the field is equals to the parsed
    value, language. It supports also wildcard and regex searches.
 - RangeConstraint: Checks if the value of the field is within the parsed range
 - ReferenceConstraint: A special form of the ValueConstraint that defaults the
    data type to references (links to other entities)
      
Keys required by all Constraint types:
 - field: the field to apply the constraint 
 - type: the type of the constraint. One of "reference", "value", "text" or "range" 

Reference Constraint keys: 
 - value: the value (usually an URI) (required) 
Example:
Search for instances of the type Place as defined in the dbpedia ontology
    { 
        "type": "reference", 
        "field": "http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type", 
        "value": "http:\/\/dbpedia.org\/ontology\/Place", 
    } 

Value Constraint keys 
 - value: the value (required)
 - dataTypes: json array with the data types of the value (by default the 
     dataType is defined by the type of the parsed value) 
Example:
Search for entities with the rdfs:label "Paris". (Note: one could also use a
TextConstraint for this
    { 
        "type": "value", 
        "field": "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label", 
        "value": "Paris", 
    } 

Text Constraint: 
 - text: the text to search (required)
 - languages: json array with the languages to search (default is all languages) 
 - patternType: one of "wildcard", "regex" or "none" (default is "none") 
 - caseSensitive: boolean (default is "false") 
Example:
Searches for entities with an german rdfs:label starting with "Frankf"
    { 
       "type": "text", 
       "languages": ["de"], 
       "patternType": "wildcard", 
       "text": "Frankf*", 
       "field": "http:\/\/www.w3.org\/2000\/01\/rdf-schema#label" 
    }, 

Range Constraint: 
 - lowerBound: The lower bound of the range (one of lower and upper bound MUST BE defined) 
 - upperBound: The upper bound of the range (one of lower and upper bound MUST BE defined) 
 - inclusive: used for both upper and lower bound (default is "false") 
Example:
Searches for entities with a population over 1 million. Note that the data type
is automatically detected based on the parsed value (integer in that case)
    { 
        "type": "range", 
        "field": "http:\/\/dbpedia.org\/ontology\/populationTotal", 
        "lowerBound": 1000000, 
        "inclusive": true, 
    }
