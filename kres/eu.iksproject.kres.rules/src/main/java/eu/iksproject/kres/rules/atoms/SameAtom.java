package eu.iksproject.kres.rules.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.rules.SPARQLComparison;

public class SameAtom extends ComparisonAtom {
	private StringFunctionAtom stringFunctionAtom1;
	private StringFunctionAtom stringFunctionAtom2;
	
	public SameAtom(StringFunctionAtom stringFunctionAtom1, StringFunctionAtom stringFunctionAtom2) {
		this.stringFunctionAtom1 = stringFunctionAtom1;
		this.stringFunctionAtom2 = stringFunctionAtom2;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		
		String argument1 = stringFunctionAtom1.toSPARQL().getObject();
		String argument2 = stringFunctionAtom2.toSPARQL().getObject();
		
		
		argument1 = argument1.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		argument2 = argument2.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		
		
		return new SPARQLComparison(argument1 + " = " + argument2);
		
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		
		return "same(" + stringFunctionAtom1.toKReSSyntax() + ", " + stringFunctionAtom2.toKReSSyntax() + ")";
		
	}

	
	private OWLLiteral getOWLTypedLiteral(Object argument){
		
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		
		OWLLiteral owlLiteral;
		if(argument instanceof String){
			owlLiteral = factory.getOWLTypedLiteral((String) argument); 
		}
		else if(argument instanceof Integer){
			owlLiteral = factory.getOWLTypedLiteral(((Integer) argument).intValue());
		}
		else if(argument instanceof Double){
			owlLiteral = factory.getOWLTypedLiteral(((Double) argument).doubleValue());
		}
		else if(argument instanceof Float){
			owlLiteral = factory.getOWLTypedLiteral(((Float) argument).floatValue());
		}
		else if(argument instanceof Boolean){
			owlLiteral = factory.getOWLTypedLiteral(((Boolean) argument).booleanValue());
		}
		else{
			owlLiteral = factory.getOWLStringLiteral(argument.toString());
		}
		
		
		
		return owlLiteral; 
	}
}
