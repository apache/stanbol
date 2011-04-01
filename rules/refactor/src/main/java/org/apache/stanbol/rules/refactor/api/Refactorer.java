package org.apache.stanbol.rules.refactor.api;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * A SemionReengineer provides methods for performing ontology refactorings. Refactoring are performed using
 * recipes that contain sets of rules that describe the refactoring to do. Rules are expressed using the both
 * the SWRL model and the KReS rules language.
 * 
 * @author andrea.nuzzolese
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
    public MGraph getRefactoredDataSet(UriRef uriRef);

    /**
     * The refactoring is perfomed by the {@code Refactorer} by invoking this method. The {@code datasetURI}
     * is the {@link IRI} of an IKS ontology and the {@code recipe} is the recipe that needs to be applied to
     * ontology in order to perform the refactoring.
     * 
     * @param refactoredDataSetURI
     *            {@link IRI}
     * @param datasetURI
     *            {@link IRI}
     * @param recipeIRI
     *            {@link IRI}
     */
    public void ontologyRefactoring(IRI refactoredDataSetURI, IRI datasetURI, IRI recipeIRI) throws RefactoringException,
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
    public OWLOntology ontologyRefactoring(OWLOntology datasetURI, IRI recipeIRI) throws RefactoringException,
                                                                                 NoSuchRecipeException;

}
