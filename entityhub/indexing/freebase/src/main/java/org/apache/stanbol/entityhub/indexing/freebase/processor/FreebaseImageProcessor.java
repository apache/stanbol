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
package org.apache.stanbol.entityhub.indexing.freebase.processor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple processor that takes values of the {@link #PARAM_SOURCE_PROPERTY} 
 * (default: <code>fb:common.topic.image</code>)
 * property and converts it to URIs that can be used to retrieve an image via
 * the google APIs
 * @author Rupert Westenthaler
 *
 */
public class FreebaseImageProcessor implements EntityProcessor {
    
    private final Logger log = LoggerFactory.getLogger(FreebaseImageProcessor.class);

    public static final String FB_NS = "http://rdf.freebase.com/ns/";
    public static final String FOAF_NS = NamespaceEnum.foaf.getNamespace();
    
    /**
     * The source property for the image links. Values MUST BE of type
     * {@link Reference} and contain Freebase topics 
     */
    public static final String PARAM_SOURCE_PROPERTY = "source-property";
    public static final String DEFAULT_SOURCE_PROPERTY = FB_NS + "common.topic.image";
    
    /**
     * The service URI of the Image Service
     */
    public static final String PARAM_IMAGE_SERVICE_URI = "img-service";
    public static final String DEFAULT_IMAGE_SERVICE_URI = "https://www.googleapis.com/freebase/v1/image/";
    
    /**
     * The property used for thumbnails. If '<code>!</code>' is parsed thumbnail
     * generation is deactivated. 
     */
    public static final String PARAM_THUMBNAIL_PROPERTY = "thumbnail-property";
    public static final String DEFAULT_THUMBNAIL_PROPERTY = FOAF_NS + "thumbnail";
    public static final String PARAM_MAX_THUMBNAIL_SIZE = "thumbnail-max-size";
    public static final int DEFAULT_MAX_THUMBNAIL_SIZE = -1; //use the default
    
    /**
     * The property used for depictions. If '<code>!</code>' is parsed depiction
     * generation is deactivated. 
     */
    public static final String PARAM_DEPICTION_PROPERTY = "depiction-property";
    public static final String DEFAULT_DEPICTION_PROPERTY = FOAF_NS + "depiction";
    public static final String PARAM_MAX_DEPICTION_WIDTH = "depicition-max-width";
    public static final int DEFAULT_MAX_DEPICTION_WIDTH = 800;
    public static final String PARAM_MAX_DEPICTION_HEIGTH = "depicition-max-heigth";
    public static final int DEFAULT_MAX_DEPICTION_HEIGTH = 600;
    
    private String serviceBase;
    private String srcProperty;
    private String thumbnailProperty;
    private int iconMaxSize;
    private String depictionProperty;
    private int[] depictionMaxSize = new int[]{-1,-1};
    
    @Override
    public void setConfiguration(Map<String,Object> config) {
        
        Object value = config.get(PARAM_SOURCE_PROPERTY);
        srcProperty = value == null ? DEFAULT_SOURCE_PROPERTY : value.toString();
                
        value = config.get(PARAM_IMAGE_SERVICE_URI);
        serviceBase = value == null ? DEFAULT_IMAGE_SERVICE_URI : value.toString();
        
        value = config.get(PARAM_THUMBNAIL_PROPERTY);
        thumbnailProperty = value == null ? DEFAULT_THUMBNAIL_PROPERTY : 
            "!".equals(value) ? null : value.toString();

        value = config.get(PARAM_MAX_THUMBNAIL_SIZE);
        if(value instanceof Number){
            iconMaxSize = ((Number)value).intValue();
        } else if( value != null){
            try {
                iconMaxSize = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse integer parameter '"
                    + PARAM_MAX_THUMBNAIL_SIZE+"' form the configured value '"
                    + value +"'!", e);
            }
        } else {
            iconMaxSize = DEFAULT_MAX_THUMBNAIL_SIZE;
        }
        value = config.get(PARAM_DEPICTION_PROPERTY);
        depictionProperty = value == null ? DEFAULT_DEPICTION_PROPERTY : 
            "!".equals(value) ? null : value.toString();

        value = config.get(PARAM_MAX_DEPICTION_WIDTH);
        if(value instanceof Number){
            depictionMaxSize[0] = ((Number)value).intValue();
        } else if( value != null){
            try {
                depictionMaxSize[0] = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse integer parameter '"
                    + PARAM_MAX_DEPICTION_WIDTH+"' form the configured value '"
                    + value +"'!", e);
            }
        } else {
            depictionMaxSize[0] = DEFAULT_MAX_DEPICTION_WIDTH;
        }
        value = config.get(PARAM_MAX_DEPICTION_HEIGTH);
        if(value instanceof Number){
            depictionMaxSize[1] = ((Number)value).intValue();
        } else if( value != null){
            try {
                depictionMaxSize[1] = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Unable to parse integer parameter '"
                    + PARAM_MAX_DEPICTION_HEIGTH+"' form the configured value '"
                    + value +"'!", e);
            }
        } else {
            depictionMaxSize[1] = DEFAULT_MAX_DEPICTION_HEIGTH;
        }
    
    }

    @Override
    public Representation process(Representation rep) {
        Iterator<Reference> images = rep.getReferences(srcProperty);
        while(images.hasNext()){
            String source = images.next().getReference();
            int nsIndex = source.lastIndexOf('/');
            if(nsIndex > 0 && nsIndex < source.length()-3){
                String entityId = source.substring(source.lastIndexOf('/')+3);
                if(source.charAt(nsIndex+1) == 'm' && source.charAt(nsIndex+2) == '.'){
                    if(thumbnailProperty != null){
                        StringBuilder url = new StringBuilder(serviceBase);
                        url.append("m/").append(entityId);
                        if(iconMaxSize > 0){
                            url.append("?maxwidth=").append(iconMaxSize);
                            url.append("&maxheight=").append(iconMaxSize);
                        } //else use default
                        rep.addReference(thumbnailProperty, url.toString());
                    }
                    if(depictionProperty != null){
                        StringBuilder url = new StringBuilder(serviceBase);
                        url.append("m/").append(entityId);
                        boolean first = true;
                        if(depictionMaxSize[0] > 0){
                            url.append("?maxwidth=").append(depictionMaxSize[0]);
                            first = false;
                        }
                        if(depictionMaxSize[1] > 0){
                            url.append(first ? '?' : '&');
                            url.append("maxheight=").append(depictionMaxSize[1]);
                        }
                        rep.addReference(depictionProperty, url.toString());
                    }
                } else {
                    log.warn(" value '{}' of entity '{}' and property '{}' is not an Freebase Entity (ignored)!",
                        new Object[]{source,rep.getId(),srcProperty});
                }
            } else {
                log.warn(" value '{}' of entity '{}' and property '{}' is not an Freebase Entity (ignored)!",
                    new Object[]{source,rep.getId(),srcProperty});
            }
        }
        return rep;
    }

    @Override
    public boolean needsInitialisation() {
        return false;
    }

    @Override
    public void initialise() {
    }

    @Override
    public void close() {
    }


}
