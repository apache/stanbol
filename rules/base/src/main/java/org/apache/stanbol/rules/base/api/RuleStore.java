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
package org.apache.stanbol.rules.base.api;

import java.io.InputStream;
import java.util.List;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.apache.stanbol.rules.base.api.util.RuleList;

/**
 * The RuleStore provides features in order to manage the persistence of recipes and rules.<br/>
 * Through the RuleStore recipes and rules can be:
 * <ul>
 * <li>stored
 * <li>accessed
 * <li>modified
 * <li>removed
 * </ul>
 * 
 * @author anuzzolese
 * 
 */

public interface RuleStore {

    /**
     * The key used to configure default location where to store the index graph
     */
    String RECIPE_INDEX_LOCATION = "org.apache.stanbol.rules.base.recipe_index";

    /**
     * Create a new recipe in the Rule Store.<br/>
     * The recipe is identified and described by the first and the second parameter respectively.<br/>
     * The description is optional and a null value can be in case passed.<br/>
     * This method returns a {@link Recipe} object, which is the representation of a recipe of rules in
     * Stanbol.<br/>
     * If some error occurs during the creation of the recipe a {@link RecipeConstructionException} is thrown.
     * 
     * @param recipeID
     *            {@link IRI}
     * @param recipeDescription
     *            {@link String}
     * @return a {@link Recipe}
     * @throws AlreadyExistingRecipeException
     */
    Recipe createRecipe(IRI recipeID, String recipeDescription) throws AlreadyExistingRecipeException;

    /**
     * The method adds a new rule passed as second parameter to a recipe passed as first parameter. <br/>
     * The descriptions contains some text about the role of the rule in the recipe. It can be
     * <code>null</code>.<br/>
     * It returns the recipe object updated with the new rule.<br/>
     * The change consisting in adding the new rule is performed permanently in the store.
     * 
     * @param recipe
     *            {@link Recipe}
     * @param rule
     *            {@link Rule}
     * @param description
     *            {@link String}
     * @return {@link Recipe}
     */
    Recipe addRuleToRecipe(Recipe recipe, Rule rule, String description);

    /**
     * The method adds one or more rules passed as second parameter to a recipe passed as first parameter. <br/>
     * The rule or the rules consist of a string that expresses the rules in Stanbol syntax.<br/>
     * The descriptions contains some text about the role of the rules in the recipe. The description can be
     * used in order to find this set of rule. It can be <code>null</code>.<br/>
     * It returns the recipe object updated with the new rule(s).<br/>
     * The change consisting in adding the new rule(s) is performed permanently in the store.
     * 
     * @param recipe
     *            {@link Recipe}
     * @param rule
     *            {@link Rule}
     * @param description
     *            {@link String}
     * @return {@link Recipe}
     */
    Recipe addRulesToRecipe(Recipe recipe, String rules, String description);

    /**
     * The method adds one or more rules passed as second parameter to a recipe passed as first parameter. <br/>
     * The rule or the rules consist of an {@link InputStream} that provides rules expressed in Stanbol
     * syntax.<br/>
     * The descriptions contains some text about the role of the rules in the recipe. The description can be
     * used in order to find this set of rule. It can be <code>null</code>.<br/>
     * It returns the recipe object updated with the new rule(s).<br/>
     * The change consisting in adding the new rule(s) is performed permanently in the store.
     * 
     * @param recipe
     *            {@link Recipe}
     * @param rule
     *            {@link Rule}
     * @param description
     *            {@link String}
     * @return {@link Recipe}
     */
    Recipe addRulesToRecipe(Recipe recipe, InputStream rules, String description);

    /**
     * It returns the rule in a recipe selected by name.<br/>
     * 
     * @param recipe
     *            {@link Recipe}
     * @param ruleName
     *            {@link String}
     * @return {@link Rule}
     * @throws NoSuchRuleInRecipeException
     */
    Rule getRule(Recipe recipe, String ruleName) throws NoSuchRuleInRecipeException;

