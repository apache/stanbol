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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.search.featured.util.SolrContentItemConverter;
import org.apache.stanbol.contenthub.search.solr.SolrSearchImpl;
import org.apache.stanbol.contenthub.servicesapi.ldpath.LDProgramManager;
import org.apache.stanbol.contenthub.servicesapi.search.SearchException;
import org.apache.stanbol.contenthub.servicesapi.search.featured.ResultantDocument;
import org.apache.stanbol.contenthub.servicesapi.search.solr.SolrSearch;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrStore;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.contenthub.web.util.JSONUtils;
import org.apache.stanbol.enhancer.jersey.resource.ContentItemResource;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.codehaus.jettison.json.JSONException;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import eu.medsea.mimeutil.MimeUtil2;

/**
 * Resource to provide a CRU[D] REST API for content items and there related enhancements.
 * <p>
 * Creation is achieved using either POST requests on the root of the store or as PUT requests on the expected
 * content item URI.
 * <p>
 * Retrieval is achieved using simple GET requests on the content item or enhancement public URIs.
 * <p>
 * Update is achieved by issue a PUT request on an existing content item public URI.
 */
@Path("/contenthub/store")
public class StoreResource extends BaseStanbolResource {

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(Arrays.asList(N3, N_TRIPLE,
        RDF_XML, TURTLE, X_TURTLE, RDF_JSON));

    private static final Logger log = LoggerFactory.getLogger(StoreResource.class);

    protected TcManager tcManager;

    protected SolrStore solrStore;

    protected SolrSearch solrSearch;

    protected Serializer serializer;

    protected UriInfo uriInfo;

    protected int offset = 0;

    protected int pageSize = 5;

    protected List<ResultantDocument> recentlyEnhanced;

    protected MimeUtil2 mimeIdentifier;

    public StoreResource(@Context ServletContext context, @Context UriInfo uriInfo) {

        solrStore = ContextHelper.getServiceFromContext(SolrStore.class, context);
        solrSearch = ContextHelper.getServiceFromContext(SolrSearch.class, context);
        tcManager = ContextHelper.getServiceFromContext(TcManager.class, context);
        serializer = ContextHelper.getServiceFromContext(Serializer.class, context);
        this.uriInfo = uriInfo;

        if (solrStore == null) {
            log.error("Missing Solr Store Service");
            throw new WebApplicationException(404);
        }
        if (solrSearch == null) {
            log.error("Missing Solr Search Service");
            throw new WebApplicationException(404);
        }
        if (tcManager == null) {
            log.error("Missing tcManager");
            throw new WebApplicationException(404);
        }
        mimeIdentifier = new MimeUtil2();
        mimeIdentifier.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector");
        mimeIdentifier.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
    }

    /*
     * Methods for retrieving various parts e.g raw content, metadata of content items
     */

    /**
     * Cool URI handler for the uploaded resource.
     * 
     * @param contentURI
     *            The URI of the resource in the Stanbol Contenthub store
     * @param headers
     *            HTTP headers
     * @return a redirection to either a browser view, the RDF metadata or the raw binary content
     */
    @GET
    @Path("/content/{uri:.+}")
    public Response getContent(@PathParam(value = "uri") String contentURI, @Context HttpHeaders headers) throws StoreException {

        ContentItem ci = solrStore.get(contentURI);
        if (ci == null) {
            throw new WebApplicationException(404);
        }

        // handle smart redirection to browser view
        for (MediaType mt : headers.getAcceptableMediaTypes()) {
            if (mt.toString().startsWith(TEXT_HTML)) {
                URI pageUri = uriInfo.getBaseUriBuilder().path("/contenthub/store/page").path(contentURI)
                        .build();
                return Response.temporaryRedirect(pageUri).build();
            }
        }

        // handle smart redirection to RDF metadata view
        for (MediaType mt : headers.getAcceptableMediaTypes()) {
            if (RDF_MEDIA_TYPES.contains(mt.toString())) {
                URI metadataUri = uriInfo.getBaseUriBuilder().path("/contenthub/store/metadata")
                        .path(contentURI).build();
                return Response.temporaryRedirect(metadataUri).build();
            }
        }
        URI rawUri = uriInfo.getBaseUriBuilder().path("/contenthub/store/raw").path(contentURI).build();
        return Response.temporaryRedirect(rawUri).build();
    }

