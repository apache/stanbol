package org.apache.stanbol.enhancer.engines.lucenefstlinking.cache;

import org.apache.lucene.document.Document;
import org.apache.solr.search.FastLRUCache;
import org.apache.solr.search.SolrCache;

/**
 * Implementation of the {@link EntityCache} interface by using the
 * {@link SolrCache} API.
 * 
 * @author Rupert Westenthaler
 *
 */
public class SolrEntityCache implements EntityCache {

    private final SolrCache<Integer,Document> cache;
    private final Object version;
    private boolean closed;
    
    public SolrEntityCache(Object version, SolrCache<Integer,Document> cache) {
        this.cache = cache;
        this.version = version;
    }
    
    @Override
    public Object getVersion() {
        return version;
    }

    @Override
    public Document get(Integer docId) {
        return !closed ? cache.get(docId) : null;
    }

    @Override
    public void cache(Integer docId, Document doc) {
        if(!closed){
            cache.put(docId, doc);
        }
    }

    @Override
    public int size() {
        return cache.size();
    }
    @Override
    public String printStatistics() {
        return cache.getStatistics().toString();
    }
    
    @Override
    public String toString() {
        return cache.getDescription();
    }
    
    void close(){
        closed = true;
        cache.clear();
        cache.close();
    }
}
