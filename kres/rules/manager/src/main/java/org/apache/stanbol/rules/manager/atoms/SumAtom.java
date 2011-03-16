package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class SumAtom extends NumericFunctionAtom {

	
	private NumericFunctionAtom numericFunctionAtom1; 
	private NumericFunctionAtom numericFunctionAtom2;
	
	public SumAtom(NumericFunctionAtom numericFunctionAtom1, NumericFunctionAtom numericFunctionAtom2) {
		this.numericFunctionAtom1 = numericFunctionAtom1;
		this.numericFunctionAtom2 = numericFunctionAtom2;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String sparqlFunction1 = numericFunctionAtom1.toSPARQL().getObject();
		String sparqlFunction2 = numericFunctionAtom2.toSPARQL().getObject();
		return new SPARQLFunction(sparqlFunction1 + " + " +sparqlFunction2);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String kReSFunction1 = numericFunctionAtom1.toKReSSyntax();
		String kReSFunction2 = numericFunctionAtom2.toKReSSyntax();
		return kReSFunction1 + " + " + kReSFunction2;
	}

}
