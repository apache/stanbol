package eu.iksproject.kres.rules.atoms;

import java.net.URI;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.felix.scr.annotations.Reference;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SWRLArgument;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLVariable;

import uk.ac.manchester.cs.owl.owlapi.OWLClassExpressionImpl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.rules.KReSRuleAtom;
import eu.iksproject.kres.api.rules.URIResource;
import eu.iksproject.kres.ontologies.SWRL;

public class ClassAtom implements KReSRuleAtom {

	private URIResource classResource;
	private URIResource argument1;
	
	public ClassAtom(URIResource classResource, URIResource argument1) {
		this.classResource = classResource;
		this.argument1 = argument1;
	}
	
	@Override
	public String toSPARQL() {
		String argument1SPARQL = null;
		String argument2SPARQL = null;
		if(argument1.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			argument1SPARQL = "?"+argument1.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			argument1SPARQL = "<"+argument1.toString()+">";
		}
		
		if(classResource.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			argument2SPARQL = "?"+classResource.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			argument2SPARQL = "<"+classResource.toString()+">";
		}
		
		
		return argument1SPARQL + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> " + argument2SPARQL;
	}

	@Override
	public Resource toSWRL(Model model) {
		
		
		Resource classAtom = model.createResource(SWRL.ClassAtom);
		
		Resource classPredicate = model.createResource(classResource.toString());
		
		classAtom.addProperty(SWRL.classPredicate, classPredicate);
		
		Resource argumentResource = argument1.createJenaResource(model);
		
		classAtom.addProperty(SWRL.argument1, argumentResource);
		
		
		return classAtom;
	}
	
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		
		OWLClass classPredicate = factory.getOWLClass(IRI.create(classResource.getURI().toString()));
		
		SWRLIArgument argumentResource;
		if(argument1 instanceof KReSResource){
			OWLIndividual owlIndividual = factory.getOWLNamedIndividual(IRI.create(argument1.getURI().toString()));
			argumentResource = factory.getSWRLIndividualArgument(owlIndividual);
		}
		else{
			argumentResource = factory.getSWRLVariable(IRI.create(argument1.getURI().toString()));
		}
		
		
		return factory.getSWRLClassAtom(classPredicate, argumentResource);
		
	}
	
	public URIResource getClassResource() {
		return classResource;
	}
	
	public URIResource getArgument1() {
		return argument1;
	}
	
	@Override
	public String toString() {
		
		return argument1.toString() + " is an individual of the class "+classResource.toString();
	}
	
	@Override
	public String toKReSSyntax(){
		String arg1 = null;
		String arg2 = null;
		
		if(argument1.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?"+argument1.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			arg1 = "<"+argument1.toString()+">";
		}
		
		if(classResource.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+classResource.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			arg2 = "<"+classResource.toString()+">";
		}
		return "is(" + arg2 + ", " + arg1 + ")";
	}

	
}
