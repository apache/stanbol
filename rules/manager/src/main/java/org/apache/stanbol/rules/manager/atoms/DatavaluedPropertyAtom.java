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
import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.JenaClauseEntryImpl;
import org.apache.stanbol.rules.manager.SPARQLNot;
import org.apache.stanbol.rules.manager.SPARQLTriple;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLIArgument;
import org.semanticweb.owlapi.model.SWRLLiteralArgument;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;

import org.apache.stanbol.rules.base.SWRL;

public class DatavaluedPropertyAtom extends CoreAtom {

	private URIResource datatypeProperty;
	private URIResource argument1;
	private Object argument2;
	
	public DatavaluedPropertyAtom(URIResource datatypeProperty, URIResource argument1, Object argument2) {
		this.datatypeProperty = datatypeProperty;
		this.argument1 = argument1;
		this.argument2 = argument2;
	}
	
	@Override
	public SPARQLObject toSPARQL() {
		String arg1 = argument1.toString();
		String arg2 = argument2.toString();
		String dtP = datatypeProperty.toString();
		
		
		boolean negativeArg1 = false;
		boolean negativeArg2 = false;
		boolean negativeDtP = false;
		
		if(arg1.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?"+arg1.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			VariableAtom variable = (VariableAtom) argument1;
			negativeArg1 = variable.isNegative();
		}
		
		if(dtP.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			dtP = "?"+dtP.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			VariableAtom variable = (VariableAtom) datatypeProperty;
			negativeDtP = variable.isNegative();
		}
		
		if(arg2.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+arg2.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			VariableAtom variable = (VariableAtom) argument2;
			negativeArg2 = variable.isNegative();
			
		}
		else{
			
			if(argument2 instanceof String){
				arg2 = argument2.toString();
			}
			else if(argument2 instanceof Integer){
				arg2 = ((Integer) argument2).toString();
			}
			else if(argument2 instanceof TypedLiteralAtom){
				
				TypedLiteralAtom kReSTypeLiteral = (TypedLiteralAtom) argument2;
				
				Object value = kReSTypeLiteral.getValue();
				String xsdType = kReSTypeLiteral.getXsdType().toString();
				
//				System.out.println("TYPED LITERAL : ");
//				System.out.println("        value : "+value);
//				System.out.println("        xsd type : "+xsdType);
				
				if(value instanceof String){
					arg2 = value + "^^" + xsdType;
				}
				else if(value instanceof Integer){
					arg2 = ((Integer) value).toString()+"^^" + xsdType;
				}
				
//				System.out.println("ARG 2 : "+arg2);
			}
			else if(argument2 instanceof StringFunctionAtom){
				arg2 = ((StringFunctionAtom) argument2).toSPARQL().getObject();
			}
			//return arg1+" "+dtP+" "+literal.getLiteral();
		}
		
		if(negativeArg1 || negativeArg2 || negativeDtP){
			String optional = arg1+" "+dtP+" "+arg2;
			
			ArrayList<String> filters = new ArrayList<String>();
			if(negativeArg1){
				filters.add("!bound(" + arg1 + ")");
			}
			if(negativeArg2){
				filters.add("!bound(" + arg2 + ")");
			}
			if(negativeDtP){
				filters.add("!bound(" + dtP + ")");
			}
			
			String[] filterArray = new String[filters.size()];
			filterArray = filters.toArray(filterArray);
			
			return new SPARQLNot(optional, filterArray);
		}
		else{
			return new SPARQLTriple(arg1+" "+dtP+" "+arg2);
		}
		
		
	}

	@Override
	public Resource toSWRL(Model model) {
		Resource datavaluedPropertyAtom = model.createResource(SWRL.DatavaluedPropertyAtom);
		
		Resource datatypePropertyResource = datatypeProperty.createJenaResource(model);
		Resource argument1Resource = argument1.createJenaResource(model);
		Literal argument2Literal = model.createTypedLiteral(argument2);
		
		datavaluedPropertyAtom.addProperty(SWRL.propertyPredicate, datatypePropertyResource);
		datavaluedPropertyAtom.addProperty(SWRL.argument1, argument1Resource);
		datavaluedPropertyAtom.addLiteral(SWRL.argument2, argument2Literal);
		
		return datavaluedPropertyAtom;
	}
	
