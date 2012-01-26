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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.search.solr.util.SolrQueryUtil;
import org.apache.stanbol.contenthub.servicesapi.Constants;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;
import org.apache.stanbol.contenthub.web.util.JSONUtils;
import org.apache.stanbol.contenthub.web.util.RestUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * 
 * @author anil.sinaci
 * @author suat
 * 
 */
@Path("/contenthub/search/featured")
public class FeaturedSearchResource extends BaseStanbolResource {

    private final static Logger log = LoggerFactory.getLogger(FeaturedSearchResource.class);

    private TcManager tcManager;

    private FeaturedSearch featuredSearch;

    public FeaturedSearchResource(@Context ServletContext context) throws IOException, InvalidSyntaxException {
        featuredSearch = ContextHelper.getServiceFromContext(FeaturedSearch.class, context);
        tcManager = ContextHelper.getServiceFromContext(TcManager.class, context);
    }

    @POST
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public final Response post(@FormParam("queryTerm") String queryTerm,
                               @FormParam("solrQuery") String solrQuery,
                               @FormParam("ldProgram") String ldProgram,
                               @FormParam("constraints") String jsonCons,
                               @FormParam("graph") String graphURI,
                               @FormParam("offset") @DefaultValue("0") int offset,
                               @FormParam("limit") @DefaultValue("10") int limit,
                               @Context HttpHeaders headers) throws IllegalArgumentException,
                                                            InstantiationException,
                                                            IllegalAccessException,
                                                            SolrServerException,
                                                            SearchException,
                                                            IOException {
        return get(queryTerm, solrQuery, ldProgram, jsonCons, graphURI, offset, limit, null, headers);
    }

    @GET
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    public final Response get(@QueryParam("queryTerm") String queryTerm,
                              @QueryParam("solrQuery") String solrQuery,
                              @QueryParam("ldProgram") String ldProgram,
                              @QueryParam("constraints") String jsonCons,
                              @QueryParam("graphURI") String graphURI,
                              @QueryParam("offset") @DefaultValue("0") int offset,
                              @QueryParam("limit") @DefaultValue("10") int limit,
                              @QueryParam("fromStore") String fromStore,
                              @Context HttpHeaders headers) throws IllegalArgumentException,
                                                           SearchException,
                                                           InstantiationException,
                                                           IllegalAccessException,
                                                           SolrServerException,
                                                           IOException {
        MediaType acceptedHeader = RestUtil.getAcceptedMediaType(headers);

        this.queryTerm = queryTerm = RestUtil.nullify(queryTerm);
        solrQuery = RestUtil.nullify(solrQuery);
        ldProgram = RestUtil.nullify(ldProgram);
        graphURI = RestUtil.nullify(graphURI);
        jsonCons = RestUtil.nullify(jsonCons);
        this.offset = offset;
        this.pageSize = limit;

        if (acceptedHeader.isCompatible(MediaType.TEXT_HTML_TYPE)) {
            if(fromStore != null) {
                return Response.ok(new Viewable("index", this), MediaType.TEXT_HTML).build();
            }
            if (queryTerm == null && solrQuery == null) {
                this.ontologies = new ArrayList<String>();
                Set<UriRef> mGraphs = tcManager.listMGraphs();
                Iterator<UriRef> it = mGraphs.iterator();
                while (it.hasNext()) {
                    graphURI = it.next().getUnicodeString();
                    if (Constants.isGraphReserved(graphURI)) {
                        continue;
                    }
                    this.ontologies.add(graphURI);
                }
                return Response.ok(new Viewable("index", this), MediaType.TEXT_HTML).build();
            } else {
                ResponseBuilder rb = performSearch(queryTerm, solrQuery, ldProgram, jsonCons, graphURI,
                    offset, limit, MediaType.TEXT_HTML_TYPE);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
        } else {
            if (queryTerm == null && solrQuery == null) {
                return Response.status(Status.BAD_REQUEST)
                        .entity("Either 'queryTerm' or 'solrQuery' should be specified").build();
            } else {
                ResponseBuilder rb = performSearch(queryTerm, solrQuery, ldProgram, jsonCons, graphURI,
                    offset, limit, MediaType.APPLICATION_JSON_TYPE);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
        }
    }

    private ResponseBuilder performSearch(String queryTerm,
                                          String solrQuery,
                                          String ldProgramName,
                                          String jsonCons,
                                          String ontologyURI,
                                          int offset,
                                          int limit,
                                          MediaType acceptedMediaType) throws SearchException {

        if (solrQuery != null) {
            this.searchResults = featuredSearch.search(new SolrQuery(solrQuery), ontologyURI, ldProgramName);
        } else if (queryTerm != null) {
            Map<String,List<Object>> constraintsMap = JSONUtils.convertToMap(jsonCons);
            this.chosenFacets = JSONUtils.convertToString(constraintsMap);
            List<String> allAvailableFacetNames = featuredSearch.getFacetNames(ldProgramName);
            if (this.chosenFacets != null) {
                SolrQuery sq = SolrQueryUtil.prepareFacetedSolrQuery(queryTerm, allAvailableFacetNames,
                    constraintsMap);
                sq.setStart(offset);
                sq.setRows(limit + 1);
                this.searchResults = featuredSearch.search(sq, ontologyURI, ldProgramName);
            } else {
                SolrQuery sq = SolrQueryUtil.prepareDefaultSolrQuery(queryTerm, allAvailableFacetNames);
                sq.setStart(offset);
                sq.setRows(limit + 1);
                this.searchResults = featuredSearch.search(sq, ontologyURI, ldProgramName);
            }
        } else {
            log.error("Should never reach here!!!!");
        }

        ResponseBuilder rb = null;
        if (acceptedMediaType.isCompatible(MediaType.TEXT_HTML_TYPE)) {
            // return HTML document
            rb = Response.ok(new Viewable("result.ftl", this));
            rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML + "; charset=utf-8");

        } else {
            // it is compatible with JSON (default) - return JSON
            rb = Response.ok(this.searchResults);
            rb.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=utf-8");
        }
        return rb;
    }

    /*
     * Services to draw HTML view
     */

    // Data holders for HTML view
    private List<String> ontologies = null;
    private String queryTerm = null;
//    private String solrQuery = null;
//    private String ldProgram = null;
//    private String graphURI = null;
    private SearchResult searchResults = null;
    private String chosenFacets = null;
    private int offset = 0;
    private int pageSize = 10;

    // ///////////////////////////

    /*
     * Helper methods for HTML view
     */
    
    public Object getMoreRecentItems() {
        if (offset >= pageSize) {
            return new Object();
        } else {
            return null;
        }
    }

    public Object getOlderItems() {
        if (searchResults.getResultantDocuments().size() <= pageSize) {
            return null;
        } else {
            return new Object();
        }
    }
    
    public int getOffset() {
        return this.offset;
    }
    
    public int getPageSize() {
        return this.pageSize;
    }

    public Object getSearchResults() {
        return this.searchResults;
    }
    
    public Object getResultantDocuments() {
        if (searchResults.getResultantDocuments().size() > pageSize) {
            return searchResults.getResultantDocuments().subList(0, pageSize);
        } else {
            return searchResults.getResultantDocuments();
        }
    }

    public Object getOntologies() {
        return this.ontologies;
    }

    public Object getQueryTerm() {
        if (queryTerm != null) {
            return queryTerm;
        }
        return "";
    }

    public String getChosenFacets() {
        return this.chosenFacets;
    }
}
