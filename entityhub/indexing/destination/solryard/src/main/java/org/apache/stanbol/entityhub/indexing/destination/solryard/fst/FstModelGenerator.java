package org.apache.stanbol.entityhub.indexing.destination.solryard.fst;

import org.apache.solr.core.SolrCore;

public class FstModelGenerator {

    private SolrCore core;
    private FstConfig config;

    protected FstModelGenerator(SolrCore core, FstConfig config) {
        this.core = core;
        this.config = config;
    }
    
}
