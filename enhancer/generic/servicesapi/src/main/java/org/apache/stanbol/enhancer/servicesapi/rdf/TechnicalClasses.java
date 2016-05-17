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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.clerezza.commons.rdf.IRI;

/**
 * Classes to be used as types for resources that are not real life entities but
 * technical data modeling for Stanbol Enhancer components.
 *
 * @author ogrisel
 */
public final class TechnicalClasses {
    
    private TechnicalClasses() {}
    /**
     * Type used for all enhancement created by Stanbol Enhancer
     */
    public static final IRI ENHANCER_ENHANCEMENT = new IRI(
            NamespaceEnum.fise+"Enhancement");

    /**
     * Type used for annotations on Text created by Stanbol Enhancer. This type is intended
     * to be used in combination with ENHANCER_ENHANCEMENT
     */
    public static final IRI ENHANCER_TEXTANNOTATION = new IRI(
            NamespaceEnum.fise+"TextAnnotation");

    /**
     * Type used for annotations of named entities. This type is intended
     * to be used in combination with ENHANCER_ENHANCEMENT
     */
    public static final IRI ENHANCER_ENTITYANNOTATION = new IRI(
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
    public static final IRI ENHANCER_TOPICANNOTATION = new IRI(
            NamespaceEnum.fise+"TopicAnnotation");

    /**
     * To be used as a type pour any semantic knowledge extraction
     */
    @Deprecated
    public static final IRI ENHANCER_EXTRACTION = new IRI(
            "http://iks-project.eu/ns/enhancer/extraction/Extraction");

    /**
     * To be used as a complement type for extraction that are relevant only to
     * the portion of context item (i.e. a sentence, an expression, a word)
     * TODO: rwesten: Check how this standard can be used for Stanbol Enhancer enhancements
     * @deprecated
     */
    @Deprecated
    public static final IRI ANNOTEA_ANNOTATION = new IRI(
            "http://www.w3.org/2000/10/annotation-ns#Annotation");

    /**
     * To be used to type the URI of the content item being annotated by Stanbol Enhancer
     */
    public static final IRI FOAF_DOCUMENT = new IRI(
            NamespaceEnum.foaf + "Document");

    /**
     * Used to indicate, that an EntityAnnotation describes an Categorisation.
     * see <a href="http://wiki.iks-project.eu/index.php/ZemantaEnhancementEngine#Mapping_of_Categories">
     * Mapping of Categories</a> for more Information)
     * @deprecated the preferred rdf:type for categories and topics is
     * {@link OntologicalClasses#SKOS_CONCEPT} (see 
     * <a href="https://issues.apache.org/jira/browse/STANBOL-617">STANBOL-617</a>)
     */
    public static final IRI ENHANCER_CATEGORY = new IRI(
            NamespaceEnum.fise + "Category");

    /**
     * DC terms Linguistic System is the type used as Range for the dc:language
     * property. As this property is also used for describing the language
     * as identified for analysed content this type is used as dc:type for
     * {@value #ENHANCER_TEXTANNOTATION} describing the language of the text
     * (see 
     * <a href="https://issues.apache.org/jira/browse/STANBOL-613">STANBOL-613</a>)
     */
    public static final IRI DCTERMS_LINGUISTIC_SYSTEM = new IRI(
            NamespaceEnum.dc + "LinguisticSystem");
    
    /**
     * The confidence level of {@link #ENHANCER_ENHANCEMENT}s
     */
    public static final IRI FNHANCER_CONFIDENCE_LEVEL = new IRI(
            NamespaceEnum.fise + "ConfidenceLevel");
    
    /**
     * Enumeration over the four instance of the {@link #FNHANCER_CONFIDENCE_LEVEL}
     * class as introduced by
     * <a herf="https://issues.apache.org/jira/browse/STANBOL-631">STANBOL-631</a>)
     * <p>
     * {@link #name()} returns the local name; {@link #toString()} the URI as
     * {@link String}.
     * @author Rupert Westenthaler
     *
     */
    public static enum CONFIDENCE_LEVEL_ENUM{
        certain,ambiguous,suggestion,uncertain;

        private final IRI uri;
        private final String localName;
        
        private CONFIDENCE_LEVEL_ENUM() {
            localName = "cl-"+name();
            uri = new IRI(NamespaceEnum.fise+localName);
        }
        
        public String getLocalName(){
            return localName;
        }
        
        public String toString() {
            return uri.toString();
        };
        
        public IRI getUri(){
            return uri;
        }
        
        private static final Map<IRI,CONFIDENCE_LEVEL_ENUM> uriRef2enum;
        private static final Map<String,CONFIDENCE_LEVEL_ENUM> uri2enum;
        static {
            Map<IRI,CONFIDENCE_LEVEL_ENUM> ur = new HashMap<IRI,TechnicalClasses.CONFIDENCE_LEVEL_ENUM>();
            Map<String,CONFIDENCE_LEVEL_ENUM> us = new HashMap<String,TechnicalClasses.CONFIDENCE_LEVEL_ENUM>();
            for(CONFIDENCE_LEVEL_ENUM cl : CONFIDENCE_LEVEL_ENUM.values()){
                ur.put(cl.getUri(), cl);
                us.put(cl.toString(), cl);
            }
            uriRef2enum = Collections.unmodifiableMap(ur);
            uri2enum = Collections.unmodifiableMap(us);
        }
        /**
         * Getter for the fise:ConfidenceLevel instance for the {@link IRI}
         * @param uri the URI
         * @return the fise:ConfidenceLevel instance or <code>null</code> if the
         * parsed URI is not one of the four defined instances
         */
        public static CONFIDENCE_LEVEL_ENUM getConfidenceLevel(IRI uri){
            return uriRef2enum.get(uri);
        }
        
        /**
         * Getter for the fise:ConfidenceLevel instance for the {@link IRI}
         * @param uri the URI string
         * @return the fise:ConfidenceLevel instance or <code>null</code> if the
         * parsed URI is not one of the four defined instances
         */
        public static CONFIDENCE_LEVEL_ENUM getConfidenceLevel(String uri){
            return uri2enum.get(uri);
        }
    }

}
