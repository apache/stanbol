/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.enhancer.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.JSON_LD;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.INCLUDE_EXECUTION_METADATA;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.OMIT_METADATA;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.OMIT_PARSED_CONTENT;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.OUTPUT_CONTENT;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.OUTPUT_CONTENT_PART;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.RDF_FORMAT;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.resource.LayoutConfiguration;
import org.apache.stanbol.commons.web.base.resource.TemplateLayoutConfiguration;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;

/**
 * Abstract super class for all enhancement endpoints that do not use/support
 * the default Enhancer Web UI.<p>
 * This is mainly used for supporting enhancement requests to single
 * enhancement engines.
 * 
 * @author Rupert Westenthaler
 *
 */
public abstract class AbstractEnhancerResource extends TemplateLayoutConfiguration {

    protected final EnhancementJobManager jobManager;
    protected final EnhancementEngineManager engineManager;
    protected final ChainManager chainManager;
    protected final ContentItemFactory ciFactory;
    private LayoutConfiguration layoutConfiguration;
    private UriInfo uriInfo;

    public AbstractEnhancerResource(
            EnhancementJobManager jobManager, 
            EnhancementEngineManager engineManager, 
            ChainManager chainManager, 
            ContentItemFactory ciFactory,
            LayoutConfiguration layoutConfiguration,
            UriInfo uriInfo) {
        this.jobManager = jobManager;
        this.engineManager = engineManager;
        this.chainManager = chainManager;
        this.ciFactory = ciFactory;
        this.layoutConfiguration = layoutConfiguration;
        this.uriInfo = uriInfo;
    }
    
    protected LayoutConfiguration getLayoutConfiguration() {
        return layoutConfiguration;
    }
    
    protected UriInfo getUriInfo() {
        return uriInfo;
    }

    /**
     * Getter for the Enhancement {@link Chain}
     * @return the enhancement chain. MUST NOT return <code>null</code>
     * @throws ChainException if the Chain is currently not available
     */
    protected abstract Chain getChain() throws ChainException;
    
    /*@OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @OPTIONS
    @Path("/ep")
    public Response handleEpCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers,HttpMethod.OPTIONS,HttpMethod.GET);
        return res.build();
    }*/

    @GET
    @Path("/ep")
    @Produces(value = {JSON_LD, APPLICATION_JSON, N3, N_TRIPLE, RDF_JSON, RDF_XML, TURTLE, X_TURTLE})
    public Response getExecutionPlan(@Context HttpHeaders headers) {
        ResponseBuilder res;
        Chain chain = null;
        try {
            chain = getChain();
            res = Response.ok(chain.getExecutionPlan());
        } catch (ChainException e) {
            String chainName = chain == null ? "" : ("'"+chain.getName()+"' ");
            res = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("The Enhancement Chain "+chainName+"is currently" +
                    		"not executeable (message: "+e.getMessage()+")!");
        }
        //addCORSOrigin(servletContext, res, headers);
        return res.build();
        
        
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
    public Response enhanceFromData(ContentItem ci,
            //NOTE: The 'uri' parameter is already consumed by the ContentItemReader
            //@QueryParam(value = "uri") String uri,
            @QueryParam(value = "executionmetadata") boolean inclExecMetadata,
            @QueryParam(value = "outputContent") Set<String> mediaTypes,
            @QueryParam(value = "omitParsed") boolean omitParsed,
            @QueryParam(value = "outputContentPart") Set<String> contentParts,
            @QueryParam(value = "omitMetadata") boolean omitMetadata,
            @QueryParam(value = "rdfFormat") String rdfFormat,
            @Context HttpHeaders headers) throws EnhancementException, IOException {
        Map<String,Object> reqProp = ContentItemHelper.initRequestPropertiesContentPart(ci);
        reqProp.put(INCLUDE_EXECUTION_METADATA, inclExecMetadata);
        if(mediaTypes != null && !mediaTypes.isEmpty()){
            reqProp.put(OUTPUT_CONTENT, mediaTypes);
        }
        reqProp.put(OMIT_PARSED_CONTENT, omitParsed);
        if(contentParts != null && !contentParts.isEmpty()){
            Set<IRI> outputContentParts = new HashSet<IRI>();
            for(String contentPartUri : contentParts){
                if(contentPartUri != null && !contentPartUri.isEmpty()){
                    if("*".equals(contentPartUri)){
                        outputContentParts.add(null); //indicated wildcard
                    } else {
                        outputContentParts.add(new IRI(contentPartUri));
                    }
                }
            }
            reqProp.put(OUTPUT_CONTENT_PART, outputContentParts);
        }
        reqProp.put(OMIT_METADATA, omitMetadata);
        if(rdfFormat != null && !rdfFormat.isEmpty()){
            try {
                reqProp.put(RDF_FORMAT,MediaType.valueOf(rdfFormat).toString());
            } catch (IllegalArgumentException e) {
                throw new WebApplicationException(e, 
                    Response.status(Response.Status.BAD_REQUEST)
                    .entity(String.format("Unable to parse MediaType form parameter" +
                    		"rdfFormat=%s",rdfFormat))
                    .build());
            }
        }
        enhance(ci,reqProp);
        ResponseBuilder rb = Response.ok(ci);
        MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
        if (mediaType != null) {
            rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
        }
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * Enhances the parsed ContentItem
     * @param ci the content item to enhance
     * @param reqProp the request properties or <code>null</code> if none
     * @throws EnhancementException
     */
    protected void enhance(ContentItem ci, Map<String,Object> reqProp) throws EnhancementException {
        if (jobManager != null) {
            jobManager.enhanceContent(ci, getChain());
        }
        Graph graph = ci.getMetadata();
        Boolean includeExecutionMetadata = RequestPropertiesHelper.isIncludeExecutionMetadata(reqProp);
        if (includeExecutionMetadata != null && includeExecutionMetadata.booleanValue()) {
            try {
                graph.addAll(ci.getPart(ExecutionMetadata.CHAIN_EXECUTION, Graph.class));
            } catch (NoSuchPartException e) {
                // no executionMetadata available
            }
        }
    }

}