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

# Apache Stanbol Contenthub

Apache Stanbol Contenthub is a persistent document store which enables text based document submission 
and semantic search together with faceted search capability on submitted documents.

## ContentHub Store

It is the subcomponent that actually stores the document and its metadata persistently. In current implementation only text/plain documents are allowed.

Storage part of Contenthub provides basic methods such as create, put, get and delete. When a document is 
submitted, it delegates the textual content to Stanbol Enhancer to get its enhancements. While submitting the document, it is also possible to specify external metadata as field value pairs to the document. 

The document itself and all specified external metadata are indexed through an embedded Apache Solr core which is created specifically for Contenthub. 
Since documents are given unique IDs while indexing, using its unique ID, a document can be retrieved or deleted from Contenthub. 
ContentHub provides an HTML interface for its functionalities under the following endpoint, which is available after running the Full Launcher of Apache Stanbol:

	http://localhost:8080/contenthub

## ContentHub Search

ContentHub has a semantic search subcomponent that allows to make search over submitted documents. HTML interface for search functionality can be reached under:

	http://localhost:8080/contenthub/search

To start a search, one should enter a keyword and choose the search engines that will execute. After having the first search results, all facets and values of these facets will also arrive. Later on, when a facet constraint is chosen, documents and facets will be dynamically updated according to chosen constraint(s).

Contenthub Search API also provides specifying an ontology which carries semantic information to make the search more semantic. How this external ontology is exploited will be explained within the search engine documentations below. Furthermore, Search API enables specifying constraints for the search operation. The aim is to provide faceted search functionality through Java interface based on the specified constraints.  

Search part of component is formed by several search engines that work sequentially and contribute to the search results. Each search engine works with a given search context. The initialization of the search context is performed before the execution of any search engine. Each search engine makes use of the information embedded in the search context and populates the context with new results, such as resulting documents, related ontological resources, new keywords etc...

Currently, three search engines are active in search subcomponent:

### Ontology Resource Search Engine

This engine works when an additional ontology is specified at the beginning of the search. A SPARQL query based on a LARQ index is executed on specified ontology to find individuals and classes related with the keyword. When a class is found, it is added to search context as a related class resource and then, subclasses, superclasses and instances 
of all these classes are found and added to the search context.

When an individual about keyword is found it is added as a related individual resource to search context and it's classes are found. These classes are added to the search context using the same methodology explained in the previos paragraph.

### Enhancement Search Engine

This engine designed to work on enhancement graph which contains all enhancements of content items submitted to the Contenthub. 

When a document is submitted to ContentHub, its content is enhanced automatically by Enhancer component. 
In a single Clerezza graph, all the enhancements are kept together and this graph is indexed with LARQ. The LARQ index is automatically updated when a new enhancement is added.

Enhancement Search Engine, executes a SPARQL query on enhancement graph to find enhancements about the given keyword.
When an enhancement is found, the document from which the enhancement was obtained is added to search context as a related document resource.

### Solr Search Engine

Solr Search Engine is the engine that gives full-text and faceted search capabilities to the Contenthub.

Since every document is indexed to Solr (to the core created for Contenthub), it is possible to do full-text
search over documents' content and metadata. After the first search, all the facet constraints of resulting documents will be available for faceted search. When a facet constraint is chosen, resulting documents and facet constraints are updated dynamically. 

Later on, related class and individual resources about the keyword, which are found by Ontology Resource Search Engine, are searched over Solr using their resource name. 

After all, document resources founded by Enhancement Search Engine is examined. If there is a document whose field values does not match with facet constraints, these document resources are removed from the search results.

## Building and Launching ContentHub

Since ContentHub is included in Full Launcher of Apache Stanbol
it is built with Apache Stanbol and can be launched under 
Apache Stanbol Full Launcher. For detailed instructions to build and launch Apache Stanbol see the README file through the following link:

	http://svn.apache.org/repos/asf/incubator/stanbol/trunk/README.md
