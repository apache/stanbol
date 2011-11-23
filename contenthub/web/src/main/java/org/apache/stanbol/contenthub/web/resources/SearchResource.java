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

package org.apache.stanbol.contenthub.web.resources;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.stanbol.commons.solr.SolrServerProviderManager;
import org.apache.stanbol.commons.solr.SolrServerTypeEnum;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.core.utils.JSONUtils;
import org.apache.stanbol.contenthub.core.utils.SearchUtils;
import org.apache.stanbol.contenthub.servicesapi.search.Search;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;
import org.apache.stanbol.contenthub.servicesapi.search.execution.DocumentResource;
import org.apache.stanbol.contenthub.servicesapi.search.execution.QueryKeyword;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.processor.SearchProcessor;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.contenthub.web.search.model.EngineInfo;
import org.apache.stanbol.contenthub.web.search.model.SearchInfo;
import org.apache.stanbol.contenthub.web.search.model.TempSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * 
 * @author cihan
 * @author anil.sinaci
 * 
 */
@Path("/contenthub/search")
public class SearchResource extends BaseStanbolResource {
    private static final Logger logger = LoggerFactory.getLogger(SearchResource.class);

    private Search searcher;
    private SearchProcessor processor;
    private TcManager tcManager;
    private Object templateData = null;
    private Object facets = null;

    private ManagedSolrServer solrDirectoryManager;
    private SolrServerProviderManager solrServerProviderManager;

    public SearchResource(@Context ServletContext context) {
        searcher = ContextHelper.getServiceFromContext(Search.class, context);
        tcManager = ContextHelper.getServiceFromContext(TcManager.class, context);
        processor = ContextHelper.getServiceFromContext(SearchProcessor.class, context);
        solrServerProviderManager = ContextHelper.getServiceFromContext(SolrServerProviderManager.class,
            context);
        solrDirectoryManager = ContextHelper.getServiceFromContext(ManagedSolrServer.class, context);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public final Response get() {
        try {
            SearchInfo si = new SearchInfo();
            Set<UriRef> mGraphs = tcManager.listMGraphs();
            Iterator<UriRef> it = mGraphs.iterator();
            while (it.hasNext()) {
                String graphURI = it.next().getUnicodeString();
                if (SearchUtils.isGraphReserved(graphURI)) continue;
                si.getOntologies().add(graphURI);
            }

            for (SearchEngine engine : processor.listEngines()) {
                si.getEngines().add(new EngineInfo(engine.toString(), engine.getClass().getCanonicalName()));
            }
            this.templateData = si;
            return Response.ok(new Viewable("index", this), MediaType.TEXT_HTML).build();

        } catch (Exception e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public final Response search(@FormParam("graph") String graphURI,
                                 @FormParam("keywords") String keywords,
                                 @FormParam("engines[]") List<String> engines,
                                 @FormParam("constraints") String jsonCons) throws IllegalArgumentException,
                                                                           InstantiationException,
                                                                           IllegalAccessException,
                                                                           SolrServerException,
                                                                           IOException {
        Map<String,List<Object>> facetMap = JSONUtils.convertToMap(jsonCons);
        String[] keywordArray = null;
        if (keywords.startsWith("\"") && keywords.endsWith("\"")) {
            keywordArray = new String[1];
            keywordArray[0] = keywords;
        } else {
            // Separate the keywords only by space character.
            keywordArray = keywords.split(" ");
        }

        // FIXME A better implementation should be used instead of this casting.
        SearchContext searchContext = (SearchContext) searcher.search(keywordArray, graphURI, engines,
            facetMap);
        this.facets = getConstraints(searchContext);
        this.templateData = new TempSearchResult(searchContext);
        return Response.ok(new Viewable("result", this)).build();
    }

    private Object getConstraints(SearchContext sc) throws InstantiationException,
                                                   IllegalAccessException,
                                                   SolrServerException,
                                                   IllegalArgumentException,
                                                   IOException {

        SolrServer server = null;
        if (solrDirectoryManager != null) {
            if (!solrDirectoryManager.isManagedIndex("contenthub")) {
                solrDirectoryManager.createSolrIndex("contenthub", "contenthub", null);
            }
            server = solrServerProviderManager.getSolrServer(SolrServerTypeEnum.EMBEDDED, "contenthub");
        }

        QueryKeyword queryKeywords = sc.getQueryKeyWords().get(0);
        List<DocumentResource> docList = queryKeywords.getRelatedDocumentResources();
        StringBuilder queryBuilder = new StringBuilder();
        for (DocumentResource doc : docList) {
            queryBuilder.append(SolrFieldName.ID.toString());
            queryBuilder.append(":\"");
            queryBuilder.append(doc.getDocumentURI());
            queryBuilder.append("\"");
            queryBuilder.append(SolrVocabulary.SOLR_OR);
        }
        String query = queryBuilder.toString();
        if (query.length() > 4) {
            query = query.substring(0, query.length() - 4);
        } else {
            return null;
        }
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query).setFacetMinCount(1);
        SolrDocumentList sdl = server.query(solrQuery).getResults();

        Set<String> fields = new HashSet<String>();
        for (SolrDocument sd : sdl) {
            Iterator<String> itr = sd.getFieldNames().iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                fields.add(fieldName);
            }
        }
        solrQuery.setFacet(true);
        for (String field : fields) {
            if (!SolrFieldName.isNameReserved(field) && !SolrVocabulary.isNameExcluded(field)) {
                solrQuery.addFacetField(field);
            }
        }
        solrQuery.setRows(0);

        QueryResponse result = server.query(solrQuery);
        List<FacetField> facets = result.getFacetFields();
        logger.debug(facets.toString());
        return facets;
    }

    public Object getTemplateData() {
        return templateData;
    }

    public Object getFacets() {
        return facets;
    }

    // TODO: This method SHOULD be written again, maybe as a SEPERATE class
    /**
     * this method is written just to see that we can explore from search keyword using entityhub
     * 
     * @param queryKeywords
     *            is the all keywords seperated by " " that has been entered in search interface
     * @return is the List of all related entity names
     */
    /*
     * public void exploreFromKeyword(String queryKeywords) { EntityHubClient ehc =
     * EntityHubClient.getInstance("http://localhost:8080/entityhub"); String keyword =
     * queryKeywords.replaceAll(" ","_"); List<String> types = new ArrayList<String>();
     * 
     * OntModel resultModel = ehc.referencedSiteFind(keyword, "en"); if(resultModel != null) { ExploreHelper
     * explorer = new ExploreHelper(resultModel);
     * 
     * Map<String, List<String>> resultMap = explorer.getSuggestedKeywords(); } }
     */
}
