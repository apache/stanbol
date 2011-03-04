package eu.iksproject.kres.rules.atoms;

import eu.iksproject.kres.rules.SPARQLFunction;
import eu.iksproject.kres.api.rules.SPARQLObject;
import eu.iksproject.kres.api.rules.URIResource;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class LengthAtom extends NumericFunctionAtom {

	
	private StringFunctionAtom stringFunctionAtom;
	
	public LengthAtom(StringFunctionAtom stringFunctionAtom) {
		this.stringFunctionAtom = stringFunctionAtom;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String sparql = "<http://www.w3.org/2005/xpath-functions#string-length> (" + stringFunctionAtom.toSPARQL().getObject() + ")";
		
		return new SPARQLFunction(sparql);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		return "length(" + stringFunctionAtom.toKReSSyntax() + ")";
	}

}
