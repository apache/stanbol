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
import java.util.List;

import org.apache.stanbol.rules.base.api.JenaClauseEntry;
import org.apache.stanbol.rules.base.api.JenaVariableMap;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.JenaClauseEntryImpl;
import org.apache.stanbol.rules.manager.SPARQLComparison;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLBuiltInAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.vocab.SWRLBuiltInsVocabulary;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.vocabulary.XSD;


public class GreaterThanAtom extends ComparisonAtom {

	
	private Object argument1;
	private Object argument2;
	
	public GreaterThanAtom(Object argument1, Object argument2) {
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
		String arg1 = argument1.toString();
		String arg2 = argument2.toString();
		
		
		if(arg1.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "str(?"+arg1.replace("http://kres.iks-project.eu/ontology/meta/variables#", "") + ")";
			
		}
		else{
			arg1 = "str("+arg1+")";
		}
		
		if(arg2.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "str(?"+arg2.replace("http://kres.iks-project.eu/ontology/meta/variables#", "") + ")";
			
		}
		else if(!arg2.startsWith("<") && !arg2.endsWith(">")){
			OWLLiteral literal = getOWLTypedLiteral(argument2);
			arg2 = "str(" + literal.getLiteral() + ")";
		}
		else{
			arg2 = "str("+arg2+")";
		}
		
		
		return new SPARQLComparison(arg1+" > "+arg2);
		
	}

	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		
		List<SWRLDArgument> swrldArguments = new ArrayList<SWRLDArgument>();
		
		SWRLDArgument swrldArgument1 = null;
		
		if(argument1.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			
			swrldArgument1 = factory.getSWRLVariable(IRI.create(argument1.toString()));
		}
		else{
			
			OWLLiteral literal = null;
			if(argument1 instanceof TypedLiteralAtom){
				TypedLiteralAtom typedLiteralAtom = (TypedLiteralAtom) argument1;
					
				URIResource xsdType = typedLiteralAtom.getXsdType();
				
				if(xsdType.getURI().equals(XSD.xboolean)){
					literal = factory.getOWLLiteral(Boolean.valueOf(argument1.toString()).booleanValue());
				}
				else if(xsdType.getURI().equals(XSD.xdouble)){
					literal = factory.getOWLLiteral(Double.valueOf(argument1.toString()).doubleValue());
				}
				else if(xsdType.getURI().equals(XSD.xfloat)){
					literal = factory.getOWLLiteral(Float.valueOf(argument1.toString()).floatValue());
				}
				else if(xsdType.getURI().equals(XSD.xint)){
					literal = factory.getOWLLiteral(Integer.valueOf(argument1.toString()).intValue());
				}
				
				else{
					literal = factory.getOWLLiteral(argument1.toString());	
				}
				
			}
			else{
				literal = factory.getOWLLiteral(argument1.toString());
			}
			
			swrldArgument1 = factory.getSWRLLiteralArgument(literal);
			
		}
		
		SWRLDArgument swrldArgument2 = null;
		
		if(argument2.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			
			swrldArgument2 = factory.getSWRLVariable(IRI.create(argument2.toString()));
		}
		else{
			
			OWLLiteral literal = null;
			if(argument2 instanceof TypedLiteralAtom){
				TypedLiteralAtom typedLiteralAtom = (TypedLiteralAtom) argument2;
					
				URIResource xsdType = typedLiteralAtom.getXsdType();
				
				if(xsdType.getURI().equals(XSD.xboolean)){
					literal = factory.getOWLLiteral(Boolean.valueOf(argument2.toString()).booleanValue());
				}
				else if(xsdType.getURI().equals(XSD.xdouble)){
					literal = factory.getOWLLiteral(Double.valueOf(argument2.toString()).doubleValue());
				}
				else if(xsdType.getURI().equals(XSD.xfloat)){
					literal = factory.getOWLLiteral(Float.valueOf(argument2.toString()).floatValue());
				}
				else if(xsdType.getURI().equals(XSD.xint)){
					literal = factory.getOWLLiteral(Integer.valueOf(argument2.toString()).intValue());
				}
				
				else{
					literal = factory.getOWLLiteral(argument2.toString());	
				}
				
			}
			else{
				literal = factory.getOWLLiteral(argument2.toString());
			}
			
			swrldArgument2 = factory.getSWRLLiteralArgument(literal);
			
		}
		
		swrldArguments.add(swrldArgument1);
		swrldArguments.add(swrldArgument2);
		
		SWRLBuiltInAtom swrlBuiltInAtom = factory.getSWRLBuiltInAtom(SWRLBuiltInsVocabulary.GREATER_THAN.getIRI(), swrldArguments);
		return swrlBuiltInAtom;
	}

	@Override
	public String toKReSSyntax() {
		String arg1 = null;
		String arg2 = null;
		
		if(argument1.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?"+argument1.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
		}
		else{
			arg1 = argument1.toString();
		}
		
		
		if(argument2.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+argument2.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
			
			return "gt(" + arg1 + ", " + arg2 +")";
		}
		else{
			
			return "gt(" + arg1 + ", " + argument2.toString() +")";
		}
	}

	
	private OWLLiteral getOWLTypedLiteral(Object argument){
		
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		
		OWLLiteral owlLiteral;
		if(argument instanceof String){
			owlLiteral = factory.getOWLTypedLiteral((String) argument); 
		}
		else if(argument instanceof Integer){
			owlLiteral = factory.getOWLTypedLiteral(((Integer) argument).intValue());
		}
		else if(argument instanceof Double){
			owlLiteral = factory.getOWLTypedLiteral(((Double) argument).doubleValue());
		}
		else if(argument instanceof Float){
			owlLiteral = factory.getOWLTypedLiteral(((Float) argument).floatValue());
		}
		else if(argument instanceof Boolean){
			owlLiteral = factory.getOWLTypedLiteral(((Boolean) argument).booleanValue());
		}
		else{
			owlLiteral = factory.getOWLStringLiteral(argument.toString());
		}
		
		
		
		return owlLiteral; 
	}
	
	
	@Override
	public JenaClauseEntry toJenaClauseEntry(JenaVariableMap jenaVariableMap) {
		
		Node arg1Node = null;
		Node arg2Node = null;
		
		String arg1 = argument1.toString();
		if(arg1.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?" + arg1.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			arg1Node = new Node_RuleVariable(arg1, jenaVariableMap.getVariableIndex(arg1));
		}
		else{
			arg1Node = getTypedLiteral(argument1);
		}
		
		String arg2 = argument2.toString();
		if(arg2.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?" + arg2.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			arg2Node = new Node_RuleVariable(arg2, jenaVariableMap.getVariableIndex(arg2));
		}
		else{
			arg2Node = getTypedLiteral(argument2);
		}
		
		java.util.List<Node> nodes = new ArrayList<Node>();
		
		nodes.add(arg1Node);
		nodes.add(arg2Node);
		
		return new JenaClauseEntryImpl(new Functor("greaterThan", nodes, new BuiltinRegistry()), jenaVariableMap);
		
	}
}

