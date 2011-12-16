/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.cmsadapter.core.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides session management of content repositories in the scope of CMS Adapter. Sessions are
 * cached such that they can be fetched later using a corresponding to session key. Same session key (so the
 * same session) can be used in multiple operations.
 * 
 * @author suat
 * 
 */
@Component
@Service(value = SessionManager.class)
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    @Reference
    RepositoryAccessManager repositoryAccessManager;

    private Map<String,SessionContext> sessions = new HashMap<String,SessionContext>();

    /**
     * Creates a unique identifier (session key) for the cached session.
     * 
     * @param session
     *            Session object to be cached
     * @param type
     *            The protocol type of the cached session e.g <b>JCR</b>, <b>CMIS</b>.
     * @return a {@link UUID} generated unique identifier.
     */
    public String createSessionKey(Object session, String type) {
        ConnectionInfo cInfo = new ConnectionInfo();
        cInfo.setConnectionType(type);
        return cacheSessionContext(new SessionContext(session, cInfo));
    }

    /**
     * Creates a unique identifier (session key) for the cached session. First the session is obtained using
     * the connection parameters.
     * 
     * @param repositoryURL
     *            URL of the content repository. For JCR repositories <b>RMI protocol</b>, for CMIS
     *            repositories <b>AtomPub Binding</b> is used. This parameter should be set according to these
     *            connection methods.
     * @param workspaceIdentifier
     *            For JCR repositories this parameter determines the workspace to be connected. On the other
     *            hand for CMIS repositories <b>repository ID</b> should be set to this parameter. In case of
     *            not setting this parameter, for JCR <b>default workspace</b> is selected, for CMIS the
     *            <b>first repository</b> obtained through the session object is selected.
     * @param username
     *            Username to connect to content repository
     * @param password
     *            Password to connect to content repository
     * @param connectionType
     *            Connection type; either <b>JCR</b> or <b>CMIS</b>
     * @return a {@link UUID} generated unique identifier.
     * @throws RepositoryAccessException
     */
    public String createSessionKey(String repositoryURL,
                                   String workspaceIdentifier,
                                   String username,
                                   String password,
                                   String connectionType) throws RepositoryAccessException {

        ConnectionInfo connectionInfo = formConnectionInfo(repositoryURL, workspaceIdentifier, username,
            password, connectionType);
        RepositoryAccess repositoryAccess = repositoryAccessManager.getRepositoryAccessor(connectionInfo);

        if (repositoryAccess == null) {
            log.warn(String
                    .format(
                        "There is no suitable RepositoryAccess instance in the environment. \n Repository URL: %s\n Workspace identifier: %s\n Username: %s\n Password: %s\n Connection type: %s",
                        repositoryURL, workspaceIdentifier, username, password, connectionType));
            throw new RepositoryAccessException(
                    "There is no suitable RepositoryAccess instance in the environment. Please refer to logs");
        }
        Object session = repositoryAccess.getSession(connectionInfo);
        return cacheSessionContext(new SessionContext(session, connectionInfo));
    }

    /**
     * Returns the cached session based on the given <code>session key</code>. If the session is not valid, it
     * tries to get another session using the previous connection parameters.
     * 
     * @param sessionKey
     *            session identifier through which the session itself will be obtained
     * @return session object
     * @throws RepositoryAccessException
     */
    public Object getSession(String sessionKey) throws RepositoryAccessException {
        SessionContext sessionContext = sessions.get(sessionKey);
        if (sessionContext != null) {
            Object session = sessionContext.getSession();
            RepositoryAccess repositoryAccess = repositoryAccessManager.getRepositoryAccess(session);
            if (repositoryAccess != null) {
                boolean validSession = repositoryAccess.isSessionValid(session);
                if (validSession) {
                    return session;
                } else {
                    ConnectionInfo connectionInfo = sessionContext.getConnectionInfo();
                    session = repositoryAccess.getSession(connectionInfo);
                    log.info(String
                            .format(
                                "Session was invalid for the connection info. \n Repository URL: %s\n Workspace identifier: %s\n Username: %s\n Password: %s\n Connection type: %s",
                                connectionInfo.getRepositoryURL(), connectionInfo.getWorkspaceName(),
                                connectionInfo.getUsername(), connectionInfo.getPassword(),
                                connectionInfo.getConnectionType()));
                    log.info("New session is created for the provided key: {}", sessionKey);
                    sessions.put(sessionKey, new SessionContext(session, connectionInfo));
                    return session;
                }
            } else {
                ConnectionInfo connectionInfo = sessionContext.getConnectionInfo();
                log.warn(String
                        .format(
                            "There is no suitable RepositoryAccess instance in the environment. \n Repository URL: %s\n Workspace identifier: %s\n Username: %s\n Password: %s\n Connection type: %s",
                            connectionInfo.getRepositoryURL(), connectionInfo.getWorkspaceName(),
                            connectionInfo.getUsername(), connectionInfo.getPassword(),
                            connectionInfo.getConnectionType()));
                throw new RepositoryAccessException(
                        "There is no suitable RepositoryAccess instance in the environment. Please refer to logs");
            }
        } else {
            log.warn("There is no cached session for the key: {}", sessionKey);
            throw new RepositoryAccessException(String.format("There is no cached session for the key: %s",
                sessionKey));
        }
    }

    /**
     * Returns the protocol type e.g <b>JCR</b>, <b>CMIS</b> based on the given session key.
     */
    public String getConnectionTypeBySessionKey(String sessionKey) throws RepositoryAccessException {
        SessionContext sessionContext = sessions.get(sessionKey);
        if (sessionContext != null) {
            return sessionContext.getConnectionInfo().getConnectionType();
        } else {
            log.warn("There is no cached session for the key: {}", sessionKey);
            throw new RepositoryAccessException(String.format("There is no cached session for the key: %s",
                sessionKey));
        }
    }

    private String cacheSessionContext(SessionContext sessionContext) {
        String newUUID = UUID.randomUUID().toString();
        sessions.put(newUUID, sessionContext);
        return newUUID;
    }

    private ConnectionInfo formConnectionInfo(String repositoryURL,
                                              String workspaceIdentifier,
                                              String username,
                                              String password,
                                              String connectionType) {
        org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo cInfo = new org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo();
        cInfo.setRepositoryURL(repositoryURL);
        cInfo.setWorkspaceName(workspaceIdentifier);
        cInfo.setUsername(username);
        cInfo.setPassword(password);
        cInfo.setConnectionType(connectionType);
        return cInfo;
    }

    private class SessionContext {
        private Object session;
        private ConnectionInfo connectionInfo;

        public SessionContext(Object session, ConnectionInfo connectionInfo) {
            this.session = session;
            this.connectionInfo = connectionInfo;
        }

        public Object getSession() {
            return this.session;
        }

        public ConnectionInfo getConnectionInfo() {
            return this.connectionInfo;
        }
    }
}
