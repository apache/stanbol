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

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;


/**
 * 
 * @author andrea.nuzzolese
 *
 */
public interface RuleManager {
	
	/**
	 * Adds one or more rules to the rule ontology of KReS. Rules are passed as {@link String} contantining rules in Rule format
	 * @param kReSRuleString {@link String}
	 * @return true if the rule is added, false otherwise.
	 */
    boolean addRules(String kReSRuleString);
	
	/**
	 * Creates a recipe with the specified ID.
	 * 
	 * @param recipeID {@link String}
	 * @return true if the recipe is created, false otherwise.
	 */
    boolean createRecipe(String recipeID);
	
	/**
	 * Creates a recipe with the specified ID and adds the rules identified by the IRIs in the set to the recipe.
	 * 
	 * @param recipeID {@link String}
	 * @param ruleIRIs {@link Set< IRI >}
	 * @return true if the recipe is created, false otherwise.
	 */

    boolean createRecipe(String recipeID, Set<IRI> ruleIRIs);
	
	/**
	 * Removes the recipe identified by the {@code recipeID}.
	 * 
	 * @param recipeID {@link String}
	 * @return true if the recipe is removed, false otherwise.
	 */
    boolean removeRecipe(String recipeID);
	
	/**
	 * Adds the rule identified by its IRI to a scpecified recipe.
	 * @param recipeIRI {@link IRI}
	 * @param swrlRuleIri {@link IRI}
	 * @return true if the rule is added to the recipe, false otherwise.
	 */
    boolean addRuleToRecipe(IRI recipeIRI, IRI swrlRuleIri);

	/**
	 * Gets the recipe specified by the IRI.
	 * 
	 * @param recipeIRI {@link IRI}
	 * @return the set ot the rules' IRIs.
	 */
    Set<IRI> getRecipe(IRI recipeIRI);
	
	
	/**
	 * Gets the selected rule from the rule base.
	 * 
	 * @param ruleIRI {@link IRI}
	 * @return the {@link Rule}.
	 */
    Rule getRule(IRI ruleIRI);

}
