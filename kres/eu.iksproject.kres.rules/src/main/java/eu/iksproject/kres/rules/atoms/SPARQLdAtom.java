package eu.iksproject.kres.rules.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.rules.SPARQLFunction;

public class SPARQLdAtom extends SPARQLDeleteAtomAbstract {

	private String sparqlConstruct;
	
	public SPARQLdAtom(String sparqlConstruct) {
		this.sparqlConstruct = sparqlConstruct.substring(1, sparqlConstruct.length()-1);
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		return new SPARQLFunction(sparqlConstruct);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		return "sparql-d(" + sparqlConstruct + ")";
	}

}
