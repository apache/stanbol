package eu.iksproject.kres.rules.atoms;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.api.rules.KReSRuleAtom;
import eu.iksproject.kres.rules.SPARQLFunction;
import eu.iksproject.kres.api.rules.SPARQLObject;
import eu.iksproject.kres.api.rules.URIResource;

public class LetAtom implements KReSRuleAtom {

	private URIResource variable;
	private StringFunctionAtom parameterFunctionAtom;
	
	public LetAtom(URIResource variable, StringFunctionAtom parameterFunctionAtom) {
		this.variable = variable;
		this.parameterFunctionAtom = parameterFunctionAtom;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		System.out.println();
		System.out.println();
		System.out.println("Parameter Function : "+parameterFunctionAtom.toSPARQL().getObject());
		System.out.println();
		System.out.println();
		String variableArgument = variable.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		String let = "LET (?" + variableArgument + " := " + parameterFunctionAtom.toSPARQL().getObject() + ")";
		SPARQLObject sparqlObject = new SPARQLFunction(let);
		return sparqlObject;
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String arg1 = "?" + variable.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		String syntax = "let(" + arg1 + ", " + parameterFunctionAtom.toKReSSyntax() + ")";  
		return syntax;
	}

	
	@Override
	public boolean isSPARQLConstruct() {
		return false;
	}
	
	@Override
	public boolean isSPARQLDelete() {
		return false;
	}
	
	@Override
	public boolean isSPARQLDeleteData() {
		return false;
	}
}
