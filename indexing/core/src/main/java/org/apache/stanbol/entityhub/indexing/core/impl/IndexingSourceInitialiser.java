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
            super(source);
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