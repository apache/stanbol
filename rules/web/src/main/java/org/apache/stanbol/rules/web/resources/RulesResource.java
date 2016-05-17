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
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
//import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
//import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

//import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
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
import javax.ws.rs.core.Response.Status;

import org.apache.clerezza.jaxrs.utils.form.MultiPartBody;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.viewable.Viewable;
//import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.rules.base.api.AlreadyExistingRecipeException;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.NoSuchRuleInRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RecipeConstructionException;
import org.apache.stanbol.rules.base.api.RecipeEliminationException;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdapterManager;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.RecipeImpl;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.sun.jersey.api.view.ImplicitProduces;
//import com.sun.jersey.multipart.FormDataParam;

/**
 * 
 * @author elvio, anuzzolese
 * 
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/rules")
//@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class RulesResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private RuleStore ruleStore;

    @Reference
    private RuleAdapterManager adapterManager;

    /**
     * To get the RuleStoreImpl where are stored the rules and the recipes
     * 
     * @param servletContext
     *            {To get the context where the REST service is running.}
     */
//    public RulesResource(@Context ServletContext servletContext) {
//        this.ruleStore = (RuleStore) ContextHelper.getServiceFromContext(RuleStore.class, servletContext);
//        this.adapterManager = (RuleAdapterManager) ContextHelper.getServiceFromContext(
//            RuleAdapterManager.class, servletContext);
//    }

    class RulesResourceResultData extends ResultData{
        
    }
    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder responseBuilder = Response.ok(new Viewable("index", new RulesResourceResultData()), TEXT_HTML);
