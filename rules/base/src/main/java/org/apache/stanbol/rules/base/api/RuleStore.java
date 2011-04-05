package org.apache.stanbol.rules.base.api;

import java.util.Set;

import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;


public interface RuleStore {

	OWLOntology getOntology();

	void setStore(OWLOntology owl);
	
	Set<IRI> listIRIRecipes();
	
	RecipeList listRecipes();
	
	Recipe getRecipe(IRI recipe) throws NoSuchRecipeException;

    String getFilePath();

    void saveOntology() throws OWLOntologyStorageException;
    
    String getRuleStoreNamespace();
    
    boolean addRecipe(IRI recipeIRI, String recipeDescription);
    
    Recipe addRuleToRecipe(String recipeID, String kReSRuleInKReSSyntax) throws NoSuchRecipeException;
    
    Recipe addRuleToRecipe(Recipe recipe, String kReSRuleInKReSSyntax);
    
    void createRecipe(String recipeID, String rulesInKReSSyntax);

    boolean removeRecipe(Recipe recipe);

    boolean removeRecipe(IRI recipeIRI);

    boolean removeRule(Rule rule);
	
}
