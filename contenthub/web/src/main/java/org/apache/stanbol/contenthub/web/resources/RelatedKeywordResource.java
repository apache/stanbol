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

import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearchManager;
import org.apache.stanbol.contenthub.web.util.RestUtil;
import org.apache.stanbol.contenthub.web.writers.SearchResultWriter;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the web resourceclass which implements the search functionality of Contenthub to look for related
 * keywords, given a keyword.
 * 
 * @author anil.sinaci
 * 
 */
@Path("/contenthub/search/related")
public class RelatedKeywordResource extends BaseStanbolResource {

    private static Logger log = LoggerFactory.getLogger(RelatedKeywordResource.class);

    private static int AUTOCOMPLETED_KEYWORD_NUMBER = 10;

    private static String DEFAULT_AUTOCOMPLETE_SEARCH_FIELD = RDFS.label.getUnicodeString();

    private ReferencedSiteManager referencedSiteManager;

    private RelatedKeywordSearchManager relatedKeywordSearchManager;

    public RelatedKeywordResource(@Context ServletContext context) {
        referencedSiteManager = ContextHelper.getServiceFromContext(ReferencedSiteManager.class, context);
        relatedKeywordSearchManager = ContextHelper.getServiceFromContext(RelatedKeywordSearchManager.class,
            context);
    }
    
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    /**
     * HTTP GET method to retrieve related keywords from all resources defined within Contenthub.
     * 
     * @param keyword
     *            The keyword whose related keywords will be retrieved.
     * @param graphURI
     *            URI of the ontology to be used during the step in which related keywords are searched in
     *            ontology resources. If this parameter is {@code null}, then no related keywords are returned
     *            from ontology resources.
     * @param headers
     *            HTTP headers
     * @return JSON string which is constructed by {@link SearchResultWriter}. {@link SearchResult} returned
     *         by {@link RelatedKeywordSearchManager#getRelatedKeywordsFromAllSources(String, String)} only contains related keywords (no resultant documents
     *         or facet fields are returned within the {@link SearchResult}).
     * @throws SearchException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public final Response findAllRelatedKeywords(@QueryParam("keyword") String keyword,
                                                 @QueryParam("graphURI") String graphURI,
                                                 @Context HttpHeaders headers) throws SearchException {

        if (!RestUtil.isJSONaccepted(headers)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

		keyword = RestUtil.nullify(keyword);
        if (keyword == null) {
            String msg = "RelatedKeywordResource.findAllRelatedKeywords requires \"keyword\" parameter. \"graphURI\" is optional";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        graphURI = RestUtil.nullify(graphURI);
        SearchResult searchResult = relatedKeywordSearchManager.getRelatedKeywordsFromAllSources(keyword,
            graphURI);

        return prepareResponse(searchResult, headers);
    }

    /**
     * HTTP GET method to retrieve related keywords from ontology resources. Given the ontology URI, this
     * method looks for subsumption/hierarchy relations among the concepts to come up with related keywords.
     * 
     * @param keyword
     *            The keyword whose related keywords will be retrieved from ontology resources.
     * @param graphURI
     *            URI of the ontology in which related keywords will be searched. The ontology should be
     *            available in the Contenthub system.
     * @param headers
     *            HTTP headers
     * @return JSON string which is constructed by {@link SearchResultWriter}. {@link SearchResult} returned
     *         by {@link RelatedKeywordSearchManager#getRelatedKeywordsFromOntology(String, String)} contains only related keywords from ontology resources.
     *         (No resultant documents or facet fields are returned within the {@link SearchResult}).
     * @throws SearchException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/ontology")
    public final Response findOntologyRelatedKeywords(@QueryParam("keyword") String keyword,
                                                      @QueryParam("graphURI") String graphURI,
                                                      @Context HttpHeaders headers) throws SearchException {
        if (!RestUtil.isJSONaccepted(headers)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        keyword = RestUtil.nullify(keyword);
        if (keyword == null) {
            String msg = "RelatedKeywordResource.findOntologyRelatedKeywords requires \"keyword\" and \"graphURI\" parameters.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        graphURI = RestUtil.nullify(graphURI);
        if (graphURI == null) {
            String msg = "RelatedKeywordResource.findOntologyRelatedKeywords requires \"keyword\" and \"graphURI\" parameters.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        SearchResult searchResult = relatedKeywordSearchManager.getRelatedKeywordsFromOntology(keyword,
            graphURI);
        return prepareResponse(searchResult, headers);
    }

    /**
     * 
     * HTTP GET method to retrieve related keywords from the referenced sites.
     * 
     * @param keyword
     *            The keyword whose related keywords will be retrieved from referenced sites.
     * @param headers
     *            HTTP headers
     * @return JSON string which is constructed by {@link SearchResultWriter}. {@link SearchResult} returned
     *         by {@link RelatedKeywordSearchManager#getRelatedKeywordsFromReferencedSites(String)} contains
     *         only related keywords from referenced sites. (No resultant documents or facet fields are
     *         returned within the {@link SearchResult}).
     * @throws SearchException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/referencedsite")
    public final Response findReferencedSiteRelatedKeywords(@QueryParam("keyword") String keyword,
                                                            @Context HttpHeaders headers) throws SearchException {
        if (!RestUtil.isJSONaccepted(headers)) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        keyword = RestUtil.nullify(keyword);
        if (keyword == null) {
            String msg = "RelatedKeywordResource.findOntologyRelatedKeywords requires a \"keyword\" parameter.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        SearchResult searchResult = relatedKeywordSearchManager
                .getRelatedKeywordsFromReferencedSites(keyword);
        return prepareResponse(searchResult, headers);
    }

    private Response prepareResponse(SearchResult searchResult, HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(searchResult);
        rb.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_TYPE + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * TODO: Not completed yet.
     * This method is used to provide data to autocomplete component. It queries entityhub with the provided
     * query term.
     */
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/autocomplete")
    public final Response bringSuggestion(@QueryParam("pattern") String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return Response.noContent().build();
        }
        pattern = pattern.trim();
        pattern += "*";

        FieldQueryFactory qf = DefaultQueryFactory.getInstance();
        FieldQuery fieldQuery = qf.createFieldQuery();
        Collection<String> selectedFields = new ArrayList<String>();
        selectedFields.add(DEFAULT_AUTOCOMPLETE_SEARCH_FIELD);
        fieldQuery.addSelectedFields(selectedFields);
        fieldQuery.setConstraint(DEFAULT_AUTOCOMPLETE_SEARCH_FIELD, new TextConstraint(pattern,
                PatternType.wildcard, false, "en"));
        fieldQuery.setLimit(AUTOCOMPLETED_KEYWORD_NUMBER);
        fieldQuery.setOffset(0);

        List<String> result = new ArrayList<String>();
        QueryResultList<Representation> entityhubResult = referencedSiteManager.find(fieldQuery);
        for (Representation rep : entityhubResult) {
            result.add(rep.getFirst(DEFAULT_AUTOCOMPLETE_SEARCH_FIELD).toString());
        }

        JSONObject jResult = new JSONObject();
        try {
            jResult.put("completedKeywords", result);
        } catch (Exception e) {

        }
        return Response.ok(jResult.toString()).build();
    }

}