//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();
    }

    /**
     * It returns the list of recipes whose descriptions match the string passed in the description parameter.<br>
     * If some recipe matches the description passed a 200 code with the list of recipes is returned.
     * Otherwise a 404 status code is returned.
     * 
     * @param description
     *            {@link String}
     * @return <ul>
     *         <li>200: The list of recipe matching the description provided is returned</li>
     *         <li>404: No recipe matches the description provided</li>
     *         </ul>
     */
    @GET
    @Produces(value = {MediaType.APPLICATION_JSON, KRFormat.RDF_XML, KRFormat.TURTLE, KRFormat.OWL_XML,
                       KRFormat.RDF_JSON, KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL,
                       MediaType.TEXT_PLAIN})
    @Path("/find/recipes")
    public Response findRecipes(@QueryParam("description") String description) {

        log.info("Searching for recipes with description like to {}.", description);

        RecipeList recipes = ruleStore.findRecipesByDescription(description);

        log.info("The recipe list is emplty? {} ", recipes.isEmpty());
        if (recipes.isEmpty()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(recipes).build();

    }

    /**
     * It returns the list of rule whose names or descriptions match the string passed in the parameter.<br>
     * If the name parameter is not null the search will be executed on that parameter, otherwise it run on
     * the description parameter. If both are bound the search will be executed on both.<br/>
     * If some rule matches the description passed a 200 code with the list of recipes is returned. Otherwise
     * a 404 status code is returned.
     * 
     * @param description
     *            {@link String}
     * @return <ul>
     *         <li>200: The list of recipe matching the description provided is returned</li>
     *         <li>404: No recipe matches the description provided</li>
     *         </ul>
     */
    @GET
    @Produces(value = {MediaType.APPLICATION_JSON, KRFormat.RDF_XML, KRFormat.TURTLE, KRFormat.OWL_XML,
                       KRFormat.RDF_JSON, KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL,
                       MediaType.TEXT_PLAIN})
    @Path("/find/rules")
    public Response findRules(@QueryParam("name") String name, @QueryParam("description") String description) {
        RuleList rules = new RuleList();
        if (name != null && !name.isEmpty()) {
            rules.addAll(ruleStore.findRulesByName(name));
        } else {
            rules.addAll(ruleStore.findRulesByDescription(description));
        }

        if (rules.isEmpty()) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok(rules).build();

    }

    /**
     * Get a recipe from the rule base (that is the ontology that contains the rules and the recipe). <br/>
     * If the second parameter is not null then the method returns the rule in the recipe identified by that
     * parameter. <br/>
     * 
     * curl -v -X GET http://localhost:8080/kres/rule/http
     * ://kres.iks-project.eu/ontology/meta/rmi.owl#ProvaParentRule
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
    @Path("/recipe/{recipe:.+}")
    @Produces(value = {KRFormat.RDF_XML, KRFormat.TURTLE, KRFormat.OWL_XML, KRFormat.RDF_JSON,
                       KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL, MediaType.TEXT_PLAIN})
    public Response getRule(@PathParam("recipe") String recipeID,
                            @QueryParam("rule") String ruleID,
                            @Context HttpHeaders headers) {

        Recipe recipe;
        Rule rule;

        ResponseBuilder responseBuilder;
        try {
        	
        	URI uri = new URI(recipeID);
			if(uri.getScheme() == null){
				recipeID = "urn:" + recipeID;
				log.info("The recipe ID is a URI without scheme. The ID is set to " + recipeID);
			}
        	
            recipe = ruleStore.getRecipe(new IRI(recipeID));

            if (ruleID != null && !ruleID.isEmpty()) {
                rule = ruleStore.getRule(recipe, new IRI(ruleID));
                RuleList ruleList = new RuleList();
                ruleList.add(rule);

                recipe = new RecipeImpl(recipe.getRecipeID(), recipe.getRecipeDescription(), ruleList);
            }

            responseBuilder = Response.ok(recipe);

        } catch (NoSuchRecipeException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_FOUND);
        } catch (RecipeConstructionException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NO_CONTENT);
        } catch (NoSuchRuleInRecipeException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_FOUND);
        } catch (URISyntaxException e) {
        	log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_ACCEPTABLE);
		}

//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();
    }

    @GET
    @Path("/recipe/{recipe:.+}")
    @Produces(value = {MediaType.TEXT_HTML})
    public Response showRecipe(@PathParam("recipe") String recipeID,
                               @QueryParam("rule") String ruleID,
                               @Context HttpHeaders headers) {

        Recipe recipe;
        Rule rule;

        ResponseBuilder responseBuilder;
        try {
        	
        	URI uri = new URI(recipeID);
			if(uri.getScheme() == null){
				recipeID = "urn:" + recipeID;
				log.info("The recipe ID is a URI without scheme. The ID is set to " + recipeID);
			}
			
            recipe = ruleStore.getRecipe(new IRI(recipeID));

            if (ruleID != null && !ruleID.isEmpty()) {
                rule = ruleStore.getRule(recipe, new IRI(ruleID));
                RuleList ruleList = new RuleList();
                ruleList.add(rule);

                recipe = new RecipeImpl(recipe.getRecipeID(), recipe.getRecipeDescription(), ruleList);
            }

            responseBuilder = Response.ok(new Viewable("rules", new RulesPrettyPrintResource(
                    uriInfo, recipe)));

        } catch (NoSuchRecipeException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_FOUND);
        } catch (RecipeConstructionException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NO_CONTENT);
        } catch (NoSuchRuleInRecipeException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_FOUND);
        } catch (URISyntaxException e) {
			log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_ACCEPTABLE);
		}

//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();
    }

    /**
     * This method implements a REST service that allows to create a new empty recipe in the store with a
     * given description.<br/>
     * The description parameter is OPTIONAL.
     * 
     * @param recipeID
     *            {@link String}
     * @param description
     *            {@link String} - OPTIONAL
     * @param headers
     *            {@link HttpHeaders}
     * @return <ul>
     *         <li>200 - if the recipe is created</li>
     *         <li>409 - if a recipe with the same identifier exists in the store</li>
     *         </ul>
     */
    @PUT
    @Consumes(MediaType.WILDCARD)
    @Path("/recipe/{recipe:.+}")
    public Response createRecipe(@PathParam("recipe") String recipeID,
                                 @QueryParam("description") String description,
                                 @Context HttpHeaders headers) {

        ResponseBuilder responseBuilder;
        try {
        	
        	URI uri = new URI(recipeID);
			if(uri.getScheme() == null){
				recipeID = "urn:" + recipeID;
				log.info("The recipe ID is a URI without scheme. The ID is set to " + recipeID);
			}
            ruleStore.createRecipe(new IRI(recipeID), description);

            responseBuilder = Response.ok();
        } catch (AlreadyExistingRecipeException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.CONFLICT);
        } catch (URISyntaxException e) {
        	log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_ACCEPTABLE);
		}

//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();

    }

    @GET
    @Path("/recipe")
    @Produces(value = {KRFormat.RDF_XML, KRFormat.TURTLE, KRFormat.OWL_XML, KRFormat.RDF_JSON,
                       KRFormat.FUNCTIONAL_OWL, KRFormat.MANCHESTER_OWL})
    public Response listRecipes(@Context HttpHeaders headers) {
        ResponseBuilder responseBuilder = null;
        try {
            RecipeList recipeList = getListRecipes();
            responseBuilder = Response.ok(recipeList);
        } catch (NoSuchRecipeException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_FOUND);
        } catch (RecipeConstructionException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR);
        }

