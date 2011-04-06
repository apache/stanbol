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
import java.util.Map;
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

public class IndexInstallTask extends InstallTask {
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
            ArchiveInputStream ais = null;
            try {
                is = getResource().getInputStream();
                ais = null;
                if("zip".equals(archiveFormat)){
                    ais = new ZipArchiveInputStream(is);
                } else {
                    if ("gz".equals(archiveFormat)) {
                            is = new GZIPInputStream(is);
                    } else if ("bz2".equals(archiveFormat)) {
                            is = new BZip2CompressorInputStream(is);
                    } else {
                        throw new IllegalStateException("Unsupported compression format "+archiveFormat+" " +
                        		"(implementation out of sync with Constants defined in "+IndexInstallerConstants.class.getName()+"). " +
                        				"Please report this to stanbol-dev mailing list!");
                    }
                    ais = new TarArchiveInputStream(is);
                }
                //now we can copy the core!
                solrDirectoryManager.createSolrDirectory(indexName, ais);
                //we are done ... set the state to installed!
                setFinishedState(ResourceState.INSTALLED);
            }catch (IOException e) {
                ctx.log("Unable to open SolrIndexArchive for index name \"%s\"! (resource=%s, arviceFormat=%s)", 
                    indexName,getResource().getURL(),archiveFormat);
                setFinishedState(ResourceState.IGNORED);
            } finally {
                if(ais != null){ //close the top most stream
                    IOUtils.closeQuietly(ais);
                } else if(is != null){
                    IOUtils.closeQuietly(is);
                }
            }
            
        }
        
    }

    @Override
    public String getSortKey() {
        return CONFIG_INSTALL_ORDER+getResource().getPriority()+"-"+getResource().getEntityId();
    }

}
