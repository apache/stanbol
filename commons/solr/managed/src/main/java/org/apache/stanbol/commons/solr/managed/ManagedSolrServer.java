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
package org.apache.stanbol.commons.solr.managed;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.utils.ConfigUtils;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.xml.sax.SAXException;

/**
 * Service that provides an managed Solr server ({@link CoreContainer}). 
 * This interface allows to register activate, deactivate and remove indexes 
 * ({@link SolrCore}s)
 * The {@link #MANAGED_SOLR_DIR_PROPERTY} property can be used to define the location of the internally
 * managed index. Implementations need to load this property by the {@link ComponentContext} if running within
 * an OSGI container or otherwise use the system properties. In cases a relative path is configured the
 * "user.dir" need to be used as base. Implementations need also to provide an default value in case no
 * configuration is present.<br>
 * Implementations need also support property substitution based on the system properties for the
 * {@link #MANAGED_SOLR_DIR_PROPERTY}. E.g. parsing the value "${user.home}/.stanbol/indexes" will create the
 * managed solr indexes within the home directory of the user.
 * <p>
 * This Service is also useful if one needs to initialise an own Solr Core for the manage Solr Server. In this
 * case the {@link #getManagedDirectory()} method can be used to get the managed Solr directory and new
 * indices can be added as sub directories. Utility methods for initialising cores are available as part of
 * the {@link ConfigUtils}.
 * 
 * @author Rupert Westenthaler
 * 
 */
public interface ManagedSolrServer {

    /**
     * The name of this server
     * @return The name of the Server
     */
    public String getServerName();
    /**
     * This property can be used to configure the location of the internally managed EmbeddedSolrServer.
     * <p>
     * Configuring an absolute path (starting with {@link File#separatorChar}) will cause the index to be
     * initialised in this directory.
     * <p>
     * Configuring an relative value will use
     * <ul>
     * <li>the working directory (<code>Systen.getProperty("user.dir")</code>) outside of an OSGI environment
     * <li>the data directory provided by the SolrYard bundle (by calling
     * {@link BundleContext#getDataFile(String)} with the relative path.
     * </ul>
     * In case this property is not present the {@link #DEFAULT_SOLR_DATA_DIR} (an relative path) is used.
     */
    String MANAGED_SOLR_DIR_PROPERTY = "org.apache.stanbol.commons.solr.managed.managedSolrDir";
    /**
     * Default value for the relative path used if the {@link #MANAGED_SOLR_DIR_PROPERTY} is not present. It
     * is not required that implementations use this as default.
     */
    String DEFAULT_SOLR_DATA_DIR = "indexes";

    
    /**
     * Checks if a solrIndex with the parsed name is managed or not. Note that
     * an Index might be managed, but not yet be initialised. To check if an
     * index is managed and can be used use {@link #getIndexState(String)}
     * @param indexName the name of the Solr index to check
     * @return <code>true</code> only if a Solr index with the parsed name is
     * already present within the manages Solr directory.
     * @throws IllegalStateException In case the managed Solr directory can not
     * be obtained (usually indicates that this component is currently 
     * deactivated)
     * @throws IllegalArgumentException In case <code>null</code> or an empty 
     * string is parsed as solrIndexName
     */
    boolean isManagedIndex(String indexName);
    /**
     * Getter for the meta data for the index with the parsed name.
     * @param indexName the name of the index
     * @return the meta data or <code>null</code> if no index with the parsed name
     * is managed by this server
     * @throws IllegalArgumentException if <code>null</code> or an empty string
     * is parsed as indexName.
     */
    IndexMetadata getIndexMetadata(String indexName);

    /**
     * Checks if the managed index is also initialised and ready to be used.
     * <p>
     * Indexes are managed as soon as they are announced to the SolrDirectoryManager. However when using the
     * {@link #createSolrIndex(String, String, Properties)} it can not be assured that the archive with
     * the actual data is already available.
     * <p>
     * 
     * @param indexName the name of the index
     * @return the state of the index or <code>null</code> if not {@link #isManagedIndex(String)}
     * @throws IllegalArgumentException if the parsed name is <code>null</code> or empty
     */
    ManagedIndexState getIndexState(String indexName);

    /**
     * Getter for all indexes in a specific state
     * @param state {@link ManagedIndexState} Filter 
     * @return A collection with the {@link IndexMetadata} of all managed
     * indexes in that state. An empty collection in case no index is in the
     * parsed state
     * @throws IllegalArgumentException if <code>null</code> is parsed as state
     */
    Collection<IndexMetadata> getIndexes(ManagedIndexState state);

    /**
     * Getter for the directory of the parsed index. Implementations need to ensure that returned directories
     * are valid Solr indices (or Solr Cores)
     * If the requested Index is currently initialising, than this method MUST
     * wait until the initialisation is finished before returning. 
     * 
     * @param indexName 
     *            the name of the requested solr index. 
     * @return the directory (instanceDir) of the index or <code>null</code> a
     *         SolrIndex with that name is not managed.
     * @throws IllegalArgumentException
     *             if the parsed solrIndexName is <code>null</code> or empty
     */
    File getSolrIndexDirectory(String indexName);

