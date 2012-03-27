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
package org.apache.stanbol.contenthub.store.solr.manager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class SolrCoreManager {

    private static final Logger log = LoggerFactory.getLogger(SolrCoreManager.class);
    private static SolrCoreManager instance;

    public static final String CONTENTHUB_DEFAULT_INDEX_NAME = "contenthub";

    private static final int SECONDS_TO_WAITFOR_CORE_TO_BEREADY = 2;

    /**
     * Map to cache solr servers that is obtained through OSGi environment. In case of a LD program name
     * specified, for not obtaining the server from OSGi again and again, we cache them in this map.
     */
    private Map<String,SolrServer> cache;

    private SolrCoreManager() {
        cache = new HashMap<String,SolrServer>();
    }

    public static SolrCoreManager getInstance(BundleContext bundleContext, ManagedSolrServer managedSolrServer) {
        if (bundleContext == null) {
            throw new IllegalArgumentException("SolrCoreManager cannot be used without a BundleContext");
        }
        if (managedSolrServer == null) {
            throw new IllegalArgumentException("SolrCoreManager cannot be used without a ManagedSolrServer");
        }
        if (instance == null) {
            instance = new SolrCoreManager();
        }
        instance.setBundleContext(bundleContext);
        instance.setManagedSolrServer(managedSolrServer);
        return instance;
    }

    private BundleContext bundleContext;
    private ManagedSolrServer managedSolrServer;

    private void setManagedSolrServer(ManagedSolrServer managedSolrServer) {
        this.managedSolrServer = managedSolrServer;
    }

    private void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void createDefaultSolrServer() throws StoreException {
        if (!cache.containsKey(CONTENTHUB_DEFAULT_INDEX_NAME)) {
            if (!managedSolrServer.isManagedIndex(CONTENTHUB_DEFAULT_INDEX_NAME)) {
                try {
                    managedSolrServer.createSolrIndex(CONTENTHUB_DEFAULT_INDEX_NAME,
                        CONTENTHUB_DEFAULT_INDEX_NAME, null);
                } catch (IOException e) {
                    String msg = "Error while creating default solr index";
                    log.error(msg, e);
                    throw new StoreException(msg, e);
                }
            }
        }
    }

    public void createSolrCore(String coreName, ArchiveInputStream coreArchive) throws StoreException {
        if (managedSolrServer.isManagedIndex(coreName)) {
            String msg = String.format("Solr index already exists with name: %s", coreName);
            log.error(msg);
            throw new StoreException(msg);
        }

        try {
            managedSolrServer.createSolrIndex(coreName, coreArchive);
        } catch (IOException e) {
            log.error("", e);
            throw new StoreException(e);
        } catch (SAXException e) {
            String msg = "ManagedSolrServer cannot parse the related XML files.";
            log.error(msg, e);
            throw new StoreException(msg, e);
        }
    }

    public void deleteSolrCore(String coreName) {
        if (managedSolrServer != null) {
            // Remove all related files of the solr core
            managedSolrServer.removeIndex(coreName, true);
        }
    }

    private SolrServer getSolrServerFromTracker(String coreName) throws StoreException {
        SolrServer solrServer = null;
        for (int i = 0; solrServer == null && i <= SECONDS_TO_WAITFOR_CORE_TO_BEREADY; i++) {
            RegisteredSolrServerTracker tracker = null;
            try {
                tracker = new RegisteredSolrServerTracker(bundleContext, new IndexReference(
                        managedSolrServer.getServerName(), coreName));
            } catch (InvalidSyntaxException e) {
                String message = e.getMessage();
                if (message == null || message.isEmpty()) {
                    message = "Failed to create a RegisteredSolrServerTracker";
                }
                throw new StoreException(message, e);
            }
            tracker.open();
            solrServer = tracker.getService();
            tracker.close();
            if (solrServer != null || i == SECONDS_TO_WAITFOR_CORE_TO_BEREADY) break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        if (solrServer == null) {
            log.error("SolrServer specified by '{}' cannot be retrieved from RegisteredSolrServerTracker",
                coreName);
            throw new StoreException(
                    String.format(
                        "SolrServer specified by '%s' cannot be retrieved from RegisteredSolrServerTracker",
                        coreName));
        }
        return solrServer;
    }

    public SolrServer getServer() throws StoreException {
        return getServer(CONTENTHUB_DEFAULT_INDEX_NAME);
    }

    public SolrServer getServer(String coreName) throws StoreException {
        SolrServer solrServer = null;
        if (coreName == null || coreName.trim().isEmpty()) {
            coreName = CONTENTHUB_DEFAULT_INDEX_NAME;
        }
        if (cache.containsKey(coreName)) {
            // check cache for the server reference
            solrServer = cache.get(coreName);
        } else if (managedSolrServer != null) {
            solrServer = getSolrServerFromTracker(coreName);
            cache.put(coreName, solrServer);
        }
        return solrServer;
    }

    public boolean isManagedSolrCore(String coreName) {
        if (managedSolrServer.isManagedIndex(coreName)) {
            return true;
        }
        return false;
    }

}
