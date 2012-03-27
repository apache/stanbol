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
package org.apache.stanbol.cmsadapter.jcr.repository;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.rmi.repository.RMIRemoteRepository;
import org.apache.jackrabbit.rmi.repository.URLRemoteRepository;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO Implement session management. Currently for each operation a new Session is retrieved.
@Component(immediate = true)
@Service
public class JCRRepositoryAccess implements RepositoryAccess {

    private static final Logger log = LoggerFactory.getLogger(JCRRepositoryAccess.class);

    /**
     * Tries to get a {@link Session} first through a {@link RMIRemoteRepository}. If the attempt is
     * unsuccessful it tries {@link URLRemoteRepository}. If the second attempt is also unsuccessful, throws a
     * {@link RepositoryAccessException}, otherwise returns a {@link Session} object.
     * 
     * @param connectionInfo
     * @return {@link Session} if it was able to get one
     * @throws RepositoryAccessException
     */
    @Override
    public Session getSession(ConnectionInfo connectionInfo) throws RepositoryAccessException {

        Session session = null;

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(JCRRepositoryAccess.class.getClassLoader());
            session = getSessionByRMI(connectionInfo);
            if (session == null) {
                session = getSessionByURL(connectionInfo);
            }
            if (session == null) {
                throw new RepositoryAccessException("Failed to get JCR Session");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

        return session;
    }

    private Session getSessionByRMI(ConnectionInfo connectionInfo) {
        Repository repository = new RMIRemoteRepository(connectionInfo.getRepositoryURL());
        Session session = null;
        try {
            String workspaceName = connectionInfo.getWorkspaceName();
            String username = connectionInfo.getUsername();
            String password = connectionInfo.getPassword();

            if (workspaceName == null || workspaceName.equals("") || workspaceName.equals("default")) {

                session = repository.login(new SimpleCredentials(username, password.toCharArray()));
            } else {
                session = repository.login(new SimpleCredentials(username, password.toCharArray()),
                    workspaceName);
            }

        } catch (LoginException e) {
            log.warn("Failed to get JCR session by RMIRemoteRepository");
            log.warn("Error message: " + e.getMessage());
        } catch (RepositoryException e) {
            log.warn("Failed to get JCR session by RMIRemoteRepository");
            log.warn("Error message: " + e.getMessage());
        }
        return session;
    }

    private Session getSessionByURL(ConnectionInfo connectionInfo) {
        Session session = null;
        try {
            Repository repository = new URLRemoteRepository(connectionInfo.getRepositoryURL());
            String workspaceName = connectionInfo.getWorkspaceName();
            String username = connectionInfo.getUsername();
            String password = connectionInfo.getPassword();

            if (workspaceName == null || workspaceName.equals("") || workspaceName.equals("default")) {

                session = repository.login(new SimpleCredentials(username, password.toCharArray()));
            } else {
                session = repository.login(new SimpleCredentials(username, password.toCharArray()),
                    workspaceName);
            }

        } catch (LoginException e) {
            log.debug("Failed to get JCR session by URLRemoteRepository");
            log.debug("Error message: " + e.getMessage());
        } catch (RepositoryException e) {
            log.debug("Failed to get JCR session by URLRemoteRepository");
            log.debug("Error message: " + e.getMessage());
        } catch (MalformedURLException e) {
            log.debug("Failed to get JCR session by URLRemoteRepository");
            log.debug("Error message: " + e.getMessage());
        }
        return session;
    }

    @Override
    public List<CMSObject> getNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        List<JCRQueryRepresentation> queries = QueryHelper.getJCRItemByPathQuery(path);
        return processQuery(connectionInfo, queries, Integer.MAX_VALUE);

    }

