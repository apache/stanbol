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

import java.util.concurrent.BlockingQueue;

import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Daemon the extracts Entities from the {@link IndexingComponent}s by iterating
 * over the entity data and making lookups for the entity scores
 * @author Rupert Westenthaler
 */
public class EntityDataBasedIndexingDaemon extends AbstractEntityIndexingDaemon {
    private final EntityDataIterable dataIterable;
    private final EntityScoreProvider scoreProvider;
    private final ScoreNormaliser normaliser;
    private final boolean indexAllEntitiesState;
    public EntityDataBasedIndexingDaemon(String name,
                                         BlockingQueue<QueueItem<Representation>> produce,
                                         BlockingQueue<QueueItem<IndexingError>> error,
                                         EntityDataIterable dataIterable,
                                         EntityScoreProvider scoreProvider,
                                         ScoreNormaliser normaliser,
                                         boolean indexAllEntitiesState) {
        super(name,produce, error);
        if(dataIterable == null){
            throw new IllegalArgumentException("The parsed EntityDataIterator MUST NOT be NULL");
        }
        if(scoreProvider == null){
            throw new IllegalArgumentException("The parsed EntityScoreProvider MUST NOT be NULL");
        }
        this.dataIterable = dataIterable;
        this.scoreProvider = scoreProvider;
        this.normaliser = normaliser;
        this.indexAllEntitiesState = indexAllEntitiesState;
    }

    @Override
    public void run() {
        log.info("...start iterating over Entity data");
        EntityDataIterator dataIterator = dataIterable.entityDataIterator();
        while(dataIterator.hasNext()){
            Long start = Long.valueOf(System.currentTimeMillis());
            String id = dataIterator.next();
            Representation rep = null;
            Float score;
            if(!scoreProvider.needsData()){
                score = scoreProvider.process(id);
            } else {
                rep = dataIterator.getRepresentation();
                score = scoreProvider.process(rep);
            }
            //normalise the score
            if(normaliser != null){
                score = normaliser.normalise(score);
            }
            if(indexAllEntitiesState || //all entities are indexed anyway
                    score == null || //no score available
                    score.compareTo(ScoreNormaliser.ZERO) >= 0){ //score >= 0
                if(rep == null){
                    rep = dataIterator.getRepresentation();
                }
                produce(rep,score,start);
            } // else ignore this entity
        }
        setFinished();
    }

}
