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
package org.apache.stanbol.commons.solr.impl.install;

import static org.apache.stanbol.commons.solr.impl.install.IndexInstallerConstants.PROPERTY_ARCHIVE_FORMAT;
import static org.apache.stanbol.commons.solr.impl.install.IndexInstallerConstants.PROPERTY_INDEX_NAME;
import static org.apache.stanbol.commons.solr.impl.install.IndexInstallerConstants.SOLR_INDEX_ARCHIVE_RESOURCE_TYPE;
import static org.apache.stanbol.commons.solr.utils.ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION;
import static org.apache.stanbol.commons.solr.utils.ConfigUtils.SUPPORTED_SOLR_ARCHIVE_FORMAT;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.commons.solr.SolrDirectoryManager;
import org.apache.stanbol.commons.solr.SolrServerProviderManager;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class implementing the two core interfaces of the Apache Sling Installer framework.
 * <p>
 * The {@link ResourceTransformer} is needed to check if installed files actually SolrIndexes. Currently this
 * check is done by checking if <code>'.'+{@link IndexInstallerConstants#SOLR_INDEX_ARCHIVE_EXTENSION}</code>
 * is contained in the filename. In addition it is checked of the archive type is hinted by the Filename. If
 * not than ".zip" (works also for ".jar") is assumed. Note also that for ".gz" and ".bz2" it is assumed that
 * ".tar" is used. File names such as "&lt;indexName&gt;.
 * {@value IndexInstallerConstants#SOLR_INDEX_ARCHIVE_EXTENSION} [.&lt;archiveType&gt;]" are used by this
 * implementation
 * <p>
 * The {@link InstallTaskFactory} interface is needed to create the actual install and remove task for
 * transformed resources. Based on the requested activity instances of {@link IndexInstallTask} or
 * {@link IndexRemoveTask} are created.
 * <p>
 * This implementation uses a {@link Constants#SERVICE_RANKING} of 100. This ensures that the this
 * implementation is called before any {@link ResourceTransformer} directly part of the Apache Sling Installer
 * framework. If {@link #transform(RegisteredResource)} returns <code>null</code> the Sling Installer
 * framework will call the next registered {@link ResourceTransformer} instance. By returning a
 * {@link TransformationResult} no further {@link ResourceTransformer} will be called.
 * <p>
 * 
 * TODO: This package should move to an own bundle supporting Sling Install capabilities for Solr. Even the
 * current version only on the {@link SolrDirectoryManager}. The reason why it is still inside the SolrYard
 * Bundle is that the remove functionality would also need to stop currently running SolrServers. this is
 * currently not possible with the current architecture because {@link SolrServer} instances returned by the
 * {@link SolrServerProviderManager} are no OSGI components.
 * 
 * 
 * @author Rupert Westenthaler
 * 
 */
@Component(immediate = true)
@Services(value = {@Service(value = InstallTaskFactory.class), @Service(value = ResourceTransformer.class)})
@Property(name = Constants.SERVICE_RANKING, intValue = 100)
// we need to be in front of the Sling Components
public class SolrIndexInstaller implements InstallTaskFactory, ResourceTransformer {

    private static final Logger log = LoggerFactory.getLogger(SolrIndexInstaller.class);

    /**
     * This service manages the SolrIndex Directory of the SolrYard. It is needed by the
     * {@link IndexInstallTask} and {@link IndexRemoveTask} to do there work.
     */
    @Reference
    private SolrDirectoryManager solrDirectoryManager;

    public InstallTask createTask(TaskResourceGroup taskResourceGroup) {
        TaskResource toActivate = taskResourceGroup.getActiveResource();
        if (SOLR_INDEX_ARCHIVE_RESOURCE_TYPE.equals(toActivate.getType())) {
            if (toActivate.getState() == ResourceState.UNINSTALL) {
                return new IndexRemoveTask(taskResourceGroup, solrDirectoryManager);
            } else {
                return new IndexInstallTask(taskResourceGroup, solrDirectoryManager);
            }
        } else {
            return null;
        }
    }

    @Override
    public TransformationResult[] transform(RegisteredResource registeredResource) {
        if (InstallableResource.TYPE_FILE.equals(registeredResource.getType())) {
            return checkIndex(registeredResource);
        } else { // this processes only files
            return null;
        }
    }

    /**
     * Checks if the installed resource is an Solr Index Archive
     * 
     * @param registeredResource
     *            the registered resource parsed by the Apache Sling installer framework
     * @return the transformed resource or <code>null</code> if the parsed resource is not an Solr Index
     *         Archive.
     */
    private TransformationResult[] checkIndex(RegisteredResource registeredResource) {
        // the URL is <schema>:<filePath>
        // where the schema is the provider that registered the resource
        Map<String,Object> properties = new HashMap<String,Object>();
        String filePath = registeredResource.getURL().substring(
            registeredResource.getURL().lastIndexOf(':') + 1);
        // get the name of the index
        String indexName = FilenameUtils.getBaseName(filePath);
        // only the String until the first '.' -> multiple endings (e.g. slrindex.zip) expected
        indexName = indexName.indexOf('.') > 0 ? indexName.substring(0, indexName.indexOf('.')) : indexName;
        properties.put(PROPERTY_INDEX_NAME, indexName);
        // now convert to lover case to ease the tests for file endings
        filePath = filePath.toLowerCase();
        if (!filePath.contains('.' + SOLR_INDEX_ARCHIVE_EXTENSION)) {
            // not an solr index archive
            return null; // -> can not transform
        }
        String extension = FilenameUtils.getExtension(filePath);
        String archiveFormat = SUPPORTED_SOLR_ARCHIVE_FORMAT.get(extension);
        if (archiveFormat == null) {
            log.error("Unable to process Solr Index Archive from Resource " + registeredResource.getURL()
                      + "because of unsupported archive format \"" + extension + "\" (supported are "
                      + SUPPORTED_SOLR_ARCHIVE_FORMAT.keySet() + ")");
            return null;
        } else {
            properties.put(PROPERTY_ARCHIVE_FORMAT, archiveFormat);
        }

        TransformationResult tr = new TransformationResult();
        // try {
        // tr.setInputStream(registeredResource.getInputStream());
        // } catch (IOException e) {
        // log.error(String.format("Unable to transform RegisteredResource %s with type %s and scheme %s",
        // registeredResource.getURL(), registeredResource.getType(), registeredResource.getScheme()),e);
        // return null;
        // }
        tr.setId(indexName + '.' + SOLR_INDEX_ARCHIVE_EXTENSION + '.' + archiveFormat);
        tr.setAttributes(properties);
        tr.setResourceType(SOLR_INDEX_ARCHIVE_RESOURCE_TYPE);
        return new TransformationResult[] {tr};
    }
}
