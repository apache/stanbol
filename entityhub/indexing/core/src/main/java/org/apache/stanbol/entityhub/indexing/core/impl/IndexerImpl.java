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
package org.apache.stanbol.entityhub.indexing.core.impl;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.INDEXING_COMPLETED_QUEUE_ITEM;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.SCORE_FIELD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.indexing.core.Indexer;
import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;
import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.indexing.core.event.IndexingEvent;
import org.apache.stanbol.entityhub.indexing.core.event.IndexingListener;
import org.apache.stanbol.entityhub.indexing.core.impl.IndexingDaemon.IndexingDaemonEventObject;
import org.apache.stanbol.entityhub.indexing.core.impl.IndexingDaemon.IndexingDaemonListener;
import org.apache.stanbol.entityhub.indexing.core.impl.IndexingSourceInitialiser.IndexingSourceEventObject;
import org.apache.stanbol.entityhub.indexing.core.impl.IndexingSourceInitialiser.IndexingSourceInitialiserListener;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.indexing.core.processor.EmptyProcessor;
import org.apache.stanbol.entityhub.indexing.core.source.LineBasedEntityIterator;
import org.apache.stanbol.entityhub.indexing.core.source.YardEntityDataProvider;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Abstract Implementation of the Indexer. <p>
 * Principally there are two ways how to index entities <ol>
 * <li> Iterate of the the entityIds/scores by using an {@link EntityIterator}
 * and lookup the data by using an {@link EntityDataProvider}.
 * <li> Iterate over the data by using an {@link EntityDataIterator} (provided
 * by an {@link EntityDataIterable}) and lookup/calculate the scores by using an 
 * {@link EntityScoreProvider}.
 * </ol>
 * This Implementation provides a static createInstance(..) method for each of
 * the two variants.<p>
 * After the Entities are loaded from the source they are processed by using the
 * configured {@link EntityProcessor}. Finally the processed entities are
 * persisted in a {@link Yard}. 
 * @author Rupert Westenthaler
 *
 */
public class IndexerImpl implements Indexer {
    //protected to allow internal classes direct access!
    protected static final Logger log = LoggerFactory.getLogger(IndexerImpl.class);

    /**
     * Holds the indexing listener
     */
    private final Set<IndexingListener> listeners;

    private int chunkSize;
    public static final int MIN_QUEUE_SIZE = 500;
    
    private boolean indexAllEntitiesState = false;
    
    //entityMode
    private EntityIterator entityIterator;
    private EntityDataProvider dataProvider;
    //dataMode
    private EntityDataIterable dataIterable;
    private EntityScoreProvider scoreProvider;
    private final Collection<IndexingComponent> indexingComponents;
    
    private final IndexingDestination indexingDestination;
    private final List<EntityProcessor> entityProcessors;
    private final ScoreNormaliser scoreNormaliser;
    
    private State state = State.UNINITIALISED;
    private final Object stateSync = new Object();
    /**
     * The file to store the IDs of indexed entities. Might be <code>null</code>
     * if {@link #entityPostProcessors} is <code>null</code>.
     */
    private File indexedEntityIdFile;
    /**
     * The {@link EntityProcessor}s used for the post processing phase or
     * <code>null</code> if none.<p>
     * If NOT <code>null</code> {@link #indexedEntityIdFile} is also
     * guaranteed to be NET <code>null</code>!
     */
    private List<EntityProcessor> entityPostProcessors;

    private OutputStream indexedEntityIdOutputStream;
    /**
     * The name of the index this indexer creates (used for logging)
     */
    private String name;
    
