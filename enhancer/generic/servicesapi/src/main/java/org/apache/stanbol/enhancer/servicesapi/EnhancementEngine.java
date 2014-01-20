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

import java.util.Map;

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
     * instead of queueing a request for enhancement.
     */
    int ENHANCE_SYNCHRONOUS = 1;

    /**
     * Return value for {@link #canEnhance}, meaning this engine can enhance
     * supplied {@link ContentItem}, and suggests queueing a request for
     * enhancement instead of enhancing it synchronously.
     */
    int ENHANCE_ASYNC = 2;

    /**
     * Indicate if this engine can enhance supplied ContentItem, and if it
     * suggests enhancing it synchronously or asynchronously. The
     * {@link EnhancementJobManager} can force sync/async mode if desired, it is
     * just a suggestion from the engine.
     * <p>
     * This method is expected to execute fast and MUST NOT change the parsed
     * {@link ContentItem}. It is called with a read lock on the ContentItem.
     * <p>
     * <b>NOTE:</b> Returning {@link #CANNOT_ENHANCE} will cause the 
     * {@link EnhancementJobManager} to skip the execution of this Engine. If
     * an {@link EngineException} is thrown the executed {@link Chain} will
     * fail (unless this engine is marked as OPTIONAL).
     *
     * @param ci The ContentItem to enhance
     * @throws EngineException if the introspecting process of the content item
     *             fails
     */
    int canEnhance(ContentItem ci) throws EngineException;

    /**
     * Compute enhancements for supplied ContentItem. The results of the process
     * are expected to be stored in the {@link ContentItem#getMetadata() metadata 
     * of the content item} or by adding/modifying any contentPart.<p>
     * Engines that do support {@link #ENHANCE_ASYNC} are required to use the
     * {@link ContentItem#getLock()} to acquire read/write locks when reading/
     * modifying information of the {@link ContentItem}. For Engines that that
     * do use {@link #ENHANCE_SYNCHRONOUS} the {@link EnhancementJobManager}
     * is responsible to acquire a write lock before calling this method. 
     * <p>
     * <b>NOTE</b>: If an EnhancementEngine can not extract any information it
     * is expected to return. In case an error is encountered during processing
     * an {@link EngineException} need to be thrown.
     *
     * @param ci The ContentItem to enhance
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
