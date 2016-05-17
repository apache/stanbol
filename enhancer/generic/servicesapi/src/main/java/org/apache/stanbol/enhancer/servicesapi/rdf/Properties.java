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
package org.apache.stanbol.enhancer.servicesapi.rdf;

import org.apache.clerezza.commons.rdf.IRI;

/**
 * Namespace of standard properties to be used as typed metadata by
 * EnhancementEngine.
 *
 * Copy and paste the URLs in a browser to access the official definitions (RDF
 * schema) of those properties to.
 *
 * @author ogrisel
 *
 */
public final class Properties {

    /**
     * Restrict instantiation
     */
    private Properties() {}

    /**
     * The canonical way to give the type of a resource. It is very common that
     * the target of this property is an owl:Class such as the ones defined is
     * {@link OntologyClass}
     */
    public static final IRI RDF_TYPE = new IRI(NamespaceEnum.rdf + "type");

    /**
     * A label for resources of any type.
     */
    public static final IRI RDFS_LABEL = new IRI(NamespaceEnum.rdfs
            + "label");

    /**
     * Used to relate a content item to another named resource such as a person,
     * a location, an organization, ...
     *
     * @deprecated use ENHANCER_ENTITY instead
     */
    @Deprecated
    public static final IRI DC_REFERENCES = new IRI(NamespaceEnum.dc
            + "references");

    /**
     * Creation date of a resource. Used by Stanbol Enhancer to annotate the creation date
     * of the enhancement by the enhancement engine
     */
    public static final IRI DC_CREATED = new IRI(NamespaceEnum.dc
            + "created");

    /**
     * Modification date of a resource. Used by Stanbol Enhancer to annotate the 
     * modification date of the enhancement if it was changed by an other
     * enhancement engine as the one creating it. Multiple changes of the
     * creating enhancement engines are not considered as modifications.
     */
    public static final IRI DC_MODIFIED = new IRI(NamespaceEnum.dc
            + "modified");

    /**
     * The entity responsible for the creation of a resource. Used by Stanbol Enhancer to
     * annotate the enhancement engine that created an enhancement
     */
    public static final IRI DC_CREATOR = new IRI(NamespaceEnum.dc
            + "creator");
    /**
     * The entity contributed to a resource. Used by Stanbol Enhancer to
     * annotate the enhancement engine that changed an enhancement originally
     * created by an other enhancemetn engine
     */
    public static final IRI DC_CONTRIBUTOR = new IRI(NamespaceEnum.dc
            + "contributor");

    /**
     * The nature or genre of the resource. Stanbol Enhancer uses this property to refer to
     * the type of the enhancement. Values should be URIs defined in some
     * controlled vocabulary
     */
    public static final IRI DC_TYPE = new IRI(NamespaceEnum.dc + "type");

    /**
     * A related resource that is required by the described resource to support
     * its function, delivery, or coherence. Stanbol Enhancer uses this property to refer to
     * other enhancements an enhancement depends on.
     */
    public static final IRI DC_REQUIRES = new IRI(NamespaceEnum.dc
            + "requires");

    /**
     * A related resource. Stanbol Enhancer uses this property to define enhancements that
     * are referred by the actual one
     */
    public static final IRI DC_RELATION = new IRI(NamespaceEnum.dc
            + "relation");

    /**
     * A point on the surface of the earth given by two signed floats (latitude
     * and longitude) concatenated as a string literal using a whitespace as
     * separator.
     */
    @Deprecated
    public static final IRI GEORSS_POINT = new IRI(NamespaceEnum.georss
            + "point");

    @Deprecated
    public static final IRI GEO_LAT = new IRI(NamespaceEnum.geo + "lat");

    @Deprecated
    public static final IRI GEO_LONG = new IRI(NamespaceEnum.geo + "long");

    public static final IRI SKOS_BROADER = new IRI(NamespaceEnum.skos + "broader");
    
    public static final IRI SKOS_NARROWER = new IRI(NamespaceEnum.skos + "narrower");
    
    /**
     * Refers to the content item the enhancement was extracted form
     */
    public static final IRI ENHANCER_EXTRACTED_FROM = new IRI(
            NamespaceEnum.fise + "extracted-from");

    /**
     * the character position of the start of a text selection.
     */
    public static final IRI ENHANCER_START = new IRI(NamespaceEnum.fise
            + "start");

    /**
     * the character position of the end of a text selection.
     */
    public static final IRI ENHANCER_END = new IRI(NamespaceEnum.fise + "end");

    /**
     * The text selected by the text annotation. This is an optional property
     */
    public static final IRI ENHANCER_SELECTED_TEXT = new IRI(
            NamespaceEnum.fise + "selected-text");

