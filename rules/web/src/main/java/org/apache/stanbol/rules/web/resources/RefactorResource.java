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
package org.apache.stanbol.rules.web.resources;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
//import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
//import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;
import static org.apache.stanbol.commons.web.base.format.KRFormat.FUNCTIONAL_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.MANCHESTER_OWL;
import static org.apache.stanbol.commons.web.base.format.KRFormat.OWL_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_JSON;
import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.commons.web.base.format.KRFormat.TURTLE;
import static org.apache.stanbol.commons.web.base.format.KRFormat.X_TURTLE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

//import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
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
//import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.jaxrs.utils.form.MultiPartBody;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
//import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RecipeConstructionException;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.RecipeImpl;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.sun.jersey.api.view.ImplicitProduces;
//import com.sun.jersey.multipart.FormDataParam;

/**
 * 
 * @author anuzzolese
 * 
 */

@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/refactor")
//@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class RefactorResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    protected Refactorer refactorer;
    @Reference
    protected RuleStore ruleStore;

//    public RefactorResource(@Context ServletContext servletContext) {
//        refactorer = (Refactorer) ContextHelper.getServiceFromContext(Refactorer.class, servletContext);
//        if (refactorer == null) throw new IllegalStateException("Refactorer missing in ServletContext");
//        ruleStore = (RuleStore) ContextHelper.getServiceFromContext(RuleStore.class, servletContext);
//        if (ruleStore == null) throw new IllegalStateException("RuleStore missing in ServletContext");
//    }

    /**
     * The apply mode allows the client to compose a recipe, by mean of string containg the rules, and apply
     * it "on the fly" to the graph in input.
     * 
     * @param recipe
     *            String
     * @param input
     *            InputStream
     * @return a Response containing the transformed graph
     */
    @POST
    @Path("/apply")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(value = {TURTLE, RDF_XML, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, RDF_JSON, X_TURTLE})
    public Response applyRefactoring(MultiPartBody data,
                                     @Context HttpHeaders headers) {
        
        String recipe = null;
        InputStream input = null;
        
        if(data.getTextParameterValues(recipe) != null){
            recipe = data.getTextParameterValues(recipe) [0];
        }
        if(data.getFormFileParameterValues("input") != null){
            input = new ByteArrayInputStream(data.getFormFileParameterValues("input") [0].getContent());
        }
        
        if(recipe == null || input == null){
            throw new WebApplicationException(BAD_REQUEST);
        }
        
        ResponseBuilder rb;
        OWLOntology output = null;
        try {
            output = doRefactoring(input,
                RuleParserImpl.parse("http://incubator.apache.com/stanbol/rules/refactor/", recipe));
        } catch (OWLOntologyCreationException e1) {
            throw new WebApplicationException(e1, INTERNAL_SERVER_ERROR);
        } catch (RefactoringException e1) {
            throw new WebApplicationException(e1, BAD_REQUEST);
        }
        if (output != null) {
            rb = Response.ok(output);
            MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
            if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
        } else rb = Response.status(NOT_FOUND);
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    /**
     * The apply mode allows the client to compose a recipe, by mean of string containg the rules, and apply
     * it "on the fly" to the graph in input.
     * 
     * @param recipe
     *            String
     * @param input
     *            InputStream
     * @return a Response containing the transformed graph
     */
    @POST
    @Path("/applyfile")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(value = {TURTLE, RDF_XML, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, RDF_JSON, X_TURTLE})
    public Response applyRefactoringFromRuleFile(MultiPartBody data,
                                                 @Context HttpHeaders headers) {
        InputStream recipeStream = null;
        InputStream input = null;
        
        if(data.getFormFileParameterValues("recipe") != null){
            recipeStream = new ByteArrayInputStream(data.getFormFileParameterValues("recipe") [0].getContent());
        }
        if(data.getFormFileParameterValues("input") != null){
            input = new ByteArrayInputStream(data.getFormFileParameterValues("input") [0].getContent());
        }
        
        if(recipeStream == null || input == null){
            throw new WebApplicationException(BAD_REQUEST);
        }
        
        ResponseBuilder rb;
        OWLOntology output = null;
        try {
            output = doRefactoring(input,
                RuleParserImpl.parse("http://incubator.apache.com/stanbol/rules/refactor/", recipeStream));
        } catch (OWLOntologyCreationException e1) {
            throw new WebApplicationException(e1, INTERNAL_SERVER_ERROR);
        } catch (RefactoringException e1) {
            throw new WebApplicationException(e1, INTERNAL_SERVER_ERROR);
        }
        if (output != null) {
            rb = Response.ok(output);
            MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
            if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
        } else rb = Response.status(NOT_FOUND);
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();

    }

    /**
     * Utility method that groups all calls to the refactorer.
     * 
     * @param input
     * @param recipe
     * @return
     * @throws OWLOntologyCreationException
     * @throws RefactoringException
     */
    private OWLOntology doRefactoring(InputStream input, KB kb) throws OWLOntologyCreationException,
                                                               RefactoringException {
        if (kb == null) return null;
        RuleList ruleList = kb.getRuleList();
        if (ruleList == null) return null;
        Recipe actualRecipe = new RecipeImpl(null, null, ruleList);

        // Parse the input ontology
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology inputOntology = manager.loadOntologyFromOntologyDocument(input);

        Graph tripleCollection = refactorer.graphRefactoring(
            OWLAPIToClerezzaConverter.owlOntologyToClerezzaGraph(inputOntology), actualRecipe);
        // Refactor
        return OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(tripleCollection);
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
//        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

    @OPTIONS
    @Path("/apply")
    public Response handleCorsPreflightApply(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
//        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

    @OPTIONS
    @Path("/applyfile")
    public Response handleCorsPreflightApplyFile(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
//        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(value = {TURTLE, RDF_XML, MANCHESTER_OWL, FUNCTIONAL_OWL, OWL_XML, RDF_JSON, X_TURTLE})
    public Response performRefactoring(MultiPartBody data,
                                       @Context HttpHeaders headers) {

        String recipe = null;
        InputStream input = null;
        
        if(data.getTextParameterValues("recipe") != null){
            recipe = data.getTextParameterValues("recipe") [0];
        }
        if(data.getFormFileParameterValues("input") != null){
            input = new ByteArrayInputStream(data.getFormFileParameterValues("input") [0].getContent());
        }
        
        if(recipe == null || input == null){
            throw new WebApplicationException(BAD_REQUEST);
        }
        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();
        ResponseBuilder rb;
        Recipe rcp;
        try {
        
        	
        	URI uri = new URI(recipe);
        	if(uri != null && uri.getScheme() == null){
				recipe = "urn:" + recipe;
				log.info("The recipe ID is a URI without scheme. The ID is set to " + recipe);
			}
        	
        	IRI recipeID = new IRI(recipe);
        	
            rcp = ruleStore.getRecipe(recipeID);

            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology inputOntology = manager.loadOntologyFromOntologyDocument(input);
            Graph tripleCollection = refactorer.graphRefactoring(
                OWLAPIToClerezzaConverter.owlOntologyToClerezzaGraph(inputOntology), rcp);
            OWLOntology outputOntology = OWLAPIToClerezzaConverter
                    .clerezzaGraphToOWLOntology(tripleCollection);
            rb = Response.ok(outputOntology);
            MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
            if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);

        } catch (NoSuchRecipeException e) {
            rb = Response.status(NOT_FOUND);
            log.error(e.getMessage(), e);
        } catch (RecipeConstructionException e) {
            rb = Response.status(NO_CONTENT);
            log.error(e.getMessage(), e);
        } catch (OWLOntologyCreationException e) {
            rb = Response.status(PRECONDITION_FAILED);
            log.error(e.getMessage(), e);
        } catch (RefactoringException e) {
            rb = Response.status(INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (URISyntaxException e) {
        	rb = Response.status(NOT_ACCEPTABLE);
        	log.error(e.getMessage(), e);
		}
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    public Response performRefactoringLazyCreateGraph(@QueryParam("recipe") String recipe,
                                                      @QueryParam("input-graph") String inputGraph,
                                                      @QueryParam("output-graph") String outputGraph,
                                                      @Context HttpHeaders headers) {

        log.info("recipe: {}", recipe);
        log.info("input-graph: {}", inputGraph);
        log.info("output-graph: {}", outputGraph);
        IRI recipeID = new IRI(recipe);
        IRI inputGraphID = new IRI(inputGraph);
        IRI outputGraphID = new IRI(outputGraph);

        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();

        ResponseBuilder responseBuilder = null;

        try {
            refactorer.graphRefactoring(outputGraphID, inputGraphID, recipeID);
            responseBuilder = Response.ok();
        } catch (RefactoringException e) {
            // refactoring exceptions are re-thrown
            log.error(e.getMessage(), e);
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        } catch (NoSuchRecipeException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(NOT_FOUND);
        }

        MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
        if (mediaType != null) responseBuilder.header(HttpHeaders.CONTENT_TYPE, mediaType);
//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();
    }

}
