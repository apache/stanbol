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
 * Interface to internal or external semantic enhancement engines. There will
 * usually be several of those, that the EnhancementJobManager uses to enhance
 * content items.
 */
public interface EnhancementEngine {

    /**
     * The property used to provide the name of an enhancement engine.
     */
    String PROPERTY_NAME = "stanbol.enhancer.engine.name";
    /**
     * Return value for {@link #canEnhance}, meaning this engine cannot enhance
     * supplied {@link ContentItem}
     */
    int CANNOT_ENHANCE = 0;

    /**
     * Return value for {@link #canEnhance}, meaning this engine can enhance
     * supplied {@link ContentItem}, and suggests enhancing it synchronously
     * instead of queuing a request for enhancement.
     */
    int ENHANCE_SYNCHRONOUS = 1;

    /**
     * Return value for {@link #canEnhance}, meaning this engine can enhance
     * supplied {@link ContentItem}, and suggests queuing a request for
     * enhancement instead of enhancing it synchronously.
     */
    int ENHANCE_ASYNC = 2;

    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link EnhancementJobManager} can force sync/async mode if desired, it is
     * just a suggestion from the engine.
     *
     * @throws EngineException if the introspecting process of the content item
     *             fails
     */
    int canEnhance(ContentItem ci) throws EngineException;

    /**
     * Compute enhancements for supplied ContentItem. The results of the process
     * are expected to be stored in the metadata of the content item.
     *
     * The client (usually an {@link EnhancementJobManager}) should take care of
     * persistent storage of the enhanced {@link ContentItem}.
     *
     * @throws EngineException if the underlying process failed to work as
     *             expected
     */
    void computeEnhancements(ContentItem ci) throws EngineException;
    /**
     * Getter for the name of this EnhancementEngine instance as configured
     * by {@link #PROPERTY_NAME}
     * @return the name
     */
    String getName();

}
