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
package org.apache.stanbol.contenthub.servicesapi.index;

import java.util.List;

import org.osgi.framework.Constants;

/**
 * Provides methods to access to managed {@link SemanticIndex}es by different means such name,
 * {@link EndpointType} or both.
 * 
 */
public interface SemanticIndexManager {
    /**
     * Retrieves the {@link SemanticIndex} instance with the given name and having highest
     * {@link Constants#SERVICE_RANKING} value.
     * 
     * @param name
     *            Name of the {@link SemanticIndex} to be retrieved
     * @return the {@link SemanticIndex} instance with the given name if there is any, otherwise {@code null}
     * @throws IndexManagementException
     */
    SemanticIndex getIndex(String name) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex} instances with the given name.
     * 
     * @param name
     *            Name of the {@link SemanticIndex}es to be retrieved
     * @return the {@link SemanticIndex}es with the given name if there is any, otherwise an empty list
     * @throws IndexManagementException
     */
    List<SemanticIndex> getIndexes(String name) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex} instance with the given {@link EndpointType} and having highest
     * {@link Constants#SERVICE_RANKING} value.
     * 
     * @param endpointType
     *            REST {@link EndpointType} of the {@link SemanticIndex} to be retrieved
     * @return the {@link SemanticIndex} instance with the given {@link EndpointType} if there is any,
     *         otherwise {@code null}
     * @throws IndexManagementException
     */
    SemanticIndex getIndex(EndpointType endpointType) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex}es with the given {@link EndpointType}.
     * 
     * @param endpointType
     *            REST {@link EndpointType} of the {@link SemanticIndex}es to be retrieved
     * @return the {@link SemanticIndex}es instances with the given {@link EndpointType} if there is any,
     *         otherwise an empty list
     * @throws IndexManagementException
     */
    List<SemanticIndex> getIndexes(EndpointType endpointType) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex} instance with the given name, {@link EndpointType} and highest
     * {@link Constants#SERVICE_RANKING} value.
     * 
     * @param name
     *            Name of the {@link SemanticIndex}es to be retrieved
     * @param endpointType
     *            REST {@link EndpointType} of the {@link SemanticIndex} to be retrieved
     * @return the {@link SemanticIndex} instance with the given name, {@link EndpointType} if there is any,
     *         otherwise {@code null}
     * @throws IndexManagementException
     */
    SemanticIndex getIndex(String name, EndpointType endpointType) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex}es instance with the given name and {@link EndpointType}.
     * 
     * @param name
     *            Name of the {@link SemanticIndex}es to be retrieved
     * @param endpointType
     *            REST {@link EndpointType} of the {@link SemanticIndex} to be retrieved
     * @return the {@link SemanticIndex} instance with the given name and {@link EndpointType} if there is
     *         any, otherwise an empty list
     * @throws IndexManagementException
     */
    List<SemanticIndex> getIndexes(String name, EndpointType endpointType) throws IndexManagementException;
}
