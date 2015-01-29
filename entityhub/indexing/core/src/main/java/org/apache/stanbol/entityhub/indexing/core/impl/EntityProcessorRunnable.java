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

import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.PROCESS_COMPLETE;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.PROCESS_DURATION;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.PROCESS_STARTED;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Consumes Representations as created by the IndexingSource and processes
 * it by using the configured {@link EntityProcessor}. In addition this
 * components adds configured keys to the Representation.
 * @author Rupert Westenthaler
 *
 */
public class EntityProcessorRunnable extends IndexingDaemon<Representation,Representation> {
    private final List<EntityProcessor> processors;
    private final Set<String> keys;
    public EntityProcessorRunnable(String name,
                                   BlockingQueue<QueueItem<Representation>> consume,
                                   BlockingQueue<QueueItem<Representation>> produce,
                                   BlockingQueue<QueueItem<IndexingError>> error,
                                   List<EntityProcessor> processors,Set<String> keys) {
        super(name,IndexerConstants.SEQUENCE_NUMBER_PROCESSOR_DAEMON,
            consume,produce,error);
        this.processors = processors;
        if(log.isDebugEnabled()){
            log.debug("Entity Processors:");
            for(EntityProcessor ep : processors){
                log.debug("  - {} (type: {})",ep, ep.getClass().getSimpleName());
            }
        }
        if(keys == null){
            this.keys = Collections.emptySet();
        } else {
            this.keys = keys;
        }
    }
    @Override
    public void run() {
        while(!isQueueFinished()){
            QueueItem<Representation> item = consume();
            if(item != null){
                Long start = Long.valueOf(System.currentTimeMillis());
                item.setProperty(PROCESS_STARTED, start);
                Iterator<EntityProcessor> it = processors.iterator();
                Representation processed = item.getItem();
                log.trace("> process {}", processed);
                EntityProcessor processor = null;
                while(processed != null && it.hasNext()){
                    processor = it.next();
                    log.trace("   - with {}", processor);
                    processed = processor.process(processed);
                }
                if(processed == null){
                    log.debug("Item {} filtered by processor {}",item.getItem().getId(),processor);
                } else {
                    log.trace("   - done");
                    for(String key : keys){
                        //consume the property and add it to the
                        //transformed representation
                        Object value = item.removeProperty(key);
                        if(value != null){
                            processed.add(key, value);
                        }
                    }
                    QueueItem<Representation> produced = new QueueItem<Representation>(processed,item);
                    Long completed = Long.valueOf(System.currentTimeMillis());
                    produced.setProperty(PROCESS_COMPLETE, completed);
                    produced.setProperty(PROCESS_DURATION, Float.valueOf(
                        (float)(completed.longValue()-start.longValue())));
                    produce(produced);
                }
            }
        }
        setFinished();
    }
}