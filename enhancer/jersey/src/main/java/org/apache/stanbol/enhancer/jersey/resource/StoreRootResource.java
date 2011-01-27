package org.apache.stanbol.enhancer.jersey.resource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.jersey.cache.EntityCacheProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.Store;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;


import static javax.ws.rs.core.MediaType.*;

/**
 * Resource to provide a CRU[D] REST API for content items and there related
 * enhancements.
 *
 * Creation is achieved using either POST requests on the root of the store or
 * as PUT requests on the expected content item URI.
 *
 * Retrieval is achieved using simple GET requests on the content item or
 * enhancement public URIs.
 *
 * Update is achieved by issue a PUT request on an existing content item public
 * URI.
 *
 * The Delete operation is not implemented yet.
 */
@Path("/store")
public class StoreRootResource extends NavigationMixin {

    private static final Logger log = LoggerFactory.getLogger(StoreRootResource.class);

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(
            Arrays.asList(SupportedFormat.N3, SupportedFormat.N_TRIPLE,
                    SupportedFormat.RDF_XML, SupportedFormat.TURTLE,
                    SupportedFormat.X_TURTLE, SupportedFormat.RDF_JSON));

    protected TcManager tcManager;

    protected Store store;

    protected EnhancementJobManager jobManager;

    protected Serializer serializer;

    protected UriInfo uriInfo;

    protected int offset = 0;

    protected int pageSize = 5;

    protected List<RecentlyEnhanced> recentlyEnhanced;

    protected TripleCollection entityCache;

    public static class RecentlyEnhanced {

        public final String localId;

        public final String uri;

        public final String mimetype;

        public final long enhancements;

        public RecentlyEnhanced(String uri, String mimetype, long enhancements) {
            this.localId = uri.substring(uri.lastIndexOf("/") + 1);
            this.uri = uri;
            this.mimetype = mimetype;
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

    public StoreRootResource(@Context ServletContext context,
            @Context UriInfo uriInfo, @QueryParam(value = "offset") int offset,
            @DefaultValue("5") @QueryParam(value = "pageSize") int pageSize)
            throws ParseException {
        tcManager = (TcManager) context.getAttribute(TcManager.class.getName());
        store = (Store) context.getAttribute(Store.class.getName());
        jobManager = (EnhancementJobManager) context.getAttribute(EnhancementJobManager.class.getName());
        serializer = (Serializer) context.getAttribute(Serializer.class.getName());
        EntityCacheProvider entityCacheProvider = (EntityCacheProvider) context.getAttribute(EntityCacheProvider.class.getName());
        if (entityCacheProvider != null) {
            entityCache = entityCacheProvider.getEntityCache();
        }

        if (store == null || tcManager == null) {
            log.error("Missing either store={} or tcManager={}", store,
                    tcManager);
            throw new WebApplicationException(404);
        }
        this.uriInfo = uriInfo;
        this.offset = offset;
        this.pageSize = pageSize;

        // compute the latest: todo move some of this as OSGi parameters
        // somewhere
        String q = "PREFIX enhancer: <http://fise.iks-project.eu/ontology/> "
                + "PREFIX dc:   <http://purl.org/dc/terms/> "
                + "SELECT DISTINCT ?content WHERE { "
                + "  ?enhancement enhancer:extracted-from ?content ."
                + "  ?enhancement dc:created ?extraction_time . } "
                + "ORDER BY DESC(?extraction_time) LIMIT %d OFFSET %d";
        q = String.format(q, pageSize, offset);

        SelectQuery query = (SelectQuery) QueryParser.getInstance().parse(q);
        ResultSet result = tcManager.executeSparqlQuery(query,
                store.getEnhancementGraph());

        recentlyEnhanced = new ArrayList<RecentlyEnhanced>();
        while (result.hasNext()) {
            SolutionMapping mapping = result.next();
            UriRef content = (UriRef) mapping.get("content");
            ContentItem ci = store.get(content.getUnicodeString());
            String mimetype = null;
            long enhancements = 0;
            if (ci != null) {
                mimetype = ci.getMimeType();
                Iterator<Triple> it = ci.getMetadata().filter(null,
                        Properties.ENHANCER_EXTRACTED_FROM, content);
                while (it.hasNext()) {
                    it.next();
                    enhancements++;
                }
                recentlyEnhanced.add(new RecentlyEnhanced(
                        content.getUnicodeString(), mimetype, enhancements));
            }
        }
    }

    public List<RecentlyEnhanced> getRecentlyEnhancedItems()
            throws ParseException {
        return recentlyEnhanced;
    }

    public URI getMoreRecentItemsUri() {
        if (offset >= pageSize) {
            return uriInfo.getBaseUriBuilder().path(getClass()).queryParam(
                    "offset", offset - pageSize).build();
        } else {
            return null;
        }
    }

    public URI getOlderItemsUri() {
        if (recentlyEnhanced.size() < pageSize) {
            return null;
        } else {
            return uriInfo.getBaseUriBuilder().path(getClass()).queryParam(
                    "offset", offset + pageSize).build();
        }
    }

    public UriRef makeContentItemUri(byte[] data) {
        // TODO: factorize this logic out in a dedicated OSGi service
        return ContentItemHelper.makeDefaultUri(uriInfo.getBaseUri()
                + "store/content/", data);
    }

    public UriRef makeContentItemUri(String localId) {
        // TODO: factorize this logic out in a dedicated OSGi service
        return new UriRef(uriInfo.getBaseUri() + "store/content/" + localId);
    }

    @GET
    @Produces(TEXT_HTML + ";qs=2")
    public Viewable getView() {
        return new Viewable("index", this);
    }

    /**
     * Cool URI handler for the uploaded resource.
     *
     * @param localId the local id of the resource in the Stanbol Enhancer store
     * @param headers
     * @return a redirection to either a browser view, the RDF metadata or the
     *         raw binary content
     */
    @GET
    @Path("/content/{localId}")
    public Response getContent(@PathParam(value = "localId") String localId,
            @Context HttpHeaders headers) {

        UriRef uri = makeContentItemUri(localId);
        ContentItem ci = store.get(uri.getUnicodeString());
        if (ci == null) {
            throw new WebApplicationException(404);
        }

        // handle smart redirection to browser view
        for (MediaType mt : headers.getAcceptableMediaTypes()) {
            if (mt.toString().startsWith(TEXT_HTML)) {
                return Response.temporaryRedirect(
                        UriBuilder.fromPath("/store/page/" + localId).build()).build();
            }
        }

        // handle smart redirection to RDF metadata view
        for (MediaType mt : headers.getAcceptableMediaTypes()) {
            if (RDF_MEDIA_TYPES.contains(mt.toString())) {
                return Response.temporaryRedirect(
                        UriBuilder.fromPath("/store/metadata/" + localId).build()).build();
            }
        }
        return Response.temporaryRedirect(
                UriBuilder.fromPath("/store/raw/" + localId).build()).build();
    }

    @GET
    @Path("/raw/{localId}")
    public Response getRawContent(@PathParam(value = "localId") String localId)
            throws IOException {
        UriRef uri = makeContentItemUri(localId);
        ContentItem ci = store.get(uri.getUnicodeString());
        if (ci == null) {
            throw new WebApplicationException(404);
        }
        return Response.ok(ci.getStream(), ci.getMimeType()).build();
    }

    @Path("/page/{localId}")
    @Produces(TEXT_HTML)
    public ContentItemResource getContentItemView(
            @PathParam(value = "localId") String localId) throws IOException {
        UriRef uri = makeContentItemUri(localId);
        ContentItem ci = store.get(uri.getUnicodeString());
        if (ci == null) {
            throw new WebApplicationException(404);
        }
        return new ContentItemResource(localId, ci, entityCache, uriInfo,
                tcManager, serializer);
    }

    @GET
    @Path("/metadata/{localId}")
    public MGraph getContentItemMetaData(
            @PathParam(value = "localId") String localId) {
        UriRef uri = makeContentItemUri(localId);
        // TODO: rewrite me to perform a CONSTRUCT query on the TcManager
        // instead
        ContentItem ci = store.get(uri.getUnicodeString());
        if (ci == null) {
            throw new WebApplicationException(404);
        }
        return ci.getMetadata();
    }

    @POST
    @Consumes(WILDCARD + ";qs=0.5")
    public Response createContentItem(byte[] data, @Context HttpHeaders headers)
            throws URISyntaxException, EngineException {
        String uri = makeContentItemUri(data).getUnicodeString();
        return createEnhanceAndRedirect(data, headers.getMediaType(), uri);
    }

    @POST
    @Consumes( { APPLICATION_FORM_URLENCODED + ";qs=1.0",
            MULTIPART_FORM_DATA + ";qs=0.9" })
    public Response createContentItemFromForm(
            @FormParam("content") String content, @FormParam("url") String url,
            @FormParam("file") File file,
            @FormParam("file") FormDataContentDisposition disposition,
            @Context HttpHeaders headers) throws URISyntaxException,
            EngineException, MalformedURLException, IOException {
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
            String lowerFilename = disposition.getFileName().toLowerCase();
            // TODO: use a mimetype sniffer lib instead
            if (lowerFilename.matches(".*\\.jpe?g")) {
                mt = MediaType.valueOf("image/jpeg");
            } else {
                mt = APPLICATION_OCTET_STREAM_TYPE;
            }
        }
        if (data != null && mt != null) {
            String uri = makeContentItemUri(data).getUnicodeString();
            return createEnhanceAndRedirect(data, mt, uri, true);
        } else {
            // TODO: add user-friendly feedback on empty requests from a form
            return Response.seeOther(new URI("/store")).build();
        }
    }

    @PUT
    @Path("/content/{localId}")
    @Consumes(WILDCARD)
    public Response createContentItemWithId(
            @PathParam(value = "localId") String localId, byte[] data,
            @Context HttpHeaders headers) throws URISyntaxException,
            EngineException {
        String uri = makeContentItemUri(localId).getUnicodeString();
        return createEnhanceAndRedirect(data, headers.getMediaType(), uri);
    }

    protected Response createEnhanceAndRedirect(byte[] data,
            MediaType mediaType, String uri) throws EngineException,
            URISyntaxException {
        return createEnhanceAndRedirect(data, mediaType, uri, false);
    }

    protected Response createEnhanceAndRedirect(byte[] data,
            MediaType mediaType, String uri, boolean useExplicitRedirect)
            throws EngineException, URISyntaxException {
        ContentItem ci = store.create(uri, data, mediaType.toString());
        jobManager.enhanceContent(ci);
        store.put(ci);
        if (useExplicitRedirect) {
            // use an redirect to point browsers to newly created content
            return Response.seeOther(new URI(uri)).build();
        } else {
            // use the correct way of notifying the RESTful client that the
            // resource has been successfully created
            return Response.created(new URI(uri)).build();
        }
    }

    // TODO: implement GET handler for dereferencable Enhancement URIs using
    // SPARQL DESCRIBE query
}
