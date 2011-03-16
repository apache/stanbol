package org.apache.stanbol.rules.manager.atoms;

import java.util.ArrayList;

import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.SPARQLNot;
import org.apache.stanbol.rules.manager.SPARQLTriple;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLIArgument;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.ontologies.SWRL;

public class IndividualPropertyAtom extends KReSCoreAtom {

	
	private URIResource objectProperty;
	private URIResource argument1;
	private URIResource argument2;
	
	public IndividualPropertyAtom(URIResource objectProperty, URIResource argument1, URIResource argument2) {
		this.objectProperty = objectProperty;
		this.argument1 = argument1;
		this.argument2 = argument2;
	}
	
	@Override
	public SPARQLObject toSPARQL() {
		String arg1 = argument1.toString();
		String arg2 = argument2.toString();
		String objP = objectProperty.toString();
		
		boolean negativeArg1 = false;
		boolean negativeArg2 = false;
		boolean negativeObjP = false;
		
		if(arg1.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?"+arg1.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			KReSVariable variable = (KReSVariable) argument1;
			if(variable.isNegative()){
				negativeArg1 = true;
			}
		}
		
		if(arg2.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+arg2.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			KReSVariable variable = (KReSVariable) argument2;
			if(variable.isNegative()){
				negativeArg2 = true;
			}
		}
		
		if(objP.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			objP = "?"+objP.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			KReSVariable variable = (KReSVariable) objectProperty;
			if(variable.isNegative()){
				negativeObjP = true;
			}
		}
		
		
		
		if(negativeArg1 || negativeArg2 || negativeObjP){
			String optional = arg1+" "+objP+" "+arg2;
			
			ArrayList<String> filters = new ArrayList<String>();
			if(negativeArg1){
				filters.add("!bound(" + arg1 + ")");
			}
			if(negativeArg2){
				filters.add("!bound(" + arg2 + ")");
			}
			if(negativeObjP){
				filters.add("!bound(" + objP + ")");
			}
			
			String[] filterArray = new String[filters.size()];
			filterArray = filters.toArray(filterArray);
			
			return new SPARQLNot(optional, filterArray);
		}
		else{
			return new SPARQLTriple(arg1+" "+objP+" "+arg2);
		}
		
	}

	@Override
	public Resource toSWRL(Model model) {
		Resource individualPropertyAtom = model.createResource(SWRL.IndividualPropertyAtom);
		
		Resource objectPropertyPredicate = objectProperty.createJenaResource(model);
		Resource argument1Resource = argument1.createJenaResource(model);
		Resource argument2Resource = argument2.createJenaResource(model);
		
		individualPropertyAtom.addProperty(SWRL.propertyPredicate, objectPropertyPredicate);
		individualPropertyAtom.addProperty(SWRL.argument1, argument1Resource);
		individualPropertyAtom.addProperty(SWRL.argument2, argument2Resource);
		
		return individualPropertyAtom;
	}

	
	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		OWLObjectProperty owlObjectProperty = factory.getOWLObjectProperty(IRI.create(objectProperty.getURI().toString()));
		
		SWRLIArgument swrliArgument1;
		SWRLIArgument swrliArgument2;
		
		if(argument1 instanceof KReSVariable){
			swrliArgument1 = factory.getSWRLVariable(IRI.create(argument1.getURI().toString()));
		}
		else{
			OWLIndividual owlIndividual = factory.getOWLNamedIndividual(IRI.create(argument1.getURI().toString()));
			swrliArgument1 = factory.getSWRLIndividualArgument(owlIndividual);
		}
		
		if(argument2 instanceof KReSVariable){
			swrliArgument2 = factory.getSWRLVariable(IRI.create(argument2.getURI().toString()));
		}
		else{
			OWLIndividual owlIndividual = factory.getOWLNamedIndividual(IRI.create(argument2.getURI().toString()));
			swrliArgument2 = factory.getSWRLIndividualArgument(owlIndividual);
		}
		
		
		
		return factory.getSWRLObjectPropertyAtom(owlObjectProperty, swrliArgument1, swrliArgument2);
	}
	
	public URIResource getObjectProperty() {
		return objectProperty;
	}
	
	public URIResource getArgument1() {
		return argument1;
	}
	
	public URIResource getArgument2() {
		return argument2;
	}
	
	@Override
	public String toString() {
		return "Individual "+argument1.toString() + " has object property "+argument1.toString()+" that refers to individual "+argument2.toString();
	}


	@Override
	public String toKReSSyntax(){
		String arg1 = null;
		String arg2 = null;
		String arg3 = null;
		
		if(argument1.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?"+argument1.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			KReSVariable variable = (KReSVariable) argument1;
			if(variable.isNegative()){
				arg1 = "notex(" + arg1 + ")";
			}
		}
		else{
			arg1 = argument1.toString();
		}
		
		
		if(objectProperty.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg3 = "?"+objectProperty.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			KReSVariable variable = (KReSVariable) objectProperty;
			if(variable.isNegative()){
				arg3 = "notex(" + arg3 + ")";
			}
		}
		else{
			arg3 = objectProperty.toString();
		}
		
		if(argument2.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+argument2.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			KReSVariable variable = (KReSVariable) argument2;
			if(variable.isNegative()){
				arg2 = "notex(" + arg2 + ")";
			}
		}
		else{
			arg2 = argument2.toString();
		}
		
		
		return "has(" + arg3 + ", "+ arg1 +", "+arg2 +")";
		
	}

	@Override
	public boolean isSPARQLConstruct() {
		return false;
	}
	
	@Override
	public boolean isSPARQLDelete() {
		return false;
	}

	
}
