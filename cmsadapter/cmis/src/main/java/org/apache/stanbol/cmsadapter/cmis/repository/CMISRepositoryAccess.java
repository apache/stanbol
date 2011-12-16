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
package org.apache.stanbol.cmsadapter.cmis.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Repository;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Component(immediate = true)
public class CMISRepositoryAccess implements RepositoryAccess {
    private static final Logger logger = LoggerFactory.getLogger(CMISRepositoryAccess.class);

    @Reference
    protected SessionFactory sessionFactory;

    @Override
    public Object getSession(ConnectionInfo connectionInfo) throws RepositoryAccessException {
        // Session parameters are set
        Session session = null;
        Map<String,String> parameters = new HashMap<String,String>();
        parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        parameters.put(SessionParameter.ATOMPUB_URL, connectionInfo.getRepositoryURL());
        parameters.put(SessionParameter.USER, connectionInfo.getUsername());
        parameters.put(SessionParameter.PASSWORD, connectionInfo.getPassword());

        List<Repository> repositories = sessionFactory.getRepositories(parameters);

        String workspace = connectionInfo.getWorkspaceName();
        if (workspace == null || workspace.isEmpty()) {
            parameters.put(SessionParameter.REPOSITORY_ID, repositories.get(0).getId());
        } else {
            parameters.put(SessionParameter.REPOSITORY_ID, workspace);
        }

        try {
            session = sessionFactory.createSession(parameters);
            logger.info("Connected to repository: {}", connectionInfo.getRepositoryURL());
        } catch (Exception e) {
            throw new RepositoryAccessException("Failed to connect to repository: "
                                                + connectionInfo.getRepositoryURL(), e);
        }
        return session;
    }

    private List<CmisObject> listCmisItemsByQuery(String query, Session session) {
        return listCmisItemsByQuery(query, session, Integer.MAX_VALUE);
    }

    private List<CmisObject> listCmisItemsByQuery(String query, Session session, int max) {
        // FIXME does this code handles pagination?
        List<CmisObject> resultObjects = new ArrayList<CmisObject>();
        Iterator<QueryResult> results = session.query(query, false).iterator();
        int index = 0;
        while (results.hasNext() && index < max) {
            QueryResult result = results.next();
            String id = result.getPropertyValueById(CMISProperty.ID.getName());
            resultObjects.add(session.getObject(CMISObjectId.getObjectId(id)));
            index++;
        }
        return resultObjects;
    }

    private List<CMSObject> convertResult(Iterator<CmisObject> cmisObjects) {
        // FIXME we should not accumulate iterator on a list while we can directly use it
        List<CmisObject> results = new ArrayList<CmisObject>();
        while (cmisObjects.hasNext()) {
            results.add(cmisObjects.next());
        }
        return CMISModelMapper.convertCMISObjects(results);
    }

    private List<CMSObject> executeAndConvertQuery(String query, Session session) {
        return CMISModelMapper.convertCMISObjects(listCmisItemsByQuery(query, session));
    }

    private List<CMSObject> executeAndConvertQuery(String query, Session session, int max) {
        return CMISModelMapper.convertCMISObjects(listCmisItemsByQuery(query, session, max));
    }

