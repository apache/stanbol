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
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.SUPPORTED_RDF_TYPES;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.isAcceptableMediaType;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getReference;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.getExecutionNode;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.isOptional;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan.EXECUTION_NODE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.enhancer.jersey.resource.EnhancerRootResource.ExecutionNode;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.NoSuchPartException;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionPlan;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
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
@Path("/enhancer")
public class EnhancerRootResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected EnhancementJobManager jobManager;

    protected TcManager tcManager;

    /**
     * The chain. The default chain in this case, but might be set by sub-classes
     */
    protected Chain chain;
    private LinkedHashSet<ExecutionNode> _executionNodes;
    private LinkedHashSet<ExecutionNode> _activeNodes;
    protected EnhancementEngineManager engineManager;
    protected ChainManager chainManager;

    protected Serializer serializer;

    
    public EnhancerRootResource(@Context ServletContext context) {
        // bind the job manager by looking it up from the servlet request context
        jobManager = ContextHelper.getServiceFromContext(EnhancementJobManager.class, context);
        tcManager = ContextHelper.getServiceFromContext(TcManager.class, context);
        serializer = ContextHelper.getServiceFromContext(Serializer.class, context);
        chainManager = ContextHelper.getServiceFromContext(ChainManager.class, context);
        engineManager = ContextHelper.getServiceFromContext(EnhancementEngineManager.class, context);
        chain = chainManager.getDefault();
    }
    
    public URI getServiceUrl(){
        return uriInfo.getAbsolutePath();
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

    public boolean isEngineActive(String name){
        return engineManager.isEngine(name);
    }
    
    /**
     * Getter for the executionNodes 
     * @return
     */
    public Set<ExecutionNode> getExecutionNodes() {
        if(_executionNodes == null){
            Graph ep;
            try {
                ep = chain.getExecutionPlan();
            } catch (ChainException e) {
                ep = null;
            }
            if(ep != null){
                _executionNodes = new LinkedHashSet<ExecutionNode>();
                Set<NonLiteral> processed = new HashSet<NonLiteral>();
                Set<NonLiteral> next;
                do {
                    next = ExecutionPlanHelper.getExecutable(ep, processed);
                    for(NonLiteral node : next){
                        _executionNodes.add(new ExecutionNode(ep, node));
                    }
                    processed.addAll(next);
                } while(!next.isEmpty());
            }
        }
        return _executionNodes;
    }
    public Set<ExecutionNode> getActiveNodes() {
        if(_activeNodes == null){
            Set<ExecutionNode> ens = getExecutionNodes();
            if(ens != null){
                _activeNodes = new LinkedHashSet<ExecutionNode>();
                for(ExecutionNode en : ens){
                    if(en.isEngineActive()){
                        _activeNodes.add(en);
                    }
                }
            }
        }
        return _activeNodes;
    }
//    public EnhancementEngine getEngine(String name){
//        return engineManager.getEngine(name);
//    }
//    public Map<String,EnhancementEngine> getActiveEngines() {
//        Graph ep;
//        try {
//            ep = chain.getExecutionPlan();
//        } catch (ChainException e) {
//            return null;
//        }
//        Map<String,EnhancementEngine> active;
//        if(ep != null){
//            active = new HashMap<String,EnhancementEngine>();
//            for(EnhancementEngine engine :  ExecutionPlanHelper.getActiveEngines(engineManager, ep)){
//                active.put(engine.getName(), engine);
//            }
//        } else {
//            active = null;
//        }
//        return active;
//    }
    
    public Chain getChain(){
        return chain;
    }

    public boolean isChainAvailable(){
        Set<ExecutionNode> nodes = getExecutionNodes();
        if(nodes == null){
            return false;
        }
        for(ExecutionNode node : getExecutionNodes()){
            if(!node.isOptional() && ! node.isEngineActive()){
                return false;
            }
        }
        return true;
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
                                    @Context HttpHeaders headers) throws EnhancementException, IOException {
        log.info("enhance from From: " + content);
        ContentItem ci = new InMemoryContentItem(content.getBytes("UTF-8"), TEXT_PLAIN);
        return enhanceAndBuildResponse(format, headers, ci, false ,buildAjaxview);
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
                                    @QueryParam(value = "executionmetadata") boolean inclExecMetadata,
                                    @Context HttpHeaders headers) throws EnhancementException, IOException {
        String format = TEXT_PLAIN;
        if (headers.getMediaType() != null) {
            format = headers.getMediaType().toString();
        }
        if (uri != null && uri.isEmpty()) {
            // let the store build an internal URI based on the content
            uri = null;
        }
        ContentItem ci = new InMemoryContentItem(uri, data, format);
        return enhanceAndBuildResponse(null, headers, ci, inclExecMetadata, false);
    }

    protected Response enhanceAndBuildResponse(String format,
                                               HttpHeaders headers,
                                               ContentItem ci,
                                               boolean inclExecMetadata ,
                                               boolean buildAjaxview) throws EnhancementException, IOException {
        if (jobManager != null) {
            jobManager.enhanceContent(ci,chain);
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
        if(inclExecMetadata){
            try {
                graph.addAll(ci.getPart(ExecutionMetadata.CHAIN_EXECUTION, MGraph.class));
            } catch (NoSuchPartException e) {
                // no executionMetadata available
            }
        }
        ResponseBuilder rb = Response.ok(graph);
        List<String> accepted = headers.getRequestHeader(HttpHeaders.ACCEPT);
        MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers,null);
        //This can be used to create a customised WebExection. Jersey will sent
        //a 500er response code in any case
//        if(isAcceptableMediaType(mediaType, SUPPORTED_RDF_TYPES)){
//           //USE THIS for special error response    
//        }
        if (mediaType != null) {
            rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
        }
        
        addCORSOrigin(servletContext,rb, headers);
        return rb.build();
    }

    public class ExecutionNode {
        
        private final NonLiteral node;
        private final TripleCollection ep;
        private final boolean optional;
        private final String engineName;
        
        public ExecutionNode(TripleCollection executionPlan, NonLiteral node) {
            this.node = node;
            this.ep = executionPlan;
            this.optional = ExecutionPlanHelper.isOptional(ep, node);
            this.engineName = ExecutionPlanHelper.getEngine(ep, node);
        }
        
        public boolean isOptional() {
            return optional;
        }
        public String getEngineName() {
            return engineName;
        }
        
        public EnhancementEngine getEngine(){
            return engineManager.getEngine(engineName);
        }
        public boolean isEngineActive(){
            return engineManager.isEngine(engineName);
        }
        @Override
        public int hashCode() {
            return node.hashCode();
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof ExecutionNode && ((ExecutionNode)o).node.equals(node);
        }
    }

}
