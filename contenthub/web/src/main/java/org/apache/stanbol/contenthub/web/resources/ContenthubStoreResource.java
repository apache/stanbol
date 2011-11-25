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
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedSolrServer;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contenthub.core.store.SolrContentItemImpl;
import org.apache.stanbol.contenthub.core.utils.ContentItemIDOrganizer;
import org.apache.stanbol.contenthub.core.utils.JSONUtils;
import org.apache.stanbol.contenthub.servicesapi.store.SolrContentItem;
import org.apache.stanbol.contenthub.servicesapi.store.SolrStore;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.apache.stanbol.enhancer.jersey.resource.ContentItemResource;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.osgi.framework.BundleContext;
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
@Path("/contenthub")
public class ContenthubStoreResource extends BaseStanbolResource {

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(Arrays.asList(N3, N_TRIPLE,
        RDF_XML, TURTLE, X_TURTLE, RDF_JSON));

    private static final Logger log = LoggerFactory.getLogger(ContenthubStoreResource.class);

    private ManagedSolrServer solrDirectoryManager;

    protected TcManager tcManager;

    protected SolrStore store;

    protected Serializer serializer;

    protected UriInfo uriInfo;

    protected int offset = 0;

    protected int pageSize = 5;

    protected List<RecentlyEnhanced> recentlyEnhanced;

    protected MimeUtil2 mimeIdentifier;

    public static class RecentlyEnhanced {

        public final String localId;
        public final String uri;
        public final String mimetype;
        public final long enhancements;

        public RecentlyEnhanced(ContentItem contentItem, String baseURI, long enhancements) {
            this.localId = ContentItemIDOrganizer.detachBaseURI(contentItem.getId());
            this.uri = baseURI + "contenthub/content/" + this.localId;
            this.mimetype = contentItem.getMimeType();
            this.enhancements = enhancements;
        }

        public String getLocalId() {
            return localId;
        }

        public String getUri() {
            return uri;
        }

        public String getMimetype() {
            return mimetype;
        }

        public long getEnhancements() {
            return enhancements;
        }

    }

    public ContenthubStoreResource(@Context ServletContext context,
                                   @Context UriInfo uriInfo,
                                   @QueryParam(value = "offset") int offset,
                                   @QueryParam(value = "pageSize") @DefaultValue("5") int pageSize) throws ParseException,
                                                                                                   IllegalArgumentException,
                                                                                                   IOException, InvalidSyntaxException {
        store = ContextHelper.getServiceFromContext(SolrStore.class, context);
        tcManager = ContextHelper.getServiceFromContext(TcManager.class, context);
        serializer = ContextHelper.getServiceFromContext(Serializer.class, context);

        if (store == null) {
            log.error("Missing store = {}", store);
            throw new WebApplicationException(404);
        }
        if (tcManager == null) {
            log.error("Missing tcManager = {}", tcManager);
            throw new WebApplicationException(404);
        }
        mimeIdentifier = new MimeUtil2();
        mimeIdentifier.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector");
        mimeIdentifier.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");

        this.uriInfo = uriInfo;
        this.offset = offset;
        this.pageSize = pageSize;

        BundleContext bundleContext = (BundleContext) context.getAttribute(BundleContext.class.getName());
        solrDirectoryManager = ContextHelper.getServiceFromContext(ManagedSolrServer.class, context);
        SolrServer solrServer = null;
        if (solrDirectoryManager != null) {
            //TODO: This should not be done on every single request
            if (!solrDirectoryManager.isManagedIndex("contenthub")) {
                solrDirectoryManager.createSolrIndex("contenthub", "contenthub", null);
            }
            RegisteredSolrServerTracker tracker = new RegisteredSolrServerTracker(
                bundleContext, new IndexReference(
                    solrDirectoryManager.getServerName(), "contenthub"));
            tracker.open();
            solrServer = tracker.getService();
            tracker.close();
            tracker = null;
        }

        ModifiableSolrParams params = new ModifiableSolrParams();

        params.set("q", "*:*");
        params.set("sort", SolrFieldName.CREATIONDATE.toString() + " desc");
        params.set("start", offset);
        // always request 1 more to arrange the "Prev-Next" links correctly
        params.set("rows", pageSize + 1);

        QueryResponse res = null;
        try {
            res = solrServer.query(params);
        } catch (SolrServerException e) {
            e.printStackTrace();
        }

        recentlyEnhanced = new ArrayList<RecentlyEnhanced>();

        for (SolrDocument result : res.getResults()) {
            ContentItem ci = null;
            String id = (String) result.getFieldValue(SolrFieldName.ID.toString());
            String content = (String) result.getFieldValue(SolrFieldName.CONTENT.toString());
            String mimeType = (String) result.getFieldValue(SolrFieldName.MIMETYPE.toString());
            Long enhancementCount = (Long) result.getFieldValue(SolrFieldName.ENHANCEMENTCOUNT.toString());
            ci = new SolrContentItemImpl(id, content.getBytes(), mimeType, null, null);
            recentlyEnhanced.add(new RecentlyEnhanced(ci, uriInfo.getBaseUri().toString(), enhancementCount
                    .longValue()));
        }
    }

