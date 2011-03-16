package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class CreateLabelAtom extends StringFunctionAtom {

	
	private StringFunctionAtom stringFunctionAtom;
	
	public CreateLabelAtom(StringFunctionAtom stringFunctionAtom) {
		this.stringFunctionAtom = stringFunctionAtom;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		
		System.out.println("Argument instance of "+stringFunctionAtom.getClass().getCanonicalName());
		
		String argument = stringFunctionAtom.toSPARQL().getObject();
		
		if(argument.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			argument = "?"+argument.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		
		String sparql = "<http://www.stlab.istc.cnr.it/semion/function#createLabel>(" + argument + ")";
		return new SPARQLFunction(sparql);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		return "createLabel(" + stringFunctionAtom.toKReSSyntax() + ")";
	}

}
