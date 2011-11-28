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
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;


public class SubstringAtom extends StringFunctionAtom {

	private StringFunctionAtom stringFunctionAtom;
	private NumericFunctionAtom start;
	private NumericFunctionAtom length;
	
	public SubstringAtom(StringFunctionAtom stringFunctionAtom, NumericFunctionAtom start, NumericFunctionAtom length) {
		this.stringFunctionAtom = stringFunctionAtom;
		this.start = start;
		this.length = length;
	}

	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		
		String uriResourceString = stringFunctionAtom.toSPARQL().getObject();
		
		if(uriResourceString.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			uriResourceString = "?"+uriResourceString.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
		}
		
		String sparql = "<http://jena.hpl.hp.com/ARQ/function#substr> (" + uriResourceString + ", " + start.toSPARQL().getObject() + ", " + length.toSPARQL().getObject() + ")"; 
			
		return new SPARQLFunction(sparql);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String uriResourceString = stringFunctionAtom.toKReSSyntax();
		
		if(uriResourceString.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			uriResourceString = "?"+uriResourceString.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
		}
		
		return "substring(" + uriResourceString + ", " + start.toKReSSyntax() + ", " + length.toKReSSyntax() + ")";
	}

	@Override
	public JenaClauseEntry toJenaClauseEntry(JenaVariableMap jenaVariableMap) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}