    /**
     * Creates a new Solr Index based on the data in the provided {@link ArchiveInputStream}
     * 
     * @param indexName
     *            the name of the index to create
     * @param ais
     *            the stream providing the data for the new index
     * @return the directory (instanceDir) of the index.
     * @throws IOException
     *             On any error while reading from the parsed input stream
     * @throws SAXException
     *             On any Error while parsing the {@link SolrCore} configuration
     *             files when registering the core for the parsed data.
     * @throws IllegalArgumentException
     *             if the parsed solrIndexName is <code>null</code> or empty
     */
    IndexMetadata createSolrIndex(String indexName, ArchiveInputStream ais) throws IOException, SAXException;

    /**
     * Creates a new {@link SolrCore} based on looking up the Index data via the {@link DataFileProvider} service
     * 
     * @param indexName
     *            The name of the solrIndex to create
     * @param indexPath
     *            the name of the dataFile looked up via the {@link DataFileProvider}
     * @param properties
     *            Additional properties describing the index
     * @return the directory (instanceDir) of the index or null if the index data could not be found
     * @throws IllegalArgumentException Invalid Index Name
     * @throws IOException Data File Not Found
     */
    IndexMetadata createSolrIndex(String indexName, String indexPath, Properties properties) throws IOException;
    /**
     * Creates/Updates the core with the parsed name to the data parsed within the
     * {@link ArchiveInputStream}.
     * @param indexName The name for the Index to create/update
     * @param ais The {@link ArchiveInputStream} used to read the data for the 
     *             Index to create/update
     * @return The metadata for the created index
     * @throws IOException
     *             On ansy Error while copying the data provided by the parsed
     *             {@link ArchiveInputStream}
     * @throws SAXException
     *             On any Error while parsing the {@link SolrCore} configuration
     *             files when registering the core for the parsed data.
     */
    IndexMetadata updateIndex(String indexName,ArchiveInputStream ais) throws IOException, SAXException;
    /**
     * Updates the data of the core with the parsed name with the data provided
     * by the resource with the parsed name. The resource is loaded by using the
     * {@link DataFileProvider} infrastructure
     * @param indexName the name of the index to be updated 
     * @param resourceName Resource Data name
     * @param properties Additional Properties describing the index
     * @throws IOException
     */
    IndexMetadata updateIndex(String indexName,String resourceName,Properties properties) throws IOException;

    IndexMetadata updateIndex(String indexName, ArchiveInputStream ais, String archiveCoreName) throws IOException,
                                                                                               SAXException;
    /**
     * Removes the index with the parsed name and optionally also deletes the
     * data on the file system.
     * @param indexName
     * @param deleteFiles boolean indicating if the physical files must be deleted
     */
    void removeIndex(String indexName, boolean deleteFiles);
    
    /**
     * Sets the index with the parsed name to the {@link ManagedIndexState#INACTIVE}
     * state. If the referenced index in not {@link ManagedIndexState#ACTIVE} this
     * method has no effect.
     * @param indexName the name of the index
     * @return The current meta data for that index or <code>null</code> if no
     * index with the parsed name is managed.
     * @throws IllegalArgumentException if the parsed indexName is <code>null</code>
     * or an empty string
     */
    IndexMetadata deactivateIndex(String indexName);

    /**
     * Can be used to activate an {@link ManagedIndexState#INACTIVE} index. <p>
     * This does not try to update the index data or to create an index with the
     * parsed name.<p>
     * If the referenced index is not within the INACTIVE state this call might
     * be ignored. The resulting state of the index can be retrieved from the
     * returned {@link IndexMetadata} instance. 
     * @param indexName the name of the index
     * @return the {@link IndexMetadata} or <code>null</code> if no index with
     * the parsed name is managed by this server.
     * @throws IOException
     *             On ansy Error while copying the data provided by the parsed
     *             {@link ArchiveInputStream}
     * @throws SAXException
     *             On any Error while parsing the {@link SolrCore} configuration
     *             files when registering the core for the parsed data.
     * @throws IllegalArgumentException if <code>null</code> or an empty string
     * is parsed as indexName
     */
    IndexMetadata activateIndex(String indexName) throws IOException, SAXException;
    /**
     * Getter for the directory on the local file system used as working directory
     * for the {@link CoreContainer} managed by this component.
     * 
     * @return the directory of the Solr Home used for the internally managed {@link CoreContainer} or
     *         <code>null</code> if running within an OSGI Environment and this component is deactivated.
     * @throws IllegalStateException
     *             In case the managed Solr directory can not be obtained (usually indicates that this
     *             component is currently deactivated) or initialised.
     */
    File getManagedDirectory();
    
    /**
     * Swaps the indexes through using the swap method of the underlying CoreContainer 
     * ({@link CoreContainer#swap(String, String)}).  
     * @param indexName1 the name of the first index
     * @param indexName2 the name of the second index
     * @throws IllegalArgumentException if one or both of the indexes is not managed i.e {@link #isManagedIndex(String)}
     * returns {@code false}.
     * @throws IllegalStateException if one or both of the index is not in {@link ManagedIndexState#ACTIVE}
     * state.
     */
    void swapIndexes(String indexName1, String indexName2);
}
