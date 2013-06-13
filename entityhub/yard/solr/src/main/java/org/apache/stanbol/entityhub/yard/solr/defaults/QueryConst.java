package org.apache.stanbol.entityhub.yard.solr.defaults;

import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;


/**
 * Defines parameters used by the {@link FieldQuery} implementation of the
 * SolrYard. Some of those might also be supported by the {@link SolrYard}
 * configuration to set default values<p>
 * 
 * @author Rupert Westenthaler
 *
 */
public class QueryConst {
    private QueryConst(){/*do not allow instances*/}
    
    /**
     * Property allowing to enable/disable the generation of Phrase queries for
     * otional query terms (without wildcards). Values are expected to be
     * {@link Boolean}
     */
    public static final String PHRASE_QUERY_STATE = "stanbol.entityhub.yard.solr.query.phraseQuery";
    /**
     * The default state for the {@link #PHRASE_QUERY_STATE} (default: false)
     */
    public static final Boolean DEFAULT_PHRASE_QUERY_STATE = Boolean.FALSE;
    /**
     * Property allowing to set a query time boost for certain query terms.
     * Values are expected to be floating point values grater than zero.
     */
    public static final String QUERY_BOOST = "stanbol.entityhub.yard.solr.query.boost";
}
