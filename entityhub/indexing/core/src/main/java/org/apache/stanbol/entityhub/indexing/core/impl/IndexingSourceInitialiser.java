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

import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;

/**
 * Initialises an {@link IndexingComponent} and {@link IndexerImpl#notifyState()} 
 * if finished
 * @author Rupert Westenthaler
 *
 */
public class IndexingSourceInitialiser implements Runnable {
    private final IndexingComponent source;
    private boolean finished = false;
    private final Set<IndexingSourceInitialiserListener> listeners;
    public IndexingSourceInitialiser(IndexingComponent source){
       this.source = source; 
       this.listeners = Collections.synchronizedSet(
           new HashSet<IndexingSourceInitialiserListener>());
    }
    @Override
    public final void run() {
        source.initialise();
        finished = true;
        fireInitialisationCompletedEvent();
    }
    public boolean finished(){
        return finished;
    }
    public boolean addIndexingSourceInitialiserListener(IndexingSourceInitialiserListener listener){
        if(listener != null){
            return listeners.add(listener);
        } else {
            return false;
        }
    }
    public boolean removeIndexingSourceInitialiserListener(IndexingSourceInitialiserListener listener){
        if(listener != null){
            return listeners.remove(listener);
        } else {
            return false;
        }
    }
    protected void fireInitialisationCompletedEvent(){
        Set<IndexingSourceInitialiserListener> copy;
        synchronized (listeners) {
            copy = new HashSet<IndexingSourceInitialiserListener>(listeners);
        }
        IndexingSourceEventObject eventObject = new IndexingSourceEventObject(this,this.source);
        for(IndexingSourceInitialiserListener listener : copy){;
            listener.indexingSourceInitialised(eventObject);
        }
    }
    
    public static interface IndexingSourceInitialiserListener {
        void indexingSourceInitialised(IndexingSourceEventObject eventObject);
    }
    public static class IndexingSourceEventObject extends EventObject{
        private static final long serialVersionUID = -1L;
        private final IndexingComponent indexingSource;
        protected IndexingSourceEventObject(IndexingSourceInitialiser initialiser,IndexingComponent source){
            super(initialiser);
            indexingSource = source;
        }
        @Override
        public IndexingSourceInitialiser getSource() {
            return (IndexingSourceInitialiser) super.getSource();
        }
        /**
         * @return the indexingSource
         */
        public IndexingComponent getIndexingSource() {
            return indexingSource;
        }
    }

}