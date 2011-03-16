package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class PropStringAtom extends StringFunctionAtom {

	
	private StringFunctionAtom namespaceArg;
	private StringFunctionAtom labelArg;
	
	public PropStringAtom(StringFunctionAtom namespaceArg, StringFunctionAtom labelArg) {
		this.namespaceArg = namespaceArg;
		this.labelArg = labelArg;
	}
	
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String ns = namespaceArg.toSPARQL().getObject();
		String label = labelArg.toSPARQL().getObject();
		
		
		
		
		String sparql = "<http://www.stlab.istc.cnr.it/semion/function#propString>(" + ns + ", " + label + ")"; 
		return new SPARQLFunction(sparql);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String ns = namespaceArg.toKReSSyntax();
		String label = labelArg.toKReSSyntax();
		String kReS = "propString(" + ns + ", " + label + ")"; 
		return kReS;
	}

}