    /**
     * The context (surroundings) of the text selected. (e.g. the sentence
     * containing a person selected by a NLP enhancer)
     */
    public static final IRI ENHANCER_SELECTION_CONTEXT = new IRI(
            NamespaceEnum.fise + "selection-context");
    /**
     * The prefix of the {@link #ENHANCER_SELECTED_TEXT}. Intended to be used
     * to find the exact position within the text if char indexes can not be used
     * @since 0.11.0
     */
    public final static IRI ENHANCER_SELECTION_PREFIX = new IRI(
        NamespaceEnum.fise + "selection-prefix");
    /**
     * The first few chars of the {@link #ENHANCER_SELECTED_TEXT}. To be used if
     * the selected text is to long to be included as a {@link PlainLiteral} (
     * e.g. when selection sentences or whole sections of the text).
     * @since 0.11.0
     */
    public final static IRI ENHANCER_SELECTION_HEAD = new IRI(
        NamespaceEnum.fise + "selection-head");
    /**
     * The last few chars of the {@link #ENHANCER_SELECTED_TEXT}. To be used if
     * the selected text is to long to be included as a {@link PlainLiteral} (
     * e.g. when selection sentences or whole sections of the text).
     * @since 0.11.0
     */
    public final static IRI ENHANCER_SELECTION_TAIL = new IRI(
        NamespaceEnum.fise + "selection-tail");
    /**
     * The suffix of the {@link #ENHANCER_SELECTED_TEXT}. Intended to be used
     * to find the exact position within the text if char indexes can not be used
     * @since 0.11.0
     */
    public final static IRI ENHANCER_SELECTION_SUFFIX = new IRI(
        NamespaceEnum.fise + "selection-suffix");

    /**
     * A positive double value to rank extractions according to the algorithm
     * confidence in the accuracy of the extraction.
     */
    public static final IRI ENHANCER_CONFIDENCE = new IRI(NamespaceEnum.fise
            + "confidence");

    /**
     * This refers to the URI identifying the referred named entity
     */
    public static final IRI ENHANCER_ENTITY_REFERENCE = new IRI(
            NamespaceEnum.fise + "entity-reference");

    /**
     * This property can be used to specify the type of the entity (Optional)
     */
    public static final IRI ENHANCER_ENTITY_TYPE = new IRI(NamespaceEnum.fise
            + "entity-type");

    /**
     * The label(s) of the referred entity
     */
    public static final IRI ENHANCER_ENTITY_LABEL = new IRI(
            NamespaceEnum.fise + "entity-label");
    /**
     * The confidence level (introducdes by
     * <a herf="https://issues.apache.org/jira/browse/STANBOL-631">STANBOL-631</a>)
     */
    public static final IRI ENHANCER_CONFIDENCE_LEVEL = new IRI(
            NamespaceEnum.fise + "confidence-level");

    /**
     * The origin can be used to reference the vocabulary (dataset, thesaurus, 
     * ontology, ...) the Entity {@link #ENHANCER_ENTITY_REFERENCE referenced}
     * by a <code>{@link TechnicalClasses#ENHANCER_ENTITYANNOTATION fise:EntiyAnnotation}</code>
     * originates from.
     * @since 0.12.1 (STANBOL-1391)
     */
    public static final IRI ENHANCER_ORIGIN = new IRI(
            NamespaceEnum.fise + "origin");
    
    /**
     * Internet Media Type of a content item.
     * 
     * @deprecated dc:FileFormat does not exist
     */
    @Deprecated
    public static final IRI DC_FILEFORMAT = new IRI(NamespaceEnum.dc
            + "FileFormat");

    /**
     * Language of the content item text.
     */
    public static final IRI DC_LANGUAGE = new IRI(NamespaceEnum.dc
            + "language");


    /**
     * The topic of the resource. Used to relate a content item to a
     * skos:Concept modelling one of the overall topic of the content.
     *
     * @deprecated rwesten: To my knowledge no longer used by Stanbol Enhancer enhancement
     *             specification
     */
    @Deprecated
    public static final IRI DC_SUBJECT = new IRI(NamespaceEnum.dc
            + "subject");

    /**
     * The sha1 hexadecimal digest of a content item.
     */
    @Deprecated
    public static final IRI FOAF_SHA1 = new IRI(NamespaceEnum.foaf
            + "sha1");

    /**
     * Link an semantic extraction or a manual annotation to a content item.
     */
    @Deprecated
    public static final IRI ENHANCER_RELATED_CONTENT_ITEM = new IRI(
            "http://iksproject.eu/ns/extraction/source-content-item");

    @Deprecated
    public static final IRI ENHANCER_RELATED_TOPIC = new IRI(
            "http://iksproject.eu/ns/extraction/related-topic");

    @Deprecated
    public static final IRI ENHANCER_RELATED_TOPIC_LABEL = new IRI(
            "http://iksproject.eu/ns/extraction/related-topic-label");

    @Deprecated
    public static final IRI ENHANCER_MENTIONED_ENTITY_POSITION_START = new IRI(
            "http://iksproject.eu/ns/extraction/mention/position-start");

    @Deprecated
    public static final IRI ENHANCER_MENTIONED_ENTITY_POSITION_END = new IRI(
            "http://iksproject.eu/ns/extraction/mention/position-end");

    @Deprecated
    public static final IRI ENHANCER_MENTIONED_ENTITY_CONTEXT = new IRI(
            "http://iksproject.eu/ns/extraction/mention/context");

    @Deprecated
    public static final IRI ENHANCER_MENTIONED_ENTITY_OCCURENCE = new IRI(
            "http://iksproject.eu/ns/extraction/mention/occurence");

}
