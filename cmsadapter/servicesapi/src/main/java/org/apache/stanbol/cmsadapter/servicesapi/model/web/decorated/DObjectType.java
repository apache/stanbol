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
package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

/**
 * Decorated form of {@link ObjectTypeDefinition}. While {@link ObjectTypeDefinition} is completely separated
 * from the repository it is generated, {@link DObjectType} is able to reconnect to the repository and fetch
 * any data that is not present in {@link ObjectTypeDefinition}. </br> Details of when the repository is
 * determined by {@link AdapterMode}s. See {@link DObjectAdapter} and {@link AdapterMode} for more details.
 * 
 * @author cihan
 * 
 */
public interface DObjectType {

    /**
     * 
     * @return Unique identifier of underlying {@link ObjectTypeDefinition}
     */
    String getID();

    /**
     * 
     * @return Localname of underlying {@link ObjectTypeDefinition}
     * 
     */
    String getName();

    /**
     * 
     * @return Namespace of underlying {@link ObjectTypeDefinition}
     */
    String getNamespace();

    /**
     * 
     * @return Property definitions of underlying {@link ObjectTypeDefinition}, wrapped as {@link DPropertyDefinition} 
     * @throws RepositoryAccessException
     *             If repository can not be accessed in <b>ONLINE</b> mode.
     */
    List<DPropertyDefinition> getPropertyDefinitions() throws RepositoryAccessException;

    /**
     * 
     * @return Parent type definitions of underlying {@link ObjectTypeDefinition}, wrapped as {@link DObjectType} 
     * @throws RepositoryAccessException
     *             If repository can not be accessed in <b>ONLINE</b> mode.
     */
    List<DObjectType> getParentDefinitions() throws RepositoryAccessException;

    /**
     * 
     * @return Child type definitions of underlying {@link ObjectTypeDefinition}, wrapped as {@link DObjectType} 
     * @throws RepositoryAccessException
     *             If repository can not be accessed in <b>ONLINE</b> mode.
     */
    List<DObjectType> getChildDefinitions() throws RepositoryAccessException;

    /**
     * 
     * @return Underlying {@link ObjectTypeDefinition}
     */
    ObjectTypeDefinition getInstance();

}
