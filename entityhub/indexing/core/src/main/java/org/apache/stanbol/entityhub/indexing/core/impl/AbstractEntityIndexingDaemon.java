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

    private final ScoreNormaliser normaliser;
    
    protected AbstractEntityIndexingDaemon(String name,
                                           ScoreNormaliser scoreNormaliser,
                                           BlockingQueue<QueueItem<Representation>> produce,
                                           BlockingQueue<QueueItem<IndexingError>> error) {
        super(name, 
            IndexerConstants.SEQUENCE_NUMBER_SOURCE_DAEMON,
            null,produce, error);
        this.normaliser = scoreNormaliser;
    }
    /**
     * Used to produce Representations by both variants of EnityIndexingDeamons
     * @param rep the {@link Representation} extracted from the 
     *  {@link IndexingComponent}s
     * @param score The score for the Representation
     */
    protected final void produce(Representation rep,Float score,Long started) {
        if(rep == null){
            return;
        }
        //normalise the score if both score and a normaliser are present
        if(score != null && normaliser != null){
            score = normaliser.normalise(score);
        }
        //first set the score of the representation
        QueueItem<Representation> item = new QueueItem<Representation>(rep);
        //set the score as additional property to the QueueItem, because
        //it needs to be added to the Representation after the processing completes
        if(score != null && score.compareTo(ScoreNormaliser.ZERO) >= 0){
            item.setProperty(SCORE_FIELD, score);
        }
        item.setProperty(SOURCE_STARTED, started);
        Long completed = Long.valueOf(System.currentTimeMillis());
        item.setProperty(SOURCE_COMPLETE, completed);
        Float duration = Float.valueOf((float)(completed.longValue()-started.longValue()));
        item.setProperty(SOURCE_DURATION, duration);
        produce(item);
    }

}