//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();
    }

    public RecipeList getListRecipes() throws NoSuchRecipeException, RecipeConstructionException {
        return ruleStore.listRecipes();
    }

    /**
     * This method allows to delete a recipe or a rule from the store.<br/>
     * If the optional rule identifier id provided as second parameter that the rule is deleted. Otherwise it
     * is the whole recipe to be deleted.
     * 
     * @param recipe
     *            {@link String}
     * @param rule
     *            {@link String} - OPTIONAL
     * @param headers
     *            {@link HttpHeaders}
     * @return <ul>
     *         <li>200 - if either the recipe or the rule is deleted</li>
     *         <li>204 - it is not possible to delete the rule because the internal construction of the recipe
     *         failed</li>
     *         <li>404 - the rule does not exist</li>
     *         <li>412 - the recipe to which the rule belongs does not exist</li>
     *         <li>500 - if a {@link RecipeEliminationException} exception is thrown</li>
     *         </ul>
     */
    @DELETE
    @Path("/recipe/{recipe:.+}")
    public Response removeRecipe(@PathParam("recipe") String recipe,
                                 @QueryParam("rule") String rule,
                                 @Context HttpHeaders headers) {

        ResponseBuilder responseBuilder;
        
        boolean stop = false;
        
        URI uri = null;
		try {
			uri = new URI(recipe);
		} catch (URISyntaxException e1) {
			log.error(e1.getMessage(), e1);
            responseBuilder = Response.status(Status.NOT_ACCEPTABLE);
            stop = true;
		}
		
		if(!stop){
			if(uri != null && uri.getScheme() == null){
				recipe = "urn:" + recipe;
				log.info("The recipe ID is a URI without scheme. The ID is set to " + recipe);
			}
	    	
			log.info("The recipe ID is : " + recipe);
	        
	        if (rule != null && !rule.isEmpty()) {
	
	            Recipe rcp;
	            try {
	            	rcp = ruleStore.getRecipe(new IRI(recipe));
	                Rule rl = ruleStore.getRule(rcp, new IRI(rule));
	                ruleStore.removeRule(rcp, rl);
	            } catch (NoSuchRecipeException e) {
	                log.error(e.getMessage(), e);
	                responseBuilder = Response.status(Status.PRECONDITION_FAILED);
	            } catch (RecipeConstructionException e) {
	                log.error(e.getMessage(), e);
	                responseBuilder = Response.status(Status.NO_CONTENT);
	            } catch (NoSuchRuleInRecipeException e) {
	                log.error(e.getMessage(), e);
	                responseBuilder = Response.status(Status.NOT_FOUND);
	            } 
	
	        } else {
	            try {
	                ruleStore.removeRecipe(new IRI(recipe));
	            } catch (RecipeEliminationException e) {
	                log.error(e.getMessage(), e);
	                responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR);
	            }
	        }
		}

        responseBuilder = Response.ok();

