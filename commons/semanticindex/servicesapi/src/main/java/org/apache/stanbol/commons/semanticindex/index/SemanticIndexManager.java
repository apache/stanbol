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
package org.apache.stanbol.commons.semanticindex.index;

import java.util.List;

import org.osgi.framework.Constants;

/**
 * Provides methods to access to managed {@link SemanticIndex}es by different means such name,
 * {@link EndpointTypeEnum} or both.
 * 
 */
public interface SemanticIndexManager {
    /**
     * Retrieves the {@link SemanticIndex} instance with the given name and having highest
     * {@link Constants#SERVICE_RANKING} value.
     * 
     * @param name
     *            Name of the {@link SemanticIndex} to be retrieved
     * @return the {@link SemanticIndex} instance with the given name if there is any, otherwise {@code null}.
     *         In case several {@link SemanticIndex}s would confirm to the parsed requirements the one with
     *         the higest {@link Constants#SERVICE_RANKING ranking} is returned.
     * @throws IndexManagementException
     */
    SemanticIndex<?> getIndex(String name) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex} instances with the given name.
     * 
     * @param name
     *            Name of the {@link SemanticIndex}es to be retrieved
     * @return the {@link SemanticIndex}es with the given name if there is any, otherwise an empty
     *         list.Returned SemanticIndexes are sorted by their {@link Constants#SERVICE_RANKING rankings}.
     * @throws IndexManagementException
     */
    List<SemanticIndex<?>> getIndexes(String name) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex} instance with the given {@link EndpointTypeEnum} and having highest
     * {@link Constants#SERVICE_RANKING} value.
     * 
     * @param endpointType
     *            The name of the endpoint type that need to be supported by the returned SemanticIndexes. See
     *            the {@link EndpointTypeEnum} for well known RESTfull endpoint types. to search for Java
     *            endpoints use {@link Class#getName()}.
     * @return the {@link SemanticIndex} instance with the given {@link EndpointTypeEnum} if there is any,
     *         otherwise {@code null}. In case several {@link SemanticIndex}s would confirm to the parsed
     *         requirements the one with the higest {@link Constants#SERVICE_RANKING ranking} is returned.
     * @throws IndexManagementException
     */
    SemanticIndex<?> getIndexByEndpointType(String endpointType) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex}es with the given {@link EndpointTypeEnum}.
     * 
     * @param endpointType
     *            The name of the endpoint type that need to be supported by the returned SemanticIndexes. See
     *            the {@link EndpointTypeEnum} for well known RESTfull endpoint types. to search for Java
     *            endpoints use {@link Class#getName()}.
     * @return the {@link SemanticIndex}es instances with the given {@link EndpointTypeEnum} if there is any,
     *         otherwise an empty list. Returned SemanticIndexes are sorted by their
     *         {@link Constants#SERVICE_RANKING rankings}.
     * @throws IndexManagementException
     */
    List<SemanticIndex<?>> getIndexesByEndpointType(String endpointType) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex} instance with the given name, {@link EndpointTypeEnum} and highest
     * {@link Constants#SERVICE_RANKING} value.
     * 
     * @param name
     *            Name of the {@link SemanticIndex}es to be retrieved. <code>null</code> is used as wildcard
     * @param endpointType
     *            The name of the endpoint type that need to be supported by the returned SemanticIndexes. See
     *            the {@link EndpointTypeEnum} for well known RESTfull endpoint types. to search for Java
     *            endpoints use {@link Class#getName()}. <code>null</code> is used as wildcard
     * @return the {@link SemanticIndex} instance with the given name, {@link EndpointTypeEnum} if there is
     *         any, otherwise {@code null}. In case several {@link SemanticIndex}s would confirm to the parsed
     *         requirements the one with the higest {@link Constants#SERVICE_RANKING ranking} is returned.
     * @throws IndexManagementException
     */
    SemanticIndex<?> getIndex(String name, String endpointType) throws IndexManagementException;

    /**
     * Retrieves the {@link SemanticIndex}es instance with the given name and {@link EndpointTypeEnum}.
     * 
     * @param name
     *            Name of the {@link SemanticIndex}es to be retrieved. <code>null</code> is used as wildcard
     * @param endpointType
     *            The name of the endpoint type that need to be supported by the returned SemanticIndexes. See
     *            the {@link EndpointTypeEnum} for well known endpoint types. <code>null</code> is used as
     *            wildcard
     * @return the {@link SemanticIndex} instance with the given name and {@link EndpointTypeEnum} if there is
     *         any, otherwise an empty list. Returned SemanticIndexes are sorted by their
     *         {@link Constants#SERVICE_RANKING rankings}.
     * @throws IndexManagementException
     */
    List<SemanticIndex<?>> getIndexes(String name, String endpointType) throws IndexManagementException;
}
