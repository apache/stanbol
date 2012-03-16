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


public class UObjectAtom extends StringFunctionAtom {

	public static final int STRING_TYPE = 0;
	public static final int INTEGER_TYPE = 1;
	public static final int VARIABLE_TYPE = 2;
	
	
	private Object argument;
	private int actualType;
	
	public UObjectAtom(Object argument) {
		this.argument = argument;
		
		if(argument instanceof VariableAtom){
			actualType = 2;
		}
		else if(argument instanceof String){
			actualType = 0;
		}
		else if(argument instanceof Integer){
			actualType = 1;
		}
		
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String argumentSPARQL = null;
		
		switch (actualType) {
		case 0:
			argumentSPARQL = "\"" + (String) argument + "\"^^<http://www.w3.org/2001/XMLSchema#string>";
			break;
		case 1:
			argumentSPARQL = ((Integer) argument).toString() + "^^<http://www.w3.org/2001/XMLSchema#int>";
			break;
		case 2:
			argumentSPARQL = "?"+argument.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			break;
		default:
			break;
		}
		
		if(argumentSPARQL != null){
			return new SPARQLFunction(argumentSPARQL);
		}
		else{
			return null;
		}
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String argumentString = null;
		switch (actualType) {
		case 0:
			argumentString = (String) argument;
			break;
		case 1:
			argumentString = ((Integer) argument).toString();
			break;
		case 2:
			argumentString = "?"+argument.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			break;
		default:
			break;
		}
		return argumentString;
	}

	@Override
	public JenaClauseEntry toJenaClauseEntry(JenaVariableMap jenaVariableMap) {
		// TODO Auto-generated method stub
		return null;
	}

}
