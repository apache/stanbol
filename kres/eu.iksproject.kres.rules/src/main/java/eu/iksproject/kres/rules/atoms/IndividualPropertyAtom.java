package eu.iksproject.kres.rules.atoms;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TypedLiteralImpl;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLVariable;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.api.rules.KReSRuleAtom;
import eu.iksproject.kres.api.rules.URIResource;
import eu.iksproject.kres.ontologies.SWRL;

public class IndividualPropertyAtom implements KReSRuleAtom {

	
	private URIResource objectProperty;
	private URIResource argument1;
	private URIResource argument2;
	
	public IndividualPropertyAtom(URIResource objectProperty, URIResource argument1, URIResource argument2) {
		this.objectProperty = objectProperty;
		this.argument1 = argument1;
		this.argument2 = argument2;
	}
	
	@Override
	public String toSPARQL() {
		String arg1 = argument1.toString();
		String arg2 = argument2.toString();
		String objP = objectProperty.toString();
		
		if(arg1.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?"+arg1.replace("http://kres.iks-project.eu/ontology/meta/variables#", ""); 
		}
		else{
			arg1 = "<"+arg1+">";
		}
		
		if(arg2.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+arg2.replace("http://kres.iks-project.eu/ontology/meta/variables#", ""); 
		}
		else{
			arg2 = "<"+arg2+">";
		}
		
		if(objP.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			objP = "?"+objP.replace("http://kres.iks-project.eu/ontology/meta/variables#", ""); 
		}
		else{
			objP = "<"+objP+">";
		}
		
		
		return arg1+" "+objP+" "+arg2+" ";
		
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
		}
		else{
			arg1 = "<"+argument1.toString()+">";
		}
		
		
		if(objectProperty.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg3 = "?"+objectProperty.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			arg3 = "<"+objectProperty.toString()+">";
		}
		
		if(argument2.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+argument2.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			arg2 = "<"+argument2.toString()+">";
		}
		
		
		return "has(" + arg3 + ", "+ arg1 +", "+arg2 +")";
		
	}

	
}
