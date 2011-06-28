package org.apache.stanbol.rules.base.api;

import java.util.Set;

import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public interface RuleStore {

    /**
     * The key used to configure default namespace of Stanbol rules.
     */
    String RULE_NAMESPACE = "org.apache.stanbol.rules.base.rule_namespace";

    /**
     * The key used to configure the path of the default rule ontology.
     */
    String RULE_ONTOLOGY = "org.apache.stanbol.rules.base.rule_ontology";

    boolean addRecipe(IRI recipeIRI, String recipeDescription);

    Recipe addRuleToRecipe(Recipe recipe, String kReSRuleInKReSSyntax);

    Recipe addRuleToRecipe(String recipeID, String kReSRuleInKReSSyntax) throws NoSuchRecipeException;

    void createRecipe(String recipeID, String rulesInKReSSyntax);

    String getFilePath();

    OWLOntology getOntology();

    Recipe getRecipe(IRI recipe) throws NoSuchRecipeException;

    String getRuleStoreNamespace();

    Set<IRI> listIRIRecipes();

    RecipeList listRecipes();

    boolean removeRecipe(IRI recipeIRI);

    boolean removeRecipe(Recipe recipe);

    boolean removeRule(Rule rule);

    void saveOntology() throws OWLOntologyStorageException;

    void setStore(OWLOntology owl);

}
