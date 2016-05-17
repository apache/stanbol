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
package org.apache.stanbol.rules.refactor.api;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RuleStore;

/**
 * The refactorer provides methods for performing ontology refactorings. Refactoring are performed using
 * recipes that contain sets of rules that describe the refactoring to do.
 * 
 * @author anuzzolese
 * 
 */
public interface Refactorer {

    /**
     * Fetch the mgraph with the selected uri from the storage.
     * 
     * @param uriRef
     *            {@link IRI}
     * @return the {@link Graph}.
     */
    Graph getRefactoredDataSet(IRI uriRef);

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetID}
     * identifies dataset to which apply the refactoring. {@code refactoredDataSetID} identifies the new
     * refactored dataset in the store. {@code recipeID} identifies the ID of the recipe in the
     * {@link RuleStore},
     * 
     * @param refactoredDataSetID
     *            {@link IRI}
     * @param datasetID
     *            {@link IRI}
     * @param recipeIRI
     *            {@link IRI}
     */
    void graphRefactoring(IRI refactoredOntologyID, IRI datasetID, IRI recipeID) throws RefactoringException,
                                                                                         NoSuchRecipeException;

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetURI}
     * is the URI of an RDF graph in KReS and the {@code recipe} is the recipe that needs to be applied to RDF
     * graph in order to obtain the refactoring.
     * 
     * @param datasetURI
     *            {@link IRI}
     * @param recipe
     *            {@link IRI}
     * @return the refactored {@link Graph}
     * @throws RefactoringException
     * @throws NoSuchRecipeException
     */
    Graph graphRefactoring(IRI datasetID, IRI recipeID) throws RefactoringException,
                                                                        NoSuchRecipeException;

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetURI}
     * is the URI of an RDF graph in KReS and the {@code recipe} is the recipe that needs to be applied to RDF
     * graph in order to obtain the refactoring.
     * 
     * @param datasetID
     *            {@link Graph}
     * @param recipe
     *            {@link Recipe}
     * @return the refactored {@link Graph}
     * @throws SemionRefactoringException
     * @throws NoSuchRecipeException
     */
    Graph graphRefactoring(Graph dataset, Recipe recipe) throws RefactoringException;

}
