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

# Htmlextractor: Metadata extraction from HTML documents

The **Htmlextractor Enhancement Engine** extracts embedded metadata from HTML documents, such as [Microformats](http://microformats.org/) and [RDFa](http://www.w3.org/TR/rdfa-syntax/).
By providing other extractors it can be configured for any kind of content extraction from HTML pages.

##Technical description

### Supported metadata types

The built-in extractors are defined in the default resource *htmlextractors.xml*. The following metadata types are supported:
*    RDFa
*    geo
*    hAtom
*    hCal
*    hCard
*    hReview
*    rel-license
*    rel-tag
*    xFolk

### Vocabularies

#### HTML Microformat Extractors

The following table describes which vocabularies are used for representing microformat data in Metaxa: 


<table border="1">
    <tr>
        <th>MF</th>
        <th>Vocabulary (Namespace)</th>
    </tr>
    <tr>
        <td>geo</td>
        <td>wgs84 (<tt>http://www.w3.org/2003/01/geo/wgs84_pos#</tt>)</td>
    </tr>
    <tr>
        <td>hAtom</td>
        <td>atom (<tt>http://www.w3.org/2005/Atom#)</td>
    </tr>
    <tr>
    <td/>
        <td>tagging (<tt>http://aperture.sourceforge.net/ontologies/tagging#</tt>)</td>
    </tr>
    <tr>
        <td>hCal</td>
        <td> ical (<tt>http://www.w3.org/2002/12/cal/icaltzd#</tt>)</td>
    </tr>
    <tr>
        <td></td>
        <td>vcard (<tt>http://www.w3.org/2006/vcard/ns#</tt>)</td>
    </tr>
    <tr>
        <td>hCard</td>
        <td>vcard (<tt>http://www.w3.org/2006/vcard/ns#</tt>)</td>
    </tr>
    <tr>
        <td>hReview</td>
        <td>review (<tt>http://www.purl.org/stuff/rev#</tt>)</td></tr>
    <tr>
        <td></td>
        <td>wgs84 (<tt>http://www.w3.org/2003/01/geo/wgs84_pos#</tt>)</td>
    </tr>
    <tr>
        <td></td>
        <td>dc (<tt>http://purl.org/dc/elements/1.1/</tt>)</td>
    </tr>
    <tr>
        <td></td>
        <td>dcterms (<tt>http://purl.org/dc/dcmitype/</tt>)</td>
    </tr>
    <tr>
        <td></td>
        <td>foaf (<tt>http://xmlns.com/foaf/0.1/</tt>)</td>
    </tr>
    <tr>
        <td></td>
        <td>vcard (<tt>http://www.w3.org/2006/vcard/ns#</tt>)</td>
    </tr>
    <tr>
        <td></td>
        <td>tag (<tt>http://www.holygoat.co.uk/owl/redwood/0.1/tags/</tt>)</td>
    </tr>
    <tr>
        <td>rel-license</td>
        <td>dc (<tt>http://purl.org/dc/elements/1.1</tt>/)</td>
    </tr>
    <tr>
        <td>rel-tag</td>
        <td> tagging (<tt>http://aperture.sourceforge.net/ontologies/tagging#</tt>)</td>
    </tr>
    <tr>
        <td>xFolk</td>
        <td>nfo (<tt>http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#</tt>)</td>
    </tr>
    <tr>
        <td></td>
        <td>dc (<tt>http://purl.org/dc/elements/1.1</tt>/)</td>
    </tr>
    <tr>
        <td></td>
        <td>tagging (<tt>http://aperture.sourceforge.net/ontologies/tagging#</tt>)</td>
    </tr>
</table>

To prevent the occurrence of unconnected graphs in the metadata extracted subgraphs get connected to the content item by the property:

* http://www.semanticdesktop.org/ontologies/2007/01/19/nie#contains

## Configuration options

By default, the Htmlextractor engine uses the extractors specified in the resource "htmlregistry.xml".
Alternative configurations and extractors can be attached to the Htmlextractor as fragment bundles, specifying as host bundle

    Fragment-Host: org.apache.stanbol.enhancer.engines.htmlextractor

The alternative configuration files then can be set as values of the property

* <pre><code>org.apache.stanbol.enhancer.engines.htmlextractor.htmlextractors</pre></code>


## Usage

Assuming that the Stanbol endpoint with the full launcher is running at

    http://localhost:8080

and the engine is activated, from the command line commands like this can be used for submitting some file as content item, where the mime type must match the document type:

* stateless interface

    curl -i -X POST -H "Content-Type:text/html" -T testpage.html http://localhost:8080/enhancer

* stateful interface

    curl -i -X PUT -H "Content-Type:text/html" -T testpage.html http://localhost:8080/contenthub/content/someFileId

Alternatively, the Stanbol web interface can be used for submitting documents
and viewing the metadata at

    http://localhost:8080/contenthub

