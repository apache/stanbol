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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FacetResult;
import org.apache.stanbol.contenthub.servicesapi.search.featured.FeaturedSearch;
import org.apache.stanbol.contenthub.servicesapi.search.featured.SearchResult;
import org.apache.stanbol.contenthub.servicesapi.search.related.RelatedKeywordSearchManager;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.web.util.JSONUtils;
import org.apache.stanbol.contenthub.web.util.RestUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * This class is the web resource which provides RESTful and HTTP interfaces for
 * {@link FeaturedSearch} services.
 * 
 * @author anil.sinaci
 * @author suat
 * 
 */
@Path("/contenthub/{index}/search/featured")
public class FeaturedSearchResource extends BaseStanbolResource {

	private final static Logger log = LoggerFactory
			.getLogger(FeaturedSearchResource.class);

	private TcManager tcManager;

	private FeaturedSearch featuredSearch;

	private String indexName;

	/**
	 * 
	 * @param context
	 * @param indexName
	 *            Name of the LDPath program (name of the Solr core/index) to be
	 *            used while storing this content item. LDPath programs can be
	 *            managed through {@link SemanticIndexManagerResource} or
	 *            {@link SemanticIndexManager}
	 * @throws IOException
	 * @throws InvalidSyntaxException
	 */
	public FeaturedSearchResource(@Context ServletContext context,
			@PathParam(value = "index") String indexName) throws IOException,
			InvalidSyntaxException {
		this.indexName = indexName;
		this.featuredSearch = ContextHelper.getServiceFromContext(
				FeaturedSearch.class, context);
		this.tcManager = ContextHelper.getServiceFromContext(TcManager.class,
				context);
	}

	@OPTIONS
	public Response handleCorsPreflight(@Context HttpHeaders headers) {
		ResponseBuilder res = Response.ok();
		enableCORS(servletContext, res, headers);
		return res.build();
	}

	/**
	 * HTTP GET method to make a featured search over Contenthub.
	 * 
	 * @param queryTerm
	 *            A keyword a statement or a set of keywords which can be
	 *            regarded as the query term.
	 * @param solrQuery
	 *            Solr query string. This is the string format which is accepted
	 *            by a Solr server. For example, {@code q="john doe"&fl=score}
	 *            is a valid value for this parameter. If this parameter exists,
	 *            search is performed based on this solrQuery and any queryTerms
	 *            are neglected.
	 * @param jsonCons
	 *            Constrainst in JSON format. These constraints are tranformed
	 *            to corresponding Solr queries to enable faceted search. Each
	 *            constraint is a facet field and values of the constraints maps
	 *            to the values of the facet fields in Solr queries.
	 * @param graphURI
	 *            URI of the ontology in which related keywords will be searched
	 *            by
	 *            {@link RelatedKeywordSearchManager#getRelatedKeywordsFromOntology(String, String)}
	 * @param offset
	 *            The offset of the document from which the resultant documents
	 *            will start as the search result. {@link offset} and
	 *            {@link limit} parameters can be used to make a pagination
	 *            mechanism for search results.
	 * @param limit
	 *            Maximum number of resultant documents to be returned as the
	 *            search result. {@link offset} and {@link limit} parameters can
	 *            be used to make a pagination mechanism for search results.
	 * @param fromStore
	 *            Special parameter for HTML view only.
	 * @param headers
	 *            HTTP headers
	 * @return HTML view or JSON representation of the search results or HTTP
	 *         BAD REQUEST(400)
	 * @throws IllegalArgumentException
	 * @throws SearchException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws SolrServerException
	 * @throws IOException
	 */
	@GET
	@Produces({ MediaType.TEXT_HTML, MediaType.APPLICATION_JSON })
	public final Response get(@QueryParam("queryTerm") String queryTerm,
			@QueryParam("solrQuery") String solrQuery,
			@QueryParam("constraints") String jsonCons,
			@QueryParam("graphURI") String graphURI,
			@QueryParam("offset") @DefaultValue("0") int offset,
			@QueryParam("limit") @DefaultValue("10") int limit,
			@QueryParam("fromStore") String fromStore,
			@Context HttpHeaders headers) throws IllegalArgumentException,
			SearchException, InstantiationException, IllegalAccessException,
			SolrServerException, IOException {
		MediaType acceptedHeader = RestUtil.getAcceptedMediaType(headers);

		this.queryTerm = queryTerm = RestUtil.nullify(queryTerm);
		solrQuery = RestUtil.nullify(solrQuery);
		graphURI = RestUtil.nullify(graphURI);
		jsonCons = RestUtil.nullify(jsonCons);
		this.offset = offset;
		this.pageSize = limit;

		if (acceptedHeader.isCompatible(MediaType.TEXT_HTML_TYPE)) {
			if (fromStore != null) {
				return Response.ok(new Viewable("index", this),
						MediaType.TEXT_HTML).build();
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
				return Response.ok(new Viewable("index", this),
						MediaType.TEXT_HTML).build();
			} else {
				ResponseBuilder rb = performSearch(queryTerm, solrQuery,
						jsonCons, graphURI, offset, limit,
						MediaType.TEXT_HTML_TYPE);
				addCORSOrigin(servletContext, rb, headers);
				return rb.build();
			}
		} else {
			if (queryTerm == null && solrQuery == null) {
				return Response
						.status(Status.BAD_REQUEST)
						.entity("Either 'queryTerm' or 'solrQuery' should be specified")
						.build();
			} else {
				ResponseBuilder rb = performSearch(queryTerm, solrQuery,
						jsonCons, graphURI, offset, limit,
						MediaType.APPLICATION_JSON_TYPE);
				addCORSOrigin(servletContext, rb, headers);
				return rb.build();
			}
		}
	}

