package eu.iksproject.fise.jersey.cache;

import org.apache.clerezza.rdf.core.MGraph;

public interface EntityCacheProvider {

    public MGraph getEntityCache();
}
