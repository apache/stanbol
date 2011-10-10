package org.apache.stanbol.explanation.api;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * A factory for explanation information objects.
 * 
 * @author alessandro
 *
 */
public interface ExplanationGenerator {

    /**
     * Creates a new explanation.
     * 
     * @param item
     * @param type
     * @param grounds
     * @return
     */
    Explanation createExplanation(Explainable<?> item, ExplanationTypes type, Set<? extends OWLAxiom> grounds);

}
