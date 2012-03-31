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
package org.apache.stanbol.contenthub.store.inmemory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.store.ChangeSet;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = false)
@Service
public class FileStore implements Store {
    private static final String FILE_STORE_PATH = "file.store.path";
    @Property(name = FILE_STORE_PATH, value = "")
    private static final String REVISION_FILE_NAME = "revisions";

    private static final String METADATA_EXTENSION = "_metadata";

    private final Logger log = LoggerFactory.getLogger(FileStore.class);

    private File storeFolder;

    // TODO replace object with a proper structure
    private Map<String,Object> revisionMap;

    @Activate
    protected void activate(ComponentContext componentContext) throws StoreException {
        String fileStorePath = (String) componentContext.getProperties().get(FILE_STORE_PATH);
        if (fileStorePath.trim().equals("")) {
            fileStorePath = componentContext.getBundleContext().getProperty("sling.home");
        }

        // load revisions
        File revisionFile = new File(fileStorePath);
        if (!revisionFile.exists()) {
            boolean created;
            try {
                created = revisionFile.createNewFile();
                if (!created) {
                    log.error("Failed to create revision file");
                    throw new StoreException("Failed to create revision file");
                }

            } catch (IOException e) {
                log.error("Failed to create revision file", e);
                throw new StoreException("Failed to create revision file", e);
            }
        } else {
            
        }
    }

    @Override
    public ContentItem remove(UriRef id) throws StoreException {
        checkStoreFolder();
        try {
            String urlEncodedId = URLEncoder.encode(id.getUnicodeString(), "UTF-8");
            
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to remove content item", e);
        }
        return null;
    }

    @Override
    public UriRef put(ContentItem ci) throws StoreException {
        // TODO Auto-generated method stub

        return null;
    }

    @Override
    public ContentItem get(UriRef id) throws StoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChangeSet changes(long revision, int batchSize) throws StoreException {
        // TODO Auto-generated method stub
        return null;
    }

    private void checkStoreFolder() throws StoreException {
        if (!storeFolder.exists()) {
            throw new StoreException("Store folder does not exist");
        }
    }
    
    private void fillRevisionMap() {
        FilenameFilter revisionFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // TODO Auto-generated method stub
                return false;
            }
        };
    }
}
