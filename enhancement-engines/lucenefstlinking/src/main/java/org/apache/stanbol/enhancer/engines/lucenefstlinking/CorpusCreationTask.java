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
package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.DirectoryReader;
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
    
    private final CorpusInfo fstInfo;
    private final SolrCore core;
    private final long enqueued;
    
    public CorpusCreationTask(SolrCore core, CorpusInfo fstInfo){
        if(core == null || fstInfo == null){
            throw new IllegalArgumentException("Pared parameters MUST NOT be NULL!");
        }
        this.core = core;
        this.fstInfo = fstInfo;
        this.enqueued = fstInfo.enqueue();
    }
    
    @Override
    public void run() {
        //check if the FST corpus was enqueued a 2nd time
        if(enqueued != fstInfo.getEnqueued()){
            return;
        }
        if(core.isClosed()){
            log.warn("Unable to build {} becuase SolrCore {} is closed!",fstInfo,core.getName());
            return;
        }
        RefCounted<SolrIndexSearcher> searcherRef = core.getSearcher();
        SolrIndexSearcher searcher = searcherRef.get();
        DirectoryReader reader = searcher.getIndexReader();
        TaggerFstCorpus corpus;
        try {
            log.info(" ... build FST corpus for {}",fstInfo);
            corpus = new TaggerFstCorpus(reader, reader.getVersion(),
                null, fstInfo.indexedField, fstInfo.storedField, fstInfo.analyzer,
                fstInfo.partialMatches,1,100);
        } catch (Exception e) {
            log.warn("Unable to build "+fstInfo+"!",e);
            return;
        } finally {
//            try {
//                reader.close();
//            } catch (IOException e) { /* ignore */ }
            searcherRef.decref();
        }
        if(fstInfo.fst.exists()){
            if(!FileUtils.deleteQuietly(fstInfo.fst)){
                log.warn("Unable to delete existing FST fiel for {}",fstInfo);
            }
        }
        try {
            corpus.save(fstInfo.fst);
        } catch (IOException e) {
            log.warn("Unable to store FST corpus " + fstInfo + " to "
                    + fstInfo.fst.getAbsolutePath() + "!", e);
        }
        //set the created corpus to the FST Info
        fstInfo.setCorpus(enqueued, corpus);
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Task: building ").append(fstInfo)
                .append(" for SolrCore ").append(core.getName()).toString();
    }

}
