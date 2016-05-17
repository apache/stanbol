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

import java.util.List;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.rules.base.api.util.RuleList;

/**
 * It represents a Recipe object.<br/>
 * A recipe is a set of rules that share for some reason a same functionality or need.
 * 
 * @author anuzzolese
 * 
 */
public interface Recipe extends Adaptable {

    /**
     * Get the rule of the recipe identified by the rule name. The rule is returned as a {@link Rule} object.
     * 
     * @param ruleName
     *            {@link String}
     * @return the object that represents a {@link Rule}
     */
    Rule getRule(String ruleName) throws NoSuchRuleInRecipeException;

    /**
     * Get the rule of the recipe identified by the rule ID. The rule is returned as a {@link Rule} object.
     * 
     * @param ruleID
     *            {@link IRI}
     * @return the object that represents a {@link Rule}
     */
    Rule getRule(IRI ruleID) throws NoSuchRuleInRecipeException;

    /**
     * Get the list of the {@link Rule} contained in the recipe.
     * 
     * @return the {@link RuleList}.
     */
    RuleList getRuleList();

    /**
     * Get the list of rule IDs contained in the recipe.
     * 
     * @return the List of {@link IRI}.
     */
    List<IRI> listRuleIDs();

    /**
     * Get the list of rule names contained in the recipe.
     * 
     * @return the List of {@link IRI}.
     */
    List<String> listRuleNames();

    /**
     * Get the ID of the recipe in the {@link RuleStore}.
     * 
     * @return the {@link IRI} expressing the recipe's ID.
     */
    IRI getRecipeID();

    /**
     * Get the description about the recipe.
     * 
     * @return the {@link String} about the recipe's description.
     */
    String getRecipeDescription();

    /**
     * Add a Rule to the recipe. This operation does not effect a change on recipe in the rule store, but only
     * in the in-memory representation of a specific recipe. To permanently change the recipe use
     * {@link RuleStore#addRuleToRecipe(IRI, String)}.
     * 
     * @param rRule
     *            the {@link Rule}.
     */
    void addRule(Rule rule);

    /**
     * Remove a Rule from the recipe. This operation does not effect a change on recipe in the rule store, but
     * only in the in-memory representation of a specific recipe. To permanently change the recipe use
     * {@link RuleStore#addRuleToRecipe(IRI, String)}.
     * 
     * @param rRule
     *            the {@link Rule}.
     */
    void removeRule(Rule rule);

}
