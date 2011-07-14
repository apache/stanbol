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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceState;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceImporter;
import org.openjena.riot.Lang;
import org.openjena.riot.RiotReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBLoader;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.tdb.store.bulkloader.BulkLoader;
import com.hp.hpl.jena.tdb.store.bulkloader.Destination;
import com.hp.hpl.jena.tdb.store.bulkloader.LoadMonitor;
import com.hp.hpl.jena.tdb.store.bulkloader.LoaderNodeTupleTable;

public class RdfResourceImporter implements ResourceImporter {

    private static final Logger log = LoggerFactory.getLogger(RdfResourceImporter.class);
    private final DatasetGraphTDB indexingDataset;
    public RdfResourceImporter(DatasetGraphTDB indexingDataset){
        if(indexingDataset == null){
            throw new IllegalArgumentException("The parsed DatasetGraphTDB instance MUST NOT be NULL!");
        }
        this.indexingDataset = indexingDataset;
    }

    @Override
    public ResourceState importResource(InputStream is, String resourceName) throws IOException {
        String name = FilenameUtils.getName(resourceName);
        if ("gz".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
            is = new GZIPInputStream(is);
            name = FilenameUtils.removeExtension(name);
            log.debug("   - from GZIP Archive");
        } else if ("bz2".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
            is = new BZip2CompressorInputStream(is);
            name = FilenameUtils.removeExtension(name);
            log.debug("   - from BZip2 Archive");
        }// TODO: No Zip Files inside Zip Files supported :o( ^^
        Lang format = Lang.guess(name);
        // For N-Triple we can use the TDBLoader
        if (format == null) {
            log.warn("ignore File {} because of unknown extension ");
            return ResourceState.IGNORED;
        } else if (format == Lang.NTRIPLES) {
            TDBLoader.load(indexingDataset, is, true);
        } else if (format != Lang.RDFXML) {
            // use RIOT to parse the format but with a special configuration
            // RiotReader!
            TDBLoader loader = new TDBLoader();
            loader.setShowProgress(true);
            Destination<Triple> dest = createDestination();
            dest.start();
            RiotReader.parseTriples(is, format, null, dest);
            dest.finish();
        } else { // RDFXML
            // in that case we need to use ARP
            Model model = ModelFactory.createModelForGraph(indexingDataset.getDefaultGraph());
            model.read(is, null);
        }
        return ResourceState.LOADED;
    }
    /**
     * Creates a triple destination for the default dataset of the
     * {@link #indexingDataset}.
     * This code is based on how Destinations are created in the {@link BulkLoader},
     * implementation. Note that
     * {@link BulkLoader#loadDefaultGraph(DatasetGraphTDB, InputStream, boolean)}
     * can not be used for formats other than {@link Lang#NTRIPLES} because it
     * hard codes this format for loading data form the parsed InputStream.
     * @return the destination!
     */
    private Destination<Triple> createDestination() {
        LoadMonitor monitor = new LoadMonitor(indexingDataset, 
            log, "triples",50000,100000);
        final LoaderNodeTupleTable loaderTriples = new LoaderNodeTupleTable(
            indexingDataset.getTripleTable().getNodeTupleTable(), "triples", monitor) ;

        Destination<Triple> sink = new Destination<Triple>() {
            long count = 0 ;
            public final void start()
            {
                loaderTriples.loadStart() ;
                loaderTriples.loadDataStart() ;
            }
            public final void send(Triple triple)
            {
                loaderTriples.load(triple.getSubject(), triple.getPredicate(), 
                    triple.getObject()) ;
                count++ ;
            }

            public final void flush() { }
            public void close() { }

            public final void finish()
            {
                loaderTriples.loadDataFinish() ;
                loaderTriples.loadIndexStart() ;
                loaderTriples.loadIndexFinish() ;
                loaderTriples.loadFinish() ;
            }
        } ;
        return sink ;
    }
}
