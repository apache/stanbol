package eu.iksproject.kres.rules.atoms;

import eu.iksproject.kres.rules.SPARQLFunction;
import eu.iksproject.kres.api.rules.SPARQLObject;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public class LowerCaseAtom extends StringFunctionAtom {

	private StringFunctionAtom stringFunctionAtom;
	
	public LowerCaseAtom(StringFunctionAtom stringFunctionAtom) {
		this.stringFunctionAtom = stringFunctionAtom;
	}

	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		
		String uriResourceString = stringFunctionAtom.toSPARQL().getObject();
		
		if(uriResourceString.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			uriResourceString = "?"+uriResourceString.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
		}
		
		String sparql = "<http://www.w3.org/2005/xpath-functions#lower-case> (" + uriResourceString + ")"; 
			
		return new SPARQLFunction(sparql);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String uriResourceString = stringFunctionAtom.toKReSSyntax();
		
		if(uriResourceString.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			uriResourceString = "?"+uriResourceString.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
		}
		
		return "lowerCase(" + uriResourceString + ")";
	}
	
	
	
}