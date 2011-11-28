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
package org.apache.stanbol.rules.base.api;

import org.apache.stanbol.rules.base.api.util.AtomList;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.SWRLRule;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;



/**
 * A Rule is a Java object that represent a rule in KReS. It contains methods to transform a rule both in SWRL and in Rule
 * syntax. 
 * 
 * @author andrea.nuzzolese
 *
 */
public interface Rule {
	
	/**
	 * Gets the name of the rule.
	 * 
	 * @return the {@link String} representing the name of the rule.
	 */
    String getRuleName();
	
	/**
	 * Sets the rule's name
	 * 
	 * @param ruleName {@link String}
	 */
    void setRuleName(String ruleName);
	
	/**
	 * Returns the representation of the rule in Rule syntax.
	 * 
	 * @return the {@link String} of the rule in Rule syntax.
	 */
    String getRule();
	
	/**
	 * Sets the rule expressed in Rule syntax
	 * 
	 * @param rule {@link String}
	 */
    void setRule(String rule);
	
	/**
	 * Maps a {@code Rule} to a Jena {@link Resource} object in a given Jena {@link Model}.
	 * @param model {@link Model}
	 * @return the {@link Resource} containing the rule.
	 */
    Resource toSWRL(Model model);
	
	/**
	 * Maps a {@code Rule} to an OWL-API {@link SWRLRule}.
	 * @param factory {@link OWLDataFactory}
	 * @return the {@link SWRLRule} containing the rule.
	 */
    SWRLRule toSWRL(OWLDataFactory factory);
	
	/**
	 * Transforms the rule to a SPARQL CONSTRUCT.
	 * 
	 * @return the string containing the SPARQL CONSTRUCT.
	 */
    String toSPARQL();
	
	/**
	 * Rules are composed by an antecedent (body) and a consequent (head). This method returnn the consequent
	 * expressed as a list of its atoms ({@link AtomList}).
	 * @return the {@link AtomList} of the consequent's atoms. 
	 */
    AtomList getHead();
	
	/**
	 * Rules are composed by an antecedent (body) and a consequent (head). This method returnn the antecedent
	 * expressed as a list of its atoms ({@link AtomList}).
	 * @return the {@link AtomList} of the antecedent's atoms. 
	 */
    AtomList getBody();
	
	
	/**
	 * Retunr the KReS syntax representation of the rule.
	 * @return the string of the rule in Rule syntax.
	 */
    String toKReSSyntax();
	
	/**
	 * If the variable forwardChain is set true than the forward chain mechanism is ebled for that rule.
	 * @return {@link boolean}.
	 */
    boolean isForwardChain();
	
	boolean isSPARQLConstruct();
	
	boolean isSPARQLDelete();
	
	boolean isSPARQLDeleteData();
	
	boolean isReflexive();
	
	RuleExpressiveness getExpressiveness();
	
	
	com.hp.hpl.jena.reasoner.rulesys.Rule toJenaRule();
	
}
