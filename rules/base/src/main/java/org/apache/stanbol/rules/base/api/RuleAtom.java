package org.apache.stanbol.rules.base.api;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public interface RuleAtom {

	Resource toSWRL(Model model);
	SPARQLObject toSPARQL();
	SWRLAtom toSWRL(OWLDataFactory factory);
	
	/**
	 * Retunr the KReS syntax representation of the atom.
	 * @return the string of the atom in KReSRule syntax.
	 */
    String toKReSSyntax();
	
	boolean isSPARQLConstruct();
	
	boolean isSPARQLDelete();
	
	boolean isSPARQLDeleteData();
	
}
