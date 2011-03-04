package eu.iksproject.kres.rules.atoms;

import java.net.URI;
import java.net.URISyntaxException;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;

import eu.iksproject.kres.api.rules.SPARQLObject;
import eu.iksproject.kres.api.rules.URIResource;
import eu.iksproject.kres.rules.SPARQLTriple;


public class BlankNodeAtom extends KReSCoreAtom {

	private URIResource argument1;
	private URIResource argument2;
	
	public BlankNodeAtom(URIResource argument1, URIResource argument2) {
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
		String sparql = argument2.toString() + " " + argument1.toString() + " _:bNode";
		
		return new SPARQLTriple(sparql);
		
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		
		return "createBN(" + argument1.toString() + ", " + argument2.toString() + ")";
	}

}