    public List<RecentlyEnhanced> getRecentlyEnhancedItems() throws ParseException {
        if(recentlyEnhanced.size() > pageSize) {
            return recentlyEnhanced.subList(0, pageSize);
        }
        else {
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
    
    @GET
    @Produces(TEXT_HTML + ";qs=2")
    public Viewable getView() {
        return new Viewable("index", this);
    }

    /**
     * Cool URI handler for the uploaded resource.
     * 
     * @param localId
     *            the local id of the resource in the Stanbol Enhancer store
     * @param headers
     * @return a redirection to either a browser view, the RDF metadata or the raw binary content
     */
    @GET
    @Path("/content/{localId:.+}")
    public Response getContent(@PathParam(value = "localId") String localId, @Context HttpHeaders headers) {

        ContentItem ci = store.get(localId);
        if (ci == null) {
            throw new WebApplicationException(404);
        }

        // handle smart redirection to browser view
        for (MediaType mt : headers.getAcceptableMediaTypes()) {
            if (mt.toString().startsWith(TEXT_HTML)) {
                URI pageUri = uriInfo.getBaseUriBuilder().path("/contenthub/page").path(localId).build();
                return Response.temporaryRedirect(pageUri).build();
            }
        }

        // handle smart redirection to RDF metadata view
        for (MediaType mt : headers.getAcceptableMediaTypes()) {
            if (RDF_MEDIA_TYPES.contains(mt.toString())) {
                URI metadataUri = uriInfo.getBaseUriBuilder().path("/contenthub/metadata").path(localId)
                        .build();
                return Response.temporaryRedirect(metadataUri).build();
            }
        }
        URI rawUri = uriInfo.getBaseUriBuilder().path("/contenthub/raw").path(localId).build();
        return Response.temporaryRedirect(rawUri).build();
    }

    @Path("/page/{localId:.+}")
    @Produces(TEXT_HTML)
    public ContentItemResource getContentItemView(@PathParam(value = "localId") String localId) throws IOException {
        ContentItem ci = store.get(localId);
        if (ci == null) {
            throw new WebApplicationException(404);
        }
        return new ContentItemResource(localId, ci, uriInfo, tcManager, serializer, servletContext);
    }

    @GET
    @Path("/store/{type}/{localId:.+}")
    public Response downloadContentItem(@PathParam(value = "type") String type,
                                        @PathParam(value = "localId") String localId) throws IOException {

        ContentItem ci = store.get(localId);
        if (ci == null) {
            throw new WebApplicationException(404);
        }
        if (type.equals("metadata")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            serializer.serialize(out, ci.getMetadata(), SupportedFormat.RDF_XML);
            String fileName = localId + "-metadata";
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
            String fileName = localId + "-raw";
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

    @GET
    @Path("/metadata/{localId:.+}")
    public Response getContentItemMetaData(@PathParam(value = "localId") String localId) throws IOException {
        // TODO: rewrite me to perform a CONSTRUCT query on the TcManager
        // instead

        ContentItem ci = store.get(localId);
        if (ci == null) {
            throw new WebApplicationException(404);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, ci.getMetadata(), SupportedFormat.RDF_XML);

        return Response.ok(out.toString(), "text/plain").build();
    }

    @GET
    @Path("/raw/{localId:.+}")
    public Response getRawContent(@PathParam(value = "localId") String localId) throws IOException {
        ContentItem ci = store.get(localId);
        if (ci == null) {
            throw new WebApplicationException(404);
        }

        return Response.ok(ci.getStream(), ci.getMimeType()).build();
    }

    @GET
    @Path("/update/{localid:.+}")
    public String editContentItem(@PathParam(value = "localid") String localid) {
        SolrContentItem sci = (SolrContentItem) store.get(localid);
        if (sci == null) {
            throw new WebApplicationException(404);
        }

        return JSONUtils.createJSONString(sci);
    }

    @POST
    @Consumes(WILDCARD + ";qs=0.5")
    public Response createContentItem(byte[] data, @Context HttpHeaders headers) throws URISyntaxException,
                                                                                EngineException {
        String uri = ContentItemHelper.makeDefaultUrn(data).getUnicodeString();
        return createEnhanceAndRedirect(data, headers.getMediaType(), uri);
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response createContentItemFromForm(@FormParam("content") String content,
                                              @FormParam("url") String url,
                                              @FormParam("constraints") String jsonCons,
                                              @FormParam("contentId") String contentId,
                                              @Context HttpHeaders headers) throws URISyntaxException,
                                                                           EngineException,
                                                                           MalformedURLException,
                                                                           IOException {
        Map<String,List<Object>> constraints = new HashMap<String,List<Object>>();
        if (jsonCons != null) {
            constraints = JSONUtils.convertToMap(jsonCons);
        }
        return createContentItemFromForm(content, contentId, url, null, null, headers, constraints);
    }

    @POST
    @Consumes(MULTIPART_FORM_DATA)
    public Response createContentItemFromForm(@FormDataParam("file") File file,
                                              @FormDataParam("file") FormDataContentDisposition disposition,
                                              @FormDataParam("constraints") String jsonCons,
                                              @Context HttpHeaders headers) throws URISyntaxException,
                                                                           EngineException,
                                                                           MalformedURLException,
                                                                           IOException {
        Map<String,List<Object>> constraints = new HashMap<String,List<Object>>();
        if (jsonCons != null) {
            constraints = JSONUtils.convertToMap(jsonCons);
        }
        return createContentItemFromForm(null, null, null, file, disposition, headers, constraints);
    }

    private Response createContentItemFromForm(String content,
                                               String contentId,
                                               String url,
                                               File file,
                                               FormDataContentDisposition disposition,
                                               HttpHeaders headers,
                                               Map<String,List<Object>> constraints) throws URISyntaxException,
                                                                                    EngineException,
                                                                                    MalformedURLException,
                                                                                    IOException {
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
            if (contentId != null && !contentId.isEmpty() && !uri.equals(contentId)) {
                deleteContentItem(contentId);
            }
            return createEnhanceAndRedirect(data, mt, uri, true, constraints);
        } else {
            // TODO: add user-friendly feedback on empty requests from a form
            return Response.seeOther(new URI("/contenthub")).build();
        }
    }

    @PUT
    @Path("/content/{localId:.+}")
    @Consumes(WILDCARD)
    public Response createContentItemWithId(@PathParam(value = "localId") String localId,
                                            byte[] data,
                                            @Context HttpHeaders headers) throws URISyntaxException,
                                                                         EngineException {
        return createEnhanceAndRedirect(data, headers.getMediaType(), localId);
    }

    @DELETE
    @Path("/content/{localid:.+}")
    public Response deleteContentItem(@PathParam(value = "localid") String localid) {
        store.deleteById(localid);
        return Response.ok().build();
    }

    protected Response createEnhanceAndRedirect(byte[] data, MediaType mediaType, String uri) throws EngineException,
                                                                                             URISyntaxException {
        return createEnhanceAndRedirect(data, mediaType, uri, false, null);
    }

    protected Response createEnhanceAndRedirect(byte[] data,
                                                MediaType mediaType,
                                                String uri,
                                                boolean useExplicitRedirect,
                                                Map<String,List<Object>> constraints) throws EngineException,
                                                                                     URISyntaxException {
        SolrContentItem sci = store.create(uri, data, mediaType.toString(), constraints);
        store.enhanceAndPut(sci);
        if (useExplicitRedirect) {
            // use an redirect to point browsers to newly created content
            return Response.seeOther(makeRedirectionURI(sci.getId())).build();
        } else {
            // use the correct way of notifying the RESTful client that the
            // resource has been successfully created
            return Response.created(makeRedirectionURI(sci.getId())).build();
        }
    }

    private URI makeRedirectionURI(String localId) throws URISyntaxException {
        return new URI(uriInfo.getBaseUri() + "contenthub/content/" + localId);
    }

    // TODO: implement GET handler for dereferencable Enhancement URIs using
    // SPARQL DESCRIBE query
}
