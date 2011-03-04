package eu.iksproject.kres.rules.atoms;

import eu.iksproject.kres.rules.SPARQLComparison;
import eu.iksproject.kres.api.rules.SPARQLObject;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class NotAtom extends ComparisonAtom {

	
	private ComparisonAtom comparisonAtom;
	
	public NotAtom(ComparisonAtom comparisonAtom) {
		this.comparisonAtom = comparisonAtom;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		SPARQLObject sparqlObject = comparisonAtom.toSPARQL();
		String sparqlAtom = "!" + sparqlObject.getObject();
		return new SPARQLComparison(sparqlAtom);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String kresSyntax = "not(" + comparisonAtom.toKReSSyntax() + ")";
		return kresSyntax;
	}

}
