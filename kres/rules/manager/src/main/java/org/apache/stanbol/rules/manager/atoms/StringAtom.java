package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class StringAtom extends StringFunctionAtom {

	private String string;
	
	public StringAtom(String string) {
		this.string = string;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		
		if(string.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			return new SPARQLFunction("?" + string.replace("http://kres.iks-project.eu/ontology/meta/variables#", ""));
		}
		
		else{
			return new SPARQLFunction(string);
		}
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		return string;
	}

}
