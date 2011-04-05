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