    /**
     * HTTP GET method specific for download operations. Raw data (content item) or only metadata of the
     * content item can be downloaded.
     * 
     * @param type
     *            Type can be {@code "metadata"} or {@code "raw"}. Based on the type, related parts of the
     *            content item will be prepared for download.
     * @param contentURI
     *            URI of the resource in the Stanbol Contenthub store
     * @return Raw content item or metadata of the content item.
     * @throws IOException
     * @throws StoreException
     */
    @GET
    @Path("/download/{type}/{uri:.+}")
    public Response downloadContentItem(@PathParam(value = "type") String type,
                                        @PathParam(value = "uri") String contentURI) throws IOException,
                                                                                    StoreException {

        ContentItem ci = solrStore.get(contentURI);
        if (ci == null) {
            throw new WebApplicationException(404);
        }
        if (type.equals("metadata")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            serializer.serialize(out, ci.getMetadata(), SupportedFormat.RDF_XML);
            String fileName = contentURI + "-metadata";
            File file = new File(fileName);
            boolean success = file.createNewFile();
            if (success) {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));
                bufferedWriter.write(out.toString());
                bufferedWriter.close();
            } else {
                log.error("File already exists");
            }

            ResponseBuilder response = Response.ok((Object) file);
            response.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.type("text/plain");
            return response.build();
        } else if (type.equals("raw")) {
            // TODO: It is only for text content
            String fileName = contentURI + "-raw";
            File file = new File(fileName);
            boolean success = file.createNewFile();
            if (success) {
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));
                bufferedWriter.write(IOUtils.toString(ci.getStream(), "UTF-8"));
                bufferedWriter.close();
            } else {
                log.error("File already exists");
            }

            ResponseBuilder response = Response.ok((Object) file);
            response.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.type(ci.getMimeType());
            return response.build();
        } else {
            throw new WebApplicationException(404);
        }

    }

    /**
     * HTTP GET method to retrieve the metadata of the content item. Generally, metadata contains the
     * enhancements of the content item.
     * 
     * @param contentURI
     *            URI id of the resource in the Stanbol Contenthub store
     * @return RDF representation of the metadata of the content item.
     * @throws IOException
     * @throws StoreException
     */
    @GET
    @Path("/metadata/{uri:.+}")
    public Response getContentItemMetaData(@PathParam(value = "uri") String contentURI) throws IOException,
                                                                                       StoreException {
        ContentItem ci = solrStore.get(contentURI);
        if (ci == null) {
            throw new WebApplicationException(404);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, ci.getMetadata(), SupportedFormat.RDF_XML);

        return Response.ok(out.toString(), "text/plain").build();
    }

    /**
     * HTTP GET method to retrieve the raw content item.
     * 
     * @param contentURI
     *            URI of the resource in the Stanbol Contenthub store
     * @return Raw data of the content item.
     * @throws IOException
     * @throws StoreException
     */
    @GET
    @Path("/raw/{uri:.+}")
    public Response getRawContent(@PathParam(value = "uri") String contentURI) throws IOException,
                                                                              StoreException {
        ContentItem ci = solrStore.get(contentURI);
        if (ci == null) {
            throw new WebApplicationException(404);
        }

        return Response.ok(ci.getStream(), ci.getMimeType()).build();
    }

    /**
     * This method creates the JSON string of a content item (to be edited) to display it in the HTML view.
     * 
     * @param contentURI
     *            URI id of the resource in the Stanbol Contenthub store
     * @return JSON representation of the {@link SolrContentItem}
     * @throws StoreException
     */
    @GET
    @Path("/edit/{uri:.+}")
    public String editContentItem(@PathParam(value = "uri") String contentURI) throws StoreException {
        SolrContentItem sci = (SolrContentItem) solrStore.get(contentURI);
        if (sci == null) {
            throw new WebApplicationException(404);
        }

        String jsonString;
        try {
            jsonString = JSONUtils.createJSONString(sci);
        } catch (JSONException e) {
            throw new StoreException(e.getMessage(), e);
        }
        return jsonString;
    }

    /*
     * Services for content item creation
     */
    // TODO other parameters like title, ldprogram should be considered for this service
    /**
     * HTTP POST method to create a content item in Contenthub. This is the very basic method to create the
     * content item. The payload of the POST method should include the raw data of the content item to be
     * created. This method stores the content in the default Solr index ("contenthub").
     * 
     * @param data
     *            Raw data of the content item
     * @param headers
     *            HTTP Headers (optional)
     * @return Redirects to "contenthub/store/content/localId" which shows the content item in the HTML view.
     * @throws URISyntaxException
     * @throws EngineException
     * @throws StoreException
     */
    @POST
    @Consumes(WILDCARD + ";qs=0.5")
    public Response createContentItem(byte[] data, @Context HttpHeaders headers) throws URISyntaxException,
                                                                                EngineException,
                                                                                StoreException {
        String uri = ContentItemHelper.makeDefaultUrn(data).getUnicodeString();
        return createEnhanceAndRedirect(data, headers.getMediaType(), uri, null);
    }

    /**
     * HTTP POST method to create a content item in Contenthub. This method requires the content to be
     * text-based.
     * 
     * @param content
     *            Actual content in text format. If this parameter is supplied, {@link url} is ommitted.
     * @param url
     *            URL where the actual content resides. If this parameter is supplied (and {@link content} is
     *            {@code null}, then the content is retrieved from this url.
     * @param jsonCons
     *            Constraints in JSON format. Constraints are used to add supplementary metadata to the
     *            content item. For example, author of the content item may be supplied as {author:
     *            "John Doe"}. Then, this constraint is added to the Solr and will be indexed if the
     *            corresponding Solr schema includes the author field. Solr indexed can be created/adjusted
     *            through LDPath programs.
     * @param contentURI
     *            URI for the content item if this post is an update action on an existing Content item.
     *            Existing content item will first removed and a new URI will be assigned for the new content.
     * @param title
     *            The title for the content item. Titles can be used to present summary of the actual content.
     *            For example, search results are presented by showing the titles of resultant content items.
     * @param ldprogram
     *            Name of the LDPath program to be used while storing this content item. LDPath programs can
     *            be managed through {@link LDProgramManagerResource} or {@link LDProgramManager}
     * @param headers
     *            HTTP headers (optional)
     * @return Redirects to "contenthub/store/content/localId" which shows the content item in the HTML view.
     * @throws URISyntaxException
     * @throws EngineException
     * @throws MalformedURLException
     * @throws IOException
     * @throws StoreException
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response createContentItemFromForm(@FormParam("content") String content,
                                              @FormParam("url") String url,
                                              @FormParam("constraints") String jsonCons,
                                              @FormParam("uri") String contentURI,
                                              @FormParam("title") String title,
                                              @FormParam("ldprogram") String ldprogram,
                                              @Context HttpHeaders headers) throws URISyntaxException,
                                                                           EngineException,
                                                                           MalformedURLException,
                                                                           IOException,
                                                                           StoreException {
        Map<String,List<Object>> constraints = new HashMap<String,List<Object>>();
        if (jsonCons != null) {
            constraints = JSONUtils.convertToMap(jsonCons);
        }
        return createContentItemFromForm(content, contentURI, url, null, null, headers, constraints, title,
            ldprogram);
    }

    /**
     * HTTP POST method to create a content item from file. File is read and loaded as the actual content.
     * 
     * @param file
     *            {@link File} which contains the content for the content item.
     * @param disposition
     *            Additional information about the {@link file} parameter
     * @param jsonCons
     *            Constraints in JSON format. Constraints are used to add supplementary metadata to the
     *            content item. For example, author of the content item may be supplied as {author:
     *            "John Doe"}. Then, this constraint is added to the Solr and will be indexed if the
     *            corresponding Solr schema includes the author field. Solr indexed can be created/adjusted
     *            through LDPath programs.
     * @param contentURI
     *            URI for the content item if this post is an update action on an existing Content item.
     *            Existing content item will first removed and a new URI will be assigned for the new content.
     * @param title
     *            The title for the content item. Titles can be used to present summary of the actual content.
     *            For example, search results are presented by showing the titles of resultant content items.
     * @param ldprogram
     *            Name of the LDPath program to be used while storing this content item. LDPath programs can
     *            be managed through {@link LDProgramManagerResource} or {@link LDProgramManager}
     * @param headers
     *            HTTP headers (optional)
     * @return Redirects to "contenthub/store/content/localId" which shows the content item in the HTML view.
     * @throws URISyntaxException
     * @throws EngineException
     * @throws MalformedURLException
     * @throws IOException
     * @throws StoreException
     */
    @POST
    @Consumes(MULTIPART_FORM_DATA)
    public Response createContentItemFromForm(@FormDataParam("file") File file,
                                              @FormDataParam("file") FormDataContentDisposition disposition,
                                              @FormDataParam("constraints") String jsonCons,
                                              @FormDataParam("uri") String contentURI,
                                              @FormDataParam("title") String title,
                                              @FormDataParam("ldprogram") String ldprogram,
                                              @Context HttpHeaders headers) throws URISyntaxException,
                                                                           EngineException,
                                                                           MalformedURLException,
                                                                           IOException,
                                                                           StoreException {
        Map<String,List<Object>> constraints = new HashMap<String,List<Object>>();
        if (jsonCons != null) {
            constraints = JSONUtils.convertToMap(jsonCons);
        }
        return createContentItemFromForm(null, contentURI, null, file, disposition, headers, constraints,
            title, ldprogram);
    }

    // TODO other parameters like title, ldprogram should be considered for this service
    /**
     * HTTP PUT method to create a content item in Contenthub.
     * 
     * @param contentURI
     *            URI for the content item. If not supplied, Contenthub automatically assigns an ID to the
     *            content item.
     * @param data
     * @param headers
     * @return
     * @throws URISyntaxException
     * @throws EngineException
     * @throws StoreException
     */
    @PUT
    @Path("/content/{uri:.+}")
    @Consumes(WILDCARD)
    public Response createContentItemWithId(@PathParam(value = "uri") String contentURI,
                                            byte[] data,
                                            @Context HttpHeaders headers) throws URISyntaxException,
                                                                         EngineException,
                                                                         StoreException {
        return createEnhanceAndRedirect(data, headers.getMediaType(), contentURI, null);
    }

    private Response createContentItemFromForm(String content,
                                               String contentURI,
                                               String url,
                                               File file,
                                               FormDataContentDisposition disposition,
                                               HttpHeaders headers,
                                               Map<String,List<Object>> constraints,
                                               String title,
                                               String ldProgram) throws URISyntaxException,
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
                // TODO: collect remote resource error message for user friendly
                // feedback
            }
        } else if (file != null) {
            data = FileUtils.readFileToByteArray(file);
            // String lowerFilename = disposition.getFileName().toLowerCase();
            Collection<?> mimeTypes = mimeIdentifier.getMimeTypes(file);
            mt = MediaType.valueOf(MimeUtil2.getMostSpecificMimeType(mimeTypes).toString());
        }

        if (data != null && mt != null) {
            String uri = ContentItemHelper.makeDefaultUrn(data).getUnicodeString();
            if (contentURI != null && !contentURI.isEmpty() && !uri.equals(contentURI)) {
                deleteContentItem(contentURI);
            }
            return createEnhanceAndRedirect(data, mt, uri, true, constraints, title, ldProgram);
        } else {
            // TODO: add user-friendly feedback on empty requests from a form
            return Response.seeOther(new URI("/contenthub/store")).build();
        }
    }

    /*
     * This method takes "title" argument as well as "constraints" so that it would be easy to specify title
     * while calling RESTful services. If the title is specified explicitly it is set as the title otherwise,
     * it is searched in the constraints.
     */
    protected Response createEnhanceAndRedirect(byte[] content,
                                                MediaType mediaType,
                                                String uri,
                                                boolean useExplicitRedirect,
                                                Map<String,List<Object>> constraints,
                                                String title,
                                                String ldprogram) throws EngineException,
                                                                 URISyntaxException,
                                                                 StoreException {

        SolrContentItem sci = solrStore.create(content, uri, title, mediaType.toString(), constraints);
        solrStore.enhanceAndPut(sci, ldprogram);
        if (useExplicitRedirect) {
            // use an redirect to point browsers to newly created content
            return Response.seeOther(makeRedirectionURI(sci.getUri().getUnicodeString())).build();
        } else {
            // use the correct way of notifying the RESTful client that the
            // resource has been successfully created
            return Response.created(makeRedirectionURI(sci.getUri().getUnicodeString())).build();
        }
    }

    private URI makeRedirectionURI(String localId) throws URISyntaxException {
        return new URI(uriInfo.getBaseUri() + "contenthub/store/content/" + localId);
    }

    /**
     * HTTP DELETE method to delete a content item from Contenhub.
     * 
     * @param contentURI
     *            URI of the content item to be deleted.
     * @return HTTP OK
     * @throws StoreException
     */
    @DELETE
    @Path("/content/{uri:.+}")
    public Response deleteContentItem(@PathParam(value = "uri") String contentURI) throws StoreException {
        solrStore.deleteById(contentURI);
        return Response.ok().build();
    }

    protected Response createEnhanceAndRedirect(byte[] data, MediaType mediaType, String uri, String ldprogram) throws EngineException,
                                                                                                               URISyntaxException,
                                                                                                               StoreException {
        return createEnhanceAndRedirect(data, mediaType, uri, false, null, null, ldprogram);
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
        this.recentlyEnhanced = new ArrayList<ResultantDocument>();

        if (!(solrSearch instanceof SolrSearchImpl)) {
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
            res = solrSearch.search(params);
        } catch (SearchException e) {
            log.error("Failed to retrieve submitted documents", e);
            return new Viewable("index", this);
        }

        for (SolrDocument result : res.getResults()) {
            recentlyEnhanced.add(SolrContentItemConverter.solrDocument2solrContentItem(result, uriInfo
                    .getBaseUri().toString()));
        }

        return new Viewable("index", this);
    }

    @Path("/page/{localId:.+}")
    @Produces(TEXT_HTML)
    public ContentItemResource getContentItemView(@PathParam(value = "localId") String localId) throws IOException,
                                                                                               StoreException {
        ContentItem ci = solrStore.get(localId);
        if (ci == null) {
            throw new WebApplicationException(404);
        }
        return new ContentItemResource(localId, ci, uriInfo, uriInfo.getBaseUriBuilder().path(
            "/contenthub/store"), tcManager, serializer, servletContext);
    }

    // Helper methods for HTML view

    public List<ResultantDocument> getRecentlyEnhancedItems() throws ParseException {
        if (recentlyEnhanced.size() > pageSize) {
            return recentlyEnhanced.subList(0, pageSize);
        } else {
            return recentlyEnhanced;
        }
    }

    public URI getMoreRecentItemsUri() {
        if (offset >= pageSize) {
            return uriInfo.getBaseUriBuilder().path(getClass()).queryParam("offset", offset - pageSize)
                    .build();
        } else {
            return null;
        }
    }

    public URI getOlderItemsUri() {
        if (recentlyEnhanced.size() <= pageSize) {
            return null;
        } else {
            return uriInfo.getBaseUriBuilder().path(getClass()).queryParam("offset", offset + pageSize)
                    .build();
        }
    }
}