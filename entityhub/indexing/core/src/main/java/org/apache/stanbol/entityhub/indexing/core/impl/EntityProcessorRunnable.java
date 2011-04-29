package org.apache.stanbol.entityhub.indexing.core.impl;

import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.PROCESS_COMPLETE;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.PROCESS_DURATION;
import static org.apache.stanbol.entityhub.indexing.core.impl.IndexerConstants.PROCESS_STARTED;

import java.util.Collections;
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
    private final EntityProcessor processor;
    private final Set<String> keys;
    public EntityProcessorRunnable(String name,
                                   BlockingQueue<QueueItem<Representation>> consume,
                                   BlockingQueue<QueueItem<Representation>> produce,
                                   BlockingQueue<QueueItem<IndexingError>> error,
                                   EntityProcessor processor,Set<String> keys) {
        super(name,IndexerConstants.SEQUENCE_NUMBER_PROCESSOR_DAEMON,
            consume,produce,error);
        this.processor = processor;
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
                Representation processed = processor.process(item.getItem());
                if(processed == null){
                    sendError(item.getItem().getId(),item, 
                        String.format("Processor %s returned NULL for Entity %s",
                            processor,item.getItem().getId()), null);
                } else {
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