package org.apache.stanbol.enhancer.jersey.cache;

import org.apache.clerezza.rdf.core.MGraph;

public interface EntityCacheProvider {

    MGraph getEntityCache();

}
