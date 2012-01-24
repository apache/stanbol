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

import java.util.List;

/**
 * Accept requests for enhancing ContentItems, and processes them either
 * synchronously or asynchronously (as decided by the enhancement engines,
 * the job manager implementation, the execution plan provided by the 
 * {@link Chain} or by some additional configurations).
 * <p>
 * The progress of the enhancement process should be made accessible in the
 * ContentItem's metadata.
 */
public interface EnhancementJobManager {

    /**
     * Enhances the parsed contentItem by using the default enhancement
     * Chain.
     * Create relevant asynchronous requests or enhance content immediately. The
     * result is not persisted right now. The caller is responsible for calling the
     * {@link Store#put(ContentItem)} afterwards in case persistence is
     * required.
     * <p>
     * TODO: define the expected semantics if asynchronous enhancements were to
     * get implemented.
     * @throws EnhancementException if the enhancement process failed
     */
    void enhanceContent(ContentItem ci) throws EnhancementException;
    
    /**
     * Processes the parsed {@link ContentItem} by using the 
     * {@link Chain#getExecutionPlan() execution plan} provided by the
     * {@link Chain}.
     * @param ci : ContentItem to be enhanced
     * @param chain : The enhancement Chain used to process the content item
     * @throws EnhancementException : if an Engine required by the Chain fails to
     * process the ContentItem
     * @throws ChainException : if the enhancement process failed
     */
    void enhanceContent(ContentItem ci, Chain chain) throws EnhancementException;

    /**
     * Return the unmodifiable list of active registered engine instance that
     * can be used by the manager.
     * @deprecated use the {@link EnhancementEngineManager} to get information
     * about currently active Engines and the {@link ChainManager} to get active
     * chains. This method will now return active engines of the default chain.
     */
    @Deprecated
    List<EnhancementEngine> getActiveEngines();

}
