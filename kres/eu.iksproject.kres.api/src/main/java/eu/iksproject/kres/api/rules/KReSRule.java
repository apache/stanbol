package eu.iksproject.kres.api.rules;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLRule;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.api.rules.util.AtomList;

/**
 * A KReSRule is a Java object that represent a rule in KReS. It contains methods to transform a rule both in SWRL and in KReSRule
 * syntax. 
 * 
 * @author andrea.nuzzolese
 *
 */
public interface KReSRule {
	
	/**
	 * Gets the name of the rule.
	 * 
	 * @return the {@link String} representing the name of the rule.
	 */
	public String getRuleName();
	
	/**
	 * Sets the rule's name
	 * 
	 * @param ruleName {@link String}
	 */
	public void setRuleName(String ruleName);
	
	/**
	 * Returns the representation of the rule in KReSRule syntax.
	 * 
	 * @return the {@link String} of the rule in KReSRule syntax.
	 */
	public String getRule();
	
	/**
	 * Sets the rule expressed in KReSRule syntax
	 * 
	 * @param rule {@link String}
	 */
	public void setRule(String rule);
	
	/**
	 * Maps a {@code KReSRule} to a Jena {@link Resource} object in a given Jena {@link Model}.
	 * @param model {@link Model}
	 * @return the {@link Resource} containing the rule.
	 */
	public Resource toSWRL(Model model);
	
	/**
	 * Maps a {@code KReSRule} to an OWL-API {@link SWRLRule}.
	 * @param factory {@link OWLDataFactory}
	 * @return the {@link SWRLRule} containing the rule.
	 */
	public SWRLRule toSWRL(OWLDataFactory factory);
	
	/**
	 * Transforms the rule to a SPARQL CONSTRUCT.
	 * 
	 * @return the string containing the SPARQL CONSTRUCT.
	 */
	public String toSPARQL();
	
	/**
	 * Rules are composed by an antecedent (body) and a consequent (head). This method returnn the consequent
	 * expressed as a list of its atoms ({@link AtomList}).
	 * @return the {@link AtomList} of the consequent's atoms. 
	 */
	public AtomList getHead();
	
	/**
	 * Rules are composed by an antecedent (body) and a consequent (head). This method returnn the antecedent
	 * expressed as a list of its atoms ({@link AtomList}).
	 * @return the {@link AtomList} of the antecedent's atoms. 
	 */
	public AtomList getBody();
	
	
	/**
	 * Retunr the KReS syntax representation of the rule.
	 * @return the string of the rule in KReSRule syntax.
	 */
	public String toKReSSyntax();

}
