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

import javax.ws.rs.QueryParam;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;

/**
 * Defines Constants and utilities for using EnhancementProperties
 * @deprecated unse the {@link RequestPropertiesHelper} instead
 */
public final class EnhancementPropertiesHelper {

    private EnhancementPropertiesHelper(){/* no instances allowed*/}
    /**
     * 
     * @see ContentItemHelper#REQUEST_PROPERTIES_URI
     * @deprecated use {@link RequestPropertiesHelper#REQUEST_PROPERTIES_URI}.
     * <b>NOTE</b> this URI has changed with <code>0.12.1</code> from 
     * <code>urn:apache.org:stanbol.web:enhancement.properties</code> to
     * <code>urn:apache.org:stanbol.enhancer:request.properties</code>. The
     * {@link #getEnhancementProperties(ContentItem)} support content parts
     * registered with both URIs.
     */
    public static final UriRef ENHANCEMENT_PROPERTIES_URI =
            RequestPropertiesHelper.REQUEST_PROPERTIES_URI;
    /**
     * Boolean switch parsed as {@link QueryParam} tha allows to deactivate the
     * inclusion of the {@link ContentItem#getMetadata()} in the Response
     * @deprecated use {@link RequestPropertiesHelper#OMIT_METADATA} instead
     */
    public static final String OMIT_METADATA = RequestPropertiesHelper.OMIT_METADATA;
    /**
     * {@link Set Set&lt;String&gt;} containing all the URIs of the
     * {@link ContentItem#getPart(UriRef, Class) ContentParts} representing 
     * RDF data (compatible to Clerezza {@link TripleCollection}). If the 
     * returned set contains '*' than all such content parts need to be returned.<p>
     * NOTE: This can also be used to include the EnhancementProperties
     * as "applciation/json" in the Response by adding this
     * {@link EnhancementPropertiesHelper#ENHANCEMENT_PROPERTIES_URI uri}.
     * @deprecated use {@link RequestPropertiesHelper#OUTPUT_CONTENT_PART} instead
     */
    public static final String OUTPUT_CONTENT_PART = RequestPropertiesHelper.OUTPUT_CONTENT_PART;
    /**
     * Allows to omit all parsed content parts regardless of the {@link #OUTPUT_CONTENT_PART}
     * configuration
     * @deprecated use {@link RequestPropertiesHelper#OMIT_PARSED_CONTENT} instead
     */
    public static final String OMIT_PARSED_CONTENT = RequestPropertiesHelper.OMIT_PARSED_CONTENT;
    /**
     * {@link Collection Collection&lt;String&gt;} containing mime types. This
     * allows to specify what versions of the parsed content to be included in
     * the response. e.g. ["text/*","application/pdf"] would include all text
     * formats and PDF.
     * @deprecated use {@link RequestPropertiesHelper#OUTPUT_CONTENT} instead
     */
    public static final String OUTPUT_CONTENT = RequestPropertiesHelper.OUTPUT_CONTENT;
    /**
     * This allows to copy the {@link ExecutionMetadata} and {@link ExecutionPlan}
     * data stored in a {@link ContentItem#getPart(UriRef, Class) contentPart} with
     * the URI {@link ExecutionMetadata#CHAIN_EXECUTION} over to the
     * {@link ContentItem#getMetadata() metadata} of the content item.<p>
     * This feature is intended to allow users to retrieve such meta information
     * without the need to use parse Multipart MIME responses.
     * @deprecated use {@link RequestPropertiesHelper#INCLUDE_EXECUTION_METADATA} instead
     */
    public static final String INCLUDE_EXECUTION_METADATA = RequestPropertiesHelper.INCLUDE_EXECUTION_METADATA;
    /**
     * The used format to encode RDF graphs for "multipart/*" responses. This
     * needs to be parsed separately, because the Accept header needs to be
     * set to "multipart/from-data" in such cases
     * @deprecated use {@link RequestPropertiesHelper#RDF_FORMAT} instead
     */
    public static final String RDF_FORMAT = RequestPropertiesHelper.RDF_FORMAT;
    /**
     * {@link Set Set&lt;String&gt;} containing all the {@link UriRef}s of 
     * {@link ContentItem#getPart(int, Class) ContentItem.getPart}(uri,{@link Blob})
     * that where parsed with the request.
     * @deprecated use {@link RequestPropertiesHelper#PARSED_CONTENT_URIS} instead
     */
    public static final String PARSED_CONTENT_URIS = RequestPropertiesHelper.PARSED_CONTENT_URIS;
    
    /**
     * Inits (get or creates) the content part holding the EnhancementProperties
     * @param ci the contentItem MUST NOT be NULL
     * @return the enhancement properties
     * @throws IllegalArgumentException if <code>null</code> is parsed as {@link ContentItem}.
     * @see ContentItemHelper#initRequestPropertiesContentPart(ContentItem)
     * @deprecated use {@link ContentItemHelper#initRequestPropertiesContentPart(ContentItem)}
     * or {@link ContentItemHelper#getRequestPropertiesContentPart(ContentItem)}
     * instead
     */
    public static Map<String,Object> getEnhancementProperties(ContentItem ci){
        return ContentItemHelper.initRequestPropertiesContentPart(ci);
    }
    
    /**
     * Getter for the value of the parsed type for a given key.
     * @param reqProp the request properties
     * @param key the key
     * @param type the type MUST NOT be <code>null</code>
     * @return the values
     * @throws ClassCastException if the value is not compatible to the
     * parsed type
     * @deprecated use {@link RequestPropertiesHelper#get(Map, String, Class)} instead
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String,Object> reqProp,String key,Class<T> type){
        return RequestPropertiesHelper.get(reqProp, key, type);
    }
    /**
     * Getter for the boolean state based on the value of the parsed key.
     * If the value is not of type {@link Boolean} the 
     * {@link Boolean#parseBoolean(String)} is used on the {@link Object#toString()}
     * method of the value.
     * @param reqProp the request properties
     * @param key the key
     * @return the state
     * @deprecated use {@link RequestPropertiesHelper#getState(Map, String)} instead
     */
    public static boolean getState(Map<String,Object> reqProp,String key){
        return RequestPropertiesHelper.getState(reqProp, key);
    }
    /**
     * Checks the request properties for the {@link #OMIT_PARSED_CONTENT} state
     * @deprecated use {@link RequestPropertiesHelper#isOmitParsedContent(Map)} instead
     */
    public static boolean isOmitParsedContent(Map<String,Object> reqProp){
        return RequestPropertiesHelper.isOmitParsedContent(reqProp);
    }
    /**
     * Checks the request properties for the {@link #INCLUDE_EXECUTION_METADATA} state
     * @deprecated use {@link RequestPropertiesHelper#isIncludeExecutionMetadata(Map)} instead
     */
    public static boolean isIncludeExecutionMetadata(Map<String,Object> reqProp){
        return RequestPropertiesHelper.isIncludeExecutionMetadata(reqProp);
    }
    /**
     * Checks the request properties for the {@link #OMIT_METADATA} state
     * @deprecated use {@link RequestPropertiesHelper#isOmitMetadata(Map)} instead
     */
    public static boolean isOmitMetadata(Map<String,Object> reqProp){
        return RequestPropertiesHelper.isOmitMetadata(reqProp);
    }
    /**
     * 
     * @param reqProp
     * @return
     * @throws ClassCastException if the value is not an Set
     * @deprecated use {@link RequestPropertiesHelper#getParsedContentURIs(Map)} instead
     */
    public static Collection<String> getParsedContentURIs(Map<String,Object> reqProp){
        return RequestPropertiesHelper.getParsedContentURIs(reqProp);
    }
    /**
     * 
     * @param reqProp
     * @return
     * @throws ClassCastException if the value is not an {@link Set}
     * @deprecated use {@link RequestPropertiesHelper#getOutputContentParts(Map)} instead
     */
    public static Collection<String> getOutputContentParts(Map<String,Object> reqProp){
        return RequestPropertiesHelper.getOutputContentParts(reqProp);
    }
    /**
     * 
     * @param reqProp
     * @return
     * @throws ClassCastException if the value is not an {@link Collections}
     * @deprecated use {@link RequestPropertiesHelper#getOutputContent(Map)} instead
     */
    public static Collection<String> getOutputContent(Map<String,Object> reqProp){
        return RequestPropertiesHelper.getOutputContent(reqProp);
    }
    /**
     * 
     * @param reqProp
     * @return
     * @throws ClassCastException if the value is not an {@link Collections}
     * @deprecated use {@link RequestPropertiesHelper#getRdfFormat(Map)} instead
     */
    public static String getRdfFormat(Map<String,Object> reqProp){
        return RequestPropertiesHelper.getRdfFormat(reqProp);
    }
}