package eu.iksproject.kres.rules.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.URIResource;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.rules.SPARQLFunction;

public class LocalNameAtom extends StringFunctionAtom {

	private URIResource uriResource;
	
	public LocalNameAtom(URIResource uriResource) {
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
		
		if(argument.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			argument = "?"+argument.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		
		String sparql = "<http://jena.hpl.hp.com/ARQ/function#localname>(" + argument + ")";
		return new SPARQLFunction(sparql);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String argument = uriResource.toString();
		
		if(argument.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			argument = "?"+argument.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		return "localname(" + argument + ")";
	}

}
