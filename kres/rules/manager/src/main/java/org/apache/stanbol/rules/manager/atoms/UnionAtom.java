package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.KReSRuleAtom;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.util.AtomList;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class UnionAtom implements KReSRuleAtom {

	private AtomList atomList1;
	private AtomList atomList2;
	
	public UnionAtom(AtomList atomList1, AtomList atomList2) {
		this.atomList1 = atomList1;
		this.atomList2 = atomList2;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String scope1 = "";
		
		for(KReSRuleAtom kReSRuleAtom : atomList1){
			if(!scope1.isEmpty()){
				scope1 += " . ";
			}
			scope1 += kReSRuleAtom.toSPARQL().getObject();
		}
		
		String scope2 = "";
		
		for(KReSRuleAtom kReSRuleAtom : atomList2){
			if(!scope2.isEmpty()){
				scope2 += " . ";
			}
			scope2 += kReSRuleAtom.toSPARQL().getObject();
		}
		
		String sparqlUnion = " { " + scope1 + " } UNION { " +scope2 + " } ";
		
		return new SPARQLFunction(sparqlUnion);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String scope1 = "";
		
		for(KReSRuleAtom kReSRuleAtom : atomList1){
			if(!scope1.isEmpty()){
				scope1 += " . ";
			}
			scope1 += kReSRuleAtom.toKReSSyntax();
		}
		
		String scope2 = "";
		
		for(KReSRuleAtom kReSRuleAtom : atomList2){
			if(!scope2.isEmpty()){
				scope2 += " . ";
			}
			scope2 += kReSRuleAtom.toKReSSyntax();
		}
		
		return "union(" + scope1 + ", " +scope2 + ")";
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
