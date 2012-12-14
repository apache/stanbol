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

# LangId: Language Identification Enhancement Engine

The **LangId** engine determines the language of text. 

## Technical Description

The provided engine is based on the language identifier of [Apache Tika](http://tika.apache.org/).
The text to be checked must be provided in plain text format in one of two forms:

* a plain text content item
* by the content item's metadata as the string value of the property 
    
    <pre><code>http://www.semanticdesktop.org/ontologies/2007/01/19/nie#plainTextContent</pre></code>

The result of language identification is added as TextAnnotation to the content item's metadata as string value of the property

    http://purl.org/dc/terms/language

This RDF snippet illustrates the output:

    <fise:TextAnnotation rdf:about="urn:enhancement-a147957b-41f9-58f7-bbf1-b880b3aa4b49">
        <dc:language>en</dc:language>
        <dc:creator>org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine</dc:creator>
    </fise:TextAnnotation>


By default the language identifier distinguishes the languages listed below. After the colon the value of the language label in the metadata is given.

* German: de
* English: en
* Estonian: et
* French: fr
* Spanish: es
* Italian: it
* Swedish: sv
* Polish: pl
* Dutch: nl
* Norwegian: no
* Finnish: fi
* Greek: el
* Danish: da
* Hungarian: hu
* Icelandic: is
* Lithuanian: lt
* Portuguese: pt
* Russian: ru
* Thai: th

Additional language models can be created as Tika [LanguageProfile](org.apache.tika.language.LanguageProfile).

## Configuration options

* <pre><code>org.apache.stanbol.enhancer.engines.langid.probe-length</pre></code>

    an integer specifying how many characters will be used for
    identification. A value of 0 or below means to use the complete
    text. Otherwise only a substring of the specified length taken from the
    middle of the text will be used. The default value is 400 characters.

## Usage

Assuming that the Stanbol endpoint with the full launcher is running at

    http://localhost:8080

and the engine is activated, from the command line commands like this
can be used for submitting some text file as content item:

* stateless interface

    curl -i -X POST -H "Content-Type:text/plain" -T testfile.txt http://localhost:8080/engines

* stateful interface

    curl -i -X PUT -H "Content-Type:text/plain" -T testfile.txt http://localhost:8080/contenthub/content/someFileId

Alternatively, the Stanbol web interface can be used for submitting documents
and viewing the metadata at

    http://localhost:8080/contenthub

