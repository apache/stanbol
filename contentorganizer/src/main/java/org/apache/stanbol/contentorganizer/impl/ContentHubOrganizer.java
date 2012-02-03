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
package org.apache.stanbol.contentorganizer.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.contentorganizer.model.Category;
import org.apache.stanbol.contentorganizer.model.Criterion;
import org.apache.stanbol.contentorganizer.servicesapi.ContentConnector;
import org.apache.stanbol.contentorganizer.servicesapi.ContentOrganizer;
import org.apache.stanbol.contentorganizer.servicesapi.ContentRetrievalException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alexdma
 *
 */
@Component(immediate = true, metatype = false)
@Service
public class ContentHubOrganizer implements ContentOrganizer<Store> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static String DEFAULT_ROOT_PATH = "datafiles/contentorganizer";
    private final static String DEFAULT_FOLDER_NAME = "metadata";

    @Reference
    protected Store contentStore;

    @Reference
    protected SolrSearch solrSearch;

    @Reference
    protected Entityhub entityHub;

    @Reference
    protected Serializer serializer;

    @Reference
    protected ReferencedSiteManager siteMgr;

    private ContentConnector connector;

    private File contentMetadataDir;

    public ContentHubOrganizer() {
        super();
    }

    public ContentHubOrganizer(Store contentStore,
                               SolrSearch solrSearch,
                               Dictionary<String,Object> configuration) {
        this();
        this.contentStore = contentStore;
        this.solrSearch = solrSearch;
        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access component context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + ContentHubOrganizer.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }

        String slingHome = context.getBundleContext().getProperty("sling.home");
        if (!slingHome.endsWith(File.separator)) slingHome += File.separator;
        contentMetadataDir = new File(slingHome + DEFAULT_ROOT_PATH, DEFAULT_FOLDER_NAME);

        // if directory for programs does not exist, create it
        if (!contentMetadataDir.exists()) {
            if (contentMetadataDir.mkdirs()) {
                log.info("Directory for metadata created succesfully");
            } else {
                log.error("Directory for metadata COULD NOT be created");
                throw new IOException("Directory : " + contentMetadataDir.getAbsolutePath()
                                      + " cannot be created");
            }
        }

        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        connector = new ContentHubConnector(contentStore, solrSearch);
        classifyContent(contentStore);

        log.debug(ContentHubOrganizer.class + " activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + ContentHubOrganizer.class + " deactivate with context " + context);

        connector = null;

        log.debug(ContentHubOrganizer.class + " deactivated.");
    }

    @Override
    public Set<Criterion> getSuitableCriteria(Collection<ContentItem> contentItems) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<ContentItem,Set<Category>> classifyContent(Store contentStore) {

        // Do stuff here for the time being...

        Set<ContentItem> contents;
        try {
            contents = connector.getContents();
        } catch (ContentRetrievalException e1) {
            log.error("Failed to retrieve stored contents.", e1);
            contents = Collections.emptySet();
        }

        MGraph mg = new SimpleMGraph();
        for (ContentItem ci : contents)
            mg.addAll(ci.getMetadata());

        File f = null;
        try {
            f = new File(contentMetadataDir, "all.rdf");
            serializer.serialize(new FileOutputStream(f), mg, SupportedFormat.RDF_XML);
        } catch (UnsupportedFormatException e) {
            log.error("Unsupported serialization format {} ! This should not happen...",
                SupportedFormat.RDF_XML);
        } catch (FileNotFoundException e) {
            log.error("Could not obtain file {} for writing. ", f);
        }

        try {
            f = new File(contentMetadataDir, "enhancement.rdf");
            serializer.serialize(new FileOutputStream(f), contentStore.getEnhancementGraph(),
                SupportedFormat.RDF_XML);
        } catch (UnsupportedFormatException e) {
            log.error("Unsupported serialization format {} ! This should not happen...",
                SupportedFormat.RDF_XML);
        } catch (FileNotFoundException e) {
            log.error("Could not obtain file {} for writing. ", f);
        }

        return null;
    }

}
