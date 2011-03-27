package org.apache.stanbol.rules.base.api;

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
	public Rule getRule(String ruleURI);
	
	/**
	 * Trasnform the rules contained in the recipe in a set of SPARQL CONSTRUCT queries.
	 * 
	 * @return the {@link String} array that contains the SPARQL CONSTRUCT queries.
	 */
	public String[] toSPARQL();
	
	/**
	 * Serialize the {@link Recipe} into a Jena {@link Model}.
	 * 
	 * @return the {@link Model} of the Recipe.
	 */
	public Model getRecipeAsRDFModel();
	
	/**
	 * Serialize the rules contained in the recipe to Rule Syntax.
	 * @return the {@link String} containing the serialization of the recipe's rules
	 * in Rule Syntax.
	 */
	public String getRulesInKReSSyntax();
	
	/**
	 * Get the list of the {@link Rule} contained in the recipe.
	 * @return the {@link RuleList}.
	 */
	public RuleList getkReSRuleList();
	
	/**
	 * Get the ID of the recipe in the {@link RuleStore}.
	 * @return the {@link IRI} expressing the recipe's ID.
	 */
	public IRI getRecipeID();
	
	/**
	 * Get the description about the recipe.
	 * @return the {@link String} about the recipe's description.
	 */
	public String getRecipeDescription();
	
	/**
	 * Add a Rule to the recipe.
	 * This operation does not effect a change on recipe in the rule store, but only in the in-memory
	 * representation of a specific recipe. To permanently change the recipe use {@link RuleStore#addRuleToRecipe(IRI, String)}.
	 * @param kReSRule the {@link Rule}.
	 */
	public void addKReSRule(Rule kReSRule);
}
