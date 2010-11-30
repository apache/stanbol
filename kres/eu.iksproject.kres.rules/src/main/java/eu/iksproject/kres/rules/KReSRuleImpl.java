package eu.iksproject.kres.rules;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.HEAD;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.iksproject.kres.api.rules.KReSRule;
import eu.iksproject.kres.api.rules.KReSRuleAtom;
import eu.iksproject.kres.api.rules.util.AtomList;
import eu.iksproject.kres.api.rules.Recipe;
import eu.iksproject.kres.ontologies.SWRL;

public class KReSRuleImpl implements KReSRule {

	
	private String ruleName;
	private String rule;
	
	
	private AtomList head;
	private AtomList body;
	
	
	public KReSRuleImpl(String ruleURI, AtomList body, AtomList head) {
		this.ruleName = ruleURI;
		this.head = head;
		this.body = body;
	}
	
	
	public String getRuleName() {
		return ruleName;
	}

	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	public String getRule() {
		
		return rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}
	
	public String toSPARQL() {
		
		String sparql = "CONSTRUCT {";
					
		boolean firstIte = true;
		
		for(KReSRuleAtom kReSRuleAtom : head){
			if(!firstIte){
				sparql += " . ";
			}
			firstIte = false;
			sparql += kReSRuleAtom.toSPARQL();
		}
		
		sparql += "} ";
		sparql += "WHERE {";
		
		firstIte = true;
		for(KReSRuleAtom kReSRuleAtom : body){
			if(!firstIte){
				sparql += " . ";
			}
			sparql += kReSRuleAtom.toSPARQL();
			firstIte = false;
		}
		
		sparql += "}";
		
		return sparql;
	}

	public Resource toSWRL(Model model) {
		
		
		Resource imp = null;
		
		if(head!=null && body!=null){
			imp = model.createResource(ruleName, SWRL.Imp);
			
			
			//RDF list for body 
			RDFList list = model.createList();			
			for(KReSRuleAtom atom : body){
				list = list.cons(atom.toSWRL(model));
			}			
			imp.addProperty(SWRL.body, list);
			
			//RDF list for head
			list = model.createList();			
			for(KReSRuleAtom atom : head){
				list = list.cons(atom.toSWRL(model));
			}			
			imp.addProperty(SWRL.head, list);
			
		}
		
		return imp;
	}
	
	
	public SWRLRule toSWRL(OWLDataFactory factory) {
		
		Set<SWRLAtom> bodyAtoms = new HashSet<SWRLAtom>();
		Set<SWRLAtom> headAtoms = new HashSet<SWRLAtom>();
		for(KReSRuleAtom atom : body){
			bodyAtoms.add(atom.toSWRL(factory));
		}
		for(KReSRuleAtom atom : head){
			headAtoms.add(atom.toSWRL(factory));
		}
		return factory.getSWRLRule(
			IRI.create(ruleName),
			bodyAtoms,
			headAtoms);
		
		
		
	}
	
	@Override
	public String toString() {
		String rule = null;
		String tab = "       ";
		if(head!=null && body!= null){
			boolean addAnd = false;
			rule = "RULE "+ruleName+" ASSERTS THAT "+System.getProperty("line.separator");
			rule += "IF"+System.getProperty("line.separator");
			for(KReSRuleAtom atom : body){
				rule += tab;
				if(addAnd){
					rule += "AND ";
				}
				else{
					addAnd = true;
				}
				rule += atom.toString()+System.getProperty("line.separator");
				
			}
			
			rule += "IMPLIES"+System.getProperty("line.separator");
			
			addAnd = false;
			for(KReSRuleAtom atom : head){
				rule += tab;
				if(addAnd){
					rule += "AND ";
				}
				else{
					addAnd = true;
				}
				rule += atom.toString()+System.getProperty("line.separator");
				
			}
		}
		return rule;
	}
	
	


	@Override
	public AtomList getBody() {
		return body;
	}

	@Override
	public AtomList getHead() {
		return head;
	}

	@Override
	public String toKReSSyntax(){
		
		Resource rs = ModelFactory.createDefaultModel().createResource(ruleName);
		String ruleInKReSSyntax = rs.getLocalName()+"[";
		boolean firstLoop = true;
		for(KReSRuleAtom atom : body){
			if(!firstLoop){
				ruleInKReSSyntax += " . ";
			}
			else{
				firstLoop = false;
			}
			ruleInKReSSyntax += atom.toKReSSyntax();
		}
		
		ruleInKReSSyntax += " -> ";
		
		firstLoop = true;
		for(KReSRuleAtom atom : head){
			if(!firstLoop){
				ruleInKReSSyntax += " . ";
			}
			else{
				firstLoop = false;
			}
			ruleInKReSSyntax += atom.toKReSSyntax();
		}
		
		ruleInKReSSyntax += "]";
		
		return ruleInKReSSyntax;
	}
	
}
