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
import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class LetAtom implements RuleAtom {
    
    private Logger log = LoggerFactory.getLogger(getClass());

	private URIResource variable;
	private StringFunctionAtom parameterFunctionAtom;
	
	public LetAtom(URIResource variable, StringFunctionAtom parameterFunctionAtom) {
		this.variable = variable;
		this.parameterFunctionAtom = parameterFunctionAtom;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		log.debug("Parameter Function : "+parameterFunctionAtom.toSPARQL().getObject());
		String variableArgument = variable.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		String let = "LET (?" + variableArgument + " := " + parameterFunctionAtom.toSPARQL().getObject() + ")";
		SPARQLObject sparqlObject = new SPARQLFunction(let);
		return sparqlObject;
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String arg1 = "?" + variable.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		String syntax = "let(" + arg1 + ", " + parameterFunctionAtom.toKReSSyntax() + ")";  
		return syntax;
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
