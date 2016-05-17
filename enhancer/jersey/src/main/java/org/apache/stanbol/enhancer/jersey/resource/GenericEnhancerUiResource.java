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

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.sparql.QueryEngine;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.stanbol.commons.web.base.resource.LayoutConfiguration;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainException;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract super class for all Enhancer endpoint that do also provide the
 * Stanbol Enhancer Web UI. This includes "/enhancer", /enhancer/chain/{name}
 * and "/engines".
 * 
 * @author Rupert Westenthaler
 *
 */
public class GenericEnhancerUiResource extends AbstractEnhancerResource {

    private final Logger log = LoggerFactory.getLogger(getClass());
    /**
     * Optional dependency - might be <code>null</code>
     */
    //protected QueryEngine queryEngine;
    protected final Serializer serializer;
    private Set<ExecutionNode> _executionNodes;
    private Set<ExecutionNode> _activeNodes;
    protected final Chain chain;
    
    public GenericEnhancerUiResource(String chainName,
            EnhancementJobManager jobManager, 
            EnhancementEngineManager engineManager, 
            ChainManager chainManager, 
            ContentItemFactory ciFactory,
            Serializer serializer, 
            LayoutConfiguration layoutConfiguration, 
            UriInfo uriInfo) {
        super(jobManager, engineManager, chainManager, ciFactory, layoutConfiguration, uriInfo);
        this.serializer = serializer;
        //this.queryEngine = queryEngine;
        if(chainName == null){
            chain = chainManager.getDefault();
        } else {
            this.chain = chainManager.getChain(chainName);
        }
        if(this.chain == null){
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok(new Viewable("index", this, GenericEnhancerUiResource.class), TEXT_HTML);
//        addCORSOrigin(servletContext, res, headers);
        return res.build();
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
                                    @Context HttpHeaders headers) throws EnhancementException,
                                                                         IOException {
        log.info("enhance from From: " + content);
        if(content == null){ //(STANBOL-1349) parsing content using 
            // 'application/x-www-form-urlencoded' is not (officially) supported.
            // ... unofficial it can be done by adding the content as value to the
            //     content parameter
            throw new WebApplicationException( 
                Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity("Parsing Content as 'application/x-www-form-urlencoded' is not supported!"
                    + "Please directly POST the content and set the 'Content-Type' "
                    + "header to the media type of the parsed content. 'application/"
                    + "octet-stream' SHOULD BE used if the media type of the parsed "
                    + "content is not known.\n")
                .build());
        }
        ContentItem ci = ciFactory.createContentItem(new StringSource(content));
        if(!buildAjaxview){ //rewrite to a normal EnhancementRequest
            return enhanceFromData(ci, false, null, false, null, false, null, headers);
        } else { //enhance and build the AJAX response
            EnhancementException enhancementException;
            try {
                enhance(ci, null);
                enhancementException = null;
            } catch (EnhancementException e){
                enhancementException = e;
            }
            ContentItemResource contentItemResource = new ContentItemResource(null, ci, getUriInfo(), "",
                    serializer, getLayoutConfiguration(), enhancementException);
            contentItemResource.setRdfSerializationFormat(format);
            Viewable ajaxView = new Viewable("/ajax/contentitem", contentItemResource, ContentItemResource.class);
            ResponseBuilder rb = Response.ok(ajaxView);
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=UTF-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }

    public boolean isEngineActive(String name) {
        return engineManager.isEngine(name);
    }

    public String getServiceUrl() {
        String uri = getUriInfo().getAbsolutePath().toString();
        return uri.charAt(uri.length()-1) == '/' ?
            uri.substring(0, uri.length()-1) : uri;
    }

    /**
     * Getter for the executionNodes
     * 
     * @return
     */
    public Set<ExecutionNode> getExecutionNodes() {
        if (_executionNodes == null) {
            ImmutableGraph ep;
            try {
                ep = chain.getExecutionPlan();
            } catch (ChainException e) {
                ep = null;
            }
            if (ep != null) {
                _executionNodes = new LinkedHashSet<ExecutionNode>();
                Set<BlankNodeOrIRI> processed = new HashSet<BlankNodeOrIRI>();
                Set<BlankNodeOrIRI> next;
                do {
                    next = ExecutionPlanHelper.getExecutable(ep, processed);
                    for (BlankNodeOrIRI node : next) {
                        _executionNodes.add(new ExecutionNode(ep, node));
                    }
                    processed.addAll(next);
                } while (!next.isEmpty());
            }
        }
        return _executionNodes;
    }

    public Set<ExecutionNode> getActiveNodes() {
        if (_activeNodes == null) {
            Set<ExecutionNode> ens = getExecutionNodes();
            if (ens != null) {
                _activeNodes = new LinkedHashSet<ExecutionNode>();
                for (ExecutionNode en : ens) {
                    if (en.isEngineActive()) {
                        _activeNodes.add(en);
                    }
                }
            }
        }
        return _activeNodes;
    }

    public Chain getChain() {
        return chain;
    }

    public boolean isChainAvailable() {
        Set<ExecutionNode> nodes = getExecutionNodes();
        if (nodes == null) {
            return false;
        }
        for (ExecutionNode node : getExecutionNodes()) {
            if (!node.isOptional() && !node.isEngineActive()) {
                return false;
            }
        }
        return true;
    }
    public class ExecutionNode {

        private final BlankNodeOrIRI node;
        private final Graph ep;
        private final boolean optional;
        private final String engineName;

        public ExecutionNode(Graph executionPlan, BlankNodeOrIRI node) {
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

        public EnhancementEngine getEngine() {
            return engineManager.getEngine(engineName);
        }

        public boolean isEngineActive() {
            return engineManager.isEngine(engineName);
        }

        @Override
        public int hashCode() {
            return node.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ExecutionNode && ((ExecutionNode) o).node.equals(node);
        }
    }
}