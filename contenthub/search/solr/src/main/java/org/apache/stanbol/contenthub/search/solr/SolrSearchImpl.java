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
package org.apache.stanbol.contenthub.search.solr;

import java.io.IOException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.stanbol.commons.semanticindex.index.IndexException;
import org.apache.stanbol.commons.semanticindex.index.IndexManagementException;
import org.apache.stanbol.commons.semanticindex.index.SemanticIndexManager;
import org.apache.stanbol.contenthub.index.solr.SolrSemanticIndex;
import org.apache.stanbol.contenthub.servicesapi.index.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.index.search.solr.SolrQueryUtil;
import org.apache.stanbol.contenthub.servicesapi.index.search.solr.SolrSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
public class SolrSearchImpl implements SolrSearch {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchImpl.class);

    @Reference
    private SemanticIndexManager semanticIndexManager;

    @Override
    public QueryResponse search(String queryTerm, String indexName) throws SearchException {
        // By default solr query, we perform a faceted search when a keyword is supplied. To customize the
        // search
        // please use the method which accepts SolrParams/SolrQuery
        SolrQuery solrQuery = null;
        SolrServer solrServer = null;
        try {
            SolrSemanticIndex semanticIndex = (SolrSemanticIndex) semanticIndexManager.getIndex(indexName);
            solrServer = semanticIndex.getServer();
            solrQuery = SolrQueryUtil.prepareSolrQuery(solrServer, queryTerm);
        } catch (SolrServerException e) {
            log.error("Failed to prepare default solr query");
            throw new SearchException("Failed to prepare default solr query", e);
        } catch (IOException e) {
            log.error("Failed to prepare default solr query");
            throw new SearchException("Failed to prepare default solr query", e);
        } catch (IndexException e) {
            log.error(e.getMessage(), e);
            throw new SearchException(e.getMessage(), e);
        } catch (IndexManagementException e) {
            log.error(e.getMessage(), e);
            throw new SearchException(e.getMessage(), e);
        }
        return executeSolrQuery(solrServer, solrQuery);
    }

    @Override
    public QueryResponse search(SolrParams solrQuery, String indexName) throws SearchException {
        SolrServer solrServer = null;
        try {
            SolrSemanticIndex semanticIndex = (SolrSemanticIndex) semanticIndexManager.getIndex(indexName);
            solrServer = semanticIndex.getServer();
        } catch (IndexManagementException e) {
            log.error(e.getMessage(), e);
            throw new SearchException(e.getMessage(), e);
        } catch (IndexException e) {
            log.error(e.getMessage(), e);
            throw new SearchException(e.getMessage(), e);
        }
        return executeSolrQuery(solrServer, solrQuery);
    }

    private QueryResponse executeSolrQuery(SolrServer solrServer, SolrParams solrQuery) throws SearchException {
        try {
            return solrServer.query(solrQuery);
        } catch (SolrServerException e) {
            String msg = "Failed to execute solr query";
            log.error(msg, e);
            throw new SearchException(msg, e);
        }
    }

}
