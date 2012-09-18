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

# OpenCalais Enhancement Engine

The **OpenCalais Enhancement Engine** provides an interface to the [OpenCalais
Webservice](http://www.opencalais.com/) for Named Entity Recognition (NER).

## Technical description

The engine will send the text of content item to the OpenCalais service and
retrieve the NER annotations in RDF format.  The OpenCalais annotations are
added to the content item's metadata as Stanbol text enhancement structures.

The engine natively supports the mime types *text/plain* and
*text/html*. Additionally, text can be processed that is provided in the content
item's metadata as value of the property

    http://www.semanticdesktop.org/ontologies/2007/01/19/nie#plainTextContent

Supported languages are

* English (en)
* French (fr)
* Spanish (es)

## Requirements for use and configuration options

The use of this component requires an API key from OpenCalais. Without
providing an API key, the engine will not do anything.  Such a key can be
obtained from [http://www.opencalais.com/APIkey](http://www.opencalais.com/APIkey).

In the OSGi configuration the key is set as value of the property

    org.apache.stanbol.enhancer.engines.opencalais.license


Also, the unit tests require the API key. Without the key some tests will be
skipped. For Maven the key can be set as a system property on the command line:

    mvn -Dorg.apache.stanbol.enhancer.engines.opencalais.license=YOUR_API_KEY [install|test]


The following configuration properties are defined:

* <tt>org.apache.stanbol.enhancer.engines.opencalais.license</tt>

    The OpenCalais license key that **must** be defined.

* <tt>org.apache.stanbol.enhancer.engines.opencalais.url</tt>

    The URL of the OpenCalais RESTful service. That needs only be changed
    when OpenCalais should change its web service address.

* <tt>org.apache.stanbol.enhancer.engines.opencalais.typeMap</tt>
    
    The value is the name
    of a file for mapping the NER types from OpenCalais to other types. By
    default, a mapping to the DBPedia types is provided in order to achieve
    compatibility with the Stanbol OpenLNLP-NER engine.  If no mapping is
    desired one might pass an empty mapping file. Types for which no
    mapping is defined are passed as is to the metadata.  The syntax of the
    mapping table is similar to that of Java property files. Each entry
    takes the form
    
    CalaisTypeURI=TargetTypeURI
    
* <tt>org.apache.stanbol.enhancer.engines.opencalais.NERonly</tt>

    A Boolean property to
    specify whether in addition to the NER enhancements also the OpenCalais
    Linked Data references are included as entity references. By default,
    these are omitted.

## Usage

Assuming that the Stanbol endpoint with the full launcher is running at

    http://localhost:8080

the license key has been defined and the engine is activated, from the
command line commands like this can be used for submitting some text file as content item:

* stateless interface

    curl -i -X POST -H "Content-Type:text/plain" -T testfile.txt http://localhost:8080/engines

* stateful interface

    curl -i -X PUT -H "Content-Type:text/plain" -T testfile.txt http://localhost:8080/contenthub/content/someFileId

Alternatively, the Stanbol web interface can be used for submitting documents
and viewing the metadata at

    http://localhost:8080/contenthub

