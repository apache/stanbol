package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLComparison;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class LessThanAtom extends ComparisonAtom {

	
	private Object argument1;
	private Object argument2;
	
	public LessThanAtom(Object argument1, Object argument2) {
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
		String arg1 = argument1.toString();
		String arg2 = argument2.toString();
		
		
		if(arg1.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "str(?"+arg1.replace("http://kres.iks-project.eu/ontology/meta/variables#", "") + ")";
			
		}
		else{
			arg1 = "str("+arg1+")";
		}
		
		if(arg2.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "str(?"+arg2.replace("http://kres.iks-project.eu/ontology/meta/variables#", "") + ")";
			
		}
		else if(!arg2.startsWith("<") && !arg2.endsWith(">")){
			OWLLiteral literal = getOWLTypedLiteral(argument2);
			arg2 = "str(" + literal.getLiteral() + ")";
		}
		else{
			arg2 = "str("+arg2+")";
		}
		
		
		return new SPARQLComparison(arg1+" < "+arg2);
		
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String arg1 = null;
		String arg2 = null;
		
		if(argument1.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?"+argument1.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			arg1 = argument1.toString();
		}
		
		
		if(argument2.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+argument2.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
			
			return "lt(" + arg1 + ", " + arg2 +")";
		}
		else{
			
			return "lt(" + arg1 + ", " + argument2.toString() +")";
		}
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