    @Override
    public List<CMSObject> getNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        try {
            Session session = (Session) getSession(connectionInfo);
            return getNodeByPath(path, session);
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public List<CMSObject> getNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        try {
            Session session = (Session) getSession(connectionInfo);
            return getNodeById(id, session);
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public List<CMSObject> getNodeByPath(String path, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            List<CmisObject> queryResults = new ArrayList<CmisObject>();
            boolean recursive = false;
            if (path.endsWith("%")) {
                recursive = true;
                path = path.substring(0, path.length() - 1);
            }
            CmisObject node = cmisSession.getObjectByPath(path);
            queryResults.add(node);
            if (recursive) {
                accumulateChildren(queryResults, node);
            }
            return CMISModelMapper.convertCMISObjects(queryResults);

        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    private List<CmisObject> accumulateChildren(List<CmisObject> childrenList, CmisObject node) {
        // FIXME May need to breadth rather than depth first
        childrenList.add(node);
        if (node instanceof Folder) {
            Iterator<CmisObject> childs = ((Folder) node).getChildren().iterator();
            while (childs.hasNext()) {
                CmisObject child = childs.next();
                accumulateChildren(childrenList, child);
            }
        }
        return childrenList;
    }

    @Override
    public List<CMSObject> getNodeById(String id, Object session) throws RepositoryAccessException {
        try {
            checkSession(session);
            return Arrays.asList(new CMSObject[] {CMISModelMapper.getCMSObject(((Session) session)
                    .getObject(CMISObjectId.getObjectId(id)))});
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public List<CMSObject> getNodeByName(String name, Object session) throws RepositoryAccessException {
        try {
            checkSession(session);
            String[] query = CMISQueryHelper.getCMISIdByNameQuery(name);
            List<CMSObject> results = executeAndConvertQuery(query[0], (Session) session);
            results.addAll(executeAndConvertQuery(query[1], (Session) session));
            return results;
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public List<CMSObject> getNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        try {
            Session session = (Session) getSession(connectionInfo);
            return getNodeByName(name, session);
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public CMSObject getFirstNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        try {
            Session session = (Session) getSession(connectionInfo);
            return getFirstNodeByPath(path, session);
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public CMSObject getFirstNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        try {
            Session session = (Session) getSession(connectionInfo);
            return getFirstNodeById(id, session);
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public CMSObject getFirstNodeByPath(String path, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            if (path.endsWith("%")) {
                path = path.substring(0, path.length() - 1);
            }
            CmisObject node = cmisSession.getObjectByPath(path);
            return CMISModelMapper.getCMSObject(node);

        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public CMSObject getFirstNodeById(String id, Object session) throws RepositoryAccessException {
        try {
            return CMISModelMapper.getCMSObject(((Session) session).getObject(CMISObjectId.getObjectId(id)));
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public CMSObject getFirstNodeByName(String name, Object session) throws RepositoryAccessException {
        try {
            checkSession(session);
            String[] query = CMISQueryHelper.getCMISIdByNameQuery(name);
            List<CMSObject> results = executeAndConvertQuery(query[0], (Session) session, 1);
            results.addAll(executeAndConvertQuery(query[1], (Session) session, 1));
            return results.get(0);

        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public CMSObject getFirstNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        try {
            Session session = (Session) getSession(connectionInfo);
            return getFirstNodeByName(name, session);
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public List<CMSObject> getChildren(CMSObject node, Object session) throws RepositoryAccessException {
        try {
            checkSession(session);
            CmisObject cmisObject = getByCMISObject(node, (Session) session);
            if (cmisObject instanceof Folder) {
                Folder cmisFolder = (Folder) cmisObject;
                // FIXME Is this code handles pagination
                Iterator<CmisObject> childIter = cmisFolder.getChildren().iterator();
                return convertResult(childIter);
            } else {
                return Arrays.asList(new CMSObject[] {});
            }
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public ObjectTypeDefinition getObjectTypeDefinition(String typeRef, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            return CMISModelMapper.getObjectTypeDefinition(cmisSession.getTypeDefinition(typeRef));
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public List<Property> getProperties(CMSObject node, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            CmisObject cmisObject = cmisSession.getObject(CMISObjectId.getObjectId(node.getUniqueRef()));
            CMISModelMapper.fillProperties(cmisObject, node);
            return node.getProperty();
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public List<PropertyDefinition> getPropertyDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            ObjectType objectType = cmisSession.getTypeDefinition(instance.getUniqueRef());
            CMISModelMapper.fillPropertyDefinitions(instance, objectType);
            return instance.getPropertyDefinition();
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public List<ObjectTypeDefinition> getParentTypeDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            ObjectType type = cmisSession.getTypeDefinition(instance.getUniqueRef());
            List<ObjectTypeDefinition> typeDefinitions = new ArrayList<ObjectTypeDefinition>();
            ObjectType parentTypeDef = type.getParentType();
            while (parentTypeDef != null) {
                typeDefinitions.add(CMISModelMapper.getObjectTypeDefinition(parentTypeDef));
                parentTypeDef = parentTypeDef.getParentType();
            }

            return typeDefinitions;
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public CMSObject getContainerObject(Property instance, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            CmisObject cmisObject = cmisSession.getObject(CMISObjectId.getObjectId(instance
                    .getContainerObjectRef()));
            return CMISModelMapper.getCMSObject(cmisObject);
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public PropertyDefinition getPropertyDefinition(Property instance, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            Map<String,org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition<?>> definitions = cmisSession
                    .getTypeDefinition(instance.getSourceObjectTypeRef()).getPropertyDefinitions();
            for (org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition<?> definition : definitions
                    .values()) {
                if (definition.getLocalName().equals(instance.getLocalname())) {
                    // TODO Property creation and then resolving propertydefinition from property should be
                    // tested.
                    // Problems may occur when comparing Properties localname vs definitions localname/id
                    return CMISModelMapper.getPropertyDefinition(definition);
                }
            }
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
        return null;
    }

    @Override
    public String getNamespaceURI(String prefix, Object session) throws RepositoryAccessException {
        // TODO Fix here
        return prefix;
    }

    @Override
    public boolean canRetrieve(ConnectionInfo connectionInfo) {
        try {
            getSession(connectionInfo);
            return true;
        } catch (Exception e) {
            logger.warn("Error at trying to get session from connection info", e);
            return false;
        }
    }

    @Override
    public boolean canRetrieve(Object session) {
        return session instanceof Session;

    }

    @Override
    public CMSObject getParentByNode(CMSObject instance, Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            CmisObject cmisObject = cmisSession.getObject(CMISObjectId.getObjectId(instance.getUniqueRef()));
            if (instance.getParentRef() != null) {
                return CMISModelMapper.getCMSObject(cmisSession.getObject(CMISObjectId.getObjectId(instance
                        .getParentRef())));
            }
            if (cmisObject instanceof Folder) {
                Folder folder = (Folder) cmisObject;
                for (Folder parent : folder.getParents()) {
                    // TODO handle multiple parents
                    return CMISModelMapper.getCMSObject(parent);
                }
            }
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
        return null;
    }

    private CmisObject getByCMISObject(CMSObject cmsObject, Session session) throws RepositoryAccessException {
        return session.getObject(CMISObjectId.getObjectId(cmsObject.getUniqueRef()));
    }

    private Session checkSession(Object session) {
        if (!(session instanceof Session)) {
            throw new IllegalArgumentException("Expected " + Session.class.getName() + "found "
                                               + session.getClass().getName());
        } else {
            return (Session) session;
        }
    }

    @Override
    public List<ObjectTypeDefinition> getChildObjectTypeDefinitions(ObjectTypeDefinition instance,
                                                                    Object session) throws RepositoryAccessException {
        try {
            Session cmisSession = checkSession(session);
            CmisObject node = cmisSession.getObject(CMISObjectId.getObjectId(instance.getUniqueRef()));
            if (node instanceof ObjectType) {
                ObjectType type = (ObjectType) node;
                CMISModelMapper.fillChildObjectTypeDefinitions(instance, type);
                return instance.getObjectTypeDefinition();
            } else {
                throw new RepositoryAccessException("No object type with id " + instance.getUniqueRef());
            }
        } catch (CmisBaseException e) {
            throw new RepositoryAccessException("Error at accessing repository", e);
        }
    }

    @Override
    public boolean isSessionValid(Object session) {
        Session cmisSession = (Session) session;
        try {
            cmisSession.clear();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}