package org.apache.stanbol.entityhub.indexing.core.impl;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;

public class EntityErrorLoggerDaemon extends IndexingDaemon<IndexingError,Object> {

    private final Logger err;
    public EntityErrorLoggerDaemon(BlockingQueue<QueueItem<IndexingError>> consume,
                             Logger err) {
        super("Indexer: Entity Error Logging Daemon",
            IndexerConstants.SEQUENCE_NUMBER_ERROR_HANDLING_DAEMON,
            consume, null, null);
        this.err = err;
    }

    @Override
    public void run() {
        while(!isQueueFinished()){
            QueueItem<IndexingError> errorItem = consume();
            if(errorItem != null){
                IndexingError error = errorItem.getItem();
                err.error(String.format("Error while indexing %s: %s",
                    error.getEntity(),error.getMessage()),error.getException());
            }
        }
        setFinished();
    }

}
