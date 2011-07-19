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

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

/**
 * Decorated form of {@link CMSObject}. While {@link CMSObject} is completely separated from the repository it
 * is generated, DObject is able to reconnect to the repository and fetch any data that is not present in
 * {@link CMSObject}. </br> Details of when the repository is determined by {@link AdapterMode}s. See
 * {@link DObjectAdapter} and {@link AdapterMode} for more details.
 * 
 * @author cihan
 * 
 */
public interface DObject {

    /**
     * 
     * @return Unique identifier of underlying CMSObject.
     */
    String getID();

    /**
     * 
     * @return Path of the underlying CMSObject, If more than one path is 
     * present returns the first path.
     */
    String getPath();

    /**
     * 
     * @return Localname of the underlying CMSObject.
     */
    String getName();

    /**
     * 
     * @return Namespace of the underlying CMSObject.
     */
    String getNamespace();
    
    /**
     * 
     * @return Underlying CMSObject.
     */
    CMSObject getInstance();

    /**
     * 
     * @return Direct children of the CMS object, wrapped as {@link DObject}s .May return null in <b>TOLERATED_OFFLINE</b> or <b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException
     *              If repository can not be accessed in <b>ONLINE</b> mode.
     */
    List<DObject> getChildren() throws RepositoryAccessException;

    /**
     * Fetches parent of the item from CMS repository.
     * 
     * @return parent of the object wrapped as {@link DObject} .May return null in <b>TOLERATED_OFFLINE</b> or <b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException
     *             If repository can not be accessed in <b>ONLINE</b> mode.
     */
    DObject getParent() throws RepositoryAccessException;

    /**
     * Fetches object type of the item from CMS repository.
     * 
     * @return Object type of the object wrapped as {@link DObjectType} .May return null in <b>TOLERATED_OFFLINE</b> or <b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException
     *             If repository can not be accessed in <b>ONLINE</b> mode.
     */
    DObjectType getObjectType() throws RepositoryAccessException;
    
    /**
     * 
     * @return Properties of the CMS object wrapped as {@link DProperty} .May return null in <b>TOLERATED_OFFLINE</b> or <b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException
     *              If repository can not be accessed in <b>ONLINE</b> mode.
     */
    List<DProperty> getProperties() throws RepositoryAccessException;

}
