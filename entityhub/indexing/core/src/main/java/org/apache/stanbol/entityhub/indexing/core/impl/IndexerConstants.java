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

import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;
import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
/**
 * Interface with the constants used by the {@link IndexerImpl} part of this
 * package. This Constants can be assumed as private and SHOULD NOT be used by
 * other components.
 * @author Rupert Westenthaler
 *
 */
public interface IndexerConstants {
    /**
     * The field used to store the score of an entity if not <code>null</code>
     * and &gt;= 0.
     */
    String SCORE_FIELD = RdfResourceEnum.entityRank.getUri();
    
    /**
     * Key used to store the time when the reading from the source started
     */
    String SOURCE_STARTED = "entity.source.started";
    /**
     * Key used to store the time when the reading from the source completed
     */
    String SOURCE_COMPLETE = "entity.source.complete";
    /**
     * Key used to store the time needed to read the entity from the source.
     * ({@link Float})
     */
    String SOURCE_DURATION = "entity.source.duration";
    /**
     * Key used to store the time when the processing of the entity started
     */
    String PROCESS_STARTED = "entity.process.started";
    /**
     * Key used to store the time when the processing of the entity completed
     */
    String PROCESS_COMPLETE = "entity.process.complete";
    /**
     * Key used to store the time needed for processing an entity. ({@link Float})
     */
    String PROCESS_DURATION = "entity.process.duration";
    /**
     * Key used to store the time when the storing of the entity started
     */
    String STORE_STARTED = "entity.store.started";
    /**
     * Key used to store the time when the storing of the entity completed
     */
    String STORE_COMPLETE = "entity.store.complete";
    /**
     * Key used to store the time needed to store the entity. ({@link Float})
     */
    String STORE_DURATION = "entity.store.duration";
    /**
     * Key used to store the time stamp when the error occurred
     */
    String ERROR_TIME = "entity.error.time";
    /**
     * Item used by the consumers to recognise that the Queue has finished.
     * See http://stackoverflow.com/questions/1956526/under-what-conditions-will-blockingqueue-take-throw-interrupted-exception Thread}
     * for an Example.
     */
    //ignore the Type safety because the item is of
    //INDEXING_COMPLETED_QUEUE_ITEM is anyway null
    @SuppressWarnings("rawtypes")
    QueueItem INDEXING_COMPLETED_QUEUE_ITEM = new QueueItem<Void>(null);

    /**
     * The sequence number for {@link IndexingDaemon}s that read from the 
     * {@link IndexingComponent}s 
     */
    Integer SEQUENCE_NUMBER_SOURCE_DAEMON = 0;
    /**
     * The sequence number for {@link IndexingDaemon}s that process Entities
     */
    Integer SEQUENCE_NUMBER_PROCESSOR_DAEMON = 1;
    /**
     * The sequence number for {@link IndexingDaemon}s that persist Entities to
     * the {@link IndexingDestination}
     */
    Integer SEQUENCE_NUMBER_PERSIT_DAEMON = 2;
    /**
     * The sequence number for {@link IndexingDaemon}s that indexed Entities
     */
    Integer SEQUENCE_NUMBER_FINISHED_DAEMON = 3;
    /**
     * The sequence number for {@link IndexingDaemon}s that handle errors
     */
    Integer SEQUENCE_NUMBER_ERROR_HANDLING_DAEMON = 4;

}
