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

package org.apache.stanbol.contenthub.search.engines.solr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.stanbol.commons.solr.SolrServerProviderManager;
import org.apache.stanbol.commons.solr.SolrServerTypeEnum;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.contenthub.core.search.execution.SearchContextImpl;
import org.apache.stanbol.contenthub.servicesapi.search.engine.EngineProperties;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngineException;
import org.apache.stanbol.contenthub.servicesapi.search.execution.ClassResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.IndividualResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContextFactory;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.Util;
import com.hp.hpl.jena.sparql.lib.Metadata;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * 
 * @author anil.pacaci
 * 
 */
@Component
@Service
public class SolrSearchEngine implements SearchEngine, EngineProperties {

    private static final Logger logger = LoggerFactory.getLogger(SolrSearchEngine.class);

    private static final Map<String,Object> properties;
    static {
        properties = new HashMap<String,Object>();
        properties.put(PROCESSING_ORDER, PROCESSING_POST);
    }

    private final static String SERVER_NAME = "contenthub";
    /**
     * for EmbeddedSolr instance, it is tried to obtained EmbbeddedSolr at activator if it can get the
     * instance, server becomes null and this SolrSearchEngine does not work
     */
    private SolrServer server;

    @Reference
    SolrServerProviderManager solrServerProviderManager;

    @Reference
    ManagedSolrServer solrDirectoryManager;

    @Activate
    /**
     * Tries to connect EmbeddedSolr at startup, if can not, server becomes null and no query is executed on server
     * @param cc
     */
    public void activate(ComponentContext cc) {
        try {
            if (!solrDirectoryManager.isManagedIndex(SERVER_NAME)) {
                solrDirectoryManager.createSolrIndex(SERVER_NAME, SERVER_NAME, null);
            }
            server = solrServerProviderManager.getSolrServer(SolrServerTypeEnum.EMBEDDED, SERVER_NAME);
            logger.warn("Could not get the EmbeddedSolr Instance since there is no SolrDirectoryManager");
        } catch (Exception e) {
            logger.warn("Could not get the EmbeddedSolr Instance at location : {}", SERVER_NAME, e);
            server = null;
        }
    }

    @Override
    /**
     * gets the keywords from search context and then queries solr with these keywords and facet constraints,
     * when it found any result, adds to searchContext as documentResouce.
     * After searching for all keywords, omits the results founded by other engines and having non matching field constraints 
     */
    public void search(SearchContext searchContext) throws SearchEngineException {
        if (server == null) {
            logger.warn("No EmbeddedSolr, so SolrSearchEngine does not work");
        } else {
            for (QueryKeyword qk : searchContext.getQueryKeyWords()) {
                searchForKeyword(qk, searchContext);
                for (Keyword kw : qk.getRelatedKeywords()) {
                    searchForKeyword(kw, searchContext);
                }
            }
            
            /*
             * if (searchContext.getConstraints() != null && !searchContext.getConstraints().isEmpty()) {
             * omitNonMatchingResult(searchContext); }
             */
        }
    }

    /**
     * @param kw
     *            the keyword to use in query
     * @param searchContext
     */
    private void searchForKeyword(Keyword kw, SearchContext searchContext) {
        String keyword = kw.getKeyword();
        SolrQuery query = SolrSearchEngineHelper.keywordQueryWithFacets(keyword,
            searchContext.getConstraints());

        // Finding document resources by querying keyword with the facets
        try {
            QueryResponse solrResult = server.query(query);
            processSolrResult(searchContext, kw, solrResult);
        } catch (SolrServerException e) {
            logger.warn("Server could not be queried", e);
        }

        // Finding document resources by querying related Class Resources about
        // the keyword with facets
        List<ClassResource> classResources = kw.getRelatedClassResources();
        try {
            for (int i = 0; i < classResources.size(); i++) {
                ClassResource classResource = classResources.get(i);
                String classURI = classResource.getClassURI();
                String className = classURI.substring(Util.splitNamespace(classURI));
                if (className != null) {
                    query = SolrSearchEngineHelper.keywordQueryWithFacets(className,
                        searchContext.getConstraints());
                    QueryResponse solrResult = server.query(query);
                    processSolrResult(searchContext, kw, solrResult);
                } else {
                    logger.info("Name of class could not be extracted from from class Resource : ",
                        classResource);
                }
            }
        } catch (Exception e) {
            logger.warn("Error while querying solr with relatedClass resources", e);
        }

        // Finding document resources by querying related Individual Resources
        // about the keyword with facets
        List<IndividualResource> individualResources = kw.getRelatedIndividualResources();
        try {
            for (int i = 0; i < individualResources.size(); i++) {
                IndividualResource individualResource = individualResources.get(i);
                String individualURI = individualResource.getIndividualURI();
                String individualName = individualURI.substring(Util.splitNamespace(individualURI));
                if (individualName != null) {
                    query = SolrSearchEngineHelper.keywordQueryWithFacets(individualName,
                        searchContext.getConstraints());
                    QueryResponse solrResult = server.query(query);
                    processSolrResult(searchContext, kw, solrResult);
                } else {
                    logger.info("Name of individual could not be extracted from individual Resource : ",
                        individualResource);
                }
            }
        } catch (Exception e) {
            logger.warn("Error while querying solr with relatedIndividual resources", e);
        }

    }

