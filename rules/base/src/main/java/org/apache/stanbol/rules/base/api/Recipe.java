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

import org.apache.stanbol.rules.base.api.util.RuleList;
import org.semanticweb.owlapi.model.IRI;

import com.hp.hpl.jena.rdf.model.Model;


public interface Recipe {
	
	/**
	 * Get the rule of the recipe identified by the ruleURI. The rule is returned as
	 * a {@link Rule} object.
	 * 
	 * @param ruleURI
	 * @return the object that represents a {@link Rule}
	 */
    Rule getRule(String ruleURI);
	
	/**
	 * Trasnform the rules contained in the recipe in a set of SPARQL CONSTRUCT queries.
	 * 
	 * @return the {@link String} array that contains the SPARQL CONSTRUCT queries.
	 */
    String[] toSPARQL();
	
	/**
	 * Serialize the {@link Recipe} into a Jena {@link Model}.
	 * 
	 * @return the {@link Model} of the Recipe.
	 */
    Model getRecipeAsRDFModel();
	
	/**
	 * Serialize the rules contained in the recipe to Rule Syntax.
	 * @return the {@link String} containing the serialization of the recipe's rules
	 * in Rule Syntax.
	 */
    String getRulesInKReSSyntax();
	
	/**
	 * Get the list of the {@link Rule} contained in the recipe.
	 * @return the {@link RuleList}.
	 */
    RuleList getkReSRuleList();
	
	/**
	 * Get the ID of the recipe in the {@link RuleStore}.
	 * @return the {@link IRI} expressing the recipe's ID.
	 */
    IRI getRecipeID();
	
	/**
	 * Get the description about the recipe.
	 * @return the {@link String} about the recipe's description.
	 */
    String getRecipeDescription();
	
	/**
	 * Add a Rule to the recipe.
	 * This operation does not effect a change on recipe in the rule store, but only in the in-memory
	 * representation of a specific recipe. To permanently change the recipe use {@link RuleStore#addRuleToRecipe(IRI, String)}.
	 * @param kReSRule the {@link Rule}.
	 */
    void addKReSRule(Rule kReSRule);
    
    
    /**
	 * Convert the recipe in a list of Jena Rules.
	 * 
	 * @return The list of Jena Rules which represents the Recipe.
	 */
    List<com.hp.hpl.jena.reasoner.rulesys.Rule> toJenaRules();
}
