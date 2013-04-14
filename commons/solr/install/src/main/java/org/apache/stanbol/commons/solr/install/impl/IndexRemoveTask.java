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
package org.apache.stanbol.commons.solr.install.impl;

import static org.apache.stanbol.commons.solr.managed.ManagedIndexConstants.INDEX_NAME;

import java.util.Map;

import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.stanbol.commons.solr.managed.ManagedIndexConstants;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes the SolrCore from the {@link ManagedSolrServer} by calling
 * {@link ManagedSolrServer#removeIndex(String, boolean)}.
 * 
 * @author Rupert Westenthaler
 * 
 */
public class IndexRemoveTask extends InstallTask {
    
    private static final Logger log = LoggerFactory.getLogger(IndexRemoveTask.class);
    /**
     * Use 11 because the RemoveConfiguration uses 10 and we need to ensure that the files are removed after
     * the services are shut down.
     */
    private static final String CONFIG_INSTALL_ORDER = "11-";
    
    /**
     * Mapping for the managed servers. The default server uses the <code>null</code>
     * key!
     */
    private final Map<String,ManagedSolrServer> managedServers;

    public IndexRemoveTask(TaskResourceGroup trg, Map<String,ManagedSolrServer> managedServers) {
        super(trg);
        if(managedServers == null){
            throw new IllegalArgumentException("The parsed map with the ManagedSolrServer MUST NOT be NULL!");
        }
        this.managedServers = managedServers;
    }

    @Override
    public void execute(InstallationContext ctx) {
        String indexName = (String) getResource().getAttribute(INDEX_NAME);
        if(indexName == null){
            log.error("Unable to remove Managed Index because the required Property '{}'" +
            		"used to define the name of the Index is missing",INDEX_NAME);
            setFinishedState(ResourceState.IGNORED);
        } else {
            String serverName = (String) getResource().getAttribute(ManagedIndexConstants.SERVER_NAME);
            ManagedSolrServer server = managedServers.get(serverName);
            if(server == null){
                log.warn("Unable to remove Managed Solr Index {} because the {} " +
                		"Server {} is currently not active!", 
                		new Object[]{indexName,serverName == null ? "default" : "",
                		        serverName != null ? serverName : ""});
                setFinishedState(ResourceState.IGNORED);
            } else {
                server.removeIndex(indexName, true);
                setFinishedState(ResourceState.UNINSTALLED);
            }
        }
    }

    @Override
    public String getSortKey() {
        return CONFIG_INSTALL_ORDER + getResource().getEntityId();
    }

}
