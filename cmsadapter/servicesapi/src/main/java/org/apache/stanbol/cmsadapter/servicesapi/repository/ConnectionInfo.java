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

/**
 * This class contains the information that is required obtain a session from a content repository.
 * 
 * @author suat
 * 
 */
public class ConnectionInfo {
    public static final String JCR_CONNECTION_STRING = "JCR";

    public static final String CMIS_CONNECTION_STRING = "CMIS";

    private String repositoryURL;

    private String workspaceIdentifier;

    private String username;

    private String password;

    private String connectionType;

    public ConnectionInfo(String repositoryURL,
                          String workspaceIdentifier,
                          String username,
                          String password,
                          String connectionType) {
        this.repositoryURL = repositoryURL;
        this.workspaceIdentifier = workspaceIdentifier;
        this.username = username;
        this.password = password;
        this.connectionType = connectionType;
    }

    public String getRepositoryURL() {
        return repositoryURL;
    }

    public String getWorkspaceIdentifier() {
        return workspaceIdentifier;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getConnectionType() {
        return connectionType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connectionType == null) ? 0 : connectionType.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((repositoryURL == null) ? 0 : repositoryURL.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        result = prime * result + ((workspaceIdentifier == null) ? 0 : workspaceIdentifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ConnectionInfo other = (ConnectionInfo) obj;
        if (connectionType == null) {
            if (other.connectionType != null) return false;
        } else if (!connectionType.equals(other.connectionType)) return false;
        if (password == null) {
            if (other.password != null) return false;
        } else if (!password.equals(other.password)) return false;
        if (repositoryURL == null) {
            if (other.repositoryURL != null) return false;
        } else if (!repositoryURL.equals(other.repositoryURL)) return false;
        if (username == null) {
            if (other.username != null) return false;
        } else if (!username.equals(other.username)) return false;
        if (workspaceIdentifier == null) {
            if (other.workspaceIdentifier != null) return false;
        } else if (!workspaceIdentifier.equals(other.workspaceIdentifier)) return false;
        return true;
    }
}
