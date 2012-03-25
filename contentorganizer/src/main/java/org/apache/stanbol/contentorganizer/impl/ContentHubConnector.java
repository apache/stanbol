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
package org.apache.stanbol.contentorganizer.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.LockableMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.contentorganizer.servicesapi.ContentConnector;
import org.apache.stanbol.contentorganizer.servicesapi.ContentRetrievalException;
import org.apache.stanbol.contentorganizer.util.ClerezzaBackendStatic;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author alexdma
 * 
 */
public class ContentHubConnector implements ContentConnector {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Store contentStore;
    private SolrSearch searchUtility;

    public static final UriRef CONTENT_ITEM = new UriRef("http://fise.iks-project.eu/ontology/ContentItem");

    public static final UriRef EXTRACTED_FROM = new UriRef("http://fise.iks-project.eu/ontology/extracted-from");

    
    public ContentHubConnector(Store contentStore, SolrSearch searchUtility) {
        this.contentStore = contentStore;
        this.searchUtility = searchUtility;
    }

    @Override
    public Set<ContentItem> getContents() throws ContentRetrievalException {
        Set<ContentItem> contents = new HashSet<ContentItem>();
        try {
            SolrDocumentList docs = searchUtility.search("*:*").getResults();

            // TODO replace this wildcard Solr query once the Store provides listing methods.
            for (SolrDocument doc : docs)
                try {
                    String id = (String) doc.getFieldValue(SolrFieldName.ID.toString());
                    contents.add(contentStore.get(id));
                } catch (Throwable t) {
                    log.error("Failed to get content item from document " + doc
                              + " . Continuing retrieval loop...", t);
                    continue;
                }

            computeMetadata(docs);

        } catch (SearchException e) {
            throw new ContentRetrievalException(e);
        }
        return contents;
    }

    private void computeMetadata(Collection<SolrDocument> docs) {

        for (SolrDocument doc : docs) {
            String id = (String) doc.getFieldValue(SolrFieldName.ID.toString());

            try {
                ContentItem ci = contentStore.get(id);

                synchronized (ci) {

                    String title = (String) doc.getFieldValue(SolrFieldName.TITLE.toString());

                    LockableMGraph mg = ci.getMetadata();
                    Lock writeLock = mg.getLock().writeLock();

                    Triple t = new TripleImpl(ci.getUri(), DC.title, ClerezzaBackendStatic.createLiteral(
                        title, Locale.getDefault(), null));
//                    System.out.println("Trying to add " + t);
                    writeLock.lock();
                    mg.add(t);
                    writeLock.unlock();

                    Object obj = doc.getFieldValue("authors_t");
                    if (obj != null && obj instanceof Collection<?>) {
                        for (Object s : (Collection<?>) obj) {
                            t = new TripleImpl(ci.getUri(), new UriRef("http://schema.org/author"),
                                    new UriRef(s.toString()));
//                            System.out.println("Trying to add " + t);
                            writeLock.lock();
                            mg.add(t);
                            writeLock.unlock();
                        }
                    }
                }

//                Iterator<Triple> it = ci.getMetadata().filter(null, DC.title, null);
//                while (it.hasNext())
//                    System.out.println("dc:title " + it.next());
//                it = ci.getMetadata().filter(null, new UriRef("http://schema.org/author"), null);
//                while (it.hasNext())
//                    System.out.println("schema:author " + it.next());

            } catch (StoreException e) {
                e.printStackTrace();
                log.error("Must skip content item " + id, e);
                continue;
            }

        }
    }

}
