package eu.iksproject.kres.rules.atoms;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.rules.SPARQLFunction;
import eu.iksproject.kres.api.rules.SPARQLObject;

public class SubstringAtom extends StringFunctionAtom {

	private StringFunctionAtom stringFunctionAtom;
	private NumericFunctionAtom start;
	private NumericFunctionAtom length;
	
	public SubstringAtom(StringFunctionAtom stringFunctionAtom, NumericFunctionAtom start, NumericFunctionAtom length) {
		this.stringFunctionAtom = stringFunctionAtom;
		this.start = start;
		this.length = length;
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
		
		String sparql = "<http://jena.hpl.hp.com/ARQ/function#substr> (" + uriResourceString + ", " + start.toSPARQL().getObject() + ", " + length.toSPARQL().getObject() + ")"; 
			
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
		
		return "substring(" + uriResourceString + ", " + start.toKReSSyntax() + ", " + length.toKReSSyntax() + ")";
	}
	
	
	
}
