/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.JenaClauseEntry;
import org.apache.stanbol.rules.base.api.JenaVariableMap;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.util.AtomList;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;


public class UnionAtom implements RuleAtom {

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
		
		for(RuleAtom kReSRuleAtom : atomList1){
			if(!scope1.isEmpty()){
				scope1 += " . ";
			}
			scope1 += kReSRuleAtom.toSPARQL().getObject();
		}
		
		String scope2 = "";
		
		for(RuleAtom kReSRuleAtom : atomList2){
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
		
		for(RuleAtom kReSRuleAtom : atomList1){
			if(!scope1.isEmpty()){
				scope1 += " . ";
			}
			scope1 += kReSRuleAtom.toKReSSyntax();
		}
		
		String scope2 = "";
		
		for(RuleAtom kReSRuleAtom : atomList2){
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

	@Override
	public JenaClauseEntry toJenaClauseEntry(JenaVariableMap jenaVariableMap) {
		// TODO Auto-generated method stub
		return null;
	}

}
