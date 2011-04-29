package org.apache.stanbol.entityhub.indexing.core.source;

import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator.EntityScore;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;

/**
 * Simple Adapter between an {@link EntityIterator} and the {@link EntityScoreProvider}
 * interface that iterates over all entities provided by the {@link EntityIterator}
 * and uses this information to initialise an {@link EntityScoreProvider}.<p>
 *  
 * @author Rupert Westenthaler
 *
 */
public class EntityIneratorToScoreProviderAdapter implements EntityScoreProvider {

    private EntityIterator entityIterator;
    private EntityScoreProvider provider;
    public EntityIneratorToScoreProviderAdapter(){
        this(null);
    }
    public EntityIneratorToScoreProviderAdapter(EntityIterator entityIterator){
        if(entityIterator == null){
            throw new IllegalArgumentException("The EntityIterator MUST NOT be NULL!");
        }
        this.entityIterator = entityIterator;
    }
    
    @Override
    public boolean needsData() {
        return provider.needsData();
    }

    @Override
    public Float process(String id) throws UnsupportedOperationException {
        return provider.process(id);
    }

    @Override
    public Float process(Representation entity) throws UnsupportedOperationException {
        return provider.process(entity);
    }

    @Override
    public boolean needsInitialisation() {
        return true;
    }
    @Override
    public void initialise() {
        //initialise the source entity iterator
        if(entityIterator.needsInitialisation()){
            entityIterator.initialise();
        }
        //initialise this instace
        Map<String,Float> entityScoreMap = new HashMap<String,Float>();
        while(entityIterator.hasNext()){
            EntityScore entityScore = entityIterator.next();
            entityScoreMap.put(entityScore.id, entityScore.score);
        }
        //close the source because it is no longer needed!
        entityIterator.close();
        provider = new MapEntityScoreProvider(entityScoreMap);
        //initialise the wrapped score provider
        if(provider.needsInitialisation()){
            provider.initialise();
        }
    }
    @Override
    public void close() {
       provider.close();
    }
    @Override
    public void setConfiguration(Map<String,Object> config) {
        //the IndexingConfig is available via the IndexingConfig.KEY_INDEXING_CONFIG key!
        IndexingConfig indexingConfig = (IndexingConfig)config.get(IndexingConfig.KEY_INDEXING_CONFIG);
        //configure first the EntityIterator to adapt
        entityIterator = indexingConfig.getEntityIdIterator();
        if(entityIterator == null){
            throw new IllegalArgumentException("No EntityIterator available via the indexing configuration "+indexingConfig.getName());
        }
    }

}
