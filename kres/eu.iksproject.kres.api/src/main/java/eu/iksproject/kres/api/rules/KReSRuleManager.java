package eu.iksproject.kres.api.rules;

import java.util.Collections;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.util.OWLEntityRemover;

import eu.iksproject.kres.api.manager.ontology.OntologySpaceModificationException;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.rules.util.KReSRuleList;


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
