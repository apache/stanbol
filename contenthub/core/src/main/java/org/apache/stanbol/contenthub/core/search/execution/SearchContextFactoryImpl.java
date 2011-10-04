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

package org.apache.stanbol.contenthub.core.search.execution;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.stanbol.contenthub.servicesapi.search.execution.ClassResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.DocumentResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.ExternalResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.IndividualResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.Keyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContextFactory;
import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.graph.Node;

/**
 * 
 * @author cihan
 * 
 */
public class SearchContextFactoryImpl implements SearchContextFactory {

    private static final double MAX_SCORE = 1.0;
    private static final double MAX_WEIGHT = 1.0;

    private final Logger logger = LoggerFactory.getLogger(SearchContextFactoryImpl.class);

    private SearchContext searchContext;

    private Map<String,QueryKeywordImpl> queryKeywords = new HashMap<String,QueryKeywordImpl>();
    private Map<String,KeywordImpl> keywords = new HashMap<String,KeywordImpl>();
    // DocumentURI -> DocumentResource
    private Map<String,DocumentResourceImpl> documents = new HashMap<String,DocumentResourceImpl>();

    private Map<String,ExternalResourceImpl> externals = new HashMap<String,ExternalResourceImpl>();

    private Map<Keyword,String> inverseKeywords = new HashMap<Keyword,String>();
    private Map<QueryKeyword,String> inverseQueryKeywords = new HashMap<QueryKeyword,String>();

    private Map<String,ClassResourceImpl> inverseClassResources = new HashMap<String,ClassResourceImpl>();
    private Map<String,IndividualResourceImpl> inverseIndividualResources = new HashMap<String,IndividualResourceImpl>();

    private String keywordName(String keyword) {
        StringBuilder sb = new StringBuilder();
        sb.append(SearchVocabulary.getUri()).append("individuals/").append(keyword);
        return sb.toString();
    }

    private String className() {
        StringBuilder sb = new StringBuilder();
        sb.append(SearchVocabulary.getUri()).append("relatedClasses/").append(UUID.randomUUID());
        return sb.toString();
    }

    private String individualName() {
        StringBuilder sb = new StringBuilder();
        sb.append(SearchVocabulary.getUri()).append("relatedIndividuals/").append(UUID.randomUUID());
        return sb.toString();
    }

    protected String documentName(String documentURI, Keyword keyword) {
        StringBuilder sb = new StringBuilder();
        sb.append(SearchVocabulary.getUri()).append("relatedDocuments/")
                .append(documentURI.substring(documentURI.lastIndexOf("/") + 1)).append(keyword.getKeyword());
        return sb.toString();
    }

    protected String externalName(String externalURI) {
        StringBuilder sb = new StringBuilder(SearchVocabulary.getUri());
        sb.append("externalEntity/").append(externalURI.replace("http://", ""));
        return sb.toString();
    }

    //
    protected QueryKeywordImpl getQueryKeyword(String uri) {
        if (queryKeywords.containsKey(uri)) {
            return queryKeywords.get(uri);
        } else {
            // FIXME search for already existing keywords in model
            throw new IllegalArgumentException("Query Keyword is not present in factory: " + uri);
        }
    }

    protected KeywordImpl getKeyword(String uri) {
        if (queryKeywords.containsKey(uri)) {
            return queryKeywords.get(uri);
        } else {
            if (keywords.containsKey(uri)) {
                return keywords.get(uri);
            } else {
                // FIXME search for already existing keywords in model
                throw new IllegalArgumentException("Keyword is not present in factory: " + uri);
            }
        }
    }

    // FIXME make not found check first
    protected String getQueryKeywordURI(QueryKeyword keyword) {
        if (inverseQueryKeywords.containsKey(keyword)) {
            logger.debug("Found query keyword {} in context factory", keyword.getKeyword());
        } else {
            logger.info("Query Keyword {} not found in context factory. Creating an instance ...",
                keyword.getKeyword());
            createQueryKeyword(keyword.getKeyword());
        }
        return inverseQueryKeywords.get(keyword);
    }

    protected String getKeywordURI(Keyword keyword) {
        if (keyword instanceof QueryKeyword) {
            if (inverseQueryKeywords.containsKey(keyword)) {
                logger.debug("Found keyword {} in context factory", keyword.getKeyword());
            } else {
                logger.info("Keyword {} not found in context factory. Creating an instance ...",
                    keyword.getKeyword());
                createQueryKeyword(keyword.getKeyword());
            }
            return inverseQueryKeywords.get(keyword);
        } else {
            if (inverseKeywords.containsKey(keyword)) {
                logger.debug("Found keyword {} in context factory", keyword.getKeyword());
            } else {
                logger.info("Keyword {} not found in context factory. Creating an instance ...",
                    keyword.getKeyword());
                createKeyword(keyword.getKeyword(), keyword.getScore(), keyword.getRelatedQueryKeyword());
            }
            return inverseKeywords.get(keyword);
        }

    }

    protected ClassResourceImpl getClassResource(String classURI) {
        if (inverseClassResources.containsKey(classURI)) {
            return inverseClassResources.get(classURI);
        } else {
            throw new IllegalArgumentException("Class resource with uri " + classURI + "not found ");
        }
    }

