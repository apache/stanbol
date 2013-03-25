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

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.stanbol.commons.viewable.Viewable;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.search.featured.util.SolrContentItemConverter;
import org.apache.stanbol.contenthub.servicesapi.ldpath.SemanticIndexManager;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.DocumentResult;
import org.apache.stanbol.contenthub.servicesapi.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.contenthub.web.util.RestUtil;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource to provide a CRU[D] REST API for content items and there related enhancements.
 * <p>
 * Creation is achieved using either POST requests on the root of the store or as PUT requests on the expected
 * content item URI.
 * <p>
 * Retrieval is achieved using simple GET requests on the content item or enhancement public URIs.
 */
@Path("/contenthub/{index}/store")
public class StoreResource extends BaseStanbolResource {

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(Arrays.asList(N3, N_TRIPLE,
        RDF_XML, TURTLE, X_TURTLE, RDF_JSON));

    private static final Logger log = LoggerFactory.getLogger(StoreResource.class);

    private SolrStore solrStore;

    private SolrSearch solrSearch;

    private Serializer serializer;

    private UriInfo uriInfo;

    private int offset = 0;

    private int pageSize = 5;

    private List<DocumentResult> recentlyEnhanced;

    private String indexName;

    /**
     * 
     * @param context
     * @param uriInfo
     * @param indexName
     *            Name of the LDPath program (name of the Solr core/index) to be used while storing this
     *            content item. LDPath programs can be managed through {@link SemanticIndexManagerResource} or
     *            {@link SemanticIndexManager}
     */
    public StoreResource(@Context ServletContext context,
                         @Context UriInfo uriInfo,
                         @PathParam(value = "index") String indexName) {

        this.indexName = indexName;
        this.solrStore = ContextHelper.getServiceFromContext(SolrStore.class, context);
        this.solrSearch = ContextHelper.getServiceFromContext(SolrSearch.class, context);
        this.serializer = ContextHelper.getServiceFromContext(Serializer.class, context);
        this.uriInfo = uriInfo;

        if (indexName == null) {
            log.error("Missing 'index' parameter");
            throw new IllegalArgumentException("Missing 'index' parameter");
        }
        if (this.solrStore == null) {
            log.error("Missing SolrStore service");
            throw new IllegalStateException("Missing SolrStore service");
        }
        if (this.solrSearch == null) {
            log.error("Missing SolrSearch service");
            throw new IllegalStateException("Missing SolrSearch service");
        }
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers, DELETE, GET, OPTIONS, POST);
        return res.build();
    }

    @OPTIONS
    @Path("/content/{uri:.+}")
    public Response handleCorsPreflightContent(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/download/{type}/{uri:.+}")
    public Response handleCorsPreflightDownload(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/metadata/{uri:.+}")
    public Response handleCorsPreflightMetadata(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/raw/{uri:.+}")
    public Response handleCorsPreflightRaw(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/{uri:.+}")
    public Response handleCorsPreflightURI(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers, DELETE, OPTIONS);
        return res.build();
    }

    /*
     * Methods for retrieving various parts e.g raw content, metadata of content items
     */

    /**
     * Cool URI handler for the uploaded resource. Based on the Accept header this service redirects the
     * incoming request to different endpoints in the following way:
     * <ul>
     * <li>If the Accept header contains the "text/html" value it is the request is redirected to the
     * <b>page</b> endpoint so that an HTML document corresponding to the ContentItem is drawn.</li>
     * <li>If the Accept header one of the RDF serialization formats defined {@link SupportedFormat}
     * annotation, the request is redirected to the <b>metadata</b> endpoint and metadata of the specified
     * {@link ContentItem} is returned.</li>
     * <li>If the previous two conditions are not satisfied the request is redirected to the <b>raw</b>
     * endpoint and raw content of the specified {@link ContentItem} is returned.</li>
     * </ul>
     * 
     * @param uri
     *            The URI of the resource in the Stanbol Contenthub store
     * @param headers
     *            HTTP headers
     * @return a redirection to either a browser view, the RDF metadata or the raw binary content
     */
    @GET
    @Path("/content/{uri:.+}")
    public Response getContent(@PathParam(value = "uri") String uri, @Context HttpHeaders headers) throws StoreException {
        uri = RestUtil.nullify(uri);
        if (uri == null) {
            return RestUtil.createResponse(servletContext, Status.BAD_REQUEST, "Missing 'uri' parameter",
                headers);
        }
        ContentItem ci = solrStore.get(uri, indexName);
        if (ci == null) {
            return RestUtil.createResponse(servletContext, Status.NOT_FOUND, null, headers);
        }

        // handle smart redirection to browser view
        for (MediaType mt : headers.getAcceptableMediaTypes()) {
            if (mt.toString().startsWith(TEXT_HTML)) {
                URI pageUri = uriInfo.getBaseUriBuilder().path("/contenthub").path(indexName)
                        .path("store/page").path(uri).build();
                ResponseBuilder rb = Response.temporaryRedirect(pageUri);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
        }

        // handle smart redirection to RDF metadata view
        for (MediaType mt : headers.getAcceptableMediaTypes()) {
            if (RDF_MEDIA_TYPES.contains(mt.toString())) {
                URI metadataUri = uriInfo.getBaseUriBuilder().path("/contenthub").path(indexName)
                        .path("store/metadata").path(uri).queryParam("format", mt.toString()).build();
                ResponseBuilder rb = Response.temporaryRedirect(metadataUri);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
        }
        URI rawUri = uriInfo.getBaseUriBuilder().path("/contenthub").path(indexName).path("store/raw")
                .path(uri).build();
        ResponseBuilder rb = Response.temporaryRedirect(rawUri);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * HTTP GET method specific for download operations. Raw data (content item) or only metadata of the
     * content item can be downloaded.
     * 
     * @param type
     *            Type can be {@code "metadata"} or {@code "raw"}. Based on the type, related parts of the
     *            content item will be prepared for download.
     * @param uri
     *            URI of the {@link ContentItem} in the Contenthub
     * @param format
     *            Rdf serialization format of metadata
     * @return Raw content item or metadata of the content item.
     * @throws IOException
     * @throws StoreException
     */
    @GET
    @Path("/download/{type}/{uri:.+}")
    public Response downloadContentItem(@PathParam(value = "type") String type,
                                        @PathParam(value = "uri") String uri,
                                        @QueryParam(value = "format") @DefaultValue(SupportedFormat.RDF_XML) String format,
                                        @Context HttpHeaders headers) throws IOException, StoreException {
        type = RestUtil.nullify(type);
        uri = RestUtil.nullify(uri);
        format = RestUtil.nullify(format);
        if (type == null) {
            return RestUtil.createResponse(servletContext, Status.BAD_REQUEST, "Missing 'type' parameter",
                headers);
        }
        if (uri == null) {
            return RestUtil.createResponse(servletContext, Status.BAD_REQUEST, "Missing 'uri' parameter",
                headers);
        }
        ContentItem ci = solrStore.get(uri, indexName);
        if (ci == null) {
            return RestUtil.createResponse(servletContext, Status.NOT_FOUND, null, headers);
        }
        if (type.equals("metadata")) {
            String fileName = URLEncoder.encode(uri, "utf-8") + "-metadata";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(baos, ci.getMetadata(), format);
            InputStream is = new ByteArrayInputStream(baos.toByteArray());

            ResponseBuilder response = Response.ok((Object) is);
            response.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.type("text/plain");
            addCORSOrigin(servletContext, response, headers);
            return response.build();
        } else if (type.equals("raw")) {
            String fileName = URLEncoder.encode(uri, "utf-8") + "-raw";
            ResponseBuilder response = Response.ok((Object) ci.getStream());
            response.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.type(ci.getMimeType());
            addCORSOrigin(servletContext, response, headers);
            return response.build();
        } else {
            throw new WebApplicationException(404);
        }

    }

    /**
     * HTTP GET method to retrieve the metadata of the content item. Generally, metadata contains the
     * enhancements of the content item.
     * 
     * @param uri
     *            URI id of the resource in the Stanbol Contenthub store
     * @return RDF representation of the metadata of the content item.
     * @throws IOException
     * @throws StoreException
     */
    @GET
    @Path("/metadata/{uri:.+}")
    public Response getContentItemMetaData(@PathParam(value = "uri") String uri,
                                           @QueryParam(value = "format") @DefaultValue(SupportedFormat.RDF_XML) String format,
                                           @Context HttpHeaders headers) throws IOException, StoreException {
        uri = RestUtil.nullify(uri);
        if (uri == null) {
            return RestUtil.createResponse(servletContext, Status.BAD_REQUEST, "Missing 'uri' parameter",
                headers);
        }
        ContentItem ci = solrStore.get(uri, indexName);
        if (ci == null) {
            return RestUtil.createResponse(servletContext, Status.NOT_FOUND, null, headers);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, ci.getMetadata(), format);
        ResponseBuilder rb = Response.ok(out.toString(), "text/plain");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * HTTP GET method to retrieve the raw content item.
     * 
     * @param uri
     *            URI of the resource in the Stanbol Contenthub store
     * @return Raw data of the content item.
     * @throws IOException
     * @throws StoreException
     */
    @GET
    @Path("/raw/{uri:.+}")
    public Response getRawContent(@PathParam(value = "uri") String uri, @Context HttpHeaders headers) throws IOException,
                                                                                                     StoreException {
        uri = RestUtil.nullify(uri);
        if (uri == null) {
            return RestUtil.createResponse(servletContext, Status.BAD_REQUEST, "Missing 'uri' parameter",
                headers);
        }
        ContentItem ci = solrStore.get(uri, indexName);
        if (ci == null) {
            return RestUtil.createResponse(servletContext, Status.NOT_FOUND, null, headers);
        }
        ResponseBuilder rb = Response.ok(ci.getStream(), ci.getMimeType());
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /*
     * Services for content item creation
     */

    /**
     * <p>
     * HTTP POST method to create a content item in Contenthub. This method takes a {@link ContentItem} object
     * directly. This means that the values provided for this service will be parsed by the multipart mime
     * serialization of Content Items. (see the following links: <a href=
     * 
     * "http://incubator.apache.org/stanbol/docs/trunk/components/enhancer/contentitem.html#multipart_mime_serialization"
     * >Content Item Multipart Serialization</a> and <a
     * href="http://incubator.apache.org/stanbol/docs/trunk/components/enhancer/enhancerrest.html">Using the
     * multi-part content item RESTful API extensions</a>)
     * </p>
     * <p>
     * If the passed {@link ContentItem} does already have the <b>metadata</b> part, it is not sent to Stanbol
     * enhancer to be enhanced.
     * </p>
     * 
     * @param ci
     *            {@link ContentItem} to be stored.
     * @param title
     *            The title for the content item. Titles can be used to present summary of the actual content.
     *            For example, search results are presented by showing the titles of resultant content items.
     * @param chain
     *            name of a particular {@link Chain} in which the enhancement engines are ordered according to
     *            a specific use case or need
     * @param headers
     *            HTTP Headers
     * @return URI of the newly created contentitem
     * @throws StoreException
     * @throws URISyntaxException
     */
    @POST
    @Consumes(MULTIPART_FORM_DATA)
    public Response createContentItem(ContentItem ci,
                                      @QueryParam(value = "title") String title,
                                      @QueryParam(value = "chain") String chain,
                                      @Context HttpHeaders headers) throws StoreException, URISyntaxException {
        title = RestUtil.nullify(title);
        chain = RestUtil.nullify(chain);
        MediaType acceptedHeader = RestUtil.getAcceptedMediaType(headers, null);
        if (title != null) {
            ci.addPart(SolrStore.TITLE_URI, title);
        }
        if (ci.getMetadata() == null || ci.getMetadata().isEmpty()) {
            solrStore.enhanceAndPut(ci, indexName, chain);
        } else {
            solrStore.put(ci, indexName);
        }

        ResponseBuilder rb = null;
        if (acceptedHeader != null && acceptedHeader.isCompatible(MediaType.TEXT_HTML_TYPE)) {
            // use a redirect to point browsers to newly created content
            rb = Response.seeOther(makeRedirectionURI(ci.getUri().getUnicodeString()));
        } else {
            rb = Response.created(makeRedirectionURI(ci.getUri().getUnicodeString()));
        }
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * HTTP POST method to create a content item in Contenthub.
     * 
     * @param data
     *            Raw data of the content item
     * 
     * @param uri
     *            URI for the content item. If not supplied, Contenthub automatically assigns a unique ID
     *            (uri) to the content item.
     * @param title
     *            The title for the content item. Titles can be used to present summary of the actual content.
     *            For example, search results are presented by showing the titles of resultant content items.
     * @param chain
     *            name of a particular {@link Chain} in which the enhancement engines are ordered according to
     *            a specific use case or need
     * @param headers
     *            HTTP headers
     * @return Redirects to "contenthub/{indexName}/store/content/uri" which shows the content item in the
     *         HTML view.
     * @throws URISyntaxException
     * @throws EngineException
     * @throws StoreException
     */
    @POST
    @Consumes(WILDCARD)
    public Response createContentItemWithRawData(byte[] data,
                                                 @QueryParam(value = "uri") String uri,
                                                 @QueryParam(value = "title") String title,
                                                 @QueryParam(value = "chain") String chain,
                                                 @Context HttpHeaders headers) throws URISyntaxException,
                                                                              EngineException,
                                                                              StoreException {
        uri = RestUtil.nullify(uri);
        title = RestUtil.nullify(title);
        chain = RestUtil.nullify(chain);
        if (uri == null) {
            uri = ContentItemHelper.makeDefaultUrn(data).getUnicodeString();
        }
        return createEnhanceAndRedirect(data, headers.getMediaType(), uri, false, title, chain, headers);
    }

    /**
     * HTTP POST method to create a content item in Contenthub. This method requires the content to be
     * text-based.
     * 
     * @param uri
     *            Optional URI for the content item to be created.
     * @param content
     *            Actual content in text format. If this parameter is supplied, {@link url} is ommitted.
     * @param url
     *            URL where the actual content resides. If this parameter is supplied (and {@link content} is
     *            {@code null}, then the content is retrieved from this url.
     * @param title
     *            The title for the content item. Titles can be used to present summary of the actual content.
     *            For example, search results are presented by showing the titles of resultant content items.
     * @param chain
     *            name of a particular {@link Chain} in which the enhancement engines are ordered according to
     *            a specific use case or need
     * @param headers
     *            HTTP headers (optional)
     * @return Redirects to "contenthub/{indexName}/store/content/uri" which shows the content item in the
     *         HTML view.
     * @throws URISyntaxException
     * @throws EngineException
     * @throws MalformedURLException
     * @throws IOException
     * @throws StoreException
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response createContentItemFromForm(@FormParam("uri") String uri,
                                              @FormParam("content") String content,
                                              @FormParam("url") String url,
                                              @FormParam("title") String title,
                                              @FormParam("chain") String chain,
                                              @Context HttpHeaders headers) throws URISyntaxException,
                                                                           EngineException,
                                                                           MalformedURLException,
                                                                           IOException,
                                                                           StoreException {
        uri = RestUtil.nullify(uri);
        url = RestUtil.nullify(url);
        title = RestUtil.nullify(title);
        chain = RestUtil.nullify(chain);
        return createContentItemFromForm(uri, content, url, headers, title, chain);
    }

    private Response createContentItemFromForm(String uri,
                                               String content,
                                               String url,
                                               HttpHeaders headers,
                                               String title,
                                               String chain) throws URISyntaxException,
                                                            EngineException,
                                                            MalformedURLException,
                                                            IOException,
                                                            StoreException {
        byte[] data = null; // TODO: rewrite me in a streamable way to avoid
        // buffering all the content in memory
        MediaType mt = null;
        if (content != null && !content.trim().isEmpty()) {
            data = content.getBytes();
            mt = TEXT_PLAIN_TYPE;
        } else if (url != null && !url.trim().isEmpty()) {
            try {
                URLConnection uc = (new URL(url)).openConnection();
                data = IOUtils.toByteArray(uc.getInputStream());
                mt = MediaType.valueOf(uc.getContentType());
            } catch (IOException e) {
                log.error("Failed to obtain content from the remote URL: {}", url, e);
                throw e;
            }
        }
        if (data != null && mt != null) {
            if (uri == null || uri.trim().equals("")) {
                uri = ContentItemHelper.makeDefaultUrn(data).getUnicodeString();
            }
            return createEnhanceAndRedirect(data, mt, uri, true, title, chain, headers);
        } else {
            log.error(String
                    .format(
                        "There should be valid values for the media type and content. Media type: %s, content==null: %b",
                        mt, content == null));
            throw new IllegalStateException(
                    String.format(
                        "There should be valid values for the media type and content. Media type: %s, content==null: %b",
                        mt, content == null));
        }
    }

    /*
     * This method takes "title" argument so that it would be easy to specify title while calling RESTful
     * services. If the title is specified explicitly it is set as the title otherwise, it is searched in the
     * constraints.
     */
    private Response createEnhanceAndRedirect(byte[] content,
                                              MediaType mediaType,
                                              String uri,
                                              boolean useExplicitRedirect,
                                              String title,
                                              String chain,
                                              HttpHeaders headers) throws EngineException,
                                                                  URISyntaxException,
                                                                  StoreException {

        ContentItem ci = solrStore.create(content, uri, title, mediaType.toString());
        solrStore.enhanceAndPut(ci, indexName, chain);
        if (useExplicitRedirect) {
            // use an redirect to point browsers to newly created content
            ResponseBuilder rb = Response.seeOther(makeRedirectionURI(ci.getUri().getUnicodeString()));
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } else {
            ResponseBuilder rb = Response.created(makeRedirectionURI(ci.getUri().getUnicodeString()));
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }

    private URI makeRedirectionURI(String uri) throws URISyntaxException {
        return new URI(uriInfo.getBaseUri() + "contenthub/" + indexName + "/store/content/" + uri);
    }

    /**
     * This method deletes the {@link ContentItem} specified by the given {@link QueryParam}.
     * 
     * @param uri
     *            URI of the {@link ContentItem} to be deleted.
     * @param headers
     * @return {@link Response#ok()} if the removal is successful
     * @throws StoreException
     */
    @DELETE
    public Response deleteContentItemByForm(@QueryParam(value = "uri") String uri,
                                            @Context HttpHeaders headers) throws StoreException {
        return deleteContentItem(uri, headers);
    }

    /**
     * HTTP DELETE method to delete a content item from Contenhub.
     * 
     * @param uri
     *            URI of the content item to be deleted.
     * @return HTTP OK
     * @throws StoreException
     */
    @DELETE
    @Path("/{uri:.+}")
    public Response deleteContentItem(@PathParam(value = "uri") String uri, @Context HttpHeaders headers) throws StoreException {
        uri = RestUtil.nullify(uri);
        if (uri == null) {
            return RestUtil.createResponse(servletContext, Status.BAD_REQUEST, "Missing 'uri' parameter",
                headers);
        }
        ContentItem ci = solrStore.get(uri, indexName);
        if (ci == null) {
            return RestUtil.createResponse(servletContext, Status.NOT_FOUND, null, headers);
        }
        solrStore.deleteById(uri, indexName);
        return RestUtil.createResponse(servletContext, Status.OK, null, headers);
    }

    /*
     * Services to draw HTML view
     */
    @GET
    @Produces(TEXT_HTML + ";qs=2")
    public Viewable getView(@Context ServletContext context,
                            @QueryParam(value = "offset") int offset,
                            @QueryParam(value = "pageSize") @DefaultValue("5") int pageSize) throws IllegalArgumentException,
                                                                                            IOException,
                                                                                            InvalidSyntaxException {
        this.offset = offset;
        this.pageSize = pageSize;
        this.recentlyEnhanced = new ArrayList<DocumentResult>();

        if (!(solrSearch instanceof SolrSearch)) {
            throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to obtain default implementation for SolrSearch").build());
        }

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", "*:*");
        params.set("sort", SolrFieldName.CREATIONDATE.toString() + " desc");
        params.set("start", offset);
        // always request 1 more to arrange the "Prev-Next" links correctly
        params.set("rows", pageSize + 1);

        QueryResponse res;
        try {
            res = solrSearch.search(params, indexName);
        } catch (SearchException e) {
            log.error("Failed to retrieve submitted documents", e);
            return new Viewable("index", this);
        }

        for (SolrDocument result : res.getResults()) {
            recentlyEnhanced.add(SolrContentItemConverter.solrDocument2solrContentItem(result, uriInfo
                    .getBaseUri().toString(), indexName));
        }

        return new Viewable("index", this);
    }

    @Path("/page/{uri:.+}")
    @Produces(TEXT_HTML)
    public ContentItemResource getContentItemView(@PathParam(value = "uri") String uri) throws IOException,
                                                                                       StoreException {
        ContentItem ci = solrStore.get(uri, indexName);
        if (ci == null) {
            throw new WebApplicationException(404);
        }
        return new ContentItemResource(uri, ci, uriInfo, "/contenthub/" + indexName + "/store/download",
                serializer, servletContext, null);
    }

    // Helper methods for HTML view

    public List<DocumentResult> getRecentlyEnhancedItems() throws ParseException {
        if (recentlyEnhanced.size() > pageSize) {
            return recentlyEnhanced.subList(0, pageSize);
        } else {
            return recentlyEnhanced;
        }
    }

    public URI getMoreRecentItemsUri() {
        if (offset >= pageSize) {
            return uriInfo.getBaseUriBuilder().path("contenthub").path(indexName).path("store")
                    .queryParam("offset", offset - pageSize).build();
        } else {
            return null;
        }
    }

    public URI getOlderItemsUri() {
        if (recentlyEnhanced.size() <= pageSize) {
            return null;
        } else {
            return uriInfo.getBaseUriBuilder().path("contenthub").path(indexName).path("store")
                    .queryParam("offset", offset + pageSize).build();
        }
    }

    public String getIndexName() {
        return this.indexName;
    }
}