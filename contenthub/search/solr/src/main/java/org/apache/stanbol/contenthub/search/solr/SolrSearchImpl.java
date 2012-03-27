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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.search.solr.util.SolrQueryUtil;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.store.solr.manager.SolrCoreManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
public class SolrSearchImpl implements SolrSearch {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchImpl.class);

    @Reference
    private ManagedSolrServer managedSolrServer;

    private BundleContext bundleContext;

    @Activate
    public void activate(ComponentContext context) {
        this.bundleContext = context.getBundleContext();
    }

    @Override
    public QueryResponse search(String queryTerm) throws SearchException {
        SolrQuery solrQuery = null;
        SolrServer solrServer = null;
        try {
            solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
            solrQuery = SolrQueryUtil.prepareDefaultSolrQuery(solrServer, queryTerm);
        } catch (StoreException e) {
            throw new SearchException(e.getMessage(), e);
        } catch (SolrServerException e) {
            throw new SearchException(e.getMessage(), e);
        } catch (IOException e) {
            throw new SearchException(e.getMessage(), e);
        }
        return executeSolrQuery(solrServer, solrQuery);
    }

    @Override
    public QueryResponse search(String queryTerm, String ldProgramName) throws SearchException {
        // By default solr query, we perform a faceted search when a keyword is supplied. To customize the search
        // please use the method which accepts SolrParams/SolrQuery
        SolrQuery solrQuery = null;
        SolrServer solrServer = null;
        try {
            solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(ldProgramName);
            solrQuery = SolrQueryUtil.prepareDefaultSolrQuery(solrServer, queryTerm);
        } catch (StoreException e) {
            throw new SearchException(e.getMessage(), e);
        } catch (SolrServerException e) {
            throw new SearchException(e.getMessage(), e);
        } catch (IOException e) {
            throw new SearchException(e.getMessage(), e);
        }
        return executeSolrQuery(solrServer, solrQuery);
    }

    @Override
    public QueryResponse search(SolrParams solrQuery) throws SearchException {
        SolrServer solrServer = null;
        try {
            solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer();
        } catch (StoreException e) {
            throw new SearchException(e);
        }
        return executeSolrQuery(solrServer, solrQuery);
    }

    @Override
    public QueryResponse search(SolrParams solrQuery, String ldProgramName) throws SearchException {
        SolrServer solrServer = null;
        try {
            solrServer = SolrCoreManager.getInstance(bundleContext, managedSolrServer).getServer(
                ldProgramName);
        } catch (StoreException e) {
            throw new SearchException(e);
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