    private void processSolrResult(SearchContext searchContext, Keyword keyword, QueryResponse solrResult) {
        SearchContextFactory factory = searchContext.getFactory();
        SolrDocumentList resultDocuments = solrResult.getResults();
        Double maxScore = resultDocuments.getMaxScore().doubleValue();

        for (int i = 0; i < resultDocuments.size(); i++) {
            SolrDocument resultDoc = resultDocuments.get(i);
            double score = Double.parseDouble(resultDoc.getFieldValue(SolrSearchEngineHelper.SCORE_FIELD)
                    .toString()) / maxScore;
            score = score > 1.0 ? 1.0 : score;

            String contenthubId = (String) resultDoc.getFieldValue(SolrFieldName.ID.toString());
            /*
             * String cmsId = (String) resultDoc.getFieldValue(SolrSearchEngineHelper.CMSID_FIELD); cmsId =
             * cmsId == null ? "" : cmsId;
             */

            String selectionText = (String) resultDoc.getFieldValue(SolrFieldName.CONTENT.toString());

            // score of the keyword is used as a weight for newly found document
            factory.createDocumentResource(contenthubId, 1.0, keyword.getScore() * score, keyword,
                selectionText/* , cmsId */);
        }
    }

    @SuppressWarnings("unused")
    private void omitNonMatchingResult(SearchContext searchContext) {
        OntModel contextModel = (SearchContextImpl) searchContext;
        ResIterator docResources = contextModel.listResourcesWithProperty(RDF.type,
            SearchVocabulary.DOCUMENT_RESOURCE);
        List<String> solrResultIds = new ArrayList<String>();

        SolrQuery query = SolrSearchEngineHelper
                .keywordQueryWithFacets("*:*", searchContext.getConstraints());
        QueryResponse solrResult = null;
        try {
            solrResult = server.query(query);
        } catch (SolrServerException e) {
            logger.warn("Error while querying with query : {} ", query, e);
        }
        // means query is done successfully, no problem about server or query
        if (solrResult != null) {
            SolrDocumentList resultDocuments = solrResult.getResults();
            for (int i = 0; i < resultDocuments.size(); i++) {
                SolrDocument resultDocument = resultDocuments.get(i);
                String docID = (String) resultDocument.getFieldValue("id");
                if (docID != null && !docID.equals("")) {
                    solrResultIds.add(docID);
                }
            }
        }

        while (docResources.hasNext()) {
            Resource docResource = docResources.next();
            String relatedDocument = docResource.getProperty(SearchVocabulary.RELATED_DOCUMENT).getObject()
                    .toString();
            if (!solrResultIds.contains(relatedDocument)) {
                /*
                 * means that documentResource is related with such a document that document does not apply
                 * facet constraints so that document resource and its external resources will be omitted from
                 * searchContext
                 */
                Filter<Statement> f = new Filter<Statement>() {
                    @Override
                    public boolean accept(Statement s) {
                        return s.getSubject().getPropertyResourceValue(RDF.type)
                                .equals(SearchVocabulary.EXTERNAL_RESOURCE);
                    }
                };
                List<Statement> omitEntity = contextModel
                        .listStatements(null, SearchVocabulary.RELATED_DOCUMENT, docResource).filterKeep(f)
                        .toList();
                // now all statements whose subject is document will be omitted
                List<Statement> omitDocument = contextModel.listStatements(docResource, null, (RDFNode) null)
                        .toList();

                try {
                    contextModel.remove(omitEntity);
                    contextModel.remove(omitDocument);
                } catch (Exception e) {
                    logger.warn("Resources could not be omitted from search context", e);
                }
            }
        }
    }

    @Override
    public Map<String,Object> getEngineProperties() {
        return properties;
    }

}
