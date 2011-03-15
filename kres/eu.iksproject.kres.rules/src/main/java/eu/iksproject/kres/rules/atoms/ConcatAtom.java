package eu.iksproject.kres.rules.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.rules.SPARQLFunction;

public class ConcatAtom extends StringFunctionAtom {

	private StringFunctionAtom argument1;
	private StringFunctionAtom argument2;
	
	public ConcatAtom(StringFunctionAtom argument1, StringFunctionAtom argument2) {
		this.argument1 = argument1;
		this.argument2 = argument2;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		
		String sparqlConcat = "<http://www.w3.org/2005/xpath-functions#concat>";
		String function = sparqlConcat + " (" + argument1.toSPARQL().getObject() + ", " + argument2.toSPARQL().getObject() + ")";
		 
		return new SPARQLFunction(function);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		
		return "concat(" + argument1.toKReSSyntax() + ", " + argument2.toKReSSyntax() + ")";
	}

}