//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();

    }

    /**
     * Add rules to a recipe. An optional description can be provided to the rules.
     * 
     * @param recipe
     *            {A string contains the IRI of the recipe to be added}
     * @param rules
     *            {A string contains the rules in Stanbol syntax}
     * @param description
     *            {A string contains a description of the rule}
     * @param headers
     *            {The {@link HttpHeaders}
     * @return Return: <br/>
     *         200 The recipe has been added<br/>
     *         409 The recipe has not been added<br/>
     *         500 Some error occurred
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(value = {KRFormat.TEXT_PLAIN, KRFormat.RDF_JSON})
    @Path("/recipe/{recipe:.+}")
    public Response addRulesToRecipe(@PathParam(value = "recipe") String recipe,
                                     MultiPartBody data,
                                     @Context HttpHeaders headers) {

        String description = null;
        InputStream rules = null;

        if(data.getTextParameterValues("description") != null){
            description = data.getTextParameterValues("description") [0];
        }
        if(data.getFormFileParameterValues("rules") != null){
            rules = new ByteArrayInputStream(data.getFormFileParameterValues("rules") [0].getContent());
        }
        
        if(recipe == null || rules == null  || description == null){
            throw new WebApplicationException(BAD_REQUEST);
        }
        
        ResponseBuilder responseBuilder;

        Recipe rcp;
        try {
        	
        	URI uri = new URI(recipe);
			if(uri.getScheme() == null){
				recipe = "urn:" + recipe;
				log.info("The recipe ID is a URI without scheme. The ID is set to " + recipe);
			}
        	
            rcp = ruleStore.getRecipe(new IRI(recipe));
            ruleStore.addRulesToRecipe(rcp, rules, description);

            responseBuilder = Response.ok();
        } catch (NoSuchRecipeException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_FOUND);
        } catch (RecipeConstructionException e) {
            log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR);
        } catch (URISyntaxException e) {
        	log.error(e.getMessage(), e);
            responseBuilder = Response.status(Status.NOT_ACCEPTABLE);
		}

//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();
    }

    /**
     * Return the String representation of a recipe adapted to another format, e.g., Jena rules, SPARQL
     * CONSTRUCTs, Clerezza, SWRL, etc.
     * 
     * @param recipe
     *            {The ID of the recipe}
     * @param format
     *            {The canonical name of the class we want have back, e.g.,
     *            org.apache.stanbol.rules.base.api.Rule for Jena Rules}
     * @param headers
     *            {@link HttpHeaders}
     * @return <ul>
     *         <li>200: it works properly and the string representation of the recipe according to the format
     *         provided is returned</li>
     *         <li>204: the recipe does not exist in the store</li>
     *         <li>403: a class exists for the format provided but there is no adapter for that</li>
     *         <li>404: no class exists in the context for the format provided</li>
     *         <li>406: some error occurred while converting a rule of the recipe</li>
     *         <li>409: some atom of a rule in the recipe cannot be converted to the format provided</li>
     *         </ul>
     */
    @GET
    @Produces(value = {KRFormat.RDF_JSON})
    @Path("/adapters/{recipe:.+}")
    public Response adaptTo(@PathParam("recipe") String recipe,
                            @QueryParam("format") String format,
                            @Context HttpHeaders headers) {

        ResponseBuilder responseBuilder = null;

        Class<?> classToLoad;
        try {
            // ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // classToLoad = loader.loadClass(format);
            classToLoad = Class.forName(format);
            
            URI uri = new URI(recipe);
			if(uri.getScheme() == null){
				recipe = "urn:" + recipe;
				log.info("The recipe ID is a URI without scheme. The ID is set to " + recipe);
			}
            
            Recipe rcp = ruleStore.getRecipe(new IRI(recipe));
            RuleAdapter adapter = adapterManager.getAdapter(rcp, classToLoad);

            Object adaptedRecipe = adapter.adaptTo(rcp, classToLoad);

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("recipe", rcp.getRecipeID().toString());
                jsonObject.put("adaptedTo", format);
                jsonObject.put("result", adaptedRecipe.toString());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }

            responseBuilder = Response.ok(jsonObject.toString());

        } catch (ClassNotFoundException e) {
            responseBuilder = Response.status(Status.NOT_FOUND);
            log.error(e.getMessage(), e);
        } catch (NoSuchRecipeException e) {
            responseBuilder = Response.status(Status.NO_CONTENT);
            log.error(e.getMessage(), e);
        } catch (RecipeConstructionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnavailableRuleObjectException e) {
            responseBuilder = Response.status(Status.NOT_ACCEPTABLE);
            log.error(e.getMessage(), e);
        } catch (RuleAtomCallExeption e) {
            responseBuilder = Response.status(Status.CONFLICT);
            log.error(e.getMessage(), e);
        } catch (UnsupportedTypeForExportException e) {
            responseBuilder = Response.status(Status.FORBIDDEN);
            log.error(e.getMessage(), e);
        } catch (URISyntaxException e) {
        	responseBuilder = Response.status(Status.NOT_ACCEPTABLE);
            log.error(e.getMessage(), e);
		}

//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();

    }

    /**
     * It returns the list of available {@link RuleAdapter} instances.
     * 
     * @param headers
     *            {@link HttpHeaders}
     * @return <ul>
     *         <li>A JSON array containing available adapters.<br/>
     *         Each element of the array is an object composed by the following fields:
     *         <ul>
     *         <li>adapter: the canonical name of the adapter class;</li>
     *         <li>adapter: the canonical name of the instances' class that the adapter provide as output.</li>
     *         </ul>
     *         </li>
     *         <li>404: No adapter exists</li>
     *         </ul>
     */
    @GET
    @Produces(value = {KRFormat.RDF_JSON})
    @Path("/adapters")
    public Response listAdaptersService(@Context HttpHeaders headers) {

        ResponseBuilder responseBuilder = null;

        List<RuleAdapter> adapters = getListAdapters();

        if (adapters != null && !adapters.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            for (RuleAdapter adapter : adapters) {
                JSONObject jsonAdapter = new JSONObject();
                try {
                    jsonAdapter.put("adapter", adapter.getClass().getCanonicalName());
                    jsonAdapter.put("adaptTo", adapter.getExportClass().getCanonicalName());
                } catch (JSONException e) {
                    log.error(e.getMessage(), e);
                }

                jsonArray.put(jsonAdapter);
            }

            try {
                jsonObject.put("adapters", jsonArray);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }

            responseBuilder = Response.ok(jsonObject.toString());
        } else {
            responseBuilder = Response.status(Status.NOT_FOUND);
        }

//        addCORSOrigin(servletContext, responseBuilder, headers);
        return responseBuilder.build();

    }

    public List<RuleAdapter> getListAdapters() {

        return adapterManager.listRuleAdapters();

    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
//        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

}