    public IndexerImpl(String name, 
                       EntityIterator entityIterator,
                       EntityDataProvider dataProvider,
                       ScoreNormaliser normaliser,
                       IndexingDestination indexingDestination, 
                       List<EntityProcessor> entityProcessors,
                       File indexedEntityIdFile,
                       List<EntityProcessor> entityPostProcessors){
        this(name, normaliser,indexingDestination,entityProcessors,indexedEntityIdFile,entityPostProcessors);
        //set entityMode interfaces
        if(entityIterator == null){
            throw new IllegalArgumentException("The EntityIterator MUST NOT be NULL!");
        }
        this.entityIterator = entityIterator;
        this.dataProvider = dataProvider;
        //add the parsed indexingSources to the list
        this.indexingComponents.add(entityIterator);
        this.indexingComponents.add(dataProvider);
    }
    public IndexerImpl(String name,
                       EntityDataIterable dataIterable, 
                       EntityScoreProvider scoreProvider, 
                       ScoreNormaliser normaliser,
                       IndexingDestination indexingDestination, 
                       List<EntityProcessor> entityProcessors,
                       File indexedEntityIdFile,
                       List<EntityProcessor> entityPostProcessors){
        this(name, normaliser,indexingDestination,entityProcessors,indexedEntityIdFile,entityPostProcessors);
        //deactivate entityMode interfaces
        this.entityIterator = null;
        if(scoreProvider == null){
            throw new IllegalArgumentException("The EntityScoreProvider MUST NOT be NULL!");
        }
        this.scoreProvider = scoreProvider;
        this.dataIterable = dataIterable;
        //add the parsed indexingSources to the list
        this.indexingComponents.add(scoreProvider);
        this.indexingComponents.add(dataIterable);
    }
    
