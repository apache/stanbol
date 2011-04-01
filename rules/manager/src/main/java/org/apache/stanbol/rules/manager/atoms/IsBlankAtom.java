package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.SPARQLComparison;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class IsBlankAtom extends ComparisonAtom {

	private URIResource uriResource;
	
	public IsBlankAtom(URIResource uriResource) {
		this.uriResource = uriResource;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String argument = uriResource.toString();
		
		String sparql; 
		
		if(argument.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			sparql = "?"+argument.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			sparql = argument;
		}
		
		sparql = "isBlank(" + sparql + ")";
		
		return new SPARQLComparison(sparql);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String argument = uriResource.toString();
		
		String kReS; 
		
		if(argument.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			kReS = "?"+argument.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			kReS = argument;
		}
		
		kReS = "isBlank(" + kReS + ")";
		return kReS;
	}

	@Override
	public boolean isSPARQLConstruct() {
		return false;
	}
	
}
