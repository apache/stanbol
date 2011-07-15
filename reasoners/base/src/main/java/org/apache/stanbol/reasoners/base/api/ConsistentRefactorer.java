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

import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * A special {@link Refactorer} which includes methods for ensuring/checking consistency in refactored
 * ontologies.
 * 
 * @author alessandro
 * 
 */
public interface ConsistentRefactorer extends Refactorer {

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetURI}
     * is the {@link IRI} of an IKS ontology and the {@code recipe} is the recipe that needs to be applied to
     * ontology in order to perform the refactoring. After the refactoring a consistency check is invoked on
     * the data set.
     * 
     * @param refactoredDataSetURI
     *            {@link IRI}
     * @param datasetURI
     *            {@link IRI}
     * @param recipeIRI
     *            {@link IRI}
     * @throws RefactoringException
     * @throws NoSuchRecipeException
     * @throws InconcistencyException
     */
    void consistentOntologyRefactoring(IRI refactoredOntologyIRI, IRI datasetURI, IRI recipeIRI) throws RefactoringException,
                                                                                                       NoSuchRecipeException,
                                                                                                       InconcistencyException;

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetURI}
     * is the URI of an RDF graph in KReS and the {@code recipe} is the recipe that needs to be applied to RDF
     * graph in order to obtain the refactoring. After the refactoring a consistency check is invoked on the
     * data set.
     * 
     * @param datasetURI
     *            {@link UriRef}
     * @param recipe
     *            {@link UriRef}
     * @return the refactored {@link MGraph}
     * @throws InconcistencyException
     * @throws NoSuchRecipeException
     * @throws RefactoringException
     */
    OWLOntology consistentOntologyRefactoring(OWLOntology inputOntology, IRI recipeIRI) throws RefactoringException,
                                                                                              NoSuchRecipeException,
                                                                                              InconcistencyException;

}
