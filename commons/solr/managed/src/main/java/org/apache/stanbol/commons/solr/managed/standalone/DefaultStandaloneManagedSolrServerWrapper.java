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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedIndexState;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.xml.sax.SAXException;

/**
 * Wrapper of the the {@link StandaloneManagedSolrServer#getManagedServer()}
 * that provides a public default constructor as needed by the
 * {@link ServiceLoader} utility.<p>
 * Basically this Wrapper allows to initialise the default managed Solr
 * server by calling
 * <pre><code>
 *    {@link ServiceLoader}&lt;ManagedSolrServer&gt; loader = 
 *        {@link ServiceLoader#load(Class) ServiceLoader.load}({@link ManagedSolrServer}.class};
 *    {@link Iterator} it = {@link ServiceLoader#iterator() loader.iterator()};
 *    {@link ManagedSolrServer} defaultServer;
 *    if(it.hasNext()){
 *      defaultServer = it.next();
 *    }
 * </code></pre>
 * @author westei
 *
 */
public class DefaultStandaloneManagedSolrServerWrapper implements ManagedSolrServer {

    ManagedSolrServer defaultServer;
    
    public DefaultStandaloneManagedSolrServerWrapper() {
        defaultServer = StandaloneManagedSolrServer.getManagedServer();
    }
    
    @Override
    public IndexMetadata createSolrIndex(String coreName, String indexPath, Properties properties) throws IOException {
        return defaultServer.createSolrIndex(coreName, indexPath, properties);
    }

    @Override
    public File getManagedDirectory() {
        return defaultServer.getManagedDirectory();
    }

    @Override
    public String getServerName() {
        return defaultServer.getServerName();
    }

    @Override
    public File getSolrIndexDirectory(String name) {
        return defaultServer.getSolrIndexDirectory(name);
    }


    @Override
    public boolean isManagedIndex(String solrIndexName) {
        return defaultServer.isManagedIndex(solrIndexName);
    }

    @Override
    public void removeIndex(String name, boolean deleteFiles) {
        defaultServer.removeIndex(name, deleteFiles);
    }

    @Override
    public IndexMetadata updateIndex(String name, String resourceName, Properties properties) throws IOException {
        return defaultServer.updateIndex(name, resourceName, properties);
    }

    @Override
    public IndexMetadata activateIndex(String indexName) throws IOException, SAXException {
        return defaultServer.activateIndex(indexName);
    }

    @Override
    public IndexMetadata createSolrIndex(String indexName, ArchiveInputStream ais) throws IOException,
                                                                                  SAXException {
        return defaultServer.createSolrIndex(indexName, ais);
    }

    @Override
    public IndexMetadata deactivateIndex(String indexName) {
        return defaultServer.deactivateIndex(indexName);
    }

    @Override
    public IndexMetadata getIndexMetadata(String indexName) {
        return defaultServer.getIndexMetadata(indexName);
    }

    @Override
    public ManagedIndexState getIndexState(String indexName) {
        return defaultServer.getIndexState(indexName);
    }

    @Override
    public Collection<IndexMetadata> getIndexes(ManagedIndexState state) {
        return defaultServer.getIndexes(state);
    }

    @Override
    public IndexMetadata updateIndex(String indexName, ArchiveInputStream ais) throws IOException,
                                                                              SAXException {
        return defaultServer.updateIndex(indexName, ais);
    }

    @Override
    public IndexMetadata updateIndex(String indexName, ArchiveInputStream ais, String archiveCoreName) throws IOException,
                                                                                                      SAXException {
        return defaultServer.updateIndex(indexName, ais, archiveCoreName);
    }
 
    @Override
    public void swapIndexes(String indexName1, String indexName2) {
        defaultServer.swapIndexes(indexName1, indexName2);
    }
}