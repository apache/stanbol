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
import java.util.Map;
import java.util.Set;

import javax.ws.rs.QueryParam;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;

/**
 * Defines Constants and utilities for using request scoped EnhancementProperties.
 * Especially those internally used by the enhancer.jersey module.<p>
 * This replaces the {@link EnhancementPropertiesHelper}
 * @since 0.12.1
 */
public final class RequestPropertiesHelper {

    private RequestPropertiesHelper(){/* no instances allowed*/}
    /**
     * @see ContentItemHelper#REQUEST_PROPERTIES_URI
     */
    public static final IRI REQUEST_PROPERTIES_URI =
            ContentItemHelper.REQUEST_PROPERTIES_URI;
    /**
     * Boolean switch parsed as {@link QueryParam} tha allows to deactivate the
     * inclusion of the {@link ContentItem#getMetadata()} in the Response
     */
    public static final String OMIT_METADATA = "stanbol.enhancer.web.omitMetadata";
    /**
     * {@link Set Set&lt;String&gt;} containing all the URIs of the
     * {@link ContentItem#getPart(IRI, Class) ContentParts} representing 
     * RDF data (compatible to Clerezza {@link Graph}). If the 
     * returned set contains '*' than all such content parts need to be returned.<p>
     * NOTE: This can also be used to include the Request Properties
     * as "applciation/json" in the Response by adding this
     * {@link RequestPropertiesHelper#REQUEST_PROPERTIES_URI uri}.
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
     * data stored in a {@link ContentItem#getPart(IRI, Class) contentPart} with
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
     * {@link Set Set&lt;String&gt;} containing all the {@link IRI}s of 
     * {@link ContentItem#getPart(int, Class) ContentItem.getPart}(uri,{@link Blob})
     * that where parsed with the request.
     */
    public static final String PARSED_CONTENT_URIS = "stanbol.enhancer.web.parsedContentURIs";
    
    private static Object get(Map<String,Object> reqProp,String key){
        return reqProp == null ? null : reqProp.get(key);
    }
    /**
     * Getter for the value of the parsed type for a given key.
     * @param reqProp the request properties
     * @param key the key
     * @param type the type MUST NOT be <code>null</code>
     * @return the values or <code>null</code> if the parsed request properties
     * where <code>null</code> or the parsed key was not present.
     * @throws ClassCastException if the value is not compatible to the
     * parsed type
     * @throws NullPointerException if the parsed key or type is <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String,Object> reqProp,String key,Class<T> type){
        if(reqProp == null){
            return null;
        }
        if(type == null){
            throw new NullPointerException("The parsed type MUST NOT be NULL!");
        }
        if(key == null){
            throw new NullPointerException("The parsed key MUST NOT be NULL!");
        }
        Object value = get(reqProp, key);
        if(value == null){
            return null;
        } else if(type.isAssignableFrom(value.getClass())){
            return (T)value;
        } else {
            throw new ClassCastException("RequestProperties value for key '"
                    + key +"' is not of the expected type "+type+" but was"
                    + value.getClass());
        }
    }
    /**
     * Getter for the boolean state based on the value of the parsed key.
     * If the value is not of type {@link Boolean} the 
     * {@link Boolean#parseBoolean(String)} is used on the {@link Object#toString()}
     * method of the value.
     * @param reqProp the request properties
     * @param key the key
     * @return the state. <code>false</code> if the parsed request property
     * map was <code>null</code> or the key was not present.
     * @throw {@link NullPointerException} if <code>null</code> is parsed as key
     */
    public static boolean getState(Map<String,Object> reqProp,String key){
        if(key == null){
            throw new NullPointerException("The parsed key MUST NOT be NULL!");
        }
        if(reqProp == null){
            return Boolean.FALSE;
        } else {
            Object state = get(reqProp, key);
            return state == null ? false : 
                state instanceof Boolean ? ((Boolean)state).booleanValue() : 
                    Boolean.parseBoolean(state.toString());
        }
    }
    /**
     * Checks the request properties for the {@link #OMIT_PARSED_CONTENT} state
     */
    public static boolean isOmitParsedContent(Map<String,Object> reqProp){
        return getState(reqProp, OMIT_PARSED_CONTENT);
    }
    /**
     * Checks the request properties for the {@link #INCLUDE_EXECUTION_METADATA} state
     */
    public static boolean isIncludeExecutionMetadata(Map<String,Object> reqProp){
        return getState(reqProp, INCLUDE_EXECUTION_METADATA);
    }
    /**
     * Checks the request properties for the {@link #OMIT_METADATA} state
     */
    public static boolean isOmitMetadata(Map<String,Object> reqProp){
        return getState(reqProp, OMIT_METADATA);
    }
    /**
     * Getter for the {@link #PARSED_CONTENT_URIS}
     * @param reqProp
     * @return
     * @throws ClassCastException if the value is not a {@link Collection}
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> getParsedContentURIs(Map<String,Object> reqProp){
        return (Collection<String>)get(reqProp, PARSED_CONTENT_URIS, Collection.class);
    }
    /**
     * Getter for the {@link #OUTPUT_CONTENT_PART}
     * @param reqProp
     * @return
     * @throws ClassCastException if the value is not an {@link Collection}
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> getOutputContentParts(Map<String,Object> reqProp){
        return (Collection<String>)get(reqProp, OUTPUT_CONTENT_PART, Collection.class);
    }
    /**
     * Getter for the {@link #OUTPUT_CONTENT}
     * @param reqProp
     * @return
     * @throws ClassCastException if the value is not a {@link Collection}
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> getOutputContent(Map<String,Object> reqProp){
        return (Collection<String>)get(reqProp, OUTPUT_CONTENT, Collection.class);
    }
    /**
     * Getter for the {@link #RDF_FORMAT}
     * @param reqProp
     * @return
     * @throws ClassCastException if the value is not a {@link String}
     */
    public static String getRdfFormat(Map<String,Object> reqProp){
        return get(reqProp,RDF_FORMAT,String.class);
    }
}