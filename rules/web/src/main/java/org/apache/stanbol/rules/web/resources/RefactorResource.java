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

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.io.InputStream;

import javax.servlet.ServletContext;
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
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.changes.RecipeImpl;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.multipart.FormDataParam;

/**
 * 
 * @author andrea.nuzzolese
 * 
 */

@Path("/refactor")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class RefactorResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    protected Refactorer refactorer;
    
    public RefactorResource(@Context ServletContext servletContext) {
        refactorer = (Refactorer) ContextHelper.getServiceFromContext(Refactorer.class, servletContext);
        if (refactorer == null) {
            throw new IllegalStateException("SemionRefactorer missing in ServletContext");
        }

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
    @Path("/apply")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(value = {KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL, KRFormat.RDF_XML,
                       KRFormat.OWL_XML, KRFormat.RDF_JSON})
    public Response applyRefactoring(@FormDataParam("recipe") String recipe,
                                     @FormDataParam("input") InputStream input,
                                     @Context HttpHeaders headers) {

        OWLOntology output = null;
        try {
            output = doRefactoring(input, RuleParserImpl.parse(recipe));
        } catch (OWLOntologyCreationException e1) {
            throw new WebApplicationException(e1, INTERNAL_SERVER_ERROR);
        } catch (RefactoringException e1) {
            throw new WebApplicationException(e1, INTERNAL_SERVER_ERROR);
        }
        if (output == null){ 
            ResponseBuilder rb = Response.status(NOT_FOUND);
            rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=utf-8");
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
        ResponseBuilder rb = Response.ok(output);
        rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
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
    @Produces(value = {KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL, KRFormat.RDF_XML,
                       KRFormat.OWL_XML, KRFormat.RDF_JSON})
    public Response applyRefactoringFromRuleFile(@FormDataParam("recipe") InputStream recipeStream,
                                                 @FormDataParam("input") InputStream input,
                                                 @Context HttpHeaders headers) {

        OWLOntology output = null;
        try {
            output = doRefactoring(input, RuleParserImpl.parse(recipeStream));
        } catch (OWLOntologyCreationException e1) {
            throw new WebApplicationException(e1, INTERNAL_SERVER_ERROR);
        } catch (RefactoringException e1) {
            throw new WebApplicationException(e1, INTERNAL_SERVER_ERROR);
        }
        if (output == null){ 
            ResponseBuilder rb = Response.status(NOT_FOUND);
            rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=utf-8");
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
        ResponseBuilder rb = Response.ok(output);
        rb.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
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
        RuleList ruleList = kb.getkReSRuleList();
        if (ruleList == null) return null;
        Recipe actualRecipe = new RecipeImpl(null, null, ruleList);

        // Parse the input ontology
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology inputOntology = manager.loadOntologyFromOntologyDocument(input);

        // Refactor
        return refactorer.ontologyRefactoring(inputOntology, actualRecipe);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(value = {KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL, KRFormat.RDF_XML,
                       KRFormat.OWL_XML, KRFormat.RDF_JSON})
    public Response performRefactoring(@FormDataParam("recipe") String recipe,
                                       @FormDataParam("input") InputStream input,
                                       @Context HttpHeaders headers) {

        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();

        IRI recipeIRI = IRI.create(recipe);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology inputOntology;
        try {
            inputOntology = manager.loadOntologyFromOntologyDocument(input);

            OWLOntology outputOntology;
            try {
                outputOntology = refactorer.ontologyRefactoring(inputOntology, recipeIRI);
            } catch (RefactoringException e) {
                // refactoring exceptions are re-thrown
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            } catch (NoSuchRecipeException e) {
                // missing recipes result in a status 404
                ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
            ResponseBuilder rb = Response.ok(outputOntology);
            MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
            if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } catch (OWLOntologyCreationException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    public Response performRefactoringLazyCreateGraph(@QueryParam("recipe") String recipe,
                                                      @QueryParam("input-graph") String inputGraph,
                                                      @QueryParam("output-graph") String outputGraph,
                                                      @Context HttpHeaders headers) {

        log.info("recipe: {}", recipe);
        log.info("input-graph: {}", inputGraph);
        log.info("output-graph: {}", outputGraph);
        IRI recipeIRI = IRI.create(recipe);
        IRI inputGraphIRI = IRI.create(inputGraph);
        IRI outputGraphIRI = IRI.create(outputGraph);

        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();

        try {
            refactorer.ontologyRefactoring(outputGraphIRI, inputGraphIRI, recipeIRI);
            ResponseBuilder rb = Response.ok();
            MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
            if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } catch (RefactoringException e) {
            // refactoring exceptions are re-thrown
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        } catch (NoSuchRecipeException e) {
            // missing recipes result in a status 404
            ResponseBuilder rb = Response.status(NOT_FOUND);
            MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
            if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }

    }
    
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

}