	@Override
	public SWRLAtom toSWRL(OWLDataFactory factory) {
		OWLDataProperty owlDataProperty = factory.getOWLDataProperty(IRI.create(datatypeProperty.getURI().toString()));
		
		SWRLIArgument swrliArgument1;
		SWRLDArgument swrliArgument2;
		
		OWLIndividual owlIndividual = factory.getOWLNamedIndividual(IRI.create(argument1.getURI().toString()));
		swrliArgument1 = factory.getSWRLIndividualArgument(owlIndividual);
		
		
		swrliArgument2 =  getSWRLTypedLiteral(factory, argument2);
		
		
		
		return factory.getSWRLDataPropertyAtom(owlDataProperty, swrliArgument1, swrliArgument2);
	}
	
	public URIResource getDatatypeProperty() {
		return datatypeProperty;
	}
	
	public URIResource getArgument1() {
		return argument1;
	} 
	
	public Object getArgument2() {
		return argument2;
	}

	
	@Override
	public String toString() {
		return "Individual "+argument1.toString() + " has datatype property "+argument1.toString()+" with value "+argument2.toString();
	}
	
	private SWRLLiteralArgument getSWRLTypedLiteral(OWLDataFactory factory, Object argument){
		
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
		
		return factory.getSWRLLiteralArgument(owlLiteral); 
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
	public String toKReSSyntax(){
		String arg1 = null;
		String arg2 = null;
		String arg3 = null;
		
		if(argument1.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg1 = "?"+argument1.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			VariableAtom variable = (VariableAtom) argument1;
			if(variable.isNegative()){
				arg1 = "notex(" + arg1 + ")";
			}
		}
		else{
			arg1 = argument1.toString();
		}
		
		
		if(datatypeProperty.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg3 = "?"+datatypeProperty.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			VariableAtom variable = (VariableAtom) datatypeProperty;
			if(variable.isNegative()){
				arg3 = "notex(" + arg3 + ")";
			}
		}
		else{
			arg3 = datatypeProperty.toString();
		}
		
		if(argument2.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			arg2 = "?"+argument2.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
			VariableAtom variable = (VariableAtom) argument2;
			if(variable.isNegative()){
				arg2 = "notex(" + arg2 + ")";
			}
			
			return "values(" + arg3 + ", " + arg1 + ", " + arg2 +")";
		}
		else{
			OWLLiteral literal = getOWLTypedLiteral(argument2);
			
			return "values(" + arg3 + ", " + arg1 + ", " + literal.getLiteral() +")";
		}
		
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
	public JenaClauseEntry toJenaClauseEntry(JenaVariableMap jenaVariableMap) {
		String subject = argument1.toString();
		Node subjectNode = null;
		if(subject.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			subject = "?" + subject.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			//subjectNode = Node_RuleVariable.createVariable(subject);
			subjectNode = new Node_RuleVariable(subject, jenaVariableMap.getVariableIndex(subject));
		}
		else{
			if(subject.startsWith("<") && subject.endsWith(">")){
				subject = subject.substring(1, subject.length()-1);
			}
			subjectNode = Node_RuleVariable.createURI(subject);	
		}
		
		
		Node objectNode = null;
		String object = argument2.toString();
		if(object.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			object = "?" + object.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			
			//objectNode = Node_RuleVariable.createVariable(object);
			objectNode = new Node_RuleVariable(object, jenaVariableMap.getVariableIndex(object));
		}
		else{
			if(object.startsWith("<") && object.endsWith(">")){
				object = object.substring(1, object.length()-1);
			}
			objectNode = Node_RuleVariable.createLiteral(object);
		}
		
		String predicate = datatypeProperty.toString();
		Node predicateNode = null;
		if(predicate.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			predicate = predicate.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			predicateNode = Node_RuleVariable.createVariable(predicate);
		}
		else{
			if(predicate.startsWith("<") && predicate.endsWith(">")){
				predicate = predicate.substring(1, predicate.length()-1);
			}
			predicateNode = Node_RuleVariable.createURI(predicate);
		}
		
		ClauseEntry clauseEntry = new TriplePattern(subjectNode, predicateNode, objectNode);
		return new JenaClauseEntryImpl(clauseEntry, jenaVariableMap);
	}
	
}
