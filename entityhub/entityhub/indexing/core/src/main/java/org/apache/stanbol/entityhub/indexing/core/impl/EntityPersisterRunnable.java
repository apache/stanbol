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
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.STORE_COMPLETE;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.STORE_DURATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardException;

/**
 * @author Rupert Westenthaler
 */
public class EntityPersisterRunnable extends IndexingDaemon<Representation,Representation> {

    private int chunkSize;
    private Yard yard;
    public EntityPersisterRunnable(String name,
                                   BlockingQueue<QueueItem<Representation>> consume, 
                                   BlockingQueue<QueueItem<Representation>> produce,
                                   BlockingQueue<QueueItem<IndexingError>> error,
                                   int chunkSize, Yard yard){
        super(name,IndexerConstants.SEQUENCE_NUMBER_PERSIT_DAEMON,
            consume,produce,error);
        this.chunkSize = chunkSize;
        this.yard = yard;
    }
    @Override
    public void run() {
        Map<String,QueueItem<Representation>> toStore = new HashMap<String,QueueItem<Representation>>();
        while(!isQueueFinished()){
            QueueItem<Representation> item;
            item = consume();
            if(item != null){
                if(item.getItem() != null){
                    toStore.put(item.getItem().getId(),item);
                }
            }
            if(toStore.size() >= chunkSize){
                process(toStore);
            }
        }
        //process the remaining
        if(!toStore.isEmpty()){
            process(toStore);
        }
        setFinished();
    }
    /**
     * processes the items within the parsed Map
     * @param toStore the items to process
     */
    private void process(Map<String,QueueItem<Representation>> toStore) {
        //keep the number of elements because store(..) will remove them!
        int elements = toStore.size(); 
        Long start = Long.valueOf(System.currentTimeMillis());
        Collection<QueueItem<Representation>> stored = store(toStore);
        Long completed = Long.valueOf(System.currentTimeMillis());
        Float duration = Float.valueOf(((float)(completed.longValue()-start.longValue()))/elements);
        for(QueueItem<Representation> storedItem : stored){
            storedItem.setProperty(STORE_COMPLETE, completed);
            storedItem.setProperty(STORE_DURATION, duration);
            produce(storedItem);
        }
    }
    /**
     * Stores the parsed Representations to the {@link #yard} and 
     * {@link #sendError(String, String, Exception)} for entities that could
     * not be stored!
     * @param toStore the Representations to store. This method removes all
     * Elements of this map while doing the work
     */
    private Set<QueueItem<Representation>> store(Map<String,QueueItem<Representation>> toStore) {
        String errorMsg;
        YardException yardException = null;
        Set<QueueItem<Representation>> stored = new HashSet<QueueItem<Representation>>();
        Collection<Representation> reps = new ArrayList<Representation>(toStore.size());
        for(QueueItem<Representation> item:toStore.values()){
            reps.add(item.getItem());
        }
        try {
            for(Representation r : yard.store(reps)){
                QueueItem<Representation> old = toStore.remove(r.getId());
                //create a new QueueItem and copy the metadata of the old one
                stored.add(new QueueItem<Representation>(r,old));
            }
            errorMsg = "Entity %s was not indexed by the Yard %s";
        } catch (YardException e) {
            errorMsg = "Unable to store Entity %s to Yard %s because of an YardException";
            yardException = e;
        }
        //the remaining Items in to store have some errors
        for(QueueItem<Representation> entry : toStore.values()){
            sendError(entry.getItem().getId(),entry,
                String.format(errorMsg,entry.getItem().getId(),yard.getId()),
                yardException);
        }
        toStore.clear(); //clear the
        return stored;
    }
    
}