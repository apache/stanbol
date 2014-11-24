/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import java.util.Map;

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
 * <p>
 * In addition this implementation supports an {@link RdfImportFilter} that
 * can be used to filter RDF triples read from RDF files before adding them
 * to the RDF TripleStore. 
 * 
 * @author Rupert Westenthaler
 *
 */
class DestinationTripleGraph implements BulkStreamRDF {
    /**
     * ImportFilter that accepts all triples. This is used in case 
     * <code>null</code> is parsed as {@link RdfImportFilter} to the constructor
     */
    private static final RdfImportFilter NO_FILTER = new RdfImportFilter() {
        @Override
        public void setConfiguration(Map<String,Object> config) {}
        @Override
        public boolean needsInitialisation() { return false;}
        @Override
        public void initialise() {}
        @Override
        public void close() {}
        @Override
        public boolean accept(Node s, Node p, Node o) {return true;}
    };
    final private DatasetGraphTDB dsg ;
    final private LoadMonitor monitor ;
    final private LoaderNodeTupleTable loaderTriples ;
    final private boolean startedEmpty ;
    private long count = 0 ;
    private long filteredCount = 0;
    private StatsCollector stats ;
    private RdfImportFilter importFilter;
    private final Logger importLog;

    DestinationTripleGraph(final DatasetGraphTDB dsg, RdfImportFilter importFilter, Logger log) {
        this.dsg = dsg ;
        startedEmpty = dsg.isEmpty() ;
        monitor = new LoadMonitor(dsg, log, "triples", BulkLoader.DataTickPoint, BulkLoader.IndexTickPoint) ;
        loaderTriples = new LoaderNodeTupleTable(dsg.getTripleTable().getNodeTupleTable(), "triples", monitor) ;
        if(importFilter == null){
            this.importFilter = NO_FILTER;
        } else {
            this.importFilter = importFilter;
        }
        this.importLog = log;
    }

    @Override
    final public void startBulk()
    {
        loaderTriples.loadStart() ;
        loaderTriples.loadDataStart() ;
        this.stats = new StatsCollector() ;
    }

    private void triple(Node s, Node p, Node o){
        if(importFilter.accept(s, p, o)){
            loaderTriples.load(s, p, o);
            stats.record(null, s, p, o);
            count++;
        } else {
            filteredCount++;
            if(filteredCount%100000 == 0){
                importLog.info("Filtered: {} triples ({}%)",filteredCount,
                    ((double)filteredCount*100/(double)(filteredCount+count)));
            }
        }
    }
    @Override
    final public void triple(Triple triple) {
        triple(triple.getSubject(),triple.getPredicate(),triple.getObject());
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
            Stats.write(filename, stats.results()) ;
        }
        forceSync(dsg) ;
    }

    @Override
    public void start(){}
    @Override
    public void quad(Quad quad) { 
        triple(quad.getSubject(),quad.getPredicate(),quad.getObject());
    }
    @Override
    public void tuple(Tuple<Node> tuple) { 
        if(tuple.size() >= 3){
            triple(tuple.get(0),tuple.get(1),tuple.get(2));
        } else {
            throw new TDBException("Tuple with < 3 Nodes encountered while loading a single graph");
        }
    }
    @Override
    public void base(String base){}
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

