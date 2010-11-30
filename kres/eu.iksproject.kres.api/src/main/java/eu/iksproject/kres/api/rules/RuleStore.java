package eu.iksproject.kres.api.rules;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import eu.iksproject.kres.api.semion.util.RecipeList;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public interface RuleStore {

	public OWLOntology getOntology();

	public void setStore(OWLOntology owl);
	
	public Set<IRI> listIRIRecipes();
	
	public RecipeList listRecipes();
	
	public Recipe getRecipe(IRI recipe) throws NoSuchRecipeException;

    public String getFilePath();

    public void saveOntology() throws OWLOntologyStorageException;
    
    public String getRuleStoreNamespace();
    
    public boolean addRecipe(IRI recipeIRI, String recipeDescription);
    
    public Recipe addRuleToRecipe(String recipeID, String kReSRuleInKReSSyntax) throws NoSuchRecipeException;
    
    public Recipe addRuleToRecipe(Recipe recipe, String kReSRuleInKReSSyntax);
    
    public void createRecipe(String recipeID, String rulesInKReSSyntax);

    public boolean removeRecipe(Recipe recipe);

    public boolean removeRecipe(IRI recipeIRI);

    public boolean removeRule(KReSRule rule);
	
}
