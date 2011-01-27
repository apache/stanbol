package org.apache.stanbol.enhancer.jersey.resource;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.stanbol.enhancer.jersey.cache.EntityCacheProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.api.view.Viewable;


import static javax.ws.rs.core.MediaType.*;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;

/**
 * RESTful interface to browse the list of available engines and allow to call
 * them in a stateless, synchronous way.
 * <p>
 * If you need the content of the extractions to be stored on the server, use
 * the StoreRootResource API instead.
 */
@Path("/engines")
@ImplicitProduces(TEXT_HTML + ";qs=2")
public class EnginesRootResource extends NavigationMixin {

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected EnhancementJobManager jobManager;

    protected TcManager tcManager;

    protected Serializer serializer;

    protected TripleCollection entityCache;

    // bind the job manager by looking it up from the servlet request context
    public EnginesRootResource(@Context ServletContext context) {
        jobManager = (EnhancementJobManager) context.getAttribute(EnhancementJobManager.class.getName());
        tcManager = (TcManager) context.getAttribute(TcManager.class.getName());
        serializer = (Serializer) context.getAttribute(Serializer.class.getName());
        EntityCacheProvider entityCacheProvider = (EntityCacheProvider) context.getAttribute(EntityCacheProvider.class.getName());
        if (entityCacheProvider != null) {
            entityCache = entityCacheProvider.getEntityCache();
        }
    }

    public List<EnhancementEngine> getActiveEngines() {
        if (jobManager != null) {
            return jobManager.getActiveEngines();
        } else {
            return Collections.emptyList();
        }
    }

    @GET
    @Produces(APPLICATION_JSON)
    public JSONArray getEnginesAsJsonArray() {
        JSONArray uriArray = new JSONArray();
        if (jobManager != null) {
            for (EnhancementEngine engine : jobManager.getActiveEngines()) {
                UriBuilder ub = uriInfo.getAbsolutePathBuilder();
                URI userUri = ub.path(makeEngineId(engine)).build();
                uriArray.put(userUri.toASCIIString());
            }
        }
        return uriArray;
    }

    public static String makeEngineId(EnhancementEngine engine) {
        // TODO: add a property on engines to provided custom local ids and make
        // this static method a method of the interface EnhancementEngine
        String engineClassName = engine.getClass().getSimpleName();
        String suffixToRemove = "EnhancementEngine";
        if (engineClassName.endsWith(suffixToRemove)) {
            engineClassName = engineClassName.substring(0,
                    engineClassName.length() - suffixToRemove.length());
        }
        return engineClassName.toLowerCase();
    }

    /**
     * Form based OpenCalais-compatible interface
     *
     * TODO: should we parse the OpenCalais paramsXML and find the closest 
     * Stanbol Enhancer semantics too?
     *
     * Note: the format parameter is not part of the official API
     *
     * @throws EngineException if the content is somehow corrupted
     * @throws IOException
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response enhanceFromForm(@FormParam("content") String content,
            @FormParam("format") String format,
            @FormParam("ajax") boolean buildAjaxview,
            @Context HttpHeaders headers) throws EngineException, IOException {
        ContentItem ci = new InMemoryContentItem(content.getBytes(),
                TEXT_PLAIN);
        return enhanceAndBuildResponse(format, headers, ci, buildAjaxview);
    }

    /**
     * Media-Type based handling of the raw POST data.
     *
     * @param data binary payload to analyze
     * @param uri optional URI for the content items (to be used as an
     *            identifier in the enhancement graph)
     * @throws EngineException if the content is somehow corrupted
     * @throws IOException
     */
    @POST
    @Consumes(WILDCARD)
    public Response enhanceFromData(byte[] data,
            @QueryParam(value = "uri") String uri, @Context HttpHeaders headers)
            throws EngineException, IOException {
        String format = TEXT_PLAIN;
        if (headers.getMediaType() != null) {
            format = headers.getMediaType().toString();
        }
        if (uri != null && uri.isEmpty()) {
            // let the store build an internal URI basted on the content
            uri = null;
        }
        ContentItem ci = new InMemoryContentItem(uri, data, format);
        return enhanceAndBuildResponse(null, headers, ci, false);
    }

    protected Response enhanceAndBuildResponse(String format,
            HttpHeaders headers, ContentItem ci, boolean buildAjaxview)
            throws EngineException, IOException {
        if (jobManager != null) {
            jobManager.enhanceContent(ci);
        }
        MGraph graph = ci.getMetadata();

        if (buildAjaxview) {
            ContentItemResource contentItemResource = new ContentItemResource(
                    null, ci, entityCache, uriInfo, tcManager, serializer);
            contentItemResource.setRdfSerializationFormat(format);
            Viewable ajaxView = new Viewable("/ajax/contentitem",
                    contentItemResource);
            return Response.ok(ajaxView).type(TEXT_HTML).build();
        }
        if (format != null) {
            // force mimetype from form params
            return Response.ok(graph, format).build();
        }
        if (headers.getAcceptableMediaTypes().contains(
                APPLICATION_JSON_TYPE)) {
            // force RDF JSON media type (TODO: move this logic
            return Response.ok(graph, RDF_JSON).build();
        } else if (headers.getAcceptableMediaTypes().isEmpty()) {
            // use RDF/XML as default format to keep compat with OpenCalais
            // clients
            return Response.ok(graph, RDF_XML).build();
        }

        // traditional response lookup
        return Response.ok(graph).build();
    }

}
