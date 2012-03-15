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


Apache Stanbol Launcher Default Configuration 
=============================================

This artifacts provides the default configuration used by the Stanbol Launchers.
It depends on the 

    org.apache.stanbol.commons.installer.bundleprovider

to be available as this module is used to actually load the provided configuration.

This [STANBOL-529](https://issues.apache.org/jira/browse/STANBOL-529) for more details.

Users that do not want to run with the defaults can stop/uninstall this
bundle to deactivate/remove the defaults.

__NOTE:__ The default configuration does not include configuration for other
'/data' modules (such as DBpedia or OpenNLP) nor '/demo' modules.

## Language Detection Chain

This configures a chain that optionally includes the 
[Tika Engine](http://incubator.apache.org/stanbol/docs/trunk/enhancer/engines/tikaengine.html)
and the [Metaxa Engine](http://incubator.apache.org/stanbol/docs/trunk/enhancer/engines/metaxaengine.html)
and the [LangId Engine](http://incubator.apache.org/stanbol/docs/trunk/enhancer/engines/langidengine.html)
to detect the language of parsed Content.
This [EnhancementChain](http://incubator.apache.org/stanbol/docs/trunk/enhancer/chains)
is intended to be used by users that are only interested in detecting the
language of some text.

This EnhancementChain can also be used if neither the Tika nor Metaxa Engine are
available. However than it will only be able to process plain text content.


## Keyword Extraction using Entityhub

A configuration that extracts Entities from parsed content based on Entities
added to the Entityhub (http://{host}:{port}/entityhub/entity).

This Engine can be used to extract and link entities that where previously 
added to the entityhub e.g. by using

    :::bash
    curl -i -X PUT -H "Content-Type:application/rdf+xml" -T {file.rdf} \
        "http://localhost:8080/entityhub/entity

The property "rdfs:label" is used for extraction. "rdfs:seeAlso" is used for
processing redirects. So make sure that the entities you use "rdf:label" to
store their names.

For the  following rdf:types mappings to dc:types used by fise:TextAnnotation 
are defined:

* Person (dc:type = dbp-ont:Person)
    * foaf:Person ([Foaf](http://www.foaf-project.org/) Ontology)
    * schema:Person ([schema.org](http://schema.org) Ontology)
* Organisation (dc:type = dbp-ont:Organisation)
    * dnp-ont:Newspaper ([dbpedia Ontology](http://wiki.dbpedia.org/Ontology))
    * schema:Organization
* Location (dc:type = dbp-ont:Place)
    * schema:Place
    * http://www.opengis.net/gml/_Feature (see [Geography Markup Language]
    (http://www.opengeospatial.org/standards/gml) for more information)
* Concept (dc:type = skos:Concept)
    * skos:Concept ([SKOS](http://www.w3.org/2004/02/skos/) Ontology)
    
If the Entities you add to the Entityhub do use one of those types the
KeywordLinkingEngine will create TextAnnotations with the according dc:type. If
not than no dc:type will be set.
 
