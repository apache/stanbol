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
package org.apache.stanbol.enhancer.servicesapi;

/**
 * Base exception thrown by EnhancementEngine implementations when they fail to
 * process the provided content item.
 * <p>
 * If the failure is imputable to a malformed input in the
 * {@link ContentItem#getStream()} or {@link ContentItem#getMetadata()} one
 * should throw the subclass {@link InvalidContentException} instead.
 *
 * @author ogrisel
 */
public class EngineException extends EnhancementException {

    private static final long serialVersionUID = 1L;
    private EnhancementEngine enhancementEngine;
    private ContentItem contentItem;
    /**
     * 
     * @param message
     * @deprecated use the constructor with {@link EnhancementEngine} and 
     * {@link ContentItem} instead
     */
    public EngineException(String message) {
        super(message);
    }
    /**
     * 
     * @param message
     * @param cause
     * @deprecated use the constructor with {@link EnhancementEngine} and 
     * {@link ContentItem} instead
     */
    public EngineException(String message, Throwable cause) {
        super(message, cause);
    }
    /**
     * 
     * @param cause
     * @deprecated use the constructor with {@link EnhancementEngine} and 
     * {@link ContentItem} instead
     */
    public EngineException(Throwable cause) {
        super(cause);
    }

    public EngineException(EnhancementEngine ee, ContentItem ci, Throwable cause) {
        this(ee,ci,null,cause);
    }
    public EngineException(EnhancementEngine ee, ContentItem ci, String message, Throwable cause) {
        super(String.format(
                "'%s' failed to process content item '%s' with type '%s': %s",
                ee.getClass().getSimpleName(), ci.getUri().getUnicodeString(), ci.getMimeType(),
                message == null ? cause : message), cause);
        this.enhancementEngine = ee;
        this.contentItem = ci;
        
    }
    
    /**
     * The EnhancementEngine parsed to the Exception
     * @return
     * @since 0.12.1
     */
    public EnhancementEngine getEnhancementEngine() {
        return enhancementEngine;
    }
    
    /**
     * The ContentITem parsed to the Exception
     * @return
     * @since 0.12.1
     */
    public ContentItem getContentItem() {
        return contentItem;
    }
}