    /**
     * It returns the rule in a recipe selected by ID.<br/>
     * 
     * @param recipe
     *            {@link Recipe}
     * @param ruleID
     *            {@link IRI}
     * @return {@link Rule}
     * @throws NoSuchRuleInRecipeException
     */
    Rule getRule(Recipe recipe, IRI ruleID) throws NoSuchRuleInRecipeException;

    /**
     * It returns the set of rules that realize the recipe passed as parameter.
     * 
     * @param recipe
     *            {@link Recipe}
     * @return {@link RuleList}
     */
    RuleList listRules(Recipe recipe);

    /**
     * It returns the {@link List} or rules' identifiers ({@link IRI}).
     * 
     * @param recipe
     *            {@link Recipe}
     * @return {@link List} of {@link IRI}
     */
    List<IRI> listRuleIDs(Recipe recipe);

    /**
     * It returns the {@link List} of rules' names.
     * 
     * @param recipe
     *            {@link Recipe}
     * @return {@link List} of {@link String}
     */
    List<String> listRuleNames(Recipe recipe);

    /**
     * It returns a {@link Recipe} object identified in the store by the recipe's identifier provided as
     * parameter.<br/>
     * If the recipe's identifier does not exist in the store a {@link NoSuchRecipeException} is thrown.<br/>
     * If some error occurs while generating the recipe object a {@link RecipeConstructionException} is
     * thrown.
     * 
     * @param recipeID
     *            {@link IRI}
     * @return {@link Recipe}
     * @throws NoSuchRecipeException
     * @throws RecipeConstructionException
     */
    Recipe getRecipe(IRI recipeID) throws NoSuchRecipeException, RecipeConstructionException;

    /**
     * It returns a list of existing recipes' IDs in the store.<br/>
     * 
     * @return {@link List} of {@link IRI}
     */
    List<IRI> listRecipeIDs();

    /**
     * It returns the list of exisitng recipes in the RuleStore.<br/>
     * 
     * @return {@link RecipeList}
     */
    RecipeList listRecipes() throws NoSuchRecipeException, RecipeConstructionException;

    /**
     * It removes the recipe identified by the ID passed as parameter.<br/>
     * If any problem occurs during the elimination of the recipe from the store a
     * {@link RecipeEliminationException} is thrown.
     * 
     * @param recipeID
     *            {@link IRI}
     * @return <code>true</code> if the recipe has been removed, false otherwise.
     * @throws RecipeEliminationException
     */
    boolean removeRecipe(IRI recipeID) throws RecipeEliminationException;

    /**
     * It removes the recipe passed as parameter.<br/>
     * If any problem occurs during the elimination of the recipe from the store a
     * {@link RecipeEliminationException} is thrown.
     * 
     * @param recipe
     *            {@link Recipe}
     * @return <code>true</code> if the recipe has been removed, false otherwise.
     * @throws RecipeEliminationException
     */
    boolean removeRecipe(Recipe recipe) throws RecipeEliminationException;

    /**
     * It removes the rule passed as second parameter from the recipe passed as first parameter.<br/>
     * 
     * @param recipe
     *            {@link Recipe}
     * @param rule
     *            {@link Rule}
     * @return <code>true</code> if the recipe has been removed, false otherwise.
     */
    Recipe removeRule(Recipe recipe, Rule rule);

    /**
     * It allows to export recipes as Clerezza's {@link Graph} objects.
     * 
     * @param recipe
     * @return
     * @throws NoSuchRecipeException
     */
    Graph exportRecipe(Recipe recipe) throws NoSuchRecipeException;

    /**
     * Find the set of recipes in the rule store whose description matches the <code>term</code>
     * 
     * @param term
     * @return the list of matching recipes
     */
    RecipeList findRecipesByDescription(String term);

    /**
     * Find the set of rules in the rule store whose name matches the <code>term</code>
     * 
     * @param term
     * @return the list of matching rules
     */
    RuleList findRulesByName(String term);

    /**
     * Find the set of rules in the rule store whose descriptions matches the <code>term</code>
     * 
     * @param term
     * @return the list of matching rules
     */
    RuleList findRulesByDescription(String term);

}
