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
package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Provides an interface to the ontologies provided by registered scopes in the
 * ontology manager.
 * 
 * @author alexdma
 * 
 */
public interface OntologyIndex extends ScopeOntologyListener,
		ScopeEventListener {

    Set<IRI> getIndexedOntologyIRIs();
    
	/**
	 * Returns an ontology having the specified IRI as its identifier, or null
	 * if no such ontology is indexed.<br>
	 * <br>
	 * Which ontology is returned in case more ontologies with this IRI are
	 * registered in different scopes is at the discretion of implementors.
	 * 
	 * @param ontologyIri
	 * @return
	 */
    OWLOntology getOntology(IRI ontologyIri);

	/**
	 * Returns the ontology loaded within an ontology scope having the specified
	 * IRI as its identifier, or null if no such ontology is loaded in that
	 * scope.
	 * 
	 * @param ontologyIri
	 * @return
	 */
    OWLOntology getOntology(IRI ontologyIri, String scopeId);

	/**
	 * Returns the set of ontology scopes where an ontology with the specified
	 * IRI is registered in either their core spaces or their custom spaces.
	 * Optionally, session spaces can be queried as well.
	 * 
	 * @param ontologyIri
	 * @param includingSessionSpaces
	 * @return
	 */
    Set<String> getReferencingScopes(IRI ontologyIri,
            boolean includingSessionSpaces);

	/**
	 * Determines if an ontology with the specified identifier is loaded within
	 * some registered ontology scope.
	 * 
	 * @param ontologyIri
	 * @return
	 */
    boolean isOntologyLoaded(IRI ontologyIri);

}
