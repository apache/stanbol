package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import org.apache.jena.atlas.lib.Tuple;
import org.slf4j.Logger;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.tdb.TDBException;
import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.solver.stats.Stats;
import com.hp.hpl.jena.tdb.solver.stats.StatsCollector;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.tdb.store.TripleTable;
import com.hp.hpl.jena.tdb.store.bulkloader.BulkLoader;
import com.hp.hpl.jena.tdb.store.bulkloader.BulkStreamRDF;
import com.hp.hpl.jena.tdb.store.bulkloader.LoadMonitor;
import com.hp.hpl.jena.tdb.store.bulkloader.LoaderNodeTupleTable;
import com.hp.hpl.jena.tdb.sys.Names;

/**
 * Special version of an {@link BulkStreamRDF} that stores Triples to the
 * {@link TripleTable} of the parsed {@link DatasetGraphTDB}. Even
 * {@link Quad}s and {@link Tuple}s with >= 3 nodes are converted to triples.
 * <p>
 * This code is based on the DestinationGraph implementation private to the 
 * {@link TDBLoader} class.
 * 
 * @author Rupert Westenthaler
 *
 */
class DestinationTripleGraph implements BulkStreamRDF {
    final private DatasetGraphTDB dsg ;
    final private LoadMonitor monitor ;
    final private LoaderNodeTupleTable loaderTriples ;
    final private boolean startedEmpty ;
    private long count = 0 ;
    private StatsCollector stats ;

    DestinationTripleGraph(final DatasetGraphTDB dsg, Logger log) {
        this.dsg = dsg ;
        startedEmpty = dsg.isEmpty() ;
        monitor = new LoadMonitor(dsg, log, "triples", BulkLoader.DataTickPoint, BulkLoader.IndexTickPoint) ;
        loaderTriples = new LoaderNodeTupleTable(dsg.getTripleTable().getNodeTupleTable(), "triples", monitor) ;
    }

    @Override
    final public void startBulk()
    {
        loaderTriples.loadStart() ;
        loaderTriples.loadDataStart() ;

        this.stats = new StatsCollector() ;
    }
    @Override
    final public void triple(Triple triple)
    {
        Node s = triple.getSubject() ;
        Node p = triple.getPredicate() ;
        Node o = triple.getObject() ;

        loaderTriples.load(s, p, o)  ;
        stats.record(null, s, p, o) ; 
        count++ ;
    }

    @Override
    final public void finishBulk()
    {
        loaderTriples.loadDataFinish() ;
        loaderTriples.loadIndexStart() ;
        loaderTriples.loadIndexFinish() ;
        loaderTriples.loadFinish() ;

        if ( ! dsg.getLocation().isMem() && startedEmpty )
        {
            String filename = dsg.getLocation().getPath(Names.optStats) ;
            Stats.write(filename, stats) ;
        }
        forceSync(dsg) ;
    }

    @Override
    public void start()                     {}
    @Override
    public void quad(Quad quad) { 
        triple(quad.asTriple());
    }
    @Override
    public void tuple(Tuple<Node> tuple) { 
        if(tuple.size() >= 3){
            loaderTriples.load(tuple.get(0), tuple.get(1), tuple.get(2))  ;
            stats.record(null, tuple.get(0), tuple.get(1), tuple.get(2)) ; 
            count++ ;
        } else {
            throw new TDBException("Tuple with < 3 Nodes encountered while loading a single graph");
        }
    }
    @Override
    public void base(String base)           { }
    @Override
    public void prefix(String prefix, String iri)  { } // TODO
    @Override
    public void finish()                    {}


    static void forceSync(DatasetGraphTDB dsg)
    {
        // Force sync - we have been bypassing DSG tables.
        // THIS DOES NOT WORK IF modules check for SYNC necessity.
        dsg.getTripleTable().getNodeTupleTable().getNodeTable().sync();
        dsg.getQuadTable().getNodeTupleTable().getNodeTable().sync();
        dsg.getQuadTable().getNodeTupleTable().getNodeTable().sync();
        dsg.getPrefixes().getNodeTupleTable().getNodeTable().sync();                
        // This is not enough -- modules check whether sync needed.
        dsg.sync() ;
        
    }
}