	private ResponseBuilder performSearch(String queryTerm, String solrQuery,
			String jsonCons, String ontologyURI, int offset, int limit,
			MediaType acceptedMediaType) throws SearchException {

		if (solrQuery != null) {
			this.searchResults = featuredSearch.search(
					new SolrQuery(solrQuery), ontologyURI, indexName);
		} else if (queryTerm != null) {
			Map<String, List<Object>> constraintsMap = JSONUtils
					.convertToMap(jsonCons);
			this.chosenFacets = JSONUtils.convertToString(constraintsMap);

			SolrQuery sq;
			if (this.chosenFacets != null) {
				List<FacetResult> allAvailableFacets = featuredSearch
						.getAllFacetResults(indexName);
				sq = SolrQueryUtil.prepareFacetedSolrQuery(queryTerm,
						allAvailableFacets, constraintsMap);
			} else {
				sq = SolrQueryUtil.prepareDefaultSolrQuery(queryTerm);
			}
			sq.setStart(offset);
			sq.setRows(limit + 1);
			this.searchResults = featuredSearch.search(sq, ontologyURI,
					indexName);
		} else {
			log.error("Should never reach here!!!!");
			throw new SearchException(
					"Either 'queryTerm' or 'solrQuery' paramater should be set");
		}

		ResponseBuilder rb = null;
		if (acceptedMediaType.isCompatible(MediaType.TEXT_HTML_TYPE)) {
			// return HTML document
			/*
			 * For HTML view, sort facets according to their names
			 */
			this.searchResults.setFacets(sortFacetResults(this.searchResults
					.getFacets()));
			rb = Response.ok(new Viewable("result.ftl", this));
			rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML
					+ "; charset=utf-8");

		} else {
			// it is compatible with JSON (default) - return JSON
			rb = Response.ok(this.searchResults);
			rb.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON
					+ "; charset=utf-8");
		}
		return rb;
	}

	private List<FacetResult> sortFacetResults(List<FacetResult> facetResults) {
		List<FacetResult> orderedFacets = new ArrayList<FacetResult>();
		int annotatedFacetNum = 0;
		for (FacetResult fr : facetResults) {
			String facetName = fr.getFacetField().getName();
			if (fr.getFacetField().getValues() == null) {
				continue;
			} else if (SolrVocabulary.SolrFieldName
					.isAnnotatedEntityFacet(facetName)) {
				orderedFacets.add(annotatedFacetNum, fr);
				annotatedFacetNum++;
			} else {
				boolean inserted = false;
				for (int j = annotatedFacetNum; j < orderedFacets.size(); j++) {
					if (facetName.compareTo(orderedFacets.get(j)
							.getFacetField().getName()) < 0) {
						orderedFacets.add(j, fr);
						inserted = true;
						break;
					}
				}
				if (inserted == false) {
					orderedFacets.add(fr);
				}
			}
		}
		return orderedFacets;
	}

	/*
	 * Services to draw HTML view
	 */

	// Data holders for HTML view
	private List<String> ontologies = null;
	private String queryTerm = null;
	// private String solrQuery = null;
	// private String ldProgram = null;
	// private String graphURI = null;
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
		if (searchResults.getDocuments().size() <= pageSize) {
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

	public Object getDocuments() {
		if (searchResults.getDocuments().size() > pageSize) {
			return searchResults.getDocuments().subList(0, pageSize);
		} else {
			return searchResults.getDocuments();
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

	public String getIndexName() {
		return this.indexName;
	}
}
