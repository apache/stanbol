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

import java.io.StringReader;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.rdf.core.Resource;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndex;
import org.apache.stanbol.commons.semanticindex.store.IndexingSource;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.contenthub.index.solr.LDPathUtils;
import org.apache.stanbol.enhancer.ldpath.EnhancerLDPath;
import org.apache.stanbol.enhancer.ldpath.backend.ContentItemBackend;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.exception.LDPathParseException;
import at.newmedialab.ldpath.model.programs.Program;

/**
 * <p>
 * Abstract class which can be used to develop new OSGi based {@link SemanticIndex} implementations. It
 * provides various kind of methods which are possibly used by different extenstion of this class. By default
 * it implements the {@link SemanticIndex} interface. However, not all of the methods are implemented. So, new
 * implementations are expected to implement the missing ones.
 * </p>
 * <p>
 * For the provided methods please refer to the specific documentations of methods below.
 * </p>
 * 
 * @author meric
 * @author suat
 * 
 */
@Properties(value = {@Property(name = AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM)})
public abstract class AbstractLDPathSemanticIndex extends AbstractSemanticIndex {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLDPathSemanticIndex.class);

    public static final String PROP_LD_PATH_PROGRAM = "org.apache.stanbol.contenthub.index.AbstractLDPathSemanticIndex.ldPathProgram";

    protected String ldPathProgram;
    protected Program<Object> objectProgram;
    protected SemanticIndexMetadataManager semanticIndexMetadataManager;

    @Reference
    protected SiteManager siteManager;

    /**
     * Initializes the common properties to be obtained from the {@link ComponentContext}. Furthermore, this
     * method tries to fetch the specified {@link IndexingSource} from the OSGi environment. This method is
     * expected to be called in the {@code activate} method of the actual implementation of this abstract
     * class.
     * 
     * @param componentContext
     *            {@link ComponentException} of the actual implementation of this abstract class
     * @throws ConfigurationException
     * @throws IndexException
     * @throws IndexManagementException
     * @throws StoreException
     */
    protected void activate(ComponentContext componentContext) throws ConfigurationException,
                                                              IndexException,
                                                              IndexManagementException,
                                                              StoreException {
        super.activate(componentContext);
        @SuppressWarnings("rawtypes")
        Dictionary properties = componentContext.getProperties();
        this.ldPathProgram = (String) OsgiUtils.checkProperty(properties,
            AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM);
        initializeLDPathProgram();
    }

    private void initializeLDPathProgram() throws IndexException, IndexManagementException {
        // create program for EntityhubLDPath
        SiteManagerBackend backend = new SiteManagerBackend(siteManager);
        ValueFactory vf = InMemoryValueFactory.getInstance();
        EntityhubLDPath ldPath = new EntityhubLDPath(backend, vf);
        try {
            this.objectProgram = ldPath.parseProgram(LDPathUtils.constructReader(this.ldPathProgram));
        } catch (LDPathParseException e) {
            logger.error("Should never happen!!!!!", e);
            throw new IndexException("Failed to create Program from the parsed LDPath", e);
        }
    }

    /**
     * Checks whether the name of the associated {@link IndexingSource} or name of the index has been changed.
     * 
     * @param name
     *            new name of the SemanticIndex
     * @param indexingSourceName
     *            new name of the {@link IndexingSource} associated with this index
     * @param oldMetadata
     *            old metadata of the SemanticIndex
     * @return {@code true} if the LDPath program has changed, otherwise {@code false}
     * @throws ConfigurationException
     */
    protected void checkUnmodifiableConfigurations(String name,
                                                   String indexingSourceName,
                                                   java.util.Properties oldMetadata) throws ConfigurationException {

        // name of the semantic index has changed
        if (!name.equals(oldMetadata.get(SemanticIndex.PROP_NAME))) {
            throw new ConfigurationException(SemanticIndex.PROP_NAME,
                    "It is not allowed to change the name of a Semantic Index");
        }

        // name of the indexing source has changed
        if (!indexingSourceName.equals(oldMetadata.get(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME))) {
            throw new ConfigurationException(AbstractSemanticIndex.PROP_INDEXING_SOURCE_NAME,
                    "For the time being, it is not allowed to change the name of the indexing source.");
        }
    }

    /**
     * Checks the reindexing condition by comparing the new current LDPath program of the index with the older
     * one
     * 
     * @param ldPath
     *            new LDPath program of the SemanticIndex
     * @param oldMetadata
     *            old metadata of the SemanticIndex
     * @return {@code true} if the LDPath has changed, otherwise {@code false}
     */
    protected boolean checkReindexingCondition(String ldPath, java.util.Properties oldMetadata) {
        // ldpath of the semantic has changed, reindexing needed
        if (!ldPath.equals(oldMetadata.get(AbstractLDPathSemanticIndex.PROP_LD_PATH_PROGRAM))) {
            return true;
        }
        return false;
    }

    /**
     * This method executes the LDPath program, which was used to configure this index, on the enhancements of
     * submitted content by means of the Entityhub. In other words, additional information is gathered from
     * the Entityhub for each entity detected in the enhancements by querying the ldpath of this index.
     * Furthermore, the same LDPath is also executed on the given {@link ContentItem} through the
     * {@link ContentItemBackend}.
     * 
     * @param contexts
     *            a {@link Set} of URIs (string representations) that are used as starting nodes to execute
     *            LDPath program of this index. The context are the URIs of the entities detected in the
     *            enhancements of the content submitted.
     * @param ci
     *            {@link ContentItem} on on which the LDPath associated with this index will be executed
     * @return the {@link Map} containing the results obtained by executing the given program on the given
     *         contexts. Keys of the map corresponds to fields in the program and values of the map
     *         corresponds to results obtained for the field specified in the key.
     * @throws IndexManagementException
     */
    protected Map<String,Collection<?>> executeProgram(Set<String> contexts, ContentItem ci) throws IndexManagementException {
        Map<String,Collection<?>> results = new HashMap<String,Collection<?>>();

        // execute the given LDPath for each context passed in contexts parameter
        SiteManagerBackend backend = new SiteManagerBackend(siteManager);
        ValueFactory vf = InMemoryValueFactory.getInstance();
        EntityhubLDPath entityhubPath = new EntityhubLDPath(backend, vf);
        Representation representation;
        for (String context : contexts) {
            representation = entityhubPath.execute(vf.createReference(context), this.objectProgram);
            Iterator<String> fieldNames = representation.getFieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                Iterator<Object> valueIterator = representation.get(fieldName);
                Set<Object> values = new HashSet<Object>();
                while (valueIterator.hasNext()) {
                    values.add(valueIterator.next());
                }
                if (results.containsKey(fieldName)) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> resultCollection = (Collection<Object>) results.get(fieldName);
                    Collection<Object> tmpCol = (Collection<Object>) values;
                    for (Object o : tmpCol) {
                        resultCollection.add(o);
                    }
                } else {
                    results.put(fieldName, values);
                }
            }
        }

        // execute the LDPath on the given ContentItem
        ContentItemBackend contentItemBackend = new ContentItemBackend(ci, true);
        LDPath<Resource> resourceLDPath = new LDPath<Resource>(contentItemBackend, EnhancerLDPath.getConfig());
        Program<Resource> resourceProgram;
        try {
            resourceProgram = resourceLDPath.parseProgram(new StringReader(this.ldPathProgram));
            Map<String,Collection<?>> ciBackendResults = resourceProgram.execute(contentItemBackend,
                ci.getUri());
            for (Entry<String,Collection<?>> result : ciBackendResults.entrySet()) {
                if (result.getValue().isEmpty()) {
                    continue;
                } else if (results.containsKey(result.getKey())) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> resultsValue = (Collection<Object>) results.get(result.getKey());
                    resultsValue.addAll(result.getValue());
                } else {
                    results.put(result.getKey(), result.getValue());
                }

            }
        } catch (LDPathParseException e) {
            logger.error("Failed to create Program<Resource> from the LDPath program", e);
        }

        return results;
    }

}
