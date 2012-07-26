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

import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.SCORE_FIELD;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.SOURCE_COMPLETE;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.SOURCE_DURATION;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.SOURCE_STARTED;

import java.util.concurrent.BlockingQueue;

import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

public abstract class AbstractEntityIndexingDaemon extends IndexingDaemon<Object,Representation> {

    
    protected AbstractEntityIndexingDaemon(String name,
                                           BlockingQueue<QueueItem<Representation>> produce,
                                           BlockingQueue<QueueItem<IndexingError>> error) {
        super(name, 
            IndexerConstants.SEQUENCE_NUMBER_SOURCE_DAEMON,
            null,produce, error);
    }
    /**
     * Used to produce Representations by both variants of EnityIndexingDeamons
     * @param rep the {@link Representation} extracted from the 
     *  {@link IndexingComponent}s
     * @param notNormalisedScore The score for the Representation
     */
    protected final void produce(Representation rep,Float normalisedScore,Long started) {
        if(rep == null){
            return;
        }
        //first set the score of the representation
        QueueItem<Representation> item = new QueueItem<Representation>(rep);
        //set the score as additional property to the QueueItem, because
        //it needs to be added to the Representation after the processing completes
        if(normalisedScore != null && normalisedScore.compareTo(ScoreNormaliser.ZERO) >= 0){
            item.setProperty(SCORE_FIELD, normalisedScore);
        }
        item.setProperty(SOURCE_STARTED, started);
        Long completed = Long.valueOf(System.currentTimeMillis());
        item.setProperty(SOURCE_COMPLETE, completed);
        Float duration = Float.valueOf((float)(completed.longValue()-started.longValue()));
        item.setProperty(SOURCE_DURATION, duration);
        produce(item);
    }

}