    protected IndexerImpl(String name, 
                          ScoreNormaliser normaliser,
                          IndexingDestination indexingDestination, 
                          List<EntityProcessor> entityProcessors,
                          File indexedEntityIdFile,
                          List<EntityProcessor> entityPostProcessors){
        this.name = name;
        if(indexingDestination == null){
            throw new IllegalArgumentException("The Yard MUST NOT be NULL!");
        }
        this.indexingDestination = indexingDestination;
        if(entityProcessors == null){
            this.entityProcessors = Collections.singletonList((EntityProcessor)new EmptyProcessor());
        } else {
            this.entityProcessors = entityProcessors;
        }
        setChunkSize(DEFAULT_CHUNK_SIZE); //init the chunk size and the cache
        this.scoreNormaliser = normaliser;
        indexingComponents = new ArrayList<IndexingComponent>();
        indexingComponents.add(indexingDestination);
        indexingComponents.addAll(entityProcessors);
        listeners = new HashSet<IndexingListener>();
        this.indexedEntityIdFile = indexedEntityIdFile;
        if(entityPostProcessors != null){
            if(entityPostProcessors.isEmpty()){
                this.entityPostProcessors = null;
            } else {
                this.entityPostProcessors = entityPostProcessors;
                if(indexedEntityIdFile == null){
                    throw new IllegalArgumentException("The file used to store" +
                    		"the IDs of indexed Entities MUST NOT be NULL if" +
                    		"EntityPostProcessors are defined!");
                }
            }
        } else {
            this.entityPostProcessors = null;
        }
    }
    public boolean addIndexListener(IndexingListener listener){
        if(listener != null){
            synchronized (listeners) {
                return listeners.add(listener);
            }
        } else {
            return false;
        }
    }
    public boolean removeIndexListener(IndexingListener listener){
        if(listener != null){
            synchronized (listeners) {
                return listeners.remove(listener);
            }
        } else {
            return false;
        }
    }
    protected void fireStateChanged(){
        IndexingEvent event = new IndexingEvent(this);
        Collection<IndexingListener> copy = new ArrayList<IndexingListener>(listeners.size());
        synchronized (listeners) {
            copy.addAll(listeners);
        }        
        for(IndexingListener listener : copy){
            listener.stateChanged(event);
            //if the state is finished also send the completed event
            if(getState() == State.FINISHED){
                listener.indexingCompleted(event);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.indexing.core.IndexerInterface#setChunkSize(int)
     */
    public void setChunkSize(int chunkSize) throws IllegalStateException {
        if(getState().ordinal() >= State.INDEXING.ordinal()){
            throw new IllegalStateException("Setting the chunkSize is only allowed before starting the indexing process!");
        }
        if(chunkSize <= 0){
            chunkSize = DEFAULT_CHUNK_SIZE;
        }
        this.chunkSize = chunkSize;
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.indexing.core.IndexerInterface#getChunkSize()
     */
    public int getChunkSize() {
        return chunkSize;
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.indexing.core.IndexerInterface#getYard()
     */
    public Yard getYard() {
        return indexingDestination.getYard();
    }
    @Override
    public void initialiseIndexing() {
        synchronized (stateSync) { //ensure that two threads do not start the
            //initialisation at the same time ...
            if(getState() != State.UNINITIALISED){
                return;
            }
            setState(State.INITIALISING);
            log.info("{}: initialisation started ...", name);
        }
        //add all IndexingSources that need to be initialised to a set
        final Collection<IndexingComponent> toInitialise = new HashSet<IndexingComponent>();
        //we need an simple listener that removes the IndexingSerouces from the
        //above list
        final IndexingSourceInitialiserListener listener = new IndexingSourceInitialiserListener() {
            @Override
            public void indexingSourceInitialised(IndexingSourceEventObject eventObject) {
                //remove the IndexingSource from the toInitialise set
                synchronized (toInitialise) {
                    toInitialise.remove(eventObject.getIndexingSource());
                    if(toInitialise.isEmpty()){ //if no more left to initialise
                        //notify others about it
                        toInitialise.notifyAll();
                    }
                }
                //finally remove this listener
                eventObject.getSource().removeIndexingSourceInitialiserListener(this);
            }
        };
        //now create the IndexingSourceInitialiser that initialise the
        //Indexing Sources in their own Thread
        for(IndexingComponent source : indexingComponents){
            if(source.needsInitialisation()){ //if it need to be initialised
                toInitialise.add(source); // add it to the list
                //create an initialiser
                IndexingSourceInitialiser initialiser = new IndexingSourceInitialiser(source);
                //add the listener
                initialiser.addIndexingSourceInitialiserListener(listener);
                //create and init the Thread
                Thread thread = new Thread(initialiser);
                thread.setDaemon(true);
                thread.start();
            } //else no initialisation is needed
        }
        //now wait until all IndexingSources are initialised!
        while(!toInitialise.isEmpty()){
            synchronized (toInitialise) {
                if(!toInitialise.isEmpty()){
                    try {
                        toInitialise.wait();
                    } catch (InterruptedException e) {
                        //year looks like all IndexingSources are initialised!
                    }
                }
            }
        }
        //initialise the stream used to write the ids of indexed entities
        try {
            indexedEntityIdOutputStream = getEntityIdFileOutputStream();
        } catch (IOException e) {
            if(entityPostProcessors != null){
                throw new IllegalStateException("Unable to open stream for writing" +
                        "the IDs of indexed Entities as required for post-" +
                        "processing!",e);
            } else {
                log.warn("Unable to open stream for writing the Ids of indexed " +
                        "Entities -> indexes entity Ids will not be available!",e);
            }
        }

        log.info("initialisation completed for {}", name);
        setState(State.INITIALISED);
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.indexing.core.IndexerInterface#index()
     */
    public void index(){
        Set<State> supportedStates = EnumSet.of(
            State.UNINITIALISED,State.INITIALISED,State.INDEXED,State.FINISHED);
        //this is only used to inform about wrong usage. It does not ensure
        //that index is called twice by different threads. This check is done
        //within the initialise, index and finalise methods!
        State state = getState();
        if(!supportedStates.contains(state)){
            throw new IllegalStateException(String.format(
                "Calling this Method is not supported while in State %s! Supported States are ",
                state,supportedStates));
        }
        log.info("{}: start initialisation ...", name);
        initialiseIndexing();
        log.info("  ... initialisation completed for {}", name);
        //if now the state is an unsupported one it indicates that
        //initialiseIndexingSources() was called by an other thread before this one!
        state = getState(); 
        if(!supportedStates.contains(state)){
            throw new IllegalStateException(String.format(
                "Calling this Method is not supported while in State %s! Supported States are ",
                state,supportedStates));
        }
        log.info("{}: start indexing ...", name);
        indexEntities();
        log.info("  ... indexing completed for {}", name);
        log.info("{}: start post-processing ...", name);
        postProcessEntities();
        log.info("  ... post-processing finished for {}",name);
        log.info("{}: start finalisation....", name);
        finaliseIndexing();
        log.info("  ... finalisation completed for {}", name);
    }
    @Override
    public void skipPostProcessEntities() {
        synchronized (stateSync) { //ensure that two threads do not start the
            //initialisation at the same time ...
            State state = getState();
            if(state.ordinal() < State.INDEXED.ordinal()){
                throw new IllegalStateException("The Indexer MUST BE already "+State.INDEXED+" when calling this Method!");
            }
            if(state == State.POSTPROCESSING){ //if state > INITIALISED
                throw new IllegalStateException("Unable to skip post processing if postprocessing is already in progress!");
            }
            if(state.ordinal() >= State.POSTPROCESSED.ordinal()){
                return; //already post processed
            }
            setState(State.POSTPROCESSED);
            log.info("Skiped postprocessing ...");
        }
    }
    @Override
    public void postProcessEntities() {
        synchronized (stateSync) { //ensure that two threads do not start the
            //initialisation at the same time ...
            State state = getState();
            if(state.ordinal() < State.INDEXED.ordinal()){
                throw new IllegalStateException("The Indexer MUST BE already "+State.INDEXED+" when calling this Method!");
            }
            if(state != State.INDEXED){ //if state > INITIALISED
                return; // ignore this call
            }
            setState(State.POSTPROCESSING);
            log.info("{}: PostProcessing started ...", name);
        }
        if(entityPostProcessors == null || entityPostProcessors.isEmpty()){
            setState(State.POSTPROCESSED);
            return; //nothing to do
        }
        //init the post processing components
        //use an EntityDataProvider based on the indexed data
        EntityDataProvider dataProvider = new YardEntityDataProvider(indexingDestination.getYard());
        //use an LineBasedEntityIterator to iterate over the indexed entity ids
        EntityIterator entityIterator;
        try {
            entityIterator = new LineBasedEntityIterator(getEntityIdFileInputStream(),"UTF-8",null);
        }  catch (IOException e) {
            throw new IllegalStateException("Unable to open file containing the " +
            		"IDs of the indexed Entities!",e);
        }
        Map<String,Object> config = new HashMap<String,Object>();
        config.put(LineBasedEntityIterator.PARAM_ID_POS, 1);
        config.put(LineBasedEntityIterator.PARAM_SCORE_POS, Integer.MAX_VALUE);
        entityIterator.setConfiguration(config);
        //init the post-processors (this time not in an own thread as this
        //does not really make sense for processors
        for(EntityProcessor processor : entityPostProcessors){
            if(processor.needsInitialisation()){
                processor.initialise();
            }
        }
        //NOTE the destination needs not to be initialised -> it will be the same
        //as for indexing!

        //initialisation complete ... now setup the poet processing
        //init the queues
        int queueSize = Math.max(MIN_QUEUE_SIZE, chunkSize*2);
        BlockingQueue<QueueItem<Representation>> indexedEntityQueue = 
                new ArrayBlockingQueue<QueueItem<Representation>>(queueSize);
        BlockingQueue<QueueItem<Representation>> processedEntityQueue = 
                new ArrayBlockingQueue<QueueItem<Representation>>(queueSize);
        BlockingQueue<QueueItem<Representation>> finishedEntityQueue = 
                new ArrayBlockingQueue<QueueItem<Representation>>(queueSize);
        BlockingQueue<QueueItem<IndexingError>> errorEntityQueue = 
                new ArrayBlockingQueue<QueueItem<IndexingError>>(queueSize);

        //Set holding all active post processing deamons
        final SortedSet<IndexingDaemon<?,?>> activeIndexingDeamons = 
            new TreeSet<IndexingDaemon<?,?>>();
        //create the IndexingDaemos
        //TODO: Here we would need to create multiple instances in case
        //      one would e.g. like to use several threads for processing entities
        //(1) the daemon reading from the IndexingSources
        String entitySourceReaderName = name + ": post-processing: Entity Reader Deamon";
        activeIndexingDeamons.add(
            new EntityIdBasedIndexingDaemon(
                entitySourceReaderName,
                indexedEntityQueue, errorEntityQueue, 
                entityIterator, 
                dataProvider, 
                null, //no score normaliser
                true)); //post-process all indexed entities
        //(2) The daemon for post-processing the entities
        activeIndexingDeamons.add(
            new EntityProcessorRunnable(
                name +": post-processing: Entity Processor Deamon",
                indexedEntityQueue, //it consumes indexed Entities
                processedEntityQueue,  //it produces processed Entities
                errorEntityQueue,
                entityPostProcessors, 
                //TODO: check that the score is not overriden by the NULL
                //      parsed by the used LineBasedEntityIterator!
                Collections.singleton(SCORE_FIELD))); //ensure the score not changed
        //(3) The daemon for persisting the entities
        activeIndexingDeamons.add(
            new EntityPersisterRunnable(
                name + ": Entity Perstisting Deamon",
                processedEntityQueue, //it consumes processed Entities
                finishedEntityQueue, //it produces finished Entities
                errorEntityQueue,
                chunkSize, indexingDestination.getYard()));
        //(4) The daemon for logging finished entities
        activeIndexingDeamons.add(
            new FinishedEntityDaemon(
                name + ": Finished Entity Logger Deamon",
                finishedEntityQueue, -1, log, 
                null)); //we have already all entity ids!
        //(5) The daemon for logging errors
        activeIndexingDeamons.add(
            new EntityErrorLoggerDaemon(
                name +": Entity Error Logging Daemon",
                errorEntityQueue, log));
        //start post-processing and wait until it has finished
        startAndWait(activeIndexingDeamons);        
        //close all post processors
        for(EntityProcessor ep : entityPostProcessors){
            ep.close();
        }

        
        setState(State.POSTPROCESSED);
    }
    /**
     * Internally used to start the indexing/post-processing daemons and wait
     * until they have finished.
     * @param activeIndexingDeamons the deamos to start
     */
    private void startAndWait(final SortedSet<IndexingDaemon<?,?>> activeIndexingDeamons) {
        //We need an listener for the IndexingDaemons we are about to start!
        final IndexingDaemonListener listener = new IndexingDaemonListener() {
            @Override
            public void indexingDaemonFinished(IndexingDaemonEventObject indexingDaemonEventObject) {
                //looks like one has finished
                IndexingDaemon<?,?> indexingDaemon = indexingDaemonEventObject.getSource();
                //handle the finished indexing daemon
                handleFinishedIndexingDaemon(activeIndexingDeamons, indexingDaemon);
                //finally remove the listener
                indexingDaemon.removeIndexingDaemonListener(this);

            }
        };
        //now start the IndexingDaemons in their own Threads
        Set<IndexingDaemon<?,?>> deamonCopy = 
            new HashSet<IndexingDaemon<?,?>>(activeIndexingDeamons);
        for(IndexingDaemon<?,?> deamon : deamonCopy){
            deamon.addIndexingDaemonListener(listener); //add the listener
            Thread thread = new Thread(deamon);// create the thread
            thread.setDaemon(true); //ensure that the JVM can terminate
            thread.setName(deamon.getName()); // set the name of the thread
            thread.start(); //start the Thread
        }
        //now we need to wait until all Threads have finished ...
        while(!activeIndexingDeamons.isEmpty()){
            synchronized (activeIndexingDeamons) {
                try {
                    activeIndexingDeamons.wait();
                } catch (InterruptedException e) {
                    //year ... looks like we are done
                }
            }
        }
        //done!
    }

    @Override
    public void finaliseIndexing() {
        synchronized (stateSync) { //ensure that two threads do not start the
            //initialisation at the same time ...
            State state = getState();
            if(state.ordinal() < State.POSTPROCESSED.ordinal()){
                throw new IllegalStateException("The Indexer MUST BE already "+State.POSTPROCESSED+" when calling this Method!");
            }
            if(state != State.POSTPROCESSED){ //if state > POSTPROCESSED
                return; // ignore this call
            }
            setState(State.FINALISING);
            log.info("{}: finalisation started ...",name);
        }
        indexingDestination.finalise();
        //close the source and the destination
        if(entityIterator != null){
            entityIterator.close();
        }
        if(dataIterable != null){
            dataIterable.close();
        }
        if(dataProvider != null){
            dataProvider.close();
        }
        if(scoreProvider != null){
            scoreProvider.close();
        }
        setState(State.FINISHED);
    }

    @Override
    public void skipIndexEntities() {
        synchronized (stateSync) { //ensure that two threads do not start the
            //initialisation at the same time ...
            State state = getState();
            if(state.ordinal() < State.INITIALISED.ordinal()){
                throw new IllegalStateException("The Indexer MUST BE already "+State.INITIALISED+" when calling this Method!");
            }
            if(state == State.INDEXING){ 
                throw new IllegalStateException("Unable to skip indexing if indexing is already in progress!");
            }
            if(state.ordinal() >= State.INDEXED.ordinal()){ //if state > INDEXING
                return; //already in INDEXED state
            }
            setState(State.INDEXED);
            log.info("Indexing of Entities skipped ...");
        }
    }
    @Override
    public void indexEntities() {
        synchronized (stateSync) { //ensure that two threads do not start the
            //initialisation at the same time ...
            State state = getState();
            if(state.ordinal() < State.INITIALISED.ordinal()){
                throw new IllegalStateException("The Indexer MUST BE already "+State.INITIALISED+" when calling this Method!");
            }
            if(state != State.INITIALISED){ //if state > INITIALISED
                return; // ignore this call
            }
            setState(State.INDEXING);
            log.info("{}: indexing started ...",name);
        }
        //init the queues
        int queueSize = Math.max(MIN_QUEUE_SIZE, chunkSize*2);
        BlockingQueue<QueueItem<Representation>> indexedEntityQueue = 
                new ArrayBlockingQueue<QueueItem<Representation>>(queueSize);
        BlockingQueue<QueueItem<Representation>> processedEntityQueue = 
                new ArrayBlockingQueue<QueueItem<Representation>>(queueSize);
        BlockingQueue<QueueItem<Representation>> finishedEntityQueue = 
                new ArrayBlockingQueue<QueueItem<Representation>>(queueSize);
        BlockingQueue<QueueItem<IndexingError>> errorEntityQueue = 
                new ArrayBlockingQueue<QueueItem<IndexingError>>(queueSize);
        
        //Set holding all active IndexingDaemons
        final SortedSet<IndexingDaemon<?,?>> activeIndexingDeamons = 
            new TreeSet<IndexingDaemon<?,?>>();
        //create the IndexingDaemos
        //TODO: Here we would need to create multiple instances in case
        //      one would e.g. like to use several threads for processing entities
        //(1) the daemon reading from the IndexingSources
        String entitySourceReaderName = name +": Entity Source Reader Deamon";
        if(entityIterator != null){
            activeIndexingDeamons.add(
                new EntityIdBasedIndexingDaemon(
                    entitySourceReaderName,
                    indexedEntityQueue, errorEntityQueue, 
                    entityIterator, 
                    dataProvider, 
                    scoreNormaliser,
                    indexAllEntitiesState));
        } else {
            activeIndexingDeamons.add(
                new EntityDataBasedIndexingDaemon(
                    entitySourceReaderName,
                    indexedEntityQueue, errorEntityQueue, 
                    dataIterable, 
                    scoreProvider, 
                    scoreNormaliser,
                    indexAllEntitiesState));
        }
        //(2) The daemon for processing the entities
        activeIndexingDeamons.add(
            new EntityProcessorRunnable(
                name +": Entity Processor Deamon",
                indexedEntityQueue, //it consumes indexed Entities
                processedEntityQueue,  //it produces processed Entities
                errorEntityQueue,
                entityProcessors, 
                Collections.singleton(SCORE_FIELD)));
        //(3) The daemon for persisting the entities
        activeIndexingDeamons.add(
            new EntityPersisterRunnable(
                name + ": Entity Perstisting Deamon",
                processedEntityQueue, //it consumes processed Entities
                finishedEntityQueue, //it produces finished Entities
                errorEntityQueue,
                chunkSize, indexingDestination.getYard()));
        //(4) The daemon for logging finished entities
        activeIndexingDeamons.add(
            new FinishedEntityDaemon(
                name + ": Finished Entity Logger Deamon",
                finishedEntityQueue, -1, log, indexedEntityIdOutputStream));
        //(5) The daemon for logging errors
        activeIndexingDeamons.add(
            new EntityErrorLoggerDaemon(
                name +": Entity Error Logging Daemon",
                errorEntityQueue, log));
        //start indexing and wait until it has finished
        startAndWait(activeIndexingDeamons);
        //close the stream with IDs
        IOUtils.closeQuietly(indexedEntityIdOutputStream);
        //call close on all indexing components
        for(EntityProcessor ep : entityProcessors){
            ep.close();
        }
        //set the new state to INDEXED
        setState(State.INDEXED);
    }
    /**
     * Handles the necessary actions if an {@link IndexingDaemon} used for the
     * work done within {@link #indexEntities()} completes its work (meaning
     * it has executed all entities).<p>
     * The parsed SortedSet is created within  {@link #indexEntities()} and 
     * contains all {@link IndexingDaemon}s that have not yet finished. It is 
     * the responsibility of this method to remove finished 
     * {@link IndexingDaemon}s from this set.<p>
     * In addition this Method is responsible to {@link BlockingQueue#put(Object)}
     * the {@link IndexerConstants#INDEXING_COMPLETED_QUEUE_ITEM} to the
     * consuming queue of {@link IndexingDaemon}s as soon as all 
     * {@link IndexingDaemon}s of the previous indexing steps have finished.
     * This is checked comparing the {@link IndexingDaemon#getSequence()} number
     * of the first entry within the sorted activeIndexingDeamons set.
     * If the sequence number of the first element has increased after the
     * finished {@link IndexingDaemon} was removed this Method puts the
     * {@link IndexerConstants#INDEXING_COMPLETED_QUEUE_ITEM} item to the
     * consuming queue of the new first entry of activeIndexingDeamons. 
     * @param activeIndexingDeamons The SortedSet containing all 
     * {@link IndexingDaemon}s that are still active.
     * @param indexingDaemon the {@link IndexingDaemon} that completed its work.
     */
    private void handleFinishedIndexingDaemon(final SortedSet<IndexingDaemon<?,?>> activeIndexingDeamons,
                                              IndexingDaemon<?,?> indexingDaemon) {
        log.info("{} completed (sequence={}) ... ",
            indexingDaemon.getName(), indexingDaemon.getSequence());
        IndexingDaemon<?,?> sendEndofQueue = null;
        synchronized (activeIndexingDeamons) {
            if(log.isDebugEnabled()){
                log.info(" Active Indexing Deamons:");
                for(IndexingDaemon<?,?> active : activeIndexingDeamons){
                    log.info(" > {} {}",active.getSequence(),active.getName());
                }
            }
            //get the SequenceNumber of the first Element
            if(!activeIndexingDeamons.isEmpty()){
                Integer sequenceNumber = activeIndexingDeamons.first().getSequence();
                log.info(" > current sequence : {}",sequenceNumber);
                //try to remove it from the activeDeamons list
                activeIndexingDeamons.remove(indexingDaemon);
                if(activeIndexingDeamons.isEmpty()){ //if no active is left
                    log.debug("  - indexingDeamons list now emoty ... notifyAll to indicate indexing has completed!");
                    activeIndexingDeamons.notifyAll(); //notify all others!
                } else { //check new SequenceNumber
                    IndexingDaemon<?,?> first = activeIndexingDeamons.first();
                    if(sequenceNumber.compareTo(first.getSequence()) < 0){
                        log.info(" > new sequence: {}",first.getSequence());
                        //sequence number increased -> 
                        // ... all Daemons for the step have completed
                        // ... send EndOfQueue
                        // ... but outside of the synchronized block!
                        sendEndofQueue = first;
                    }
                }
            } //already empty ... nothing todo
        }
        if(sendEndofQueue != null){ //send endOfQueue
            //to the consuming Queue of this one
            try {
                //ignore the Type safety because the item is of
                //INDEXING_COMPLETED_QUEUE_ITEM is anyway null
                log.info("Send end-of-queue to Deamons with Sequence "+sendEndofQueue.getSequence());
                sendEndofQueue.getConsumeQueue().put(INDEXING_COMPLETED_QUEUE_ITEM);
            } catch (InterruptedException e) {
                log.error("Interupped while sending EnodOfQueue Item to consuming queue of "+sendEndofQueue.getName(),e);
            }
        }
    }        
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.indexing.core.IndexerInterface#setIndexAllEntitiesState(boolean)
     */
    public void setIndexAllEntitiesState(boolean indexAllEntitiesState) {
        this.indexAllEntitiesState = indexAllEntitiesState;
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.indexing.core.IndexerInterface#isIndexAllEntitiesState()
     */
    public boolean isIndexAllEntitiesState() {
        return indexAllEntitiesState;
    }
    /**
     * Setter for the state. <p>
     * Implementation Note: This setter is synchronised to the sync object for
     * the state
     * @param state the state to set
     */
    private void setState(State state) {
        boolean changed;
        synchronized (stateSync) {
            changed = state != this.state;
            if(changed){
                this.state = state;
            }
        }
        if(changed){ //do not fire events within synchronized blocks ...
            fireStateChanged();
        }
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.indexing.core.IndexerInterface#getState()
     */
    public State getState() {
        return state;
    }
    /**
     * Opens a stream to read data from the {@link #indexedEntityIdFile}. 
     * Can only be called in {@link State}s earlier that {@link State#INDEXING}.
     * @return the stream
     * @throws IOException on any error while opening the stream
     * @throws IllegalStateException if {@link #getState()} is later than
     * {@link State#INITIALISED}
     */
    protected OutputStream getEntityIdFileOutputStream() throws IOException {
        if(indexedEntityIdFile == null){
            return null;
        }
        State state = getState();
        if(state.ordinal() > State.INITIALISED.ordinal()){
            throw new IllegalStateException("Opening an OutputStrem to the "
                    + "indexed entity id file '"+indexedEntityIdFile+"' is not "
                    + "allowed for states > "+State.INITIALISED +" (current: "+state+")!");
        }
        if(indexedEntityIdFile.isFile()){//exists
            log.info(" ... delete existing IndexedEntityId file "+indexedEntityIdFile);
            indexedEntityIdFile.delete(); //delete existing data
        }
        //support compression
        String extension = FilenameUtils.getExtension(indexedEntityIdFile.getName());
        OutputStream out = new FileOutputStream(indexedEntityIdFile);
        if("zip".equalsIgnoreCase(extension)){
            out = new ZipOutputStream(out);
            ((ZipOutputStream)out).putNextEntry(new ZipEntry("entity-ids.txt"));
        } else if("gz".equalsIgnoreCase(extension)) {
            out = new GZIPOutputStream(out);
        } else if("bz2".equalsIgnoreCase(extension)) {
            out = new BZip2CompressorOutputStream(out);
        }
        return out;
    }
    /**
     * Opens a stream to read data from the {@link #indexedEntityIdFile}. 
     * Can only be called in {@link State}s later that {@link State#INDEXED}.
     * @return the stream
     * @throws IOException on any error while creating the stream
     * @throws IllegalStateException if {@link #getState()} is earlier than
     * {@link State#INDEXED}
     */
    protected InputStream getEntityIdFileInputStream() throws IOException {
        if(indexedEntityIdFile == null){
            return null;
        }
        State state = getState();
        if(state.ordinal() < State.INDEXED.ordinal()){
            throw new IllegalStateException("The indexed entity id data is not" +
            		"available for states < "+State.INDEXED +" (current: "+state+")!");
        }
        //support compression
        String extension = FilenameUtils.getExtension(indexedEntityIdFile.getName());
        InputStream in = new FileInputStream(indexedEntityIdFile);
        if("zip".equalsIgnoreCase(extension)){
            in = new ZipInputStream(in);
            ((ZipInputStream)in).getNextEntry();
        } else if("gz".equalsIgnoreCase(extension)) {
            in = new GZIPInputStream(in);
        } else if("bz2".equalsIgnoreCase(extension)) {
            in = new BZip2CompressorInputStream(in);
        }
        return in;
    }

}
