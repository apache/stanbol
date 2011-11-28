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
import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.SPARQLComparison;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLAtom;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;


public class IsBlankAtom extends ComparisonAtom {

	private URIResource uriResource;
	
	public IsBlankAtom(URIResource uriResource) {
		this.uriResource = uriResource;
	}
	
	@Override
	public Resource toSWRL(Model model) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SPARQLObject toSPARQL() {
		String argument = uriResource.toString();
		
		String sparql; 
		
		if(argument.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			sparql = "?"+argument.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			sparql = argument;
		}
		
		sparql = "isBlank(" + sparql + ")";
		
		return new SPARQLComparison(sparql);
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toKReSSyntax() {
		String argument = uriResource.toString();
		
		String kReS; 
		
		if(argument.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			kReS = "?"+argument.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			kReS = argument;
		}
		
		kReS = "isBlank(" + kReS + ")";
		return kReS;
	}

	@Override
	public boolean isSPARQLConstruct() {
		return false;
	}
	
	
	@Override
	public JenaClauseEntry toJenaClauseEntry(JenaVariableMap jenaVariableMap) {
		
		/*
		 * TODO
		String argument = uriResource.toString();
		if(argument.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			argument = "?" + argument.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		
		java.util.List<Node> nodes = new ArrayList<Node>();
		
		nodes.add(Node.createURI(argument));
		
		return new Functor("isBNode", nodes);
		*/
		return null;
	}
	
}
