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
package org.apache.stanbol.contenthub.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager to keep track of metadata of {@link SemanticIndex}es. This manager stores the managed metadata
 * persistently within file system directory specified in its constructor:
 * {@link #SemanticIndexMetadataManager(File)}.
 * 
 * @author suat
 * 
 */
public class SemanticIndexMetadataManager {
    private final Logger logger = LoggerFactory.getLogger(SemanticIndexMetadataManager.class);

    private Map<String,Properties> indexMetadataMap = new HashMap<String,Properties>();
    private File indexMetadataDirectory;

    /**
     * Default constructor of this manager class. It gets file in {@code indexMetadataDirectory} and tries to
     * create a directory if it does not exist. If there already exists such a directory, it loads the
     * serialized {@link Properties} and aggregates them within the {@link #indexMetadataMap}.
     * 
     * @param indexMetadataDirectory
     *            the file system directory in which the index metadata will be stored
     * @throws IndexManagementException
     */
    public SemanticIndexMetadataManager(File indexMetadataDirectory) throws IndexManagementException {
        this.indexMetadataDirectory = indexMetadataDirectory;

        // if directory for programs does not exist, create it
        if (!indexMetadataDirectory.exists()) {
            if (indexMetadataDirectory.mkdirs()) {
                logger.info("Directory for index metadata created succesfully");
            } else {
                logger.error("Directory for index metadata COULD NOT be created");
                throw new IndexManagementException("Directory : " + indexMetadataDirectory.getAbsolutePath()
                                                   + " cannot be created");
            }
        }

        // load index metadata to memory
        synchronized (indexMetadataMap) {
            File[] metadataList = indexMetadataDirectory.listFiles();
            for (File configFile : metadataList) {
                String id = configFile.getName().substring(0, configFile.getName().lastIndexOf('.'));
                Properties props = new Properties();
                InputStream is = null;
                try {
                    is = new FileInputStream(configFile);
                    props.load(is);
                    indexMetadataMap.put(id, props);
                    logger.info("Index metadata has been loaded from the location: {}",
                        configFile.getAbsolutePath());
                } catch (FileNotFoundException e) {
                    logger.error("IndexMetadata file cannot be found");
                    throw new IndexManagementException("IndexMetadata file cannot be found", e);
                } catch (IOException e) {
                    logger.error("Failed to read from input stream");
                    throw new IndexManagementException("Failed to read from input stream", e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }
    }

    /**
     * Returns whether an index for the given {@code id} is already configured.
     * 
     * @param id
     *            identifier of the {@link SemanticIndex}
     * @return
     */
    public boolean isConfigured(String id) {
        synchronized (indexMetadataMap) {
            return indexMetadataMap.containsKey(id);
        }
    }

    /**
     * Returns the {@link Map} containing metadata of the all indexes.
     * 
     * @return
     */
    public Map<String,Properties> getAllIndexMetadata() {
        synchronized (indexMetadataMap) {
            return indexMetadataMap;
        }
    }

    /**
     * Returns the metadata of the index specified with the given id
     * 
     * @param id
     *            identifier of the {@link SemanticIndex}
     * @return
     */
    public Properties getIndexMetadata(String id) {
        synchronized (indexMetadataMap) {
            return indexMetadataMap.get(id);
        }
    }

    /**
     * Stores the metadata of the index speficied with the given id either in memory and persistently.
     * 
     * @param id
     *            identifier of the {@link SemanticIndex}
     * @param indexMetadata
     * @throws IndexManagementException
     */
    public void storeIndexMetadata(String id, Properties indexMetadata) throws IndexManagementException {
        String indexMetadataFilePath = indexMetadataDirectory.getAbsolutePath() + File.separator + id
                                       + ".props";
        synchronized (indexMetadataMap) {
            FileOutputStream out = null;
            Properties stringValues = getStringValues(indexMetadata);
            try {
                out = new FileOutputStream(indexMetadataFilePath);
                stringValues.store(out, null);
            } catch (IOException e) {
                logger.error("Failed to write indexMetadataFilePath to the specified output stream");
                throw new IndexManagementException(
                        "Failed to write indexMetadataFilePath to the specified output stream", e);
            } finally {
                IOUtils.closeQuietly(out);
            }
            indexMetadataMap.put(id, stringValues);
        }
    }

    /**
     * Updates the metadata of the index specified with the given id
     * 
     * @param id
     *            identifier of the {@link SemanticIndex}
     * @param indexMetadata
     * @throws IndexManagementException
     * @throws IOException
     */
    public void updateIndexMetadata(String id, Properties indexMetadata) throws IndexManagementException {
        synchronized (indexMetadataMap) {
            storeIndexMetadata(id, indexMetadata);
        }
    }

    public Properties removeIndexMetadata(String id) throws IndexManagementException {
        String indexMetadataFilePath = indexMetadataDirectory.getAbsolutePath() + File.separator + id
                                       + ".props";
        File file = new File(indexMetadataFilePath);
        logger.info("Index metadata file to be deleted: {}", file.getAbsolutePath());
        if (file.exists()) {
            file.delete();
        } else {
            logger.error("Failed to delete IndexMetadata file");
            throw new IndexManagementException("Failed to delete IndexMetadata file");
        }

        Properties indexMetadata;
        synchronized (indexMetadataMap) {
            indexMetadata = indexMetadataMap.remove(id);
        }

        return indexMetadata;
    }

    private Properties getStringValues(Properties indexMetadata) {
        Properties properties = new Properties();
        for (Entry<Object,Object> property : indexMetadata.entrySet()) {
            Object value = property.getValue();
            if (value instanceof String) {
                properties.put(property.getKey(), value);
            } else {
                properties.put(property.getKey(), value.toString());
            }
        }
        return properties;
    }
}
