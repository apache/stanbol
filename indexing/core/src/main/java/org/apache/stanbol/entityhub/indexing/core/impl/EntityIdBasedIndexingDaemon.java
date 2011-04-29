package org.apache.stanbol.entityhub.indexing.core.impl;

import java.util.concurrent.BlockingQueue;

import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator.EntityScore;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

public class EntityIdBasedIndexingDaemon extends AbstractEntityIndexingDaemon {

    private final EntityIterator entityIdIterator;
    private final EntityDataProvider dataProvider;
    private final boolean indexAllEntitiesState;
    public EntityIdBasedIndexingDaemon(String name,
                                          BlockingQueue<QueueItem<Representation>> produce,
                                          BlockingQueue<QueueItem<IndexingError>> error,
                                          EntityIterator entityIdIterator,
                                          EntityDataProvider dataProvider,
                                          ScoreNormaliser normaliser,
                                          boolean indexAllEntitiesState) {
        super(name, normaliser,produce, error);
        if(entityIdIterator == null){
            throw new IllegalArgumentException("The parsed EntityIterator MUST NOT be NULL");
        }
        if(dataProvider == null){
            throw new IllegalArgumentException("The parsed EntityDataProvider MUST NOT be NULL");
        }
        this.entityIdIterator = entityIdIterator;
        this.dataProvider = dataProvider;
        this.indexAllEntitiesState = indexAllEntitiesState;
    }

    @Override
    public void run() {
        while(entityIdIterator.hasNext()){
            Long start = Long.valueOf(System.currentTimeMillis());
            EntityScore entityScore = entityIdIterator.next();
            if(indexAllEntitiesState || //all entities are indexed anyway
                    entityScore.score == null || //no score available
                    entityScore.score.compareTo(ScoreNormaliser.ZERO) >= 0){ //score >= 0
                Representation rep = dataProvider.getEntityData(entityScore.id);
                
                produce(rep,entityScore.score,start);
            } //else ignore this entity
        }
        setFinished();
    }

}
