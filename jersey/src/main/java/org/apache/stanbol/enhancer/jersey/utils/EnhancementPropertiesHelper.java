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
package org.apache.stanbol.enhancer.jersey.utils;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;

/**
 * Defines Constants and utilities for using EnhancementProperties
 */
public final class EnhancementPropertiesHelper {

    private EnhancementPropertiesHelper(){/* no instances allowed*/}
    /**
     * URI used to register an {@link ContentItem#getPart(int, Class) contentPart}
     * of the type {@link Map Map&lt;String,Objext&gt;} that contains properties
     * for the enhancement process. <p>
     * TODO: This might move to servicesapi and also accessible to enhancement
     * engines
     */
    public static final UriRef ENHANCEMENT_PROPERTIES_URI = new UriRef(
            "urn:apache.org:stanbol.web:enhancement.properties");
    /**
     * Boolean switch parsed as {@link QueryParam} tha allows to deactivate the
     * inclusion of the {@link ContentItem#getMetadata()} in the Response
     */
    public static final String OMIT_METADATA = "stanbol.enhancer.web.omitMetadata";
    /**
     * {@link Set Set&lt;String&gt;} containing all the URIs of the
     * {@link ContentItem#getPart(UriRef, Class) ContentParts} representing 
     * RDF data (compatible to Clerezza {@link TripleCollection}). If the 
     * returned set contains '*' than all such content parts need to be returned.<p>
     * NOTE: This can also be used to include the EnhancementProperties
     * as "applciation/json" in the Response by adding this
     * {@link EnhancementPropertiesHelper#ENHANCEMENT_PROPERTIES_URI uri}.
     */
    public static final String OUTPUT_CONTENT_PART = "stanbol.enhancer.web.outputContentPart";
    /**
     * Allows to omit all parsed content parts regardless of the {@link #OUTPUT_CONTENT_PART}
     * configuration
     */
    public static final String OMIT_PARSED_CONTENT = "stanbol.enhancer.web.omitParsed";
    /**
     * {@link Collection Collection&lt;String&gt;} containing mime types. This
     * allows to specify what versions of the parsed content to be included in
     * the response. e.g. ["text/*","application/pdf"] would include all text
     * formats and PDF.
     */
    public static final String OUTPUT_CONTENT = "stanbol.enhancer.web.outputContent";
    /**
     * This allows to copy the {@link ExecutionMetadata} and {@link ExecutionPlan}
     * data stored in a {@link ContentItem#getPart(UriRef, Class) contentPart} with
     * the URI {@link ExecutionMetadata#CHAIN_EXECUTION} over to the
     * {@link ContentItem#getMetadata() metadata} of the content item.<p>
     * This feature is intended to allow users to retrieve such meta information
     * without the need to use parse Multipart MIME responses.
     */
    public static final String INCLUDE_EXECUTION_METADATA = "stanbol.enhancer.web.executionmetadata";
    /**
     * The used format to encode RDF graphs for "multipart/*" responses. This
     * needs to be parsed separately, because the Accept header needs to be
     * set to "multipart/from-data" in such cases
     */
    public static final String RDF_FORMAT = "stanbol.enhancer.web.rdfFormat";
    /**
     * {@link Set Set&lt;String&gt;} containing all the {@link UriRef}s of 
     * {@link ContentItem#getPart(int, Class) ContentItem.getPart}(uri,{@link Blob})
     * that where parsed with the request.
     */
    public static final String PARSED_CONTENT_URIS = "stanbol.enhancer.web.parsedContentURIs";
    
    /**
     * Getter for the EnhancementProperties for an {@link ContentItem}. If they
     * do not already exist they are created and added to the ContentItem as
     * contentPart with the URI {@link #ENHANCEMENT_PROPERTIES_URI}
     * @param ci the contentItem MUST NOT be NULL
     * @return the enhancement properties
     * @throws IllegalArgumentException if <code>null</code> is parsed as {@link ContentItem}.
     */
    @SuppressWarnings("unchecked")
    public static Map<String,Object> getEnhancementProperties(ContentItem ci){
        if(ci == null){
            throw new IllegalArgumentException("The parsed ContentItem MUST NOT be NULL!");
        }
        Map<String,Object> enhancementProperties;
        try {
            enhancementProperties = ci.getPart(ENHANCEMENT_PROPERTIES_URI, Map.class);
        } catch (RuntimeException e) {
           enhancementProperties = new HashMap<String,Object>();
           ci.addPart(ENHANCEMENT_PROPERTIES_URI, enhancementProperties);
        }
        return enhancementProperties;
    }
    
    
    private static Object get(Map<String,Object> enhancementProperties,String key){
        return enhancementProperties == null ? null : enhancementProperties.get(key);
    }
    /**
     * Getter for the value of the parsed type for a given key.
     * @param enhancementProperties the enhancement properties
     * @param key the key
     * @param type the type MUST NOT be <code>null</code>
     * @return the values
     * @throws ClassCastException if the value is not compatible to the
     * parsed type
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String,Object> enhancementProperties,String key,Class<T> type){
        if(type == null){
            throw new IllegalArgumentException("The parsed type MUST NOT be NULL!");
        }
        Object value = get(enhancementProperties, key);
        if(value == null){
            return null;
        } else if(type.isAssignableFrom(value.getClass())){
            return (T)value;
        } else {
            throw new ClassCastException("EnhancementProperties value for key '"
                    + key +"' is not of the expected type "+type+" but was"
                    + value.getClass());
        }
    }
    /**
     * Getter for the boolean state based on the value of the parsed key.
     * If the value is not of type {@link Boolean} the 
     * {@link Boolean#parseBoolean(String)} is used on the {@link Object#toString()}
     * method of the value.
     * @param enhancementProperties the enhancementProperties
     * @param key the key
     * @return the state
     */
    public static boolean getState(Map<String,Object> enhancementProperties,String key){
        Object state = get(enhancementProperties, key);
        return state == null ? false : 
            state instanceof Boolean ? ((Boolean)state).booleanValue() : 
                Boolean.parseBoolean(state.toString());
    }
    public static boolean isOmitParsedContent(Map<String,Object> enhancementProperties){
        return getState(enhancementProperties, OMIT_PARSED_CONTENT);
    }
    public static boolean isIncludeExecutionMetadata(Map<String,Object> enhancementProperties){
        return getState(enhancementProperties, INCLUDE_EXECUTION_METADATA);
    }
    public static boolean isOmitMetadata(Map<String,Object> enhancementProperties){
        return getState(enhancementProperties, OMIT_METADATA);
    }
    /**
     * 
     * @param enhancementProperties
     * @return
     * @throws ClassCastException if the value is not an Set
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> getParsedContentURIs(Map<String,Object> enhancementProperties){
        return (Collection<String>)get(enhancementProperties, PARSED_CONTENT_URIS, Collection.class);
    }
    /**
     * 
     * @param enhancementProperties
     * @return
     * @throws ClassCastException if the value is not an {@link Set}
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> getOutputContentParts(Map<String,Object> enhancementProperties){
        return (Collection<String>)get(enhancementProperties, OUTPUT_CONTENT_PART, Collection.class);
    }
    /**
     * 
     * @param enhancementProperties
     * @return
     * @throws ClassCastException if the value is not an {@link Collections}
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> getOutputContent(Map<String,Object> enhancementProperties){
        return (Collection<String>)get(enhancementProperties, OUTPUT_CONTENT, Collection.class);
    }
    /**
     * 
     * @param enhancementProperties
     * @return
     * @throws ClassCastException if the value is not an {@link Collections}
     */
    public static String getRdfFormat(Map<String,Object> enhancementProperties){
        return (String) get(enhancementProperties,RDF_FORMAT,String.class);
    }
}