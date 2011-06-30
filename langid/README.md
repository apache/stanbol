# LangId: Language Identification Enhancement Engine

The **LangId** engine determines the language of text. 

## Technical Description

The provided engine is based on the [TextCat library](http://textcat.sourceforge.net/).
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
* French: fr
* Spanish: es
* Italian: it
* Swedish: sv
* Polish: pl
* Dutch: nl
* Norwegian: no
* Finnish: fi
* Albanian: sq
* Slovak (ASCII): sk
* Slovenian (ASCII): sl
* Danish: da
* Hungarian: hu

Tools for creating new or additional language models are provided by the underlying TextCat system as documented at [http://textcat.sourceforge.net](http://textcat.sourceforge.net/).

## Configuration options

* <pre><code>org.apache.stanbol.enhancer.engines.langid.probe-length</pre></code>

    an integer specifying how many characters will be used for
    identification. A value of 0 or below means to use the complete
    text. Otherwise only a substring of the specified length taken from the
    middle of the text will be used. The default value is 400 characters.

* <pre><code>org.apache.stanbol.enhancer.engines.langid.model-configuration-file</pre></code>

    the name of a file that defines which statistical language models are
    used and the mappings from statistical model names to language labels
    that will appear as the value in the enhancement structure. By default
    the resource file *languageLabelsMap.txt* is used.

## Usage

Assuming that the Stanbol endpoint with the full launcher is running at

    http://localhost:8080

and the engine is activated, from the command line commands like this
can be used for submitting some text file as content item:

* stateless interface

    curl -i -X PUT -H "Content-Type:text/plain" -T testfile.txt http://localhost:8080/engines

* stateful interface

    curl -i -X PUT -H "Content-Type:text/plain" -T testfile.txt http://localhost:8080/contenthub/content/someFileId

Alternatively, the Stanbol web interface can be used for submitting documents
and viewing the metadata at

    http://localhost:8080/contenthub

