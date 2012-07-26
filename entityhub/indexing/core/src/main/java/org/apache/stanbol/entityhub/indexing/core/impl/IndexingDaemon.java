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
/**
 * 
 */
package org.apache.stanbol.entityhub.indexing.core.impl;

import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.ERROR_TIME;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.INDEXING_COMPLETED_QUEUE_ITEM;

import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IndexingDaemon<CI,PI> implements Comparable<IndexingDaemon<?,?>>,Runnable {
    protected final Logger log;
    private boolean queueFinished = false;
    private final BlockingQueue<QueueItem<CI>> consume;
    private final BlockingQueue<QueueItem<PI>> produce;
    private final BlockingQueue<QueueItem<IndexingError>> error;
    private boolean finisehd = false;
    private final Set<IndexingDaemonListener> listeners;
    /**
     * Typically used to set the name of the {@link Thread} running this Runnable
     */
    private final String name;
    /**
     * Used for {@link #compareTo(IndexingDaemon)}
     */
    private final Integer sequence;
    protected IndexingDaemon(String name,
                             Integer sequence,
                             BlockingQueue<QueueItem<CI>> consume,
                             BlockingQueue<QueueItem<PI>> produce,
                             BlockingQueue<QueueItem<IndexingError>> error){
        if(name == null || name.isEmpty()){
            this.name = getClass().getSimpleName()+" Deamon";
        } else {
            this.name = name;
        }
        if(sequence == null){
            this.sequence = Integer.valueOf(0);
        } else {
            this.sequence = sequence;
        }
        this.consume = consume;
        this.produce = produce;
        this.error = error;
        //get the logger for the actual implementation
        this.log = LoggerFactory.getLogger(getClass());
        this.listeners = Collections.synchronizedSet(
            new HashSet<IndexingDaemonListener>());
        
    }
    protected final void sendError(String entityId, String message, Exception ex){
        if(entityId == null){
            return;
        } else {
            putError(new QueueItem<IndexingError>(
                    new IndexingError(entityId, message, ex)));
        }
    }
    protected final void sendError(String entityId,QueueItem<?> item, String message, Exception ex){
        if(entityId == null){
            return;
        }
        putError((new QueueItem<IndexingError>(
                new IndexingError(entityId, message, ex)
                ,item)));
    }
    private void putError(QueueItem<IndexingError> errorItem){
        if(error == null){
            log.warn("Unable to process Error because Error Queue is NULL!");
        }
        Long errorTime = Long.valueOf(System.currentTimeMillis());
        errorItem.setProperty(ERROR_TIME, errorTime);
        try {
            error.put(errorItem);
        } catch (InterruptedException e) {
            log.error("Interupped while sending an Error for Entity "+errorItem.getItem().getEntity());
        }

    }
    protected final void produce(QueueItem<PI> item){
        if(produce == null){
            log.warn("Unable to produce Items because produce queue is NULL!");
        }
        if(item != null){
            try {
                produce.put(item);
            } catch (InterruptedException e) {
                log.error("Interupped while producing item "+item.getItem(), e);
            }
        }
    }
    protected final QueueItem<CI> consume(){
        if(queueFinished){
            return null;
        }
        if(consume == null){
            log.warn("Unable to consume items because consume queue is NULl!");
        }
        try {
            QueueItem<CI> consumed = consume.take();
            if(consumed == INDEXING_COMPLETED_QUEUE_ITEM){
                queueFinished = true;
                consume.put(consumed); //put it back to the list
                return null;
            } else {
                return consumed;
            }
        } catch (InterruptedException e) {
            log.error("Interupped while consuming -> return null");
            return null;
        }
    }
    /**
     * @return the queueFinished
     */
    protected final boolean isQueueFinished() {
        return queueFinished;
    }
    /**
     * Method has to be called by the subclasses to signal that this Runnable
     * has finished. It will set {@link #finished()} to <code>true</code>
     */
    protected final void setFinished(){
        this.finisehd = true;
        //tell listener that his one has finished!
        fireIndexingDaemonEvent();
        
    }
    public final boolean finished(){
        return finisehd;
    }
    public final boolean addIndexingDaemonListener(IndexingDaemonListener listener){
        if(listener != null){
            return listeners.add(listener);
        } else {
            return false;
        }
    }
    public boolean removeIndexingDaemonListener(IndexingDaemonListener listener){
        if(listener != null){
            return listeners.remove(listener);
        } else {
            return false;
        }
    }
    public void fireIndexingDaemonEvent(){
        Set<IndexingDaemonListener> copy;
        synchronized (listeners) {
            copy = new HashSet<IndexingDaemonListener>(listeners);
        }
        IndexingDaemonEventObject eventObject = new IndexingDaemonEventObject(this);
        for(IndexingDaemonListener listener : copy){
            listener.indexingDaemonFinished(eventObject);
        }
    }
    /**
     * Currently only used to notify listener that this Daemon has processed
     * all entities
     * TODO: I would like to use generics here, but I was not able to figure out
     * how to used them in a way, that one can still register an Listener that
     * uses <code>IndexingDaemonListener&lt;? super CI,? super&gt;</code> with
     * the {@link IndexingDaemon#addIndexingDaemonListener(IndexingDaemonListener)}
     * and {@link IndexingDaemon#removeIndexingDaemonListener(IndexingDaemonListener)}
     * methods.
     * @author Rupert Westenthaler
     *
     */
    public static interface IndexingDaemonListener {
        void indexingDaemonFinished(IndexingDaemonEventObject indexingDaemonEventObject);
    }

    public static class IndexingDaemonEventObject extends EventObject {
        private static final long serialVersionUID = -1L;
        public IndexingDaemonEventObject(IndexingDaemon<?,?> indexingDaemon){
            super(indexingDaemon);
        }
        @Override
        public IndexingDaemon<?,?> getSource() {
            return (IndexingDaemon<?,?>)super.getSource();
        }
    }
    protected final BlockingQueue<QueueItem<CI>> getConsumeQueue() {
        return consume;
    }
    protected final BlockingQueue<QueueItem<PI>> getProduceQueue() {
        return produce;
    }

    public String getName() {
        return this.name;
    }
    /**
     * The order of this Daemon. Guaranteed to be NOT NULL
     * @return the order
     */
    public final Integer getSequence() {
        return sequence;
    }
    @Override
    public int compareTo(IndexingDaemon<?,?> o) {
        int compare = sequence.compareTo(o.sequence);
        if(compare != 0){
            return compare;
        } else {
            //the ordering within the same sequence position is of no importance
            //but it is important to only return 0 if the two Objects are
            //equals because we will use this class together with SortedSets!
            if(hashCode() == o.hashCode()){
                if(equals(o)){
                    return 0;
                } else {
                    return -1; //no idea if that is OK
                }
            } else {
                return hashCode()-o.hashCode();
            }
        }
    }
}