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
package org.apache.stanbol.reasoners.base.api;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;


/**
 * The KReS Reasoner provides all the reasoning services to the KReS.
 * 
 * 
 * @author andrea.nuzzolese
 *
 */
public interface Reasoner {
	
	/**
	 * Gets the reasoner.
	 * 
	 * @param ontology {@link OWLOntology}
	 * @return the reasoner {@link OWLReasoner}.
	 */
    OWLReasoner getReasoner(OWLOntology ontology);
	
	/**
	 * Runs a consistency check on the ontology.
	 * 
	 * @param owlReasoner {@link OWLReasoner}
	 * @return true if the ontology is consistent, false otherwise.
	 */
    boolean consistencyCheck(OWLReasoner owlReasoner);
	
	
	/**
	 * Launch the reasoning on a set of rules applied to a gien ontology.
	 * @param ontology
	 * @param ruleOntology
	 * @return the inferred ontology
	 */
    OWLOntology runRules(OWLOntology ontology, OWLOntology ruleOntology);

}
