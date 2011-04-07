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
package org.apache.stanbol.entityhub.yard.solr.impl.install;

import static org.apache.stanbol.entityhub.yard.solr.impl.install.IndexInstallerConstants.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.stanbol.entityhub.yard.solr.SolrDirectoryManager;
import org.apache.stanbol.entityhub.yard.solr.impl.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexInstallTask extends InstallTask {
    
    private static final Logger log = LoggerFactory.getLogger(IndexInstallTask.class);
    /**
     * use 19 because the config install uses 20 and the files MUST be installed
     * before the config triggering the initialisation of the services. 
     */
    private static final String CONFIG_INSTALL_ORDER = "19-";

    private final SolrDirectoryManager solrDirectoryManager;
    public IndexInstallTask(TaskResourceGroup trg, SolrDirectoryManager solrDirectoryManager){
        super(trg);
        this.solrDirectoryManager = solrDirectoryManager;
    }
    @Override
    public void execute(InstallationContext ctx) {
        String indexName = (String)getResource().getAttribute(PROPERTY_INDEX_NAME);
        Map<String,File> existingIndexes = solrDirectoryManager.getManagedIndices();
        if(existingIndexes.containsKey(indexName)){
            //an Index with that name already exists -> ignore
            ctx.log("Unable to install the Index with the name \"%s\" becuase an index with that name is already managed by the the SolrYard " +
            		"(resource %s | location of the existing index %s)!",
                indexName,getResource().getURL(),existingIndexes.get(indexName));
            setFinishedState(ResourceState.IGNORED);
        } else { //this index does not exist
            String archiveFormat = (String)getResource().getAttribute(PROPERTY_ARCHIVE_FORMAT);
            InputStream is = null;
            try {
                is = getResource().getInputStream();
                if("properties".equals(archiveFormat)){
                    Map<String,String> properties = new HashMap<String,String>();
                    InputStreamReader reader = new InputStreamReader(is,"UTF-8");
                    try {
                        Properties props = new Properties();
                        props.load(reader);
                        for(Entry<Object,Object> config : props.entrySet()){
                            properties.put(config.getKey().toString(),config.getValue()!= null?config.getValue().toString():null);
                        }
                    } finally {
                       IOUtils.closeQuietly(reader);
                    }
                    String indexPath = properties.get(PROPERTY_INDEX_ARCHIVE);
                    if(indexPath == null){
                        indexPath = indexName+'.'+IndexInstallerConstants.SOLR_INDEX_ARCHIVE_EXTENSION;
                        log.info("Property \""+PROPERTY_INDEX_ARCHIVE+"\" not present within the SolrIndex references file. Will use the default name \""+indexPath+"\"");
                    }
                    solrDirectoryManager.createSolrDirectory(indexName,indexPath,properties);
                    setFinishedState(ResourceState.INSTALLED);
                } else {
                    ArchiveInputStream ais = null;
                    try {
                        ais = ConfigUtils.getArchiveInputStream(archiveFormat, is);
                        solrDirectoryManager.createSolrIndex(indexName, ais);
                        //we are done ... set the state to installed!
                        setFinishedState(ResourceState.INSTALLED);
                    } finally {
                        IOUtils.closeQuietly(ais);
                    }
                }
                //now we can copy the core!
            }catch (Exception e) {
                String message = String.format("Unable to install SolrIndexArchive for index name \"%s\"! (resource=%s, arviceFormat=%s)", 
                    indexName,getResource().getURL(),archiveFormat);
                log.error(message,e);
                ctx.log("%s! Reason: %s",message,e.getMessage());
                setFinishedState(ResourceState.IGNORED);
            } finally {
                IOUtils.closeQuietly(is);
            }
            
        }
        
    }

    @Override
    public String getSortKey() {
        return CONFIG_INSTALL_ORDER+getResource().getPriority()+"-"+getResource().getEntityId();
    }

}
