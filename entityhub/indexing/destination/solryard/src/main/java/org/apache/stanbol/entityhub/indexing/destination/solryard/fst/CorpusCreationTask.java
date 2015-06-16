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
package org.apache.stanbol.entityhub.indexing.destination.solryard.fst;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.opensextant.solrtexttagger.TaggerFstCorpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime creation of FST corpora is done as {@link Callable}. This allows
 * users to decide by the configuration of the {@link ExecutorService} to
 * control how Corpora are build (e.g. how many can be built at a time.
 * @author Rupert Westenthaler
 *
 */
public class CorpusCreationTask implements Runnable{

    private final Logger log = LoggerFactory.getLogger(CorpusCreationTask.class);
    
    CorpusCreationInfo corpusInfo;
    SolrCore core;
    
    public CorpusCreationTask(SolrCore core, CorpusCreationInfo corpus){
        this.core = core;
        this.corpusInfo = corpus;
    }
    
    @Override
    public void run() {
        TaggerFstCorpus corpus = null;
        RefCounted<SolrIndexSearcher> searcherRef = core.getSearcher();
        try {
            SolrIndexSearcher searcher = searcherRef.get();
            //we do get the AtomicReader, because TaggerFstCorpus will need it
            //anyways. This prevents to create another SlowCompositeReaderWrapper.
            IndexReader reader = searcher.getAtomicReader();
            log.info(" ... build {}", corpusInfo);
            corpus = new TaggerFstCorpus(reader, searcher.getIndexReader().getVersion(),
                null, corpusInfo.indexedField, corpusInfo.storedField, corpusInfo.analyzer,
                corpusInfo.partialMatches,1,200);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Information to build "
                    + corpusInfo + " from SolrIndex '" + core.getName() + "'!", e);
        } finally {
            searcherRef.decref(); //ensure that we dereference the searcher
        }
        if(corpusInfo.fst.exists()){
            if(!FileUtils.deleteQuietly(corpusInfo.fst)){
                log.warn("Unable to delete existing FST fiel for {}",corpusInfo);
            }
        }
        if(corpus.getPhrases() != null){ //the FST is not empty
            try { //NOTE saving an empty corpus results in a NPE
                corpus.save(corpusInfo.fst);
            } catch (IOException e) {
                log.warn("Unable to store FST corpus " + corpusInfo + " to "
                        + corpusInfo.fst.getAbsolutePath() + "!", e);
            }
        } else {
           log.info("FST for {} is empty ... no FST will be stored",corpusInfo); 
        }
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Task: building ").append(corpusInfo)
                .append(" for SolrCore ").append(core.getName()).toString();
    }

}
