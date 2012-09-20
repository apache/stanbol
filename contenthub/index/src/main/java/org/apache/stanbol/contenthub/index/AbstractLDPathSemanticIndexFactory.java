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
package org.apache.stanbol.contenthub.index;

import java.util.Properties;

import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;

/**
 * This is an abstract factory which can be used to build other {@link SemanticIndex} factories.
 * 
 * @author meric
 * 
 */
public abstract class AbstractLDPathSemanticIndexFactory {
    /**
     * Manager to keep track of the metadata regarding the {@link SemanticIndex}es managed by this factory
     */
    protected SemanticIndexMetadataManager semanticIndexMetadataManager;

    /**
     * Returns the {@link SemanticIndexMetadataManager} associated with this factory
     * 
     * @return
     */
    public SemanticIndexMetadataManager getSemanticIndexMetadataManager() {
        if (this.semanticIndexMetadataManager != null) {
            return this.semanticIndexMetadataManager;
        } else {
            throw new IllegalStateException("SemanticIndexMetadataManager has not been initialized yet");
        }
    }

    /**
     * Creates an {@link SemanticIndex} instance based on the given name, description and LDPath program.
     * 
     * @param indexName
     *            name of the index to be created
     * @param indexDescription
     *            description of the index to be created
     * @param ldPathProgram
     *            LDPath program of the index
     * @throws IndexManagementException
     */
    public abstract String createIndex(String indexName, String indexDescription, String ldPathProgram) throws IndexManagementException;

    /**
     * Creates an {@link SemanticIndex} instance based on the given index metadata. However, provided
     * {@link Properties} must include the following mandatory items.
     * <ul>
     * <li><b>{@link SemanticIndex#PROP_NAME}</b></li>
     * <li><b>{@link AbstractLDPathSemanticIndex#PROP_LD_PATH_PROGRAM}</b></li>
     * </ul>
     * 
     * Other parameters required for specific implementations of this method should be seen in their own
     * documentations.
     * 
     * @param indexMetadata
     *            {@link Properties} containing the possible metadata about the index to be created
     * @throws IndexManagementException
     */
    public abstract String createIndex(Properties indexMetadata) throws IndexManagementException;

    /**
     * Remove the underlying index and all of its metadata associated with the {@code pid}
     * 
     * @param pid
     *            persistent identifier (pid) of the factory configuration of {@link SemanticIndex}
     * @throws IndexManagementException
     */
    public abstract void removeIndex(String pid) throws IndexManagementException;
}
