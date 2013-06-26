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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotReader;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceImporter;
import org.apache.stanbol.entityhub.indexing.core.source.ResourceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;

public class RdfResourceImporter implements ResourceImporter {

    private static final Logger log = LoggerFactory.getLogger(RdfResourceImporter.class);
   // private final DatasetGraphTDB indexingDataset;
    private final DestinationTripleGraph destination;
    public RdfResourceImporter(DatasetGraphTDB indexingDataset, RdfImportFilter importFilter){
        if(indexingDataset == null){
            throw new IllegalArgumentException("The parsed DatasetGraphTDB instance MUST NOT be NULL!");
        }
        //this.indexingDataset = indexingDataset;
        this.destination = new DestinationTripleGraph(indexingDataset,importFilter,log);
    }

    @Override
    public ResourceState importResource(InputStream is, String resourceName) throws IOException {
        String name = FilenameUtils.getName(resourceName);
        if ("gz".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
            is = new GZIPInputStream(is);
            name = FilenameUtils.removeExtension(name);
            log.debug("   - from GZIP Archive");
        } else if ("bz2".equalsIgnoreCase(FilenameUtils.getExtension(name))) {
            is = new BZip2CompressorInputStream(is,
                true); //use true as 2nd param (see http://s.apache.org/QbK) 
            name = FilenameUtils.removeExtension(name);
            log.debug("   - from BZip2 Archive");
        }// TODO: No Zip Files inside Zip Files supported :o( ^^
        Lang format = RDFLanguages.filenameToLang(name);
        if (format == null) {
            log.warn("ignore File {} because of unknown extension ");
            return ResourceState.IGNORED;
        } else {
            log.info("    - bulk loading File {} using Format {}",resourceName,format);
            try {
            destination.startBulk() ;
            RiotReader.parse(is, format, null, destination) ;
            }catch (RuntimeException e) {
                return ResourceState.ERROR;
            } finally {
                destination.finishBulk() ;
            }
        }
// old code - just keep it in case the above else does not support any of the below RDF formats.
//        if (format == Lang.NTRIPLES) {
//            BulkLoader.
//            TDBLoader.load(indexingDataset, is, true);
//        } else if(format == Lang.NQUADS || format == Lang.TRIG){ //quads
//            TDBLoader loader = new TDBLoader();
//            loader.setShowProgress(true);
//            RDFSt dest = createQuad2TripleDestination();
//            dest.start();
//            RiotReader.parseQuads(is,format,null, dest);
//            dest.finish();
//        } else if (format != Lang.RDFXML) {
//            // use RIOT to parse the format but with a special configuration
//            // RiotReader!
//            TDBLoader loader = new TDBLoader();
//            loader.setShowProgress(true);
//            Destination<Triple> dest = createDestination();
//            dest.start();
//            RiotReader.parseTriples(is, format, null, dest);
//            dest.finish();
//        } else { // RDFXML
//            // in that case we need to use ARP
//            Model model = ModelFactory.createModelForGraph(indexingDataset.getDefaultGraph());
//            model.read(is, null);
//        }
        return ResourceState.LOADED;
    }
}
