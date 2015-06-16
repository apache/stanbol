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
package org.apache.stanbol.entityhub.indexing.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants;
import org.apache.stanbol.entityhub.indexing.core.impl.IndexerImpl;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.indexing.core.source.EntityIneratorToScoreProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory used to create {@link Indexer} instances
 * @author Rupert Westenthaler
 *
 */
public final class IndexerFactory {
    private static final Logger log = LoggerFactory.getLogger(IndexerFactory.class);
    /**
     * singleton instance
     */
    private static IndexerFactory instance = null;
    
    /**
     * Singleton constructor
     */
    private IndexerFactory(){
        //Singleton pattern 
    }
    /**
     * Getter for the singleton of this factory
     * @return the singleton
     */
    public static IndexerFactory getInstance(){
        if(instance == null){
            instance = new IndexerFactory();
        }
        return instance;
    }
    /**
     * Creates an {@link Indexer} instance based on the configuration
     * relative to the working directory.<p>
     * The configuration is expected within "{workingdir}/indexing"
     * @return The configured Indexer or an Exception when the configuration was
     * not found or is not valid
     */
    public Indexer create() {
        return create(null);
    }
    /**
     * Creates an {@link Indexer} instance based on the configuration. The 
     * configuration is expected within the "/indexing" directory of the parsed 
     * location.<p>
     * In case a relative path is parsed the current working directory is used
     * as context. That means that the configuration is expected within 
     * folder "{workingDir}/{parsedPath}/indexing". For absolute paths the
     * configuration is expected at "{parsedPath}/indexing".
     * @return The configured Indexer or an Exception when the configuration was
     * not found or is not valid
     */
    public Indexer create(String dir){
        return create(dir, null);
    }
    /**
     * Internally used for unit testing. Allows to parse an offset for loading
     * the indexer configuration from the classpath. Currently a protected
     * feature, but might be moved to the public API at a later point of time.
     * (would allow to include multiple default configurations via the
     * classpath).
     * @param dir
     * @param classpathOffset
     * @return
     */
    protected Indexer create(String dir,String classpathOffset){
        Indexer indexer;
        IndexingConfig config;
        if(classpathOffset != null){
            config= new IndexingConfig(dir,classpathOffset){};
        } else {
            config= new IndexingConfig(dir);
        }
        //get the mode based on the configured IndexingComponents
        String name = config.getName();
        EntityDataIterable dataIterable = config.getDataIterable();
        EntityIterator idIterator = config.getEntityIdIterator();
        EntityDataProvider dataProvider = config.getEntityDataProvider();
        EntityScoreProvider scoreProvider = config.getEntityScoreProvider();
        
        
        IndexingDestination destination = config.getIndexingDestination();
        if(destination == null){
            log.error("The indexing configuration does not provide an " +
                "indexing destination. This needs to be configured by the key " +
                "'{}' in the indexing.properties within the directory {}",
                IndexingConstants.KEY_INDEXING_DESTINATION,config.getConfigFolder());
            throw new IllegalArgumentException("No IndexingDestination present");
        }
        List<EntityProcessor> processors = config.getEntityProcessors();
        if(processors == null){
            log.error("The indexing configuration does not provide an " +
                "entity processor. This needs to be configured by the key " +
                "'{}' in the indexing.properties within the directory {}",
                IndexingConstants.KEY_ENTITY_PROCESSOR,config.getConfigFolder());
        }
        List<EntityProcessor> postProcessors = config.getEntityPostProcessors();
        log.info("Present Source Configuration:");
        log.info(" - EntityDataIterable: {}",dataIterable);
        log.info(" - EntityIterator: {}",idIterator);
        log.info(" - EntityDataProvider: {}",dataProvider);
        log.info(" - EntityScoreProvider: {}",scoreProvider);
        log.info(" - EntityProcessors ({}):",processors.size());
        if(postProcessors != null){
            log.info(" - EntityPostProcessors ({}):",postProcessors.size());
        }
        int i=0;
        for(EntityProcessor processor : processors){
            i++;
            log.info("    {}) {}",i,processor);
        }
        if(dataIterable != null && scoreProvider != null){
            // iterate over data and lookup scores
            indexer = new IndexerImpl(name, dataIterable, scoreProvider, 
                config.getNormaliser(),destination, processors,
                config.getIndexedEntitiesIdsFile(),postProcessors);
        } else if(idIterator != null && dataProvider != null){
            // iterate over id and lookup data
            indexer = new IndexerImpl(name, idIterator,dataProvider,
                config.getNormaliser(),destination, processors,
                config.getIndexedEntitiesIdsFile(),postProcessors);
        } else if(dataIterable != null && idIterator != null){
            // create an EntityIterator to EntityScoreProvider adapter
            log.info(
                "Create Adapter from the configured EntityIterator '{}' to the " +
                "required EntityScoreProvider as needed together with the " +
            	"configured EntityDataIterable '{}'",
            	idIterator.getClass(), dataIterable.getClass());
            indexer = new IndexerImpl(config.getName(), dataIterable,
                new EntityIneratorToScoreProviderAdapter(idIterator),
                config.getNormaliser(),destination, processors,
                config.getIndexedEntitiesIdsFile(),postProcessors);
        } else {
            log.error("Invalid Indexing Source configuration: ");
            log.error(" - To iterate over the data and lookup scores one need to " +
            		"configure an EntityDataIterable and an EntityScoreProvider ");
            log.error(" - To iterate over the Id and and lookup data one need to " +
            "configure an EntityIterator and an EntityDataProvider");
            throw new IllegalArgumentException("Invalid Indexing Source configuration");
        }
        return indexer;
    }

    public Indexer create(String name, EntityIterator idIterator, EntityDataProvider dataProvider,
                          ScoreNormaliser normaliser,
                          List<EntityProcessor> processors, IndexingDestination destination){
        return new IndexerImpl(name, idIterator, dataProvider, normaliser,destination, processors,null,null);
    }
    public Indexer create(String name, EntityIterator idIterator, EntityDataProvider dataProvider,
                          ScoreNormaliser normaliser,
                          List<EntityProcessor> processors, List<EntityProcessor> postProcessors,
                          IndexingDestination destination){
        File tmp;
        try {
            tmp = File.createTempFile("ind-ent-ids",".zip");
            tmp.deleteOnExit();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create temporary file for storing the" +
                    "indexed Entity IDs",e);
        }
        return new IndexerImpl(name, idIterator, dataProvider, normaliser,destination, processors,
            tmp,postProcessors);
    }
    
    public Indexer create(String name, EntityDataIterable dataIterable,EntityScoreProvider scoreProvider,
                          ScoreNormaliser normaliser,
                          List<EntityProcessor> processors, IndexingDestination destination){
        return new IndexerImpl(name, dataIterable, scoreProvider, normaliser,destination, processors,null,null);
    }
    public Indexer create(String name, EntityDataIterable dataIterable,EntityScoreProvider scoreProvider,
                          ScoreNormaliser normaliser,
                          List<EntityProcessor> processors,  List<EntityProcessor> postProcessors,
                          IndexingDestination destination){
        File tmp;
        try {
            tmp = File.createTempFile("ind-ent-ids",".zip");
            tmp.deleteOnExit();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create temporary file for storing the" +
                    "indexed Entity IDs",e);
        }
        return new IndexerImpl(name, dataIterable, scoreProvider, normaliser,destination, processors,
            tmp,postProcessors);
    }
}
