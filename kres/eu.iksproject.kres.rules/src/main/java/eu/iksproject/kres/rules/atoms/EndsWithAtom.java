package eu.iksproject.kres.rules.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.rules.SPARQLComparison;

public class EndsWithAtom extends ComparisonAtom {

	
	private StringFunctionAtom argument; 
	private StringFunctionAtom term;
	
	public EndsWithAtom(StringFunctionAtom argument, StringFunctionAtom term) {
		this.argument = argument;
		this.term = term;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String argumentSparql = argument.toSPARQL().getObject();
		
		
		
		return new SPARQLComparison("<http://www.w3.org/2005/xpath-functions#ends-with> (" + argumentSparql + ", " + term.toSPARQL().getObject() + ")");
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		
		
		
		return "endsWith(" + argument.toKReSSyntax() + ", " + term.toKReSSyntax() + ")";
	}
	
	

}
