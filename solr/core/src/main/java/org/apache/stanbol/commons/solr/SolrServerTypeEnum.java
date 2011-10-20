package org.apache.stanbol.commons.solr;

/**
 * SolrServer types defined here to avoid java dependencies to the according java classes
 * 
 * @author Rupert Westenthaler
 * 
 */
public enum SolrServerTypeEnum {
    /**
     * Uses an embedded SolrServer that runs within the same virtual machine
     */
    EMBEDDED,
    /**
     * The default type that can be used for query and updates
     */
    HTTP,
    /**
     * This server is preferable used for updates
     */
    STREAMING,
    /**
     * This allows to use load balancing on multiple SolrServers via a round robin algorithm.
     */
    LOAD_BALANCE
}