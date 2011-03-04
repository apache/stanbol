package eu.iksproject.kres.api.rules;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public interface KReSRuleAtom {

	public Resource toSWRL(Model model);
	public SPARQLObject toSPARQL();
	public SWRLAtom toSWRL(OWLDataFactory factory);
	
	/**
	 * Retunr the KReS syntax representation of the atom.
	 * @return the string of the atom in KReSRule syntax.
	 */
	public String toKReSSyntax();
	
	public boolean isSPARQLConstruct();
	
	public boolean isSPARQLDelete();
	
	public boolean isSPARQLDeleteData();
	
}
