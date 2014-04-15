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
package org.apache.stanbol.commons.solr.managed.standalone;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.IndexReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides {@link EmbeddedSolrServer} instances based on the
 * {@link StandaloneManagedSolrServer} implementation. Only intended to be used
 * outside of an OSGI environment. If running within an OSGI Environment
 * this functionality is provided by an {@link SolrServerProvider} implementation
 * that uses {@link CoreContainer}s and {@link SolrCore}s registered as
 * OSGI services.
 * @see StandaloneManagedSolrServer
 * @author Rupert Westenthaler
 *
 */
public final class StandaloneEmbeddedSolrServerProvider {
    
    private static StandaloneEmbeddedSolrServerProvider instance;
    
    public static StandaloneEmbeddedSolrServerProvider getInstance(){
        if(instance == null){
            instance = new StandaloneEmbeddedSolrServerProvider();
        }
        return instance;
    }
    
    private final Logger log = LoggerFactory.getLogger(StandaloneEmbeddedSolrServerProvider.class);
    /**
     * Private constructor used to create the singleton.
     */
    private StandaloneEmbeddedSolrServerProvider() {}
    
    /**
     * Getter for the SolrServer based on the parsed IndexReference
     * @param indexRef the index reference
     * @return the SolrServer or <code>null</code> if the referenced SolrServer
     * is not managed
     */
    public SolrServer getSolrServer(IndexReference indexRef){
        return getSolrServer(indexRef,null);
    }
    /**
     * Getter for the SolrServer based on the parsed IndexReference. If the
     * parsed <code>configName</code> is NOT <code>null</code> than the
     * referenced index is created if it does not yet exist. 
     * @param indexRef the index reference
     * @param configName the SolrCore configuration used to create the SolrCore
     * on the ManagedSOlrServer
     * @return the SolrServer, <code>null</code> if it does not exist and 
     * configName is not present.
     * @throws IllegalStateException if the SolrServer could not be created
     * by using the configName on the ManagedSolrServer referenced by 
     * {@link IndexReference#getServer()}
     */
    public EmbeddedSolrServer getSolrServer(IndexReference indexRef, String configName){
        if(indexRef == null){
            throw new IllegalArgumentException("The parsed InexReference MUST NOT be NULL!");
        }
        StandaloneManagedSolrServer server;
        log.debug("Create EmbeddedSolrServer for Server: {}, Index: {}",
            indexRef.getServer(),indexRef.getIndex());
        if(indexRef.getServer() == null){
            server = StandaloneManagedSolrServer.getManagedServer();
        } else {
            server = StandaloneManagedSolrServer.getManagedServer(indexRef.getServer());
            if(server == null && configName != null){
                server = StandaloneManagedSolrServer.createManagedServer(indexRef.getServer());
            }
        }
        if(server == null){
            if(configName == null){
                log.debug("  > Managed Solr server with name {} not found -> return null",
                    indexRef.getServer());
                return null;
            }
        }
        log.debug("  > use managed Solr server with name {}",server.getServerName());

        String coreName;
        if(indexRef.getIndex() == null){
            coreName = server.getDefaultCore();
        } else if(indexRef.isPath()){
            coreName = server.getCoreForDirectory(indexRef.getIndex());
        } else {
            coreName = indexRef.getIndex();
        } 
        if(coreName != null && !server.isManagedIndex(coreName)){
            if(configName != null){
                try {
                    server.createSolrIndex(coreName, configName, null);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Unable to create SolrCore '"
                        + coreName +"' by using config '"+configName+"' on ManagedSolrServer '"
                        + server.getServerName()+"'!",e);
                }
            } else {
                log.info("Core with the name '"+coreName+"' is not managed on the "
                    + "ManagedSolrServer '"+server.getServerName()+"'. Initialisation "
                    + "might fail if the core was not initialised by some other component.");
            }
        }
        if(coreName != null){
            return new EmbeddedSolrServer(server.getCoreContainer(), coreName);
        } else {
            return null;
        }
    }
}
