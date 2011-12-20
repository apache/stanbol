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

import org.apache.stanbol.rules.base.SWRL;
import org.apache.stanbol.rules.base.api.JenaClauseEntry;
import org.apache.stanbol.rules.base.api.JenaVariableMap;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.URIResource;
import org.apache.stanbol.rules.manager.JenaClauseEntryImpl;
import org.apache.stanbol.rules.manager.SPARQLNot;
import org.apache.stanbol.rules.manager.SPARQLTriple;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLIArgument;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;

public class IndividualPropertyAtom extends CoreAtom {

    private URIResource objectProperty;
    private URIResource argument1;
    private URIResource argument2;

    public IndividualPropertyAtom(URIResource objectProperty, URIResource argument1, URIResource argument2) {
        this.objectProperty = objectProperty;
        this.argument1 = argument1;
        this.argument2 = argument2;
    }

    @Override
    public SPARQLObject toSPARQL() {
        String arg1 = argument1.toString();
        String arg2 = argument2.toString();
        String objP = objectProperty.toString();

        boolean negativeArg1 = false;
        boolean negativeArg2 = false;
        boolean negativeObjP = false;

        if (arg1.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")) {
            arg1 = "?" + arg1.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
            VariableAtom variable = (VariableAtom) argument1;
            if (variable.isNegative()) {
                negativeArg1 = true;
            }
        }

        if (arg2.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")) {
            arg2 = "?" + arg2.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
            VariableAtom variable = (VariableAtom) argument2;
            if (variable.isNegative()) {
                negativeArg2 = true;
            }
        }

        if (objP.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")) {
            objP = "?" + objP.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
            VariableAtom variable = (VariableAtom) objectProperty;
            if (variable.isNegative()) {
                negativeObjP = true;
            }
        }

        if (negativeArg1 || negativeArg2 || negativeObjP) {
            String optional = arg1 + " " + objP + " " + arg2;

            ArrayList<String> filters = new ArrayList<String>();
            if (negativeArg1) {
                filters.add("!bound(" + arg1 + ")");
            }
            if (negativeArg2) {
                filters.add("!bound(" + arg2 + ")");
            }
            if (negativeObjP) {
                filters.add("!bound(" + objP + ")");
            }

            String[] filterArray = new String[filters.size()];
            filterArray = filters.toArray(filterArray);

            return new SPARQLNot(optional, filterArray);
        } else {
            return new SPARQLTriple(arg1 + " " + objP + " " + arg2);
        }

    }

    @Override
    public Resource toSWRL(Model model) {
        Resource individualPropertyAtom = model.createResource(SWRL.IndividualPropertyAtom);

        Resource objectPropertyPredicate = objectProperty.createJenaResource(model);
        Resource argument1Resource = argument1.createJenaResource(model);
        Resource argument2Resource = argument2.createJenaResource(model);

        individualPropertyAtom.addProperty(SWRL.propertyPredicate, objectPropertyPredicate);
        individualPropertyAtom.addProperty(SWRL.argument1, argument1Resource);
        individualPropertyAtom.addProperty(SWRL.argument2, argument2Resource);

        return individualPropertyAtom;
    }

    @Override
    public SWRLAtom toSWRL(OWLDataFactory factory) {
        OWLObjectProperty owlObjectProperty = factory.getOWLObjectProperty(IRI.create(objectProperty.getURI()
                .toString()));

        SWRLIArgument swrliArgument1;
        SWRLIArgument swrliArgument2;

        if (argument1 instanceof VariableAtom) {
            swrliArgument1 = factory.getSWRLVariable(IRI.create(argument1.getURI().toString()));
        } else {
            OWLIndividual owlIndividual = factory.getOWLNamedIndividual(IRI.create(argument1.getURI()
                    .toString()));
            swrliArgument1 = factory.getSWRLIndividualArgument(owlIndividual);
        }

        if (argument2 instanceof VariableAtom) {
            swrliArgument2 = factory.getSWRLVariable(IRI.create(argument2.getURI().toString()));
        } else {
            OWLIndividual owlIndividual = factory.getOWLNamedIndividual(IRI.create(argument2.getURI()
                    .toString()));
            swrliArgument2 = factory.getSWRLIndividualArgument(owlIndividual);
        }

        return factory.getSWRLObjectPropertyAtom(owlObjectProperty, swrliArgument1, swrliArgument2);
    }

    public URIResource getObjectProperty() {
        return objectProperty;
    }

    public URIResource getArgument1() {
        return argument1;
    }

    public URIResource getArgument2() {
        return argument2;
    }

    @Override
    public String toString() {
        return "Individual " + argument1.toString() + " has object property " + argument1.toString()
               + " that refers to individual " + argument2.toString();
    }

    @Override
    public String toKReSSyntax() {
        String arg1 = null;
        String arg2 = null;
        String arg3 = null;

        if (argument1.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")) {
            arg1 = "?"
                   + argument1.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
            VariableAtom variable = (VariableAtom) argument1;
            if (variable.isNegative()) {
                arg1 = "notex(" + arg1 + ")";
            }
        } else {
            arg1 = argument1.toString();
        }

        if (objectProperty.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")) {
            arg3 = "?"
                   + objectProperty.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#",
                       "");
            VariableAtom variable = (VariableAtom) objectProperty;
            if (variable.isNegative()) {
                arg3 = "notex(" + arg3 + ")";
            }
        } else {
            arg3 = objectProperty.toString();
        }

        if (argument2.toString().startsWith("http://kres.iks-project.eu/ontology/meta/variables#")) {
            arg2 = "?"
                   + argument2.toString().replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
            VariableAtom variable = (VariableAtom) argument2;
            if (variable.isNegative()) {
                arg2 = "notex(" + arg2 + ")";
            }
        } else {
            arg2 = argument2.toString();
        }

        return "has(" + arg3 + ", " + arg1 + ", " + arg2 + ")";

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
		
		String object = argument2.toString();
		Node objectNode = null;
		if(object.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			object = subject.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			if(object.startsWith("?")){
				object = object.substring(1);
			}
			
			object = "?" + object;
			objectNode = new Node_RuleVariable(object, jenaVariableMap.getVariableIndex(object));
		}
		else{
			if(object.startsWith("<") && object.endsWith(">")){
				object = object.substring(1, object.length()-1);
			}
			objectNode = Node_RuleVariable.createURI(object);
		}
		
		String predicate = objectProperty.toString();
		Node predicateNode = null;
		if(predicate.startsWith("http://kres.iks-project.eu/ontology/meta/variables#")){
			predicate = predicate.replace("http://kres.iks-project.eu/ontology/meta/variables#", "");
			//predicateNode = Node_RuleVariable.createVariable(predicate);
			predicateNode = new Node_RuleVariable(predicate, 2);
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
