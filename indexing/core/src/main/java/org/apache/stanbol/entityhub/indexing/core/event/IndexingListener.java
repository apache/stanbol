package org.apache.stanbol.entityhub.indexing.core.event;

public interface IndexingListener {

    void stateChanged(IndexingEvent event);
    void indexingCompleted(IndexingEvent event);
}
