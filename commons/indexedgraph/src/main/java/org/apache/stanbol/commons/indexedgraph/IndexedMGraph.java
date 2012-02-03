package org.apache.stanbol.commons.indexedgraph;

import java.util.Collection;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.impl.SimpleGraph;

public class IndexedMGraph extends IndexedTripleCollection implements MGraph {

    public IndexedMGraph() {
        super();
    }

    public IndexedMGraph(Collection<Triple> baseCollection) {
        super(baseCollection);
    }

    public IndexedMGraph(Iterator<Triple> iterator) {
        super(iterator);
    }

    @Override
    public Graph getGraph() {
        return new IndexedGraph(this);
    }

}
