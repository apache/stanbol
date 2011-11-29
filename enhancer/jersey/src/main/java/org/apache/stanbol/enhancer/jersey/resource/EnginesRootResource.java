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
package org.apache.stanbol.enhancer.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * RESTful interface to browse the list of available engines and allow to call them in a stateless,
 * synchronous way.
 * <p>
 * If you need the content of the extractions to be stored on the server, use the StoreRootResource API
 * instead.
 */
@Path("/engines")
public class EnginesRootResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected EnhancementJobManager jobManager;

    protected TcManager tcManager;

    protected Serializer serializer;

    public EnginesRootResource(@Context ServletContext context) {
        // bind the job manager by looking it up from the servlet request context
        jobManager = ContextHelper.getServiceFromContext(EnhancementJobManager.class, context);
        tcManager = ContextHelper.getServiceFromContext(TcManager.class, context);
        serializer = ContextHelper.getServiceFromContext(Serializer.class, context);
    }
    
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext,res, headers);
        return res.build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok(new Viewable("index", this),TEXT_HTML);
        addCORSOrigin(servletContext,res, headers);
        return res.build();
    }

    public List<EnhancementEngine> getActiveEngines() {
        if (jobManager != null) {
            return jobManager.getActiveEngines();
        } else {
            return Collections.emptyList();
        }
    }

    public static String makeEngineId(EnhancementEngine engine) {
        // TODO: add a property on engines to provided custom local ids and make
        // this static method a method of the interface EnhancementEngine
        String engineClassName = engine.getClass().getSimpleName();
        String suffixToRemove = "EnhancementEngine";
        if (engineClassName.endsWith(suffixToRemove)) {
            engineClassName = engineClassName
                    .substring(0, engineClassName.length() - suffixToRemove.length());
        }
        return engineClassName.toLowerCase();
    }

    /**
     * Form-based OpenCalais-compatible interface
     * 
     * TODO: should we parse the OpenCalais paramsXML and find the closest Stanbol Enhancer semantics too?
     * 
     * Note: the format parameter is not part of the official API
     * 
     * @throws EngineException
     *             if the content is somehow corrupted
     * @throws IOException
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response enhanceFromForm(@FormParam("content") String content,
                                    @FormParam("format") String format,
                                    @FormParam("ajax") boolean buildAjaxview,
                                    @Context HttpHeaders headers) throws EngineException, IOException {
        log.info("enhance from From: " + content);
        ContentItem ci = new InMemoryContentItem(content.getBytes("UTF-8"), TEXT_PLAIN);
        return enhanceAndBuildResponse(format, headers, ci, buildAjaxview);
    }

    /**
     * Media-Type based handling of the raw POST data.
     * 
     * @param data
     *            binary payload to analyze
     * @param uri
     *            optional URI for the content items (to be used as an identifier in the enhancement graph)
     * @throws EngineException
     *             if the content is somehow corrupted
     * @throws IOException
     */
    @POST
    @Consumes(WILDCARD)
    public Response enhanceFromData(byte[] data,
                                    @QueryParam(value = "uri") String uri,
                                    @Context HttpHeaders headers) throws EngineException, IOException {
        String format = TEXT_PLAIN;
        if (headers.getMediaType() != null) {
            format = headers.getMediaType().toString();
        }
        if (uri != null && uri.isEmpty()) {
            // let the store build an internal URI based on the content
            uri = null;
        }
        ContentItem ci = new InMemoryContentItem(uri, data, format);
        return enhanceAndBuildResponse(null, headers, ci, false);
    }

    protected Response enhanceAndBuildResponse(String format,
                                               HttpHeaders headers,
                                               ContentItem ci,
                                               boolean buildAjaxview) throws EngineException, IOException {
        if (jobManager != null) {
            jobManager.enhanceContent(ci);
        }

        if (buildAjaxview) {
            ContentItemResource contentItemResource = new ContentItemResource(null, ci, uriInfo, tcManager,
                    serializer, servletContext);
            contentItemResource.setRdfSerializationFormat(format);
            Viewable ajaxView = new Viewable("/ajax/contentitem", contentItemResource);
            ResponseBuilder rb =  Response.ok(ajaxView);
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=UTF-8");
            addCORSOrigin(servletContext,rb, headers);
            return rb.build();
        }
        
        MGraph graph = ci.getMetadata();
        ResponseBuilder rb = Response.ok(graph);
        if (headers.getAcceptableMediaTypes().isEmpty()) {
            // use RDF/XML as default format to keep compat with OpenCalais
            // clients
            System.out.println("NO ACCEPTABLE MEDIA TYPE");
            rb.header(HttpHeaders.CONTENT_TYPE, RDF_XML+"; charset=UTF-8");
        }
        
        addCORSOrigin(servletContext,rb, headers);
        return rb.build();
    }

}
