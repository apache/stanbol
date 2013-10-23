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

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
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
     *            {@link UriRef}
     * @return the {@link MGraph}.
     */
    MGraph getRefactoredDataSet(UriRef uriRef);

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetID}
     * identifies dataset to which apply the refactoring. {@code refactoredDataSetID} identifies the new
     * refactored dataset in the store. {@code recipeID} identifies the ID of the recipe in the
     * {@link RuleStore},
     * 
     * @param refactoredDataSetID
     *            {@link UriRef}
     * @param datasetID
     *            {@link UriRef}
     * @param recipeIRI
     *            {@link UriRef}
     */
    void graphRefactoring(UriRef refactoredOntologyID, UriRef datasetID, UriRef recipeID) throws RefactoringException,
                                                                                         NoSuchRecipeException;

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetURI}
     * is the URI of an RDF graph in KReS and the {@code recipe} is the recipe that needs to be applied to RDF
     * graph in order to obtain the refactoring.
     * 
     * @param datasetURI
     *            {@link UriRef}
     * @param recipe
     *            {@link UriRef}
     * @return the refactored {@link MGraph}
     * @throws RefactoringException
     * @throws NoSuchRecipeException
     */
    TripleCollection graphRefactoring(UriRef datasetID, UriRef recipeID) throws RefactoringException,
                                                                        NoSuchRecipeException;

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetURI}
     * is the URI of an RDF graph in KReS and the {@code recipe} is the recipe that needs to be applied to RDF
     * graph in order to obtain the refactoring.
     * 
     * @param datasetID
     *            {@link TripleCollection}
     * @param recipe
     *            {@link Recipe}
     * @return the refactored {@link TripleCollection}
     * @throws SemionRefactoringException
     * @throws NoSuchRecipeException
     */
    TripleCollection graphRefactoring(TripleCollection dataset, Recipe recipe) throws RefactoringException;

}
