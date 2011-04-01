package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class NumberAtom extends NumericFunctionAtom {

	private String number;
	
	public NumberAtom(String number) {
		this.number = number;
	}
	
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		
		if(number.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			number = "?"+number.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
		}
		
		return new SPARQLFunction(number);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		
		if(number.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			number = "?"+number.replace("http://kres.iks-project.eu/ontology/meta/variables#", "") + ")";
			
		}
		return number;
	}

}
