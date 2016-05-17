/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.enhancer.nlp.nif;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.nlp.utils.NIFHelper;

public enum StringOntology {
    /**
     * The URI of this String was created with the URI Recipe Context-Hash, see
     * http://aksw.org/Projects/NIF#context-hash-nif-uri-recipe.
     * 
     * @see NIFHelper#getNifHashURI(IRI, int, int, String)
     */
    ContextHashBasedString,
    /**
     * A document is a string that can be considered a closed unit content-wise. In NIF a document is given an
     * URL that references the String of the document. Furthermore a document can have several sources. It can
     * be a string, a HTML document, a PDF document, text file or any other arbitrary string. The uri denoting
     * the actual document should be able to reproduce that document, i.e. either the string is directly
     * included via the property sourceString or an url can be given that contains the string via the property
     * sourceUrl. Depending on the feedback, this might also become the ImmutableGraph URI or a subclass of
     * owl:Ontology
     */
    Document,
    /**
     * The URI of this String was created with the URI Recipe Context-Hash, see
     * http://aksw.org/Projects/NIF#offset-nif-uri-recipe
     */
    OffsetBasedString,
    /**
     * temporariliy added this declaration.
     */
    menas,
    /**
     * The source url, which makes up the document. Annotators should ensure that the source text can be
     * downloaded from the url and stays stable otherwise :sourceString should be used.
     */
    sourceUri,
    subString,
    subStringTrans,
    superString,
    superStringTrans,
    /**
     * The string, which the uri is representing as an RDF Literal. This property is mandatory for every
     * String.
     */
    anchorOf,
    /**
     * The index of the first character of the String relative to the document. This should be identical with
     * the first number used in the offset URI recipe.
     */
    beginIndex,
    /**
     * The index of last character of the String relative to the document. This should be identical with the
     * second number used in the offset URI recipe.
     */
    endIndex,
    /**
     * The left context of the string. The length of the context is undefined. To fix the length subProperties
     * can be used: e.g. :leftContext20 rdfs:subPropertyOf :leftContext gives the 20 characters to the left of
     * the string. Using this property can increase the size of the produced RDF immensely.
     */
    leftContext,
    /**
     * The right context of the string. The length of the context is undefined. To fix the length
     * subProperties can be used: e.g. :rightContext20 rdfs:subPropertyOf :rightContext gives the 20
     * characters to the right of the string. Using this property can increase the size of the produced RDF
     * immensely.
     */
    rightContext,
    /**
     * The source string, which makes up the document. Used to reproduce the original text. Takes priority
     * over :sourceUrl . Not to be confused with :anchorOf
     */
    sourceString;
    public final static String NAMESPACE = "http://nlp2rdf.lod2.eu/schema/string/";

    IRI uri;

    private StringOntology() {
        uri = new IRI(NAMESPACE + name());
    }

    public String getLocalName() {
        return name();
    }

    public IRI getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return uri.getUnicodeString();
    }
}
