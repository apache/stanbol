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
package org.apache.stanbol.cmsadapter.servicesapi.repository;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

public interface RepositoryAccess {
    Object getSession(ConnectionInfo connectionInfo) throws RepositoryAccessException;

    /**
     * Opens a new connection using connection info and retrieves the item identified by path.
     * 
     * @param path
     * @param connectionInfo
     * @return
     * @throws RepositoryAccessException
     */
    List<CMSObject> getNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException;

    /**
     * Opens a new connection using connection info and retrieves the item identified by id.
     * 
     * @param id
     * @param connectionInfo
     * @return
     * @throws RepositoryAccessException
     */
    List<CMSObject> getNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException;

    /**
     * Uses an already established Session if, it is not desired to open a new connection.
     * 
     * @param path
     * @param Session
     * @return
     * @throws RepositoryAccessException
     */

    List<CMSObject> getNodeByPath(String path, Object session) throws RepositoryAccessException;

    /**
     * Uses an already established Session if, it is not desired to open a new connection.
     * 
     * @param id
     * @param Session
     * @return
     * @throws RepositoryAccessException
     */
    List<CMSObject> getNodeById(String id, Object session) throws RepositoryAccessException;

    /**
     * Returns all nodes by name. Uses existing connection
     * 
     * @param name
     * @param session
     * @return
     * @throws RepositoryAccessException
     */
    List<CMSObject> getNodeByName(String name, Object session) throws RepositoryAccessException;

    /**
     * Returns all nodes by name, through a newly opened connection.
     * 
     * @param name
     * @param connectionInfo
     * @return
     * @throws RepositoryAccessException
     */
    List<CMSObject> getNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException;

    /**
     * Opens a new connection using connection info and retrieves the item identified by path.
     * 
     * @param path
     * @param connectionInfo
     * @return
     * @throws RepositoryAccessException
     */
    CMSObject getFirstNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException;

    /**
     * Opens a new connection using connection info and retrieves the item identified by id.
     * 
     * @param id
     * @param connectionInfo
     * @return
     * @throws RepositoryAccessException
     */
    CMSObject getFirstNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException;

    /**
     * Uses an already established Session if, it is not desired to open a new connection.
     * 
     * @param path
     * @param Session
     * @return
     * @throws RepositoryAccessException
     */

    CMSObject getFirstNodeByPath(String path, Object session) throws RepositoryAccessException;

    /**
     * Uses an already established Session if, it is not desired to open a new connection.
     * 
     * @param id
     * @param Session
     * @return
     * @throws RepositoryAccessException
     */
    CMSObject getFirstNodeById(String id, Object session) throws RepositoryAccessException;

    /**
     * Returns all nodes by name. Uses existing connection
     * 
     * @param name
     * @param session
     * @return
     * @throws RepositoryAccessException
     */
    CMSObject getFirstNodeByName(String name, Object session) throws RepositoryAccessException;

    /**
     * Returns all nodes by name, through a newly opened connection.
     * 
     * @param name
     * @param connectionInfo
     * @return
     * @throws RepositoryAccessException
     */
    CMSObject getFirstNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException;

    /**
     * Method to retrieve children of a specific node. Uses existing connection.
     * 
     * @param node
     * @param session
     *            An open connection, ready to access repository.
     * @return list of children of the specified node as {@link CMSObject}s
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>node</b> can not be found.
     */
    List<CMSObject> getChildren(CMSObject node, Object session) throws RepositoryAccessException;

    /**
     * Method to retrieve an {@link ObjectTypeDefinition}, given its reference. Uses existing connection.
     * 
     * @param typeRef
     *            Unique identifier of the type in the repository.
     * @param session
     *            An open connection, ready to access repository.
     * @return type definition identified by <b>typeRef</b> as {@link ObjectTypeDefinition}
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>node</b> can not be found.
     */
    ObjectTypeDefinition getObjectTypeDefinition(String typeRef, Object session) throws RepositoryAccessException;

    /**
     * Method to retrieve properties of a CMS object. Uses an existing connection.
     * 
     * @param node
     *            A {@link CMSObject} that represents a CMS object in a repository.
     * @param session
     *            An open connection, ready to access repository.
     * @return list of properties of the CMS object, as {@link Property}
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>node</b> can not be found.
     */
    List<Property> getProperties(CMSObject node, Object session) throws RepositoryAccessException;

    /**
     * Method to retrieve property definitions of a specific type. Uses an existing connection.
     * 
     * @param instance
     * @param session
     *            An open connection, ready to access repository.
     * @return list of property definitions of the type, as {@link PropertyDefinition}
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>instance</b> can not be found.
     */
    List<PropertyDefinition> getPropertyDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException;

    /**
     * Method to retrieve parent type definitions of a specific type definition. Uses an existing connection.
     * 
     * @param instance
     * @param session
     *            An open connection, ready to access repository.
     * @return list of type definitions as {@link ObjectTypeDefinition}
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>instance</b> can not be found.
     */
    List<ObjectTypeDefinition> getParentTypeDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException;

    /**
     * Method to retrieve child type definitions of a specific type definition. Uses an existing connection.
     * 
     * @param instance
     * @param session
     *            An open connection, ready to access repository.
     * @return list of type definitions, as {@link ObjectTypeDefinition}
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>instance</b> can not be found.
     */
    List<ObjectTypeDefinition> getChildObjectTypeDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException;

    /**
     * Method to get the CMS object which has <b>instance</b> as a property. Uses an existing connection.
     * 
     * @param instance
     * @param session
     *            An open connection, ready to access repository.
     * @return CMS object, as a {@link CMSObject}
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>instance</b> can not be found.
     */
    CMSObject getContainerObject(Property instance, Object session) throws RepositoryAccessException;

    /**
     * Method to get property definition of a property that belongs to a CMS object.
     * 
     * @param instance
     * @param session
     *            An open connection, ready to access repository.
     * @return Property definition of the <b>instance</b>, as a {@link PropertyDefinition}
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>instance</b> can not be found.
     * 
     */
    PropertyDefinition getPropertyDefinition(Property instance, Object session) throws RepositoryAccessException;

    /**
     * Method to resolve namespace prefixes used by CMS repository. Uses an existing connection.
     * 
     * @param prefix
     *            Prefix to be resolved.
     * @param session
     *            An open connection, ready to access repository.
     * @return full URI of the namespace
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>prefix</b> can not be resolved.
     */
    String getNamespaceURI(String prefix, Object session) throws RepositoryAccessException;

    /**
     * Method to retrieve parent of a CMS node.
     * 
     * @param instance
     * @param session
     *            An open connection, ready to access repository.
     * @return parent of the node, as a {@link CMSObject}
     * @throws RepositoryAccessException
     *             If repository is not accessible or <b>prefix</b> can not be resolved.
     */
    CMSObject getParentByNode(CMSObject instance, Object session) throws RepositoryAccessException;

    /**
     * Checks the specified session object still can be used to connect content repository
     * 
     * @param session
     *            Session object to be validated
     * @return <code>true</code> if the specified <code>session</code> is still valid, otherwise
     *         <code>false</code>.
     */
    boolean isSessionValid(Object session);

    /**
     * 
     * @param connectionInfo
     * @return true if the {@link RepositoryAccess} instance can connect to repository with the given
     *         <b>connectionInfo</b>
     */
    boolean canRetrieve(ConnectionInfo connectionInfo);

    /**
     * 
     * @param session
     * @return true if the {@link RepositoryAccess} instance can connect to repository with the given
     *         <b>session</b>
     */
    boolean canRetrieve(Object session);
}