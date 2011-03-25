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
package org.apache.stanbol.entityhub.yard.solr;

import java.io.File;
import java.util.Map;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.stanbol.entityhub.yard.solr.SolrServerProvider.Type;
import org.apache.stanbol.entityhub.yard.solr.utils.ConfigUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * Service that provides access to an managed Solr directory - a directory that
 * manages the files (configuration and data) needed to create a Solr Server. <p>
 * Note that is only refers to the Files and not the Solr server (or 
 * EmbeddedSolrServer). Users need to use the 
 * {@link SolrServerProvider#getSolrServer(Type, String, String...)} to get an 
 * {@link SolrServer} instance based on the directory provided by this 
 * Interface.<p>
 * The {@link #MANAGED_SOLR_DIR_PROPERTY} property can be used to define the location
 * of the internally managed index. Implementations need to load this property
 * by the {@link ComponentContext} if running within an OSGI container or 
 * otherwise use the system properties. In cases a relative path is configured
 * the "user.dir" need to be used as base. Implementations need also to provide
 * an default value in case no configuration is present.<br>
 * Implementations need also support  property substitution based on the system
 * properties for the {@link #MANAGED_SOLR_DIR_PROPERTY}. E.g. parsing the value 
 * "${user.home}/.stanbol/indexes" will create the managed solr indexes within 
 * the home directory of the user.<p>
 * This Service is also useful if one needs to initialise an own Solr Core
 * for the manage Solr Server. In this case the {@link #getManagedDirectory()}
 * method can be used to get the managed Solr directory and new indices can be
 * added as sub directories. Utility methods for initialising cores are available
 * as part of the {@link ConfigUtils}.
 * 
 * @author Rupert Westenthaler
 *
 */
public interface SolrDirectoryManager {

    /**
     * This property can be used to configure the location of the internally
     * managed EmbeddedSolrServer.<p>
     * Configuring an absolute path (starting with {@link File#separatorChar}) 
     * will cause the index to be initialised in this directory.<p>
     * Configuring an relative value will use  <ul>
     * <li> the working directory (<code>Systen.getProperty("user.dir")</code>)
     *      outside of an OSGI environment
     * <li> the data directory provided by the SolrYard bundle (by calling
     *      {@link BundleContext#getDataFile(String)} with the relative path.
     * </ul>
     * In case this property is not present the {@link #DEFAULT_SOLR_DATA_DIR}
     * (an relative path) is used.
     */
    public static final String MANAGED_SOLR_DIR_PROPERTY = "org.apache.stanbol.entityhub.yard.solr.managedSolrDir";
    /**
     * Default value for the relative path used if the {@link #MANAGED_SOLR_DIR_PROPERTY}
     * is not present. It is not required that implementations use this as default.
     */
    public static final String DEFAULT_SOLR_DATA_DIR = "indexes";

    /**
     * Checks if a solrIndex with the parsed name is managed or not.
     * @param solrIndexName the name of the Solr index to check
     * @return <code>true</code> only if a Solr index with the parsed name is
     * already present within the manages Solr directory.
     * @throws IllegalStateException In case the managed Solr directory can not
     * be obtained (usually indicates that this component is currently 
     * deactivated)
     * @throws IllegalArgumentException In case <code>null</code> or an empty 
     * string is parsed as solrIndexName
     */
    public abstract boolean isManagedIndex(String solrIndexName) throws IllegalStateException;

    /**
     * Getter for all the indexes currently available in the managed solr directory.
     * The key is the name of the index and the value is the File pointing to the
     * directory.
     * @return map containing all the currently available indexes
     * @throws IllegalStateException In case the managed Solr directory can not
     * be obtained (usually indicates that this component is currently 
     * deactivated) or initialised.
     */
    public abstract Map<String,File> getManagedIndices() throws IllegalStateException;

    /**
     * Getter for the directory of the parsed index. In case the requested index
     * does not already exist (in the managed solr directory) it is initialised
     * by using the default solr core configuration contained in this bundle.<p>
     * Directories returned by this method are typically used as second parameter
     * of {@link SolrServerProvider#getSolrServer(Type, String, String...)} to
     * create an {@link SolrServer} instance.
     * @param solrPathOrUri the name of the requested solr index. If no index
     * with that name does exist a new one will be initialised base on the
     * default core configuration part of this bundle.
     * @return the directory (instanceDir) of the index.
     * @throws IllegalArgumentException if the parsed solrIndexName is 
     * <code>null</code> or empty
     */
    public abstract File getSolrDirectory(final String solrIndexName) throws IllegalArgumentException;

    /**
     * Getter for the managed Solr Directory.
     * @return the directory of the Solr Home used for the internally managed
     * {@link CoreContainer} or <code>null</code> if running within an OSGI
     * Environment and this component is deactivated.
     * @throws IllegalStateException In case the managed Solr directory can not
     * be obtained (usually indicates that this component is currently 
     * deactivated) or initialised.
     */
    public abstract File getManagedDirectory() throws IllegalStateException;

}