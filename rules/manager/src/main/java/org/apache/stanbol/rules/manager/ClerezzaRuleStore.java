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

package org.apache.stanbol.rules.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.core.sparql.query.SelectQuery;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.UnionGraph;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.rules.base.api.AlreadyExistingRecipeException;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.NoSuchRuleInRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RecipeConstructionException;
import org.apache.stanbol.rules.base.api.RecipeEliminationException;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.Symbols;
import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an implementation of the {@link RuleStore} based on Clerezza.<br/>
 * Recipe are managed as {@link Graph} graphs. <br/>
 * The vocabulary used in these graphs is provided by {@link Symbols}.
 * 
 * @author elvio
 * @author anuzzolese
 * 
 */
@Component(immediate = true, metatype = true)
@Service(RuleStore.class)
public class ClerezzaRuleStore implements RuleStore {

    @Reference
    TcManager tcManager;

    public static final String _RECIPE_INDEX_LOCATION_DEFAULT = "http://incubator.apache.org/stanbol/rules/recipe_index";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(name = RuleStore.RECIPE_INDEX_LOCATION, value = _RECIPE_INDEX_LOCATION_DEFAULT)
    private String recipeIndexLocation;

    private List<IRI> recipes;

    /**
     * This construct returns RuleStoreImpl object with inside an ontology where to store the rules.
     * 
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the RuleStoreImpl instances do need to be configured! YOU
     * NEED TO USE {@link #ClerezzaRuleStore(Dictionary, TcManager)} or its overloads, to parse the
     * configuration and then initialise the rule store if running outside a OSGI environment.
     */
    public ClerezzaRuleStore() {}

    /**
     * To be invoked by non-OSGi environments. <br/>
     * This construct returns an ontology where to store the rules.
     * 
     * @param owl
     *            {OWLOntology object contains rules and recipe}
     */
    public ClerezzaRuleStore(Dictionary<String,Object> configuration, TcManager tcManager) {
        this();

        try {
            this.tcManager = tcManager;
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            this.tcManager = null;
        }

        try {
            // activator has a branch for existing owlfile
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }

    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + ClerezzaRuleStore.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Should be called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        if (recipeIndexLocation == null || recipeIndexLocation.trim().isEmpty()) {
            String value = (String) configuration.get(RECIPE_INDEX_LOCATION);
            if (value != null && !value.trim().isEmpty()) recipeIndexLocation = value;
            else recipeIndexLocation = _RECIPE_INDEX_LOCATION_DEFAULT;
        }

        recipes = new ArrayList<IRI>();

        Graph tripleCollection = null;
        try {
            tripleCollection = tcManager.getGraph(new IRI(recipeIndexLocation));
        } catch (NoSuchEntityException e) {
            tripleCollection = tcManager.createGraph(new IRI(recipeIndexLocation));
        }

        for (Triple triple : tripleCollection) {
            IRI recipeID = (IRI) triple.getSubject();
            recipes.add(recipeID);
        }

        log.info("Rule Store activated. It contains " + recipes.size() + " recipes.", this);
    }

    /*
     * Moved form AddRecipe class. The AddRecipe should not be used anymore.
     */
    @Override
    public Recipe createRecipe(IRI recipeID, String recipeDescription) throws AlreadyExistingRecipeException {

        Graph tripleCollection;
        try {
            // create the Graph in the TcManager
            tripleCollection = tcManager.createGraph(recipeID);
        } catch (EntityAlreadyExistsException e) {
            throw new AlreadyExistingRecipeException(e.getMessage());
        }

        Triple recipeTriple = new TripleImpl(recipeID, RDF.type, Symbols.Recipe);

        Graph recipeIndexGraph = tcManager.getGraph(new IRI(recipeIndexLocation));
        recipeIndexGraph.add(recipeTriple);

        if (recipeDescription != null && !recipeDescription.isEmpty()) {
            Triple descriptionTriple = new TripleImpl(recipeID, Symbols.description, new PlainLiteralImpl(
                    recipeDescription));
            tripleCollection.add(descriptionTriple);

            recipeIndexGraph.add(descriptionTriple);
        }

        // add the recpe ID to the list of known recipes
        recipes.add(recipeID);

        return new RecipeImpl(recipeID, recipeDescription, null);
    }

    /**
     * 
     * @param recipe
     *            the recipe
     * @param RuleRule
     *            the rule in Rule syntax
     * 
     * @return the recipe we the new rule.
     */
    @Override
    public Recipe addRuleToRecipe(Recipe recipe, Rule rule, String description) {
        log.debug("Adding rule to recipe " + recipe);
        log.info("Rule : " + rule.toString());

        IRI recipeID = recipe.getRecipeID();

        Graph tripleCollection = tcManager.getGraph(recipeID);

        // add the rule object to the graph representation of the recipe by the TcManager
        tripleCollection.add(new TripleImpl(recipeID, Symbols.hasRule, rule.getRuleID()));

        /*
         * extract the rule body and head and add them to the rule object existing in the graph representation
         * of the recipe.
         */
        String stanbolSyntax = rule.toString();
        int indexOfLPar = stanbolSyntax.indexOf('[');
        int indexOfRPar = stanbolSyntax.indexOf(']');
        stanbolSyntax = stanbolSyntax.substring(indexOfLPar + 1, indexOfRPar);

        String[] parts = stanbolSyntax.split("->");

        String body = parts[0].trim();
        String head = parts[1].trim();

        tripleCollection.add(new TripleImpl(rule.getRuleID(), Symbols.ruleName, new PlainLiteralImpl(rule
                .getRuleName())));
        if (description != null && !description.isEmpty()) {
            tripleCollection.add(new TripleImpl(rule.getRuleID(), Symbols.description, new PlainLiteralImpl(
                    description)));
        }
        tripleCollection.add(new TripleImpl(rule.getRuleID(), Symbols.ruleBody, new PlainLiteralImpl(body)));
        tripleCollection.add(new TripleImpl(rule.getRuleID(), Symbols.ruleHead, new PlainLiteralImpl(head)));

        if (description != null) {
            rule.setDescription(description);
        }

        recipe.addRule(new RecipeRule(recipe, rule));

        return recipe;
    }

    /**
     * 
     * Parse the set of rules provided by the rulesStream parameter as Stanbol syntax rules and add them to
     * the Recipe in the store.<br/>
     * The recipe is a {@link Graph} managed by the {@link TcManager}.
     * 
     * 
     * @param recipe
     *            {@link Recipe} the recipe
     * @param rulesStream
     *            {@link InputStream} the rule in Stanbol syntax
     * 
     * @return the recipe with the new rule.
     */
    @Override
    public Recipe addRulesToRecipe(Recipe recipe, InputStream rulesStream, String description) {
        log.debug("Adding rule to recipe " + recipe);

        IRI recipeID = recipe.getRecipeID();
        String namespace = recipeID.toString().substring(1, recipeID.toString().length() - 1) + "/";
        
        log.info("Rule Namespace is " + namespace);
        RuleList ruleList = RuleParserImpl.parse(namespace, rulesStream).getRuleList();

        for (Rule rule : ruleList) {
            recipe = addRuleToRecipe(recipe, rule, description);
        }

        return recipe;
    }

    /**
     * 
     * @param recipeIRI
     *            the IRI of the recipe
     * @param stanbolRule
     *            the rule in Rule syntax
     */
    @Override
    public Recipe addRulesToRecipe(Recipe recipe, String stanbolRule, String description) {

        IRI recipeID = recipe.getRecipeID();
        String namespace = recipeID.toString().substring(1, recipeID.toString().length() - 1) + "/";

        RuleList ruleList = RuleParserImpl.parse(namespace, stanbolRule).getRuleList();

        for (Rule rule : ruleList) {
            recipe = addRuleToRecipe(recipe, rule, description);
        }

        return recipe;

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + ClerezzaRuleStore.class + " deactivate with context " + context);
    }

    @Override
    public Recipe getRecipe(IRI recipeID) throws NoSuchRecipeException, RecipeConstructionException {

        log.info("Called get recipe for id: " + recipeID);

        Graph recipeGraph = null;

        /**
         * Throw a NoSuchRecipeException in case of the TcManager throws a NoSuchEntityException with respect
         * to IRI representing the recipe.
         */
        try {
            recipeGraph = tcManager.getGraph(recipeID);
        } catch (NoSuchEntityException e) {
            throw new NoSuchRecipeException(recipeID.toString());
        }

        Iterator<Triple> descriptions = recipeGraph.filter(null, Symbols.description, null);
        String recipeDescription = null;
        if (descriptions != null && descriptions.hasNext()) {
            recipeDescription = descriptions.next().getObject().toString();
        }

        String query = "SELECT ?rule ?ruleName ?ruleBody ?ruleHead " + "WHERE { " + "	" + recipeID.toString()
                       + " " + Symbols.hasRule.toString() + " ?rule . " + "	?rule "
                       + Symbols.ruleName.toString() + " ?ruleName . " + "	?rule "
                       + Symbols.ruleBody.toString() + " ?ruleBody . " + "	?rule "
                       + Symbols.ruleHead.toString() + " ?ruleHead . " + "}";

        Query sparql;
        try {
            sparql = QueryParser.getInstance().parse(query);

            ResultSet resultSet = tcManager.executeSparqlQuery((SelectQuery) sparql, recipeGraph);

            StringBuilder stanbolRulesBuilder = new StringBuilder();

            boolean firstIteration = true;
            while (resultSet.hasNext()) {
                SolutionMapping solutionMapping = resultSet.next();
                RDFTerm nameResource = solutionMapping.get("ruleName");
                RDFTerm bodyResource = solutionMapping.get("ruleBody");
                RDFTerm headResource = solutionMapping.get("ruleHead");

                StringBuilder stanbolRuleBuilder = new StringBuilder();
                stanbolRuleBuilder.append(((Literal) nameResource).getLexicalForm());
                stanbolRuleBuilder.append("[");
                stanbolRuleBuilder.append(((Literal) bodyResource).getLexicalForm());
                stanbolRuleBuilder.append(" -> ");
                stanbolRuleBuilder.append(((Literal) headResource).getLexicalForm());
                stanbolRuleBuilder.append("]");

                if (!firstIteration) {
                    stanbolRulesBuilder.append(" . ");
                } else {
                    firstIteration = false;
                }

                String stanbolSyntax = stanbolRuleBuilder.toString();

                log.info("Rule content {}", stanbolSyntax);
                stanbolRulesBuilder.append(stanbolSyntax);
            }

            String stanbolSyntax = stanbolRulesBuilder.toString();

            RuleList ruleList = null;
            if (!stanbolSyntax.isEmpty()) {
                String namespace = recipeID.toString().substring(1, recipeID.toString().length() - 1) + "/";
                ruleList = RuleParserImpl.parse(namespace, stanbolSyntax).getRuleList();
            }

            return new RecipeImpl(recipeID, recipeDescription, ruleList);

        } catch (ParseException e) {
            throw new RecipeConstructionException(e);
        }

    }

    @Override
    public List<IRI> listRecipeIDs() {

        return recipes;
    }

    @Override
    public RecipeList listRecipes() throws NoSuchRecipeException, RecipeConstructionException {
        RecipeList recipeList = new RecipeList();

        for (IRI recipeID : recipes) {
            Recipe recipe;
            try {
                recipe = getRecipe(recipeID);
            } catch (NoSuchRecipeException e) {
                throw e;
            } catch (RecipeConstructionException e) {
                throw e;
            }
            recipeList.add(recipe);
        }

        log.info("The Clerezza rule store contains {} recipes", recipeList.size());

        return recipeList;

    }

    @Override
    public boolean removeRecipe(IRI recipeID) throws RecipeEliminationException {

        // remove the recipe from the TcManager
        try {
            tcManager.deleteGraph(recipeID);
        } catch (NoSuchEntityException e) {
            throw new RecipeEliminationException(e);
        }

        Graph recipeIndexGraph = tcManager.getGraph(new IRI(recipeIndexLocation));
        Triple triple = new TripleImpl(recipeID, RDF.type, Symbols.Recipe);
        recipeIndexGraph.remove(triple);

        // System.out.println("Recipes: " +recipes.size());
        // remove the recipe ID from in-memory list
        recipes.remove(recipeID);

        return true;

    }

    @Override
    public boolean removeRecipe(Recipe recipe) throws RecipeEliminationException {

        return removeRecipe(recipe.getRecipeID());

    }

    @Override
    public Recipe removeRule(Recipe recipe, Rule rule) {
        Graph tripleCollection = tcManager.getGraph(recipe.getRecipeID());

        // remove from the graph recipe all the triples having the ruleID as subject.
        Iterator<Triple> triplesIterator = tripleCollection.filter(rule.getRuleID(), null, null);
        while (triplesIterator.hasNext()) {
            tripleCollection.remove(triplesIterator.next());
        }

        // remove from the graph recipe the triple recipeID hasRule ruleID
        tripleCollection.remove(new TripleImpl(recipe.getRecipeID(), Symbols.hasRule, rule.getRuleID()));

        recipe.removeRule(rule);

        return recipe;
    }

    @Override
    public Rule getRule(Recipe recipe, String ruleName) throws NoSuchRuleInRecipeException {

        return recipe.getRule(ruleName);
    }

    @Override
    public Rule getRule(Recipe recipe, IRI ruleID) throws NoSuchRuleInRecipeException {

        return recipe.getRule(ruleID);
    }

    @Override
    public List<IRI> listRuleIDs(Recipe recipe) {
        return recipe.listRuleIDs();
    }

    @Override
    public List<String> listRuleNames(Recipe recipe) {
        return recipe.listRuleNames();
    }

    @Override
    public RuleList listRules(Recipe recipe) {
        return recipe.getRuleList();
    }

    @Override
    public Graph exportRecipe(Recipe recipe) throws NoSuchRecipeException {

        try {
            return tcManager.getGraph(recipe.getRecipeID());
        } catch (NoSuchEntityException e) {
            throw new NoSuchRecipeException(recipe.toString());
        }
    }

    @Override
    public RecipeList findRecipesByDescription(String term) {

        String sparql = "SELECT ?recipe " + "WHERE { ?recipe a " + Symbols.Recipe.toString() + " . "
                        + "?recipe " + Symbols.description + " ?description . "
                        + "FILTER (regex(?description, \"" + term + "\", \"i\"))" + "}";

        Graph tripleCollection = tcManager.getGraph(new IRI(recipeIndexLocation));

        RecipeList matchingRecipes = new RecipeList();

        try {

            SelectQuery query = (SelectQuery) QueryParser.getInstance().parse(sparql);

            ResultSet resultSet = tcManager.executeSparqlQuery(query, tripleCollection);

            while (resultSet.hasNext()) {
                SolutionMapping solutionMapping = resultSet.next();
                IRI recipeID = (IRI) solutionMapping.get("recipe");

                try {
                    Recipe recipe = getRecipe(recipeID);

                    log.info("Found recipe {}.", recipeID.toString());
                    matchingRecipes.add(recipe);
                    log.info("Found {} matching recipes.", matchingRecipes.size());
                } catch (NoSuchRecipeException e) {
                    // in this case go on in the iteration by fetching other matching recipes
                } catch (RecipeConstructionException e) {
                    // in this case go on in the iteration by fetching other matching recipes
                }

            }
        } catch (ParseException e) {
            log.error("The sparql query contains errors: ", e);
        }

        return matchingRecipes;
    }

    @Override
    public RuleList findRulesByName(String term) {
        String sparql = "SELECT ?recipe ?rule ?description " + "WHERE { " + "?recipe " + Symbols.hasRule
                        + " ?rule . " + "?rule " + Symbols.ruleName + " ?name . " + "?rule "
                        + Symbols.description + " ?description . " + "FILTER (regex(?name, \"" + term
                        + "\", \"i\"))" + "}";

        List<IRI> recipeIDs = listRecipeIDs();

        Graph[] tripleCollections = new Graph[recipeIDs.size()];

        for (int i = 0; i < tripleCollections.length; i++) {
            tripleCollections[i] = tcManager.getGraph(recipeIDs.get(i));
        }

        UnionGraph unionGraph = new UnionGraph(tripleCollections);

        RuleList matchingRules = new RuleList();

        try {

            SelectQuery query = (SelectQuery) QueryParser.getInstance().parse(sparql);

            ResultSet resultSet = tcManager.executeSparqlQuery(query, unionGraph);

            while (resultSet.hasNext()) {
                SolutionMapping solutionMapping = resultSet.next();
                IRI recipeID = (IRI) solutionMapping.get("recipe");
                IRI ruleID = (IRI) solutionMapping.get("rule");
                Literal description = (Literal) solutionMapping.get("description");

                try {
                    Recipe recipe = getRecipe(recipeID);
                    Rule rule = new RecipeRule(recipe, getRule(recipe, ruleID));
                    if (description != null) {
                        rule.setDescription(description.getLexicalForm());
                    }

                    matchingRules.add(rule);
                } catch (NoSuchRecipeException e) {
                    // in this case go on in the iteration by fetching other matching recipes
                } catch (RecipeConstructionException e) {
                    // in this case go on in the iteration by fetching other matching recipes
                } catch (NoSuchRuleInRecipeException e) {
                    // in this case go on in the iteration by fetching other matching recipes
                }

            }
        } catch (ParseException e) {
            log.error("The sparql query contains errors: ", e);
        }

        return matchingRules;
    }

    @Override
    public RuleList findRulesByDescription(String term) {
        String sparql = "SELECT ?recipe ?rule ?description " + "WHERE { " + "?recipe " + Symbols.hasRule
                        + " ?rule . " + "?rule " + Symbols.description + " ?description . "
                        + "FILTER (regex(?description, \"" + term + "\", \"i\"))" + "}";

        List<IRI> recipeIDs = listRecipeIDs();

        Graph[] tripleCollections = new Graph[recipeIDs.size()];

        for (int i = 0; i < tripleCollections.length; i++) {
            tripleCollections[i] = tcManager.getGraph(recipeIDs.get(i));
        }

        UnionGraph unionGraph = new UnionGraph(tripleCollections);

        RuleList matchingRules = new RuleList();

        try {

            SelectQuery query = (SelectQuery) QueryParser.getInstance().parse(sparql);

            ResultSet resultSet = tcManager.executeSparqlQuery(query, unionGraph);

            while (resultSet.hasNext()) {
                SolutionMapping solutionMapping = resultSet.next();
                IRI recipeID = (IRI) solutionMapping.get("recipe");
                IRI ruleID = (IRI) solutionMapping.get("rule");
                Literal description = (Literal) solutionMapping.get("description");

                try {
                    Recipe recipe = getRecipe(recipeID);
                    Rule rule = new RecipeRule(recipe, getRule(recipe, ruleID));
                    if (description != null) {
                        rule.setDescription(description.getLexicalForm());
                    }

                    matchingRules.add(rule);
                } catch (NoSuchRecipeException e) {
                    // in this case go on in the iteration by fetching other matching recipes
                } catch (RecipeConstructionException e) {
                    // in this case go on in the iteration by fetching other matching recipes
                } catch (NoSuchRuleInRecipeException e) {
                    // in this case go on in the iteration by fetching other matching recipes
                }

            }
        } catch (ParseException e) {
            log.error("The sparql query contains errors: ", e);
        }

        return matchingRules;
    }
    
    public static void main(String[] args){
        
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(new File("/Users/mac/Desktop/domain.rule"));
            
            RuleList ruleList = RuleParserImpl.parse("http://www.prova.it/", inputStream).getRuleList();

            for (Rule rule : ruleList) {
                
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
