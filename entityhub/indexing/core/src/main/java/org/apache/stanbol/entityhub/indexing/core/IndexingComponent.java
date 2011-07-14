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
package org.apache.stanbol.entityhub.indexing.core;

import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.impl.IndexerImpl;

/**
 * Parent Interface defining the indexing work flow methods for all kinds of
 * data sources used for the Indexer.
 * @author Rupert Westenthaler
 *
 */
public interface IndexingComponent {

    /**
     * Setter for the configuration
     * @param config the configuration
     */
    public void setConfiguration(Map<String,Object> config);
    /**
     * Used by the {@link IndexerImpl} to check if this source needs to be
     * {@link #initialise()}.
     * @return If <code>true</code> is returned the {@link IndexerImpl} will call
     * {@link #initialise()} during the initialisation phase of the indexing
     * process.
     */
    public boolean needsInitialisation();
    /**
     * Initialise the IndexingSource. This should be used to perform 
     * time consuming initialisations.
     */
    public void initialise();
    
    /**
     * Called by the {@link IndexerImpl} as soon as this source is no longer needed
     * for the indexing process.
     */
    public void close();
    
}
