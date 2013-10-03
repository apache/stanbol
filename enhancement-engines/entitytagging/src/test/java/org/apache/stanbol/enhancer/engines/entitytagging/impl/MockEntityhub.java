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
package org.apache.stanbol.enhancer.engines.entitytagging.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.managed.standalone.StandaloneEmbeddedSolrServerProvider;
import org.apache.stanbol.entityhub.core.model.EntityImpl;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubConfiguration;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYardConfig;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mocks an Entityhub for the {@link NamedEntityTaggingEngine} for Unit Testing<p>
 * This loads the dbpedia default data and wraps it as entityhub.
 * @author Rupert Westenthaler
 *
 */
class MockEntityhub implements Entityhub {

    private static final Logger log = LoggerFactory.getLogger(MockEntityhub.class);
    
    public static final String TEST_SOLR_CORE_CONFIGURATION = "dbpedia_26k.solrindex.bz2";
    protected SolrYard yard;
    
    protected MockEntityhub(){
        SolrYardConfig config = new SolrYardConfig("dbpedia", "dbpedia");
        config.setIndexConfigurationName(TEST_SOLR_CORE_CONFIGURATION);
        config.setAllowInitialisation(true);
        IndexReference solrIndexRef = IndexReference.parse(config.getSolrServerLocation());
        SolrServer server = StandaloneEmbeddedSolrServerProvider.getInstance().getSolrServer(
            solrIndexRef, config.getIndexConfigurationName());
        Assert.assertNotNull("Unable to initialise SolrServer for testing",server);
        try {
            yard = new SolrYard(server,config,null);
            Representation paris = yard.getRepresentation("http://dbpedia.org/resource/Paris");
            if(paris == null){
                throw new IllegalStateException("Initialised Yard does not contain the expected resource dbpedia:Paris!");
            }
        } catch (YardException e) {
            throw new IllegalStateException("Unable to init Yard!",e);
        }
    }
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     * Only the following two Methods are used by the EntityLinkingengine
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
     */

    @Override
    public FieldQueryFactory getQueryFactory() {
        return DefaultQueryFactory.getInstance();
    }
    @Override
    public QueryResultList<Entity> findEntities(FieldQuery query) throws EntityhubException {
        log.info("Performing Query: {}",query);
        QueryResultList<Representation> results = yard.findRepresentation(query);
        log.info("  ... {} results",results.size());
        Collection<Entity> entities = new ArrayList<Entity>(results.size());
        for(Representation r : results){
            log.info("    > {}",r.getId());
            entities.add(new EntityImpl("dbpedia", r, null));
        }
        return new QueryResultListImpl<Entity>(results.getQuery(),entities,Entity.class);
    }
    
    // UNUSED METHODS
    
    @Override
    public Yard getYard() {
        return yard;
    }

    @Override
    public Entity lookupLocalEntity(String reference) throws EntityhubException {
        return null;
    }

    @Override
    public Entity lookupLocalEntity(String reference, boolean create) throws IllegalArgumentException,
                                                                     EntityhubException {
        return null;
    }

    @Override
    public Entity getEntity(String entityId) throws IllegalArgumentException, EntityhubException {
        return null;
    }

    @Override
    public Entity importEntity(String reference) throws IllegalStateException,
                                                IllegalArgumentException,
                                                EntityhubException {
        return null;
    }

    @Override
    public Entity getMappingById(String id) throws EntityhubException, IllegalArgumentException {
        return null;
    }

    @Override
    public Entity getMappingBySource(String source) throws EntityhubException {
        return null;
    }

    @Override
    public FieldMapper getFieldMappings() {
        return null;
    }

    @Override
    public Collection<Entity> getMappingsByTarget(String entityId) throws EntityhubException {
        return null;
    }

    @Override
    public QueryResultList<String> findEntityReferences(FieldQuery query) throws EntityhubException {
        return null;
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws EntityhubException {
        return null;
    }

    @Override
    public boolean isRepresentation(String id) throws EntityhubException, IllegalArgumentException {
        return false;
    }

    @Override
    public Entity store(Representation representation) throws EntityhubException, IllegalArgumentException {
        return null;
    }

    @Override
    public Entity delete(String id) throws EntityhubException, IllegalArgumentException {
        return null;
    }

    @Override
    public Entity setState(String id, ManagedEntityState state) throws EntityhubException,
                                                               IllegalArgumentException {
        return null;
    }

    @Override
    public EntityhubConfiguration getConfig() {
        return null;
    }

    @Override
    public void deleteAll() throws EntityhubException {
    }

}
