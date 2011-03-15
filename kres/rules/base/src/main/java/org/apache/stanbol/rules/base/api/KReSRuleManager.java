package org.apache.stanbol.rules.base.api;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;


/**
 * 
 * @author andrea.nuzzolese
 *
 */
public interface KReSRuleManager {
	
	/**
	 * Adds one or more rules to the rule ontology of KReS. Rules are passed as {@link String} contantining rules in KReSRule format
	 * @param kReSRuleString {@link String}
	 * @return true if the rule is added, false otherwise.
	 */
	public boolean addRules(String kReSRuleString);
	
	/**
	 * Creates a recipe with the specified ID.
	 * 
	 * @param recipeID {@link String}
	 * @return true if the recipe is created, false otherwise.
	 */
	public boolean createRecipe(String recipeID);
	
	/**
	 * Creates a recipe with the specified ID and adds the rules identified by the IRIs in the set to the recipe.
	 * 
	 * @param recipeID {@link String}
	 * @param ruleIRIs {@link Set< IRI >}
	 * @return true if the recipe is created, false otherwise.
	 */
		
	public boolean createRecipe(String recipeID, Set<IRI> ruleIRIs);
	
	/**
	 * Removes the recipe identified by the {@code recipeID}.
	 * 
	 * @param recipeID {@link String}
	 * @return true if the recipe is removed, false otherwise.
	 */
	public boolean removeRecipe(String recipeID);
	
	/**
	 * Adds the rule identified by its IRI to a scpecified recipe.
	 * @param recipeIRI {@link IRI}
	 * @param swrlRuleIri {@link IRI}
	 * @return true if the rule is added to the recipe, false otherwise.
	 */
	public boolean addRuleToRecipe(IRI recipeIRI, IRI swrlRuleIri);

	/**
	 * Gets the recipe specified by the IRI.
	 * 
	 * @param recipeIRI {@link IRI}
	 * @return the set ot the rules' IRIs.
	 */
	public Set<IRI> getRecipe(IRI recipeIRI);
	
	
	/**
	 * Gets the selected rule from the rule base.
	 * 
	 * @param ruleIRI {@link IRI}
	 * @return the {@link KReSRule}.
	 */
	public KReSRule getRule(IRI ruleIRI);

}