    protected IndividualResourceImpl getIndividualResource(String individualURI) {
        if (inverseIndividualResources.containsKey(individualURI)) {
            return inverseIndividualResources.get(individualURI);
        } else {
            throw new IllegalArgumentException("Individual resource with uri " + individualURI + "not found ");
        }
    }

    protected DocumentResourceImpl getDocumentResource(String documentURI) {
        if (documents.containsKey(documentURI)) {
            return documents.get(documentURI);
        } else {
            throw new IllegalArgumentException("Document resource with uri " + documentURI + "not found ");
        }
    }

    protected ExternalResourceImpl getExternalResource(String externalResourceURI) {
        if (externals.containsKey(externalResourceURI)) {
            return externals.get(externalResourceURI);
        } else {
            throw new IllegalArgumentException("External resource with uri " + externalResourceURI
                                               + "not found");
        }
    }

    @Override
    public QueryKeyword createQueryKeyword(String keyword) {
        Node n = Node.createURI(keywordName(keyword));
        QueryKeywordImpl k = new QueryKeywordImpl(n, (EnhGraph) searchContext, keyword, MAX_SCORE,
                MAX_WEIGHT, this);
        queryKeywords.put(k.getURI(), k);
        inverseQueryKeywords.put(k, k.getURI());
        return k;
    }

    @Override
    public Keyword createKeyword(String keyword, double score, QueryKeyword queryKeyword) {
        Node n = Node.createURI(keywordName(keyword));
        KeywordImpl k = new KeywordImpl(n, (EnhGraph) searchContext, keyword, MAX_WEIGHT, score, this);
        keywords.put(k.getURI(), k);
        inverseKeywords.put(k, k.getURI());
        if (queryKeyword != null) {
            queryKeyword.addRelatedKeyword(k);
        }
        return k;
    }

    @Override
    public ClassResource createClassResource(String classURI,
                                             double weight,
                                             double score,
                                             Keyword relatedKeyword) {

        try {
			if (inverseClassResources.containsKey(classURI)) {
			    ClassResource cr = inverseClassResources.get(classURI);
			    cr.updateScore(score, weight);
			    return cr;
			} else {
			    Node n = Node.createURI(className());
			    ClassResourceImpl cri = new ClassResourceImpl(n, (EnhGraph) searchContext, weight, score,
			            relatedKeyword, classURI, this);
			    cri.setDereferenceableURI(resolveReference(classURI));
			    inverseClassResources.put(classURI, cri);
			    return cri;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    @Override
    public IndividualResource createIndividualResource(String individualURI,
                                                       double weight,
                                                       double score,
                                                       Keyword relatedKeyword) {
        if (inverseIndividualResources.containsKey(individualURI)) {
            IndividualResource ir = inverseIndividualResources.get(individualURI);
            ir.updateScore(score, weight);
            return ir;
        } else {
            Node n = Node.createURI(individualName());
            IndividualResourceImpl iri = new IndividualResourceImpl(n, (EnhGraph) searchContext, weight,
                    score, relatedKeyword, individualURI, this);
            iri.setDereferenceableURI(resolveReference(individualURI));
            inverseIndividualResources.put(individualURI, iri);
            return iri;
        }
    }

    @Override
    public DocumentResource createDocumentResource(String documentURI,
                                                   double weight,
                                                   double score,
                                                   Keyword relatedKeyword,
                                                   String relatedText/*
                                                                      * , String contentRepositoryItem
                                                                      */) {
        String key = documentName(documentURI, relatedKeyword);
        if (documents.containsKey(key)) {
            DocumentResource dr = documents.get(key);
            dr.updateScore(score, weight);
            return dr;
        } else {
            Node n = Node.createURI(key);
            DocumentResourceImpl dri = new DocumentResourceImpl(n, (EnhGraph) searchContext, documentURI,
                    weight, score, relatedKeyword, this);
            dri.setRelatedText(relatedText);
            /* dri.setRelatedContentRepositoryItem(contentRepositoryItem); */
            documents.put(key, dri);
            return dri;
        }
    }

    @Override
    public ExternalResource createExternalResource(String reference,
                                                   double weight,
                                                   double score,
                                                   Keyword relatedKeyword) {
        String key = externalName(reference);
        if (documents.containsKey(key)) {
            ExternalResourceImpl er = externals.get(key);
            er.updateScore(score, weight);
            return er;
        } else {
            Node n = Node.createURI(key);
            ExternalResourceImpl eri = new ExternalResourceImpl(n, (EnhGraph) searchContext, weight, score,
                    reference, relatedKeyword, this);
            eri.setDereferenceableURI(reference);
            externals.put(key, eri);
            return eri;
        }
    }

    private String resolveReference(String uri) {
        return uri;
        /*
         String path = null;
         * if (resourceManager != null && uri != null) { path = resourceManager.getResourceFullPath(uri); } if
         * (path == null || path.isEmpty()) { path = uri; }
         */
        // TODO : that should return the dereferenceable uri of resource in ontology store
    }

    @Override
    public void setSearchContext(SearchContext searchContext) {
        this.searchContext = searchContext;

    }
}
