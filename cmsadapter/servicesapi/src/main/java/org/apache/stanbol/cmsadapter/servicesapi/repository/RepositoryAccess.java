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

    List<CMSObject> getChildren(CMSObject node, Object session) throws RepositoryAccessException;

    ObjectTypeDefinition getObjectTypeDefinition(String typeRef, Object session) throws RepositoryAccessException;

    List<Property> getProperties(CMSObject node, Object session) throws RepositoryAccessException;

    List<PropertyDefinition> getPropertyDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException;

    List<ObjectTypeDefinition> getParentTypeDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException;

    List<ObjectTypeDefinition> getChildObjectTypeDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException;

    CMSObject getContainerObject(Property instance, Object session) throws RepositoryAccessException;

    PropertyDefinition getPropertyDefinition(Property instance, Object session) throws RepositoryAccessException;

    String getNamespaceURI(String prefix, Object session) throws RepositoryAccessException;

    boolean canRetrieve(ConnectionInfo connectionInfo);

    boolean canRetrieve(Object session);

    CMSObject getParentByNode(CMSObject instance, Object session) throws RepositoryAccessException;

}
