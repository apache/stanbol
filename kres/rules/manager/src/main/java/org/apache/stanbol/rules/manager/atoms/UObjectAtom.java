package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class UObjectAtom extends StringFunctionAtom {

	public static final int STRING_TYPE = 0;
	public static final int INTEGER_TYPE = 1;
	public static final int VARIABLE_TYPE = 2;
	
	
	private Object argument;
	private int actualType;
	
	public UObjectAtom(Object argument) {
		this.argument = argument;
		
		if(argument instanceof VariableAtom){
			actualType = 2;
		}
		else if(argument instanceof String){
			actualType = 0;
		}
		else if(argument instanceof Integer){
			actualType = 1;
		}
		
		System.out.println("UObject : " + argument);
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String argumentSPARQL = null;
		
		switch (actualType) {
		case 0:
			argumentSPARQL = "\"" + (String) argument + "\"^^<http://www.w3.org/2001/XMLSchema#string>";
			break;
		case 1:
			argumentSPARQL = ((Integer) argument).toString() + "^^<http://www.w3.org/2001/XMLSchema#int>";
			break;
		case 2:
			argumentSPARQL = "?"+argument.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			break;
		default:
			break;
		}
		
		if(argumentSPARQL != null){
			return new SPARQLFunction(argumentSPARQL);
		}
		else{
			return null;
		}
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String argumentString = null;
		switch (actualType) {
		case 0:
			argumentString = (String) argument;
			break;
		case 1:
			argumentString = ((Integer) argument).toString();
			break;
		case 2:
			argumentString = "?"+argument.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			break;
		default:
			break;
		}
		return argumentString;
	}

}
