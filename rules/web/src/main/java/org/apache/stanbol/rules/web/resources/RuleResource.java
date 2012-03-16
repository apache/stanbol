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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
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

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.AddRecipe;
import org.apache.stanbol.rules.manager.changes.AddRule;
import org.apache.stanbol.rules.manager.changes.GetRecipe;
import org.apache.stanbol.rules.manager.changes.GetRule;
import org.apache.stanbol.rules.manager.changes.RemoveRecipe;
import org.apache.stanbol.rules.manager.changes.RemoveRule;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author elvio, andrea.nuzzolese, alberto.musetti
 * 
 */
@Path("/rule")
public class RuleResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    private RuleStore ruleStore;
    private HashMap<IRI,String> map;
    private String desc;

    /**
     * To get the RuleStoreImpl where are stored the rules and the recipes
     * 
     * @param servletContext
     *            {To get the context where the REST service is running.}
     */
    public RuleResource(@Context ServletContext servletContext) {
        this.ruleStore = (RuleStoreImpl) ContextHelper.getServiceFromContext(RuleStore.class,
            servletContext);
    }

    /**
     * Get a rule from the rule base (that is the ontology that contains the rules and the recipe). curl -v -X
     * GET http://localhost:8080/kres/rule/http ://kres.iks-project.eu/ontology/meta/rmi.owl#ProvaParentRule
     * 
     * @param uri
     *            {A string contains the IRI full name of the rule.}
     * @return Return: <br/>
     *         200 The rule is retrieved (import declarations point to KReS Services) <br/>
     *         404 The rule does not exists in the manager <br/>
     *         500 Some error occurred
     * 
     */
    @GET
    @Path("/{uri:.+}")
    @Produces(value = {KRFormat.RDF_XML, KRFormat.TURTLE, KRFormat.OWL_XML, KRFormat.RDF_JSON,
                       KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL})
    public Response getRule(@PathParam("uri") String uri, @Context HttpHeaders headers) {

        try {

            GetRule recipe = new GetRule(ruleStore);
            if (uri.equals("all")) {

                HashMap<IRI,String> rule = recipe.getAllRules();
                Iterator<IRI> keys = rule.keySet().iterator();

                if (rule.isEmpty()){
                    ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }else {

                    OWLOntology onto = ruleStore.getOntology();
                    OWLOntology newmodel = OWLManager.createOWLOntologyManager().createOntology(
                        onto.getOntologyID());
                    OWLDataFactory factory = onto.getOWLOntologyManager().getOWLDataFactory();

                    Iterator<OWLOntology> importedonto = onto.getDirectImports().iterator();
                    List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
                    OWLDataFactory auxfactory = onto.getOWLOntologyManager().getOWLDataFactory();

                    while (importedonto.hasNext()) {
                        OWLOntology auxonto = importedonto.next();
                        additions.add(new AddImport(newmodel, auxfactory.getOWLImportsDeclaration(auxonto
                                .getOWLOntologyManager().getOntologyDocumentIRI(auxonto))));
                    }

                    if (!additions.isEmpty()) newmodel.getOWLOntologyManager().applyChanges(additions);

                    while (keys.hasNext()) {
                        OWLNamedIndividual ind = factory.getOWLNamedIndividual(keys.next());
                        Set<OWLIndividualAxiom> ax = onto.getAxioms(ind);
                        newmodel.getOWLOntologyManager().addAxioms(newmodel, ax);
                    }

                    // try {
                    // OWLManager.createOWLOntologyManager().saveOntology(
                    // newmodel,
                    // newmodel.getOWLOntologyManager()
                    // .getOntologyFormat(newmodel),
                    // System.out);
                    // } catch (OWLOntologyStorageException e) {
                    // // TODO Auto-generated catch block
                    // e.printStackTrace();
                    // }

                    ResponseBuilder rb = Response.ok(newmodel);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

            } else {

                HashMap<IRI,String> rule = recipe.getRule(IRI.create(uri));

                if (rule == null) {
                    ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                } else {
                    OWLOntology onto = ruleStore.getOntology();

                    OWLDataFactory factory = onto.getOWLOntologyManager().getOWLDataFactory();
                    OWLNamedIndividual ind = factory.getOWLNamedIndividual(IRI.create(uri));
                    Set<OWLIndividualAxiom> ax = onto.getAxioms(ind);
                    OWLOntology newmodel = OWLManager.createOWLOntologyManager().createOntology(
                        onto.getOntologyID());

                    Iterator<OWLOntology> importedonto = onto.getDirectImports().iterator();
                    List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
                    OWLDataFactory auxfactory = onto.getOWLOntologyManager().getOWLDataFactory();

                    while (importedonto.hasNext()) {
                        OWLOntology auxonto = importedonto.next();
                        additions.add(new AddImport(newmodel, auxfactory.getOWLImportsDeclaration(auxonto
                                .getOWLOntologyManager().getOntologyDocumentIRI(auxonto))));
                    }

                    if (!additions.isEmpty()) newmodel.getOWLOntologyManager().applyChanges(additions);

                    newmodel.getOWLOntologyManager().addAxioms(newmodel, ax);

                    // try {
                    // OWLManager.createOWLOntologyManager().saveOntology(
                    // newmodel,
                    // newmodel.getOWLOntologyManager()
                    // .getOntologyFormat(newmodel),
                    // System.out);
                    // } catch (OWLOntologyStorageException e) {
                    // // TODO Auto-generated catch block
                    // e.printStackTrace();
                    // }

                    
                    ResponseBuilder rb = Response.ok(newmodel);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }
            }
        } catch (Exception e) {
            // Some error occurred
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }

    }

    @GET
    @Path("/of-recipe/{uri:.+}")
    @Produces(value = {KRFormat.RDF_XML, KRFormat.TURTLE, KRFormat.OWL_XML, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getRulesOfRecipe(@PathParam("uri") String recipeURI,@Context HttpHeaders headers) {

        GetRule kReSGetRule = new GetRule(ruleStore);
        String recipeURIEnc;
        try {
            recipeURIEnc = URLEncoder.encode(
                "http://kres.iks-project.eu/ontology/meta/rmi_config.owl#MyRecipeA", "UTF-8");
            log.debug("Recipe: " + recipeURIEnc);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.debug("Recipe IRI: " + IRI.create(recipeURI));
        OWLOntology ontology = kReSGetRule.getAllRulesOfARecipe(IRI.create(recipeURI));

        ResponseBuilder rb = Response.ok(ontology);
        MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
        if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();

    }

    /**
     * To add a rule to a recipe at the end of the sequence. curl -v -X POST -F
     * "recipe=http://kres.iks-project.eu/ontology/meta/rmi.owl%23ProvaParentRecipe" -F
     * "rule=http://kres.iks-project.eu/ontology/meta/rmi.owl%23ProvaRuleNEW" -F "kres-syntax=body -> head" -F
     * "description=prova di aggiunta regola" http://localhost:8080/kres/rule
     * 
     * @param recipe
     *            {A string contains the IRI of the recipe where to add the rule}
     * @param rule
     *            {A string contains the IRI of the rule to be added at the recipe}
     * @param kres_syntax
     *            {A string contains the body and the head of the kres rule. If not specified the rule is
     *            search in the Ontology otherwise is added as new.}
     * @param description
     *            {A string contains a description of the rule}
     * @return Return: <br/>
     *         200 The rule has been added <br/>
     *         204 The rule has not been added <br/>
     *         400 The rule and recipe are not specified<br/>
     *         404 Recipe or rule not found<br/>
     *         409 The rule has not been added<br/>
     *         500 Some error occurred
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = {KRFormat.RDF_XML, KRFormat.TURTLE, KRFormat.OWL_XML, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response addRuleToRecipe(@FormParam(value = "recipe") String recipe,
                                    @FormParam(value = "rule") String rule,
                                    @FormParam(value = "kres-syntax") String kres_syntax,
                                    @FormParam(value = "description") String description,
                                    @Context HttpHeaders headers) {

        // System.err.println("recipe "+recipe);
        // System.err.println("rule " + rule);
        // System.err.println("kres-syntax "+kres_syntax);
        // System.err.println("description "+description);
        //
        // return Response.ok().build();

        try {

            if ((recipe == null) && (rule == null)) {
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }

            recipe = recipe.replace(" ", "").trim();
            rule = rule.replace(" ", "").trim();

            // The rule is already inside the rule store
            if ((kres_syntax == null)) {
                // Get the rule
                GetRule inrule = new GetRule(ruleStore);
                this.map = inrule.getRule(IRI.create(rule));

                if (map == null) {
                    ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                // Get the recipe
                GetRecipe getrecipe = new GetRecipe(ruleStore);
                this.map = getrecipe.getRecipe(IRI.create(recipe));
                if (map != null) {
                    this.desc = getrecipe.getDescription(IRI.create(recipe));
                    if (desc == null) Response.status(Status.NOT_FOUND).build();
                } else {
                    ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                String[] sequence = map.get(IRI.create(recipe)).split(",");
                Vector<IRI> ruleseq = new Vector();
                if (!sequence[0].isEmpty()) for (String seq : sequence)
                    ruleseq.add(IRI.create(seq.replace(" ", "").trim()));

                // Add the new rule to the end
                ruleseq.add(IRI.create(rule));
                // Remove the old recipe
                RemoveRecipe remove = new RemoveRecipe(ruleStore);
                boolean ok = remove.removeRecipe(IRI.create(recipe));

                if (!ok){
                    ResponseBuilder rb = Response.status(Status.CONFLICT);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                // Add the recipe with the new rule
                AddRecipe newadd = new AddRecipe(ruleStore);
                ok = newadd.addRecipe(IRI.create(recipe), ruleseq, desc);

                if (ok) {
                    ruleStore.saveOntology();
                    ResponseBuilder rb = Response.ok();
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                } else {
                    ResponseBuilder rb = Response.status(Status.NO_CONTENT);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }
            }

            // The rule is added to the store and to the recipe
            if ((kres_syntax != null) & (description != null)) {
                // Get the rule
                AddRule inrule = new AddRule(ruleStore);
                boolean ok = inrule.addRule(IRI.create(rule), kres_syntax, description);
                if (!ok) {
                    log.error("Problem to add: " + rule);
                    ResponseBuilder rb = Response.status(Status.CONFLICT);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                // Get the recipe
                GetRecipe getrecipe = new GetRecipe(ruleStore);
                this.map = getrecipe.getRecipe(IRI.create(recipe));
                if (map != null) {
                    this.desc = getrecipe.getDescription(IRI.create(recipe));

                    if (desc == null){
                        ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                        MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                        if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                        addCORSOrigin(servletContext, rb, headers);
                        return rb.build();
                    }
                } else {
                    ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                String[] sequence = map.get(IRI.create(recipe)).split(",");
                Vector<IRI> ruleseq = new Vector();
                if (!sequence[0].isEmpty()) for (String seq : sequence)
                    ruleseq.add(IRI.create(seq.replace(" ", "").trim()));

                // Add the new rule to the end
                ruleseq.add(IRI.create(rule));
                // Remove the old recipe
                RemoveRecipe remove = new RemoveRecipe(ruleStore);
                ok = remove.removeRecipe(IRI.create(recipe));
                if (!ok) {
                    log.error("ERROR TO REMOVE OLD RECIPE: " + recipe);
                    ResponseBuilder rb = Response.status(Status.CONFLICT);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                // Add the recipe with the new rule
                AddRecipe newadd = new AddRecipe(ruleStore);
                ok = newadd.addRecipe(IRI.create(recipe), ruleseq, desc);
                if (ok) {
                    ruleStore.saveOntology();
                    ResponseBuilder rb = Response.ok();
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                } else {
                    ResponseBuilder rb = Response.status(Status.NO_CONTENT);
                    MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                    if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }
            } else {
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                MediaType mediaType = MediaTypeUtil.getAcceptableMediaType(headers, null);
                if (mediaType != null) rb.header(HttpHeaders.CONTENT_TYPE, mediaType);
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }

        } catch (Exception e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * To delete a rule from a recipe or from the ontology. If the recipe is not specified the rule is deleted
     * from the ontology. curl -v -X DELETE -G \ -d recipe=
     * "http://kres.iks-project.eu/ontology/meta/rmi.owl#ProvaParentRecipe" \ -d rule
     * ="http://kres.iks-project.eu/ontology/meta/rmi.owl#ProvaParentNewRule" \
     * http://localhost:port/kres/rule
     * 
     * @Param rule {A string contains an IRI of the rule to be removed}
     * @param recipe
     *            {A string contains an IRI of the recipe where remove the rule}
     * @return Return: <br/>
     *         200 The rule has been deleted<br/>
     *         204 The rule has not been deleted<br/>
     *         404 Recipe or rule not found<br/>
     *         409 The recipe has not been deleted<br/>
     *         500 Some error occurred
     */
    @DELETE
    // @Consumes(MediaType.TEXT_PLAIN)
    @Produces(KRFormat.TEXT_PLAIN)
    public Response removeRule(@QueryParam(value = "rule") String rule,
                               @QueryParam(value = "recipe") String recipe,
                               @Context HttpHeaders headers) {

        boolean ok;

        try {

            // Delete from the recipe
            if ((recipe != null) && (rule != null)) {
                recipe = recipe.replace(" ", "").trim();
                rule = rule.replace(" ", "").trim();
                // Get the rule
                GetRule getrule = new GetRule(ruleStore);
                this.map = getrule.getRule(IRI.create(rule));
                if (map == null) {
                    ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                    rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                // Get the recipe
                GetRecipe getrecipe = new GetRecipe(ruleStore);
                this.map = getrecipe.getRecipe(IRI.create(recipe));
                if (map != null) {
                    this.desc = getrecipe.getDescription(IRI.create(recipe));
                    if (desc.isEmpty()){
                        //return Response.status(Status.NOT_FOUND).build();
                        
                        ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                        addCORSOrigin(servletContext, rb, headers);
                        return rb.build();
                    }
                } else {
                    
                    ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                    rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                // Remove rule from recipe
                RemoveRule remove = new RemoveRule(ruleStore);
                ok = remove.removeRuleFromRecipe(IRI.create(rule), IRI.create(recipe));

                if (ok) {
                    ruleStore.saveOntology();
                    ResponseBuilder rb = Response.ok();
                    rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                } else {
                    ResponseBuilder rb = Response.status(Status.NO_CONTENT);
                    rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }
            }

            // Delete from the ontology
            if ((recipe == null) && (rule != null)) {
                rule = rule.replace(" ", "").trim();
                // Get the rule
                GetRule getrule = new GetRule(ruleStore);
                this.map = getrule.getRule(IRI.create(rule));
                if (map == null) {
                    ResponseBuilder rb = Response.status(Status.NOT_FOUND);
                    rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }

                // Remove the old rule
                RemoveRule remove = new RemoveRule(ruleStore);
                ok = remove.removeRule(IRI.create(rule));
                if (ok) {
                    ruleStore.saveOntology();
                    ResponseBuilder rb = Response.ok();
                    rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                } else {
                    ResponseBuilder rb = Response.status(Status.NO_CONTENT);
                    rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                    addCORSOrigin(servletContext, rb, headers);
                    return rb.build();
                }
            } else {
                ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }

        } catch (Exception e) {
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

}
