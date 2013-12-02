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
package org.apache.stanbol.enhancer.engines.dereference;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityDereferenceEngine implements EnhancementEngine, ServiceProperties, DereferenceConstants {

    private final Logger log = LoggerFactory.getLogger(EntityDereferenceEngine.class);
    
    /**
     * By default the EntityDereferenceEngine does use {@link ServiceProperties#ORDERING_POST_PROCESSING}
     */
    public static final int DEFAULT_ENGINE_ORDERING = ServiceProperties.ORDERING_POST_PROCESSING;
    
    /**
     * If the offline mode is enabled enforced for dereferencing Entities
     */
    private boolean offline;
    
    protected final EntityDereferencer dereferencer;
    
    protected final DereferenceEngineConfig config;
    
    protected final String name;
    
    protected final boolean filterContentLanguages;
    
    protected final boolean filterAcceptLanguages;
    
    /**
     * The Map holding the {@link #serviceProperties} for this engine.
     */
    protected final Map<String,Object> serviceProperties = new HashMap<String,Object>();
    /**
     * Unmodifiable view over {@link #serviceProperties} returned by
     * {@link #getServiceProperties()}
     */
    private final Map<String,Object> unmodServiceProperties = Collections.unmodifiableMap(serviceProperties);
    
    public EntityDereferenceEngine(EntityDereferencer dereferencer, DereferenceEngineConfig config){
        if(config == null){
            throw new IllegalArgumentException("The parsed DereferenceEngineConfig MUST NOT be NULL!");
        }
        this.config = config;
        this.name = config.getEngineName();
        this.filterContentLanguages = config.isFilterContentLanguages();
        this.filterAcceptLanguages = config.isFilterAcceptLanguages();
        if(dereferencer == null){
            throw new IllegalArgumentException("The parsed EntityDereferencer MUST NOT be NULL!");
        }
        this.dereferencer = dereferencer;
        //init the defautl ordering
        setEngineOrdering(DEFAULT_ENGINE_ORDERING);
    }
    
    /**
     * Setter for the offline mode. This method is typically called of
     * {@link OfflineMode} is injected to the component registering an instance
     * of this Engine implementation
     * @param mode the offline mode
     */
    public void setOfflineMode(boolean mode){
        this.offline = mode;
    }
    
    public boolean isOfflineMode(){
        return offline;
    }
    /**
     * Setter for the {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING
     * engine ordering}.
     * @param ordering The ordering or <code>null</code> to set the 
     * {@value #DEFAULT_ENGINE_ORDERING default} for this engine.
     */
    public void setEngineOrdering(Integer ordering){
        serviceProperties.put(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, 
            ordering == null ? DEFAULT_ENGINE_ORDERING : ordering);
    }
    
    public Integer getEngineOrdering(){
        return (Integer)serviceProperties.get(ENHANCEMENT_ENGINE_ORDERING);
    }

    /**
     * Getter for the config of this engine
     * @return the Dereference Engine Configuration
     */
    public DereferenceEngineConfig getConfig() {
        return config;
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return unmodServiceProperties;
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        if(offline && !dereferencer.supportsOfflineMode()){
            return CANNOT_ENHANCE;
        } else {
            return ENHANCE_ASYNC;
        }
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        if(offline && !dereferencer.supportsOfflineMode()){
            //entity dereferencer does no longer support offline mode
            return;
        }
        log.debug("> dereference Entities for ContentItem {}", ci.getUri());
        final DereferenceContext derefContext = new DereferenceContext(offline);
        Set<String> includedLangs = new HashSet<String>();
        //TODO: parse accept languages as soon as Enhancement properties are implemented
        final MGraph metadata = ci.getMetadata();
        Set<UriRef> referencedEntities = new HashSet<UriRef>();
        //(1) read all Entities we need to dereference from the parsed contentItem
        ci.getLock().readLock().lock();
        try {
            //parse the languages detected for the content
            if(filterContentLanguages){
                for(NonLiteral langAnno : EnhancementEngineHelper.getLanguageAnnotations(metadata)){
                    includedLangs.add(EnhancementEngineHelper.getString(metadata, langAnno, DC_LANGUAGE));
                }
            } //no content language filtering - leave contentLanguages empty
            //parse the referenced entities from the graph
            Iterator<Triple> entityReferences = metadata.filter(null, ENHANCER_ENTITY_REFERENCE, null);
            while(entityReferences.hasNext()){
                Triple triple = entityReferences.next();
                Resource entityReference = triple.getObject();
                if(entityReference instanceof UriRef){
                    boolean added = referencedEntities.add((UriRef)entityReference);
                    if(added && log.isTraceEnabled()){
                        log.trace("  ... schedule Entity {}", entityReference);
                    }
                } else if(log.isWarnEnabled()){
                    //log enhancement that use a fise:entiy-reference with a non UriRef value!
                    NonLiteral enhancement = triple.getSubject();
                    log.warn("Can not dereference invalid Enhancement {}",enhancement);
                    for(Iterator<Triple> it = metadata.filter(enhancement, null, null);it.hasNext();){
                        log.warn("   {}", it.next());
                    }
                }
            }
        } finally {
            ci.getLock().readLock().unlock();
        }
        if(!includedLangs.isEmpty()){
            includedLangs.add(null); //also include literals without language
            //and set the list to the dereference context
            derefContext.setLanguages(includedLangs);
        } //else no filterLanguages set ... nothing to do

        final Lock writeLock = ci.getLock().writeLock();
        log.trace(" - scheduled {} Entities for dereferencing", 
            referencedEntities.size());
        //(2) dereference the Entities
        ExecutorService executor = dereferencer.getExecutor();
        long start = System.currentTimeMillis();
        Set<UriRef> failedEntities = new HashSet<UriRef>();
        int dereferencedCount = 0;
        List<DereferenceJob> dereferenceJobs = new ArrayList<DereferenceJob>(
                referencedEntities.size());
        if(executor != null && !executor.isShutdown()){ //dereference using executor
            //schedule all entities to dereference
            for(final UriRef entity : referencedEntities){
                DereferenceJob dereferenceJob = new DereferenceJob(entity, 
                    metadata, writeLock, derefContext);
                dereferenceJob.setFuture(executor.submit(dereferenceJob));
                dereferenceJobs.add(dereferenceJob);
            }
            //wait for all entities to be dereferenced
            for(DereferenceJob dereferenceJob : dereferenceJobs){
                try {
                    if(dereferenceJob.await()){
                        dereferencedCount++;
                    }
                } catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                    throw new EngineException(this, ci, 
                        "Interupted while waiting for dereferencing Entities", e);
                } catch (ExecutionException e) {
                    if(e.getCause() instanceof DereferenceException){
                        failedEntities.add(dereferenceJob.entity);
                        log.debug(" ... error while dereferencing " 
                            + dereferenceJob.entity + "!", e);
                    } else { //unknown error
                        throw new EngineException(this,ci, "Unchecked Error while "
                            + "dereferencing Entity " + dereferenceJob.entity
                            + "!", e);
                    }
                }
            }
        } else { //dereference using the current thread
            for(UriRef entity : referencedEntities){
                try {
                    log.trace("  ... dereference {}", entity);
                    if(dereferencer.dereference(entity, metadata, writeLock, derefContext)){
                        dereferencedCount++;
                        log.trace("    + success");
                    } else {
                        log.trace("    - not found");
                    }
                } catch (DereferenceException e) {
                    log.debug(" ... error while dereferencing " + entity + "!", e);
                    failedEntities.add(entity);
                }
            }
        }
        long duration = System.currentTimeMillis() - start;
        if(!failedEntities.isEmpty()){
            log.warn(" - unable to dereference {} of {} for ContentItem {}",
                new Object[] {failedEntities.size(),referencedEntities.size(), 
                    ci.getUri()});
        }
        if(log.isDebugEnabled()){
            log.debug(" - dereferenced {} of {} Entities in {}ms ({}ms/dereferenced)", 
                new Object[]{dereferencedCount, referencedEntities.size(),
                    duration, (duration*100/dereferencedCount)/100.0f});
        }
        
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Used both as {@link Callable} submitted to the {@link ExecutorService}
     * and as object to {@link #await()} the completion of the task.
     * @author Rupert Westenthaler
     *
     */
    class DereferenceJob implements Callable<Boolean> {
        
        final UriRef entity;
        final MGraph metadata;
        final Lock writeLock;
        final DereferenceContext derefContext;

        private Future<Boolean> future;
        
        DereferenceJob(UriRef entity, MGraph metadata, Lock writeLock, 
            DereferenceContext derefContext){
            this.entity = entity;
            this.metadata = metadata;
            this.writeLock = writeLock;
            this.derefContext = derefContext;
        }
        
        @Override
        public Boolean call() throws DereferenceException {
            log.trace("  ... dereference {}", entity);
            boolean state = dereferencer.dereference(entity, metadata, writeLock, derefContext);
            if(state){
                log.trace("    + success");
            } else {
                log.trace("    - not found");
            }
            return state;
        }

        void setFuture(Future<Boolean> future){
            this.future = future;
        }
        
        public boolean await() throws InterruptedException, ExecutionException {
            return future.get();
        }
    }
    
}
