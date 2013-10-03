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

# LangDetect: Language Identification Enhancement Engine

The **LanguageDetection** engine determines the language of text. 

## Technical Description

The provided engine is based on the [language detection library](http://code.google.com/p/language-detection/).
The text to be checked must be provided in plain text format by the content item.

The result of language identification is added as TextAnnotation to the content item's metadata as string value of the property

    http://purl.org/dc/terms/language

This RDF snippet illustrates the output:

    <fise:TextAnnotation rdf:about="urn:enhancement-a147957b-41f9-58f7-bbf1-b880b3aa4b49">
        <dc:language>en</dc:language>
        <dc:creator>org.apache.stanbol.enhancer.engines.langdetect.LanguageDetectionEnhancementEngine</dc:creator>
    </fise:TextAnnotation>


By default the language identifier distinguishes [53 languages](http://code.google.com/p/language-detection/wiki/LanguageList) listed here:

* af:	Afrikaans
* ar:	Arabic
* bg:	Bulgarian
* bn:	Bengali
* cs:	Czech
* da:	Dannish
* de:	German
* el:	Greek
* en:	English
* es:	Spanish
* et:	Estonian
* fa: Persian
* fi: Finnish
* fr: French
* gu: Gujarati
* he: Hebrew
* hi: Hindi
* hr: Croatian
* hu: Hungarian
* id: Indonesian
* it: Italian
* ja: Japanese
* kn: Kannada
* ko: Korean
* lt: Lithuanian
* lv: Latvian
* mk: Macedonian
* ml: Malayalam
* mr: Marathi
* ne: Nepali
* nl: Dutch
* no: Norwegian
* pa: Punjabi
* pl: Polish
* pt: Portuguese
* ro: Romanian
* ru: Russian
* sk: Slovak
* sl: Slovene
* so: Somali
* sq: Albanian
* sv: Swedish
* sw: Swahili
* ta: Tamil
* te: Telugu
* th: Thai
* tl: Tagalog
* tr: Turkish
* uk: Ukrainian
* ur: Urdu
* vi: Vietnamese
* zh-cn:	Simplified Chinese
* zh-tw:	Traditional Chinese

Additional language models can be created by the [tools](http://code.google.com/p/language-detection/wiki/Tools).

## Configuration options

* <pre><code>org.apache.stanbol.enhancer.engines.langdetect.probe-length</pre></code>

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

