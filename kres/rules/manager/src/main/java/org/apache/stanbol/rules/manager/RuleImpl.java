package org.apache.stanbol.rules.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleExpressiveness;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.util.AtomList;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLRule;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.Resource;

import org.apache.stanbol.rules.base.SWRL;

public class RuleImpl implements Rule {

	
	private String ruleName;
	private String rule;
	
	
	private AtomList head;
	private AtomList body;
	
	private boolean forwardChain;
	private boolean reflexive;
	private boolean sparqlC;
	private boolean sparqlD;
	
	RuleExpressiveness expressiveness;
	
	public RuleImpl(String ruleURI, AtomList body, AtomList head, RuleExpressiveness expressiveness) {
		this.ruleName = ruleURI;
		this.head = head;
		this.body = body;
		this.expressiveness = expressiveness;
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
		
		String sparql = null;
		
		if(isSPARQLConstruct() || isSPARQLDelete()){
			boolean found = false;
			Iterator<RuleAtom> it = body.iterator();
			while(it.hasNext() && !found){
				RuleAtom kReSRuleAtom = it.next();
				sparql = kReSRuleAtom.toSPARQL().getObject();
				found = true;
			}
			
		}
		
		else{
		
			sparql = "CONSTRUCT {";
						
			boolean firstIte = true;
			
			for(RuleAtom kReSRuleAtom : head){
				if(!firstIte){
					sparql += " . ";
				}
				firstIte = false;
				sparql += kReSRuleAtom.toSPARQL().getObject();
			}
			
			sparql += "} ";
			sparql += "WHERE {";
			
			firstIte = true;
			ArrayList<SPARQLObject> sparqlObjects = new ArrayList<SPARQLObject>();
			for(RuleAtom kReSRuleAtom : body){
				SPARQLObject sparqlObject = kReSRuleAtom.toSPARQL();
				if(sparqlObject instanceof SPARQLNot){
					sparqlObjects.add((SPARQLNot) sparqlObject);
				}
				else if(sparqlObject instanceof SPARQLComparison){
					sparqlObjects.add((SPARQLComparison) sparqlObject);
				}
				else{
					if(!firstIte){
						sparql += " . ";
					}
					else{
						firstIte = false;
					}
					sparql += kReSRuleAtom.toSPARQL().getObject();
				}
			}
			
			firstIte = true;
			
			
			String optional = "";
			String filter = "";
			for(SPARQLObject sparqlObj : sparqlObjects){
				if(sparqlObj instanceof SPARQLNot){
					SPARQLNot sparqlNot = (SPARQLNot) sparqlObj;
					if(!firstIte){
						optional += " . ";
					}	
					else{
						firstIte = false;
					}
					
					optional += sparqlNot.getObject();
					
					String[] filters = sparqlNot.getFilters();
					for(String theFilter : filters){
						if(!filter.isEmpty()){
							filter += " && ";
						}
						filter += theFilter;
					}
				}
				else if(sparqlObj instanceof SPARQLComparison){
					SPARQLComparison sparqlDifferent = (SPARQLComparison) sparqlObj;
					
					String theFilter = sparqlDifferent.getObject();
					
					if(!filter.isEmpty()){
						filter += " && ";
					}
					
					filter += theFilter;
				}
			}
			
			if(!optional.isEmpty()){
				sparql += " . OPTIONAL { " + optional + " } ";
			}
			if(!filter.isEmpty()){
				sparql += " . FILTER ( " + filter + " ) ";
			}
			
			sparql += "}";
		}
		
		return sparql;
	}

	public Resource toSWRL(Model model) {
		
		
		Resource imp = null;
		
		if(head!=null && body!=null){
			imp = model.createResource(ruleName, SWRL.Imp);
			
			
			//RDF list for body 
			RDFList list = model.createList();			
			for(RuleAtom atom : body){
				list = list.cons(atom.toSWRL(model));
			}			
			imp.addProperty(SWRL.body, list);
			
			//RDF list for head
			list = model.createList();			
			for(RuleAtom atom : head){
				list = list.cons(atom.toSWRL(model));
			}			
			imp.addProperty(SWRL.head, list);
			
		}
		
		return imp;
	}
	
	
	public SWRLRule toSWRL(OWLDataFactory factory) {
		
		Set<SWRLAtom> bodyAtoms = new HashSet<SWRLAtom>();
		Set<SWRLAtom> headAtoms = new HashSet<SWRLAtom>();
		for(RuleAtom atom : body){
			bodyAtoms.add(atom.toSWRL(factory));
		}
		for(RuleAtom atom : head){
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
			for(RuleAtom atom : body){
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
			for(RuleAtom atom : head){
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
		
		
		if(isSPARQLConstruct() || isSPARQLDelete()){
			boolean found = false;
			Iterator<RuleAtom> it = body.iterator();
			while(it.hasNext() && !found){
				RuleAtom kReSRuleAtom = it.next();
				ruleInKReSSyntax = kReSRuleAtom.toSPARQL().getObject();
				found = true;
			}
			
		}
		else{
		
			boolean firstLoop = true;
			for(RuleAtom atom : body){
				if(!firstLoop){
					ruleInKReSSyntax += " . ";
				}
				else{
					firstLoop = false;
				}
				ruleInKReSSyntax += atom.toKReSSyntax();
			}
			
			if(head != null){
			
				ruleInKReSSyntax += " -> ";
				
				firstLoop = true;
				for(RuleAtom atom : head){
					if(!firstLoop){
						ruleInKReSSyntax += " . ";
					}
					else{
						firstLoop = false;
					}
					ruleInKReSSyntax += atom.toKReSSyntax();
				}
			}
		}
		
		ruleInKReSSyntax += "]";
		
		return ruleInKReSSyntax;
	}
	
	@Override
	public boolean isForwardChain() {
		switch (expressiveness) {
		case ForwardChaining:
			return true;

		default:
			return false;
		}
	}

	@Override
	public boolean isSPARQLConstruct() {
		switch (expressiveness) {
		case SPARQLConstruct:
			return true;

		default:
			return false;
		}
	}

	@Override
	public boolean isSPARQLDelete() {
		switch (expressiveness) {
		case SPARQLDelete:
			return true;

		default:
			return false;
		}
	}
	
	@Override
	public boolean isSPARQLDeleteData() {
		switch (expressiveness) {
		case SPARQLDeleteData:
			return true;

		default:
			return false;
		}
	}
	

	@Override
	public boolean isReflexive() {
		switch (expressiveness) {
		case Reflexive:
			return true;

		default:
			return false;
		}
	}

	
	@Override
	public RuleExpressiveness getExpressiveness() {
		return expressiveness;
	}
}
