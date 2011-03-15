package eu.iksproject.kres.rules.atoms;

import org.apache.stanbol.rules.base.api.KReSRuleAtom;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.URIResource;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.rules.SPARQLFunction;

public class NewNodeAtom implements KReSRuleAtom {

	
	private URIResource newNodeVariable; 
	private Object binding;
	
	public NewNodeAtom(URIResource newNodeVariable, Object nodeName) {
		this.newNodeVariable = newNodeVariable;
		this.binding = nodeName;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String variable = "?"+newNodeVariable.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		String popertyFunction = "<http://www.stlab.istc.cnr.it/semion/function#createURI>";
		
		String bindingString = null;
		
		if(binding instanceof KReSVariable){
			bindingString = "?"+binding.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			bindingString = binding.toString();
		}
		
		SPARQLObject sparqlObject = new SPARQLFunction(variable + " " + popertyFunction + " " + bindingString);
		return sparqlObject;
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String variable = "?"+newNodeVariable.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		
		String bindingString = null;
		
		if(binding instanceof KReSVariable){
			bindingString = "?"+binding.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			bindingString = binding.toString();
		}
		
		return "newNode(" + variable + ", " + bindingString + ")";
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