    @Override
    public List<CMSObject> getNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByIDQuery(id);
        return processQuery(connectionInfo, Arrays.asList(new JCRQueryRepresentation[] {query}),
            Integer.MAX_VALUE);
    }

    @Override
    public boolean canRetrieve(ConnectionInfo connectionInfo) {
        return connectionInfo.getConnectionType().equals("JCR");
    }

    private QueryResult executeQuery(JCRQueryRepresentation queryRep, ConnectionInfo connectionInfo) throws RepositoryAccessException {

        Session session = getSession(connectionInfo);
        return executeQuery(queryRep, session);
    }

    private QueryResult executeQuery(JCRQueryRepresentation queryRep, Session session) throws RepositoryAccessException {
        try {

            QueryManager qm;
            qm = session.getWorkspace().getQueryManager();
            Query query = qm.createQuery(queryRep.getQueryString(), queryRep.getQueryType());
            QueryResult queryResult = query.execute();
            return queryResult;

        } catch (InvalidQueryException e1) {
            throw new RepositoryAccessException("Invalid Query", e1);
        } catch (RepositoryException e) {
            throw new RepositoryAccessException("Error at query execution", e);
        }
    }

    private List<CMSObject> processQuery(ConnectionInfo connInfo,
                                         List<JCRQueryRepresentation> queryReps,
                                         int max) throws RepositoryAccessException {
        List<CMSObject> results = new ArrayList<CMSObject>();
        for (JCRQueryRepresentation queryRep : queryReps) {
            QueryResult queryResult = executeQuery(queryRep, connInfo);
            processResult(results, queryResult, max);
        }
        return results;

    }

    private List<CMSObject> processQuery(Session session, List<JCRQueryRepresentation> queryReps, int max) throws RepositoryAccessException {
        List<CMSObject> results = new ArrayList<CMSObject>();
        for (JCRQueryRepresentation queryRep : queryReps) {
            QueryResult queryResult = executeQuery(queryRep, session);
            processResult(results, queryResult, max);
        }
        return results;
    }

    private void processResult(List<CMSObject> cmsObjects, QueryResult queryResult, int max) throws RepositoryAccessException {
        try {
            NodeIterator nodes = queryResult.getNodes();
            while (nodes.hasNext() && max > 0) {
                Node current = nodes.nextNode();
                try {
                    cmsObjects.add(JCRModelMapper.getCMSObject(current));
                } catch (RepositoryException e) {
                    log.info("Can not create ObjectType for node {}", current.getPath());
                }
                max--;
            }

        } catch (RepositoryException e) {
            throw new RepositoryAccessException("Error accesing result nodes", e);
        }
    }

    @Override
    public List<CMSObject> getNodeByPath(String path, Object session) throws RepositoryAccessException {
        List<JCRQueryRepresentation> queries = QueryHelper.getJCRItemByPathQuery(path);
        return processQuery((Session) session, queries, Integer.MAX_VALUE);
    }

    @Override
    public List<CMSObject> getNodeById(String id, Object session) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByIDQuery(id);
        return processQuery((Session) session, Arrays.asList(new JCRQueryRepresentation[] {query}),
            Integer.MAX_VALUE);
    }

    @Override
    public boolean canRetrieve(Object session) {
        return session instanceof Session;
    }

    @Override
    public List<CMSObject> getNodeByName(String name, Object session) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByNameQuery(name);
        return processQuery((Session) session, Arrays.asList(new JCRQueryRepresentation[] {query}),
            Integer.MAX_VALUE);
    }

    @Override
    public List<CMSObject> getNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByNameQuery(name);
        return processQuery(connectionInfo, Arrays.asList(new JCRQueryRepresentation[] {query}),
            Integer.MAX_VALUE);
    }

    @Override
    public CMSObject getFirstNodeByPath(String path, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByPathQuery(path).get(0);
        return selectFirst(processQuery(connectionInfo, Arrays.asList(new JCRQueryRepresentation[] {query}),
            1));
    }

    @Override
    public CMSObject getFirstNodeById(String id, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByIDQuery(id);
        return selectFirst(processQuery(connectionInfo, Arrays.asList(new JCRQueryRepresentation[] {query}),
            1));
    }

    @Override
    public CMSObject getFirstNodeByPath(String path, Object session) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByPathQuery(path).get(0);
        return selectFirst(processQuery((Session) session,
            Arrays.asList(new JCRQueryRepresentation[] {query}), 1));
    }

    @Override
    public CMSObject getFirstNodeById(String id, Object session) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByIDQuery(id);
        return selectFirst(processQuery((Session) session,
            Arrays.asList(new JCRQueryRepresentation[] {query}), 1));
    }

    @Override
    public CMSObject getFirstNodeByName(String name, Object session) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByNameQuery(name);
        return selectFirst(processQuery((Session) session,
            Arrays.asList(new JCRQueryRepresentation[] {query}), 1));
    }

    @Override
    public CMSObject getFirstNodeByName(String name, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        JCRQueryRepresentation query = QueryHelper.getJCRItemByNameQuery(name);
        return selectFirst(processQuery(connectionInfo, Arrays.asList(new JCRQueryRepresentation[] {query}),
            1));

    }

    private CMSObject selectFirst(List<CMSObject> nodes) {
        if (nodes.size() != 1) {
            return null;
        } else {
            return nodes.get(0);
        }
    }

    @Override
    public List<CMSObject> getChildren(CMSObject node, Object session) throws RepositoryAccessException {
        try {
            Node jcrNode = ((Session) session).getNodeByIdentifier(node.getUniqueRef());
            JCRModelMapper.fillCMSObjectChildren(node, jcrNode);
            return node.getChildren();
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }
    }

    @Override
    public ObjectTypeDefinition getObjectTypeDefinition(String typeRef, Object session) throws RepositoryAccessException {
        try {
            NodeType nodeType = ((Session) session).getWorkspace().getNodeTypeManager().getNodeType(typeRef);
            return JCRModelMapper.getObjectTypeDefinition(nodeType);
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }

    }

    @Override
    public List<Property> getProperties(CMSObject node, Object session) throws RepositoryAccessException {
        try {
            Node jcrNode = ((Session) session).getNodeByIdentifier(node.getUniqueRef());
            JCRModelMapper.fillProperties(node, jcrNode);
            return node.getProperty();
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }

    }

    @Override
    public List<PropertyDefinition> getPropertyDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException {
        try {
            NodeType nodeType = ((Session) session).getWorkspace().getNodeTypeManager()
                    .getNodeType(instance.getUniqueRef());
            JCRModelMapper.fillPropertyDefinitions(instance, nodeType);
            return instance.getPropertyDefinition();
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }
    }

    @Override
    public List<ObjectTypeDefinition> getParentTypeDefinitions(ObjectTypeDefinition instance, Object session) throws RepositoryAccessException {
        try {
            List<ObjectTypeDefinition> parentsResolved = new ArrayList<ObjectTypeDefinition>(instance
                    .getParentRef().size());
            for (String parentRef : instance.getParentRef()) {
                NodeType nodeType = ((Session) session).getWorkspace().getNodeTypeManager()
                        .getNodeType(parentRef);
                parentsResolved.add(JCRModelMapper.getObjectTypeDefinition(nodeType));
            }

            return parentsResolved;
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }
    }

    @Override
    public List<ObjectTypeDefinition> getChildObjectTypeDefinitions(ObjectTypeDefinition instance,
                                                                    Object session) throws RepositoryAccessException {
        try {
            NodeType nodeType = ((Session) session).getWorkspace().getNodeTypeManager()
                    .getNodeType(instance.getUniqueRef());

            JCRModelMapper.fillChildObjectDefinitions(instance, nodeType);
            return instance.getObjectTypeDefinition();
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }

    }

    @Override
    public CMSObject getContainerObject(Property instance, Object session) throws RepositoryAccessException {
        try {
            Node jcrNode = ((Session) session).getNodeByIdentifier(instance.getContainerObjectRef());
            return JCRModelMapper.getCMSObject(jcrNode);
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }
    }

    @Override
    public PropertyDefinition getPropertyDefinition(Property instance, Object session) throws RepositoryAccessException {
        try {
            NodeType sourceObjectType = ((Session) session).getWorkspace().getNodeTypeManager()
                    .getNodeType(instance.getSourceObjectTypeRef());
            JCRModelMapper.fillPropertyDefinition(instance, sourceObjectType);

            return instance.getPropertyDefinition();
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }
    }

    @Override
    public String getNamespaceURI(String prefix, Object session) throws RepositoryAccessException {
        try {
            return ((Session) session).getNamespaceURI(prefix);
        } catch (NamespaceException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RepositoryAccessException(e.getMessage(), e);
        }
    }

    @Override
    public CMSObject getParentByNode(CMSObject instance, Object session) throws RepositoryAccessException {
        Node parent;
        try {
            if (instance.getParentRef() == null) {
                Node jcrNode = ((Session) session).getNodeByIdentifier(instance.getUniqueRef());
                try {
                    parent = jcrNode.getParent();
                    instance.setParentRef(parent.getIdentifier());
                } catch (Exception e) {
                    // No parent
                    parent = null;
                }
            } else {
                parent = ((Session) session).getNodeByIdentifier(instance.getParentRef());
            }
            return JCRModelMapper.getCMSObject(parent);
        } catch (RepositoryException e) {
            throw new RepositoryAccessException("Error at accessing parent", e);
        }
    }

    @Override
    public boolean isSessionValid(Object session) {
        return ((Session) session).isLive();
    }
}