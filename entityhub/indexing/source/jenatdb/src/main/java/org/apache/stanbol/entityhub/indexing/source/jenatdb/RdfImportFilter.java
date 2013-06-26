package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import org.apache.stanbol.entityhub.indexing.core.IndexingComponent;

import com.hp.hpl.jena.graph.Node;

/**
 * Allows to filter Triples parsed from RDF files. Useful to NOT import some
 * RDF triples from RDF dumps that are not relevant for the indexing process.
 * @author Rupert Westenthaler
 *
 */
public interface RdfImportFilter extends IndexingComponent{

    
    public boolean accept(Node s, Node p, Node o);
    
}
