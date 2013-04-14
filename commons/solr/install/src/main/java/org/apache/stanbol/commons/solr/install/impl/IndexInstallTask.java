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

import static org.apache.stanbol.commons.solr.install.IndexInstallerConstants.PROPERTY_ARCHIVE_FORMAT;
import static org.apache.stanbol.commons.solr.managed.ManagedIndexConstants.INDEX_ARCHIVES;
import static org.apache.stanbol.commons.solr.managed.ManagedIndexConstants.INDEX_NAME;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.stanbol.commons.solr.managed.ManagedIndexConstants;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.solr.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexInstallTask extends InstallTask {

    private static final Logger log = LoggerFactory.getLogger(IndexInstallTask.class);
    /**
     * use 19 because the config install uses 20 and the files MUST be installed before the config triggering
     * the initialisation of the services.
     */
    private static final String CONFIG_INSTALL_ORDER = "19-";

    /**
     * Mapping for the managed servers. The default server uses the <code>null</code>
     * key!
     */
    private final Map<String,ManagedSolrServer> managedServers;

    public IndexInstallTask(TaskResourceGroup trg, Map<String,ManagedSolrServer> managedServers) {
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
                log.warn("Unable to install Managed Solr Index {} because the {} " +
                        "Server {} is currently not active!", 
                        new Object[]{indexName,serverName == null ? "default" : "",
                                serverName != null ? serverName : ""});
                setFinishedState(ResourceState.IGNORED); //needs still to be installed
            } else {
                //we have an index name and a server to in stall it ... 
                //  ... let's do the work
                String archiveFormat = (String) getResource().getAttribute(PROPERTY_ARCHIVE_FORMAT);
                InputStream is = null;
                try {
                    is = getResource().getInputStream();
                    if ("properties".equals(archiveFormat)) {
                        InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                        Properties props = new Properties();
                        try {
                            props.load(reader);
                        } finally {
                            IOUtils.closeQuietly(reader);
                        }
                        //TODO install to the right server!
                        String indexPath = props.getProperty(INDEX_ARCHIVES);
                        if (indexPath == null) {
                            indexPath = indexName + '.' + ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION;
                            log.info("Property \""
                                     + INDEX_ARCHIVES
                                     + "\" not present within the SolrIndex references file. Will use the default name \""
                                     + indexPath + "\"");
                        }
                        server.updateIndex(indexName, indexPath, props);
                        setFinishedState(ResourceState.INSTALLED);
                    } else {
                        ArchiveInputStream ais = null;
                        try {
                            ais = ConfigUtils.getArchiveInputStream(archiveFormat, is);
                            server.updateIndex(indexName, ais);
                            // we are done ... set the state to installed!
                            setFinishedState(ResourceState.INSTALLED);
                        } finally {
                            IOUtils.closeQuietly(ais);
                        }
                    }
                    // now we can copy the core!
                } catch (Exception e) {
                    String message = String.format(
                        "Unable to install SolrIndexArchive for index name '%s'!" +
                        " (resource=%s, arviceFormat=%s)",
                        indexName, getResource().getURL(), archiveFormat);
                    log.error(message, e);
                    ctx.log("%s! Reason: %s", message, e.getMessage());
                    setFinishedState(ResourceState.IGNORED);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }

    }

    @Override
    public String getSortKey() {
        return CONFIG_INSTALL_ORDER + getResource().getPriority() + "-" + getResource().getEntityId();
    }

}
