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

import java.util.ArrayList;

import org.apache.stanbol.rules.base.api.JenaClauseEntry;
import org.apache.stanbol.rules.base.api.JenaVariableMap;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.manager.SPARQLFunction;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;


public class ConcatAtom extends StringFunctionAtom {

	private StringFunctionAtom argument1;
	private StringFunctionAtom argument2;
	
	public ConcatAtom(StringFunctionAtom argument1, StringFunctionAtom argument2) {
		this.argument1 = argument1;
		this.argument2 = argument2;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		
		String sparqlConcat = "<http://www.w3.org/2005/xpath-functions#concat>";
		String function = sparqlConcat + " (" + argument1.toSPARQL().getObject() + ", " + argument2.toSPARQL().getObject() + ")";
		 
		return new SPARQLFunction(function);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		
		return "concat(" + argument1.toKReSSyntax() + ", " + argument2.toKReSSyntax() + ")";
	}
	
	@Override
	public JenaClauseEntry toJenaClauseEntry(JenaVariableMap jenaVariableMap) {
		/*
		 * TODO
		String arg1 = argument1.toString();
		if(arg1.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?" + arg1.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		
		String arg2 = argument1.toString();
		if(arg2.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?" + arg2.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		
		java.util.List<Node> nodes = new ArrayList<Node>();
		
		nodes.add(Node.createURI(arg1));
		nodes.add(Node.createURI(arg2));
		nodes.add(Node.createURI("?t324"));
		
		return new Functor("strConcat", nodes);
		*/
		
		return null;
	}

}
