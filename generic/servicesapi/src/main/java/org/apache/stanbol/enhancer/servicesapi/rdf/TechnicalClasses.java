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

import org.apache.clerezza.rdf.core.UriRef;

/**
 * Classes to be used as types for resources that are not real life entities but
 * technical data modeling for Stanbol Enhancer components.
 *
 * @author ogrisel
 */
public class TechnicalClasses {

    /**
     * Type used for all enhancement created by Stanbol Enhancer
     */
    public static final UriRef ENHANCER_ENHANCEMENT = new UriRef(
            NamespaceEnum.fise+"Enhancement");

    /**
     * Type used for annotations on Text created by Stanbol Enhancer. This type is intended
     * to be used in combination with ENHANCER_ENHANCEMENT
     */
    public static final UriRef ENHANCER_TEXTANNOTATION = new UriRef(
            NamespaceEnum.fise+"TextAnnotation");

    /**
     * Type used for annotations of named entities. This type is intended
     * to be used in combination with ENHANCER_ENHANCEMENT
     */
    public static final UriRef ENHANCER_ENTITYANNOTATION = new UriRef(
            NamespaceEnum.fise+"EntityAnnotation");
    
    /**
     * Type used for annotations documents. This type is intended
     * to be used in combination with ENHANCER_ENHANCEMENT and
     * ENHANCER_ENTITYANNOTATION as a complimentary marker to suggest
     * that the referenced is the one of the primary topic of the
     * whole document or of a specific section specified by a linked
     * TextAnnotation. 
     * 
     * The entity or concept is not necessarily explicitly mentioned
     * in the document (like a traditional entity occurrence would).
     */
    public static final UriRef ENHANCER_TOPICANNOTATION = new UriRef(
            NamespaceEnum.fise+"TopicAnnotation");

    /**
     * To be used as a type pour any semantic knowledge extraction
     */
    @Deprecated
    public static final UriRef ENHANCER_EXTRACTION = new UriRef(
            "http://iks-project.eu/ns/enhancer/extraction/Extraction");

    /**
     * To be used as a complement type for extraction that are relevant only to
     * the portion of context item (i.e. a sentence, an expression, a word)
     * TODO: rwesten: Check how this standard can be used for Stanbol Enhancer enhancements
     * @deprecated
     */
    @Deprecated
    public static final UriRef ANNOTEA_ANNOTATION = new UriRef(
            "http://www.w3.org/2000/10/annotation-ns#Annotation");

    /**
     * To be used to type the URI of the content item being annotated by Stanbol Enhancer
     */
    public static final UriRef FOAF_DOCUMENT = new UriRef(
            NamespaceEnum.foaf + "Document");

    /**
     * Used to indicate, that an EntityAnnotation describes an Categorisation.
     * see <a href="http://wiki.iks-project.eu/index.php/ZemantaEnhancementEngine#Mapping_of_Categories">
     * Mapping of Categories</a> for more Information)
     */
    public static final UriRef ENHANCER_CATEGORY = new UriRef(
            NamespaceEnum.fise + "Category");

    private TechnicalClasses() {
    }

}
