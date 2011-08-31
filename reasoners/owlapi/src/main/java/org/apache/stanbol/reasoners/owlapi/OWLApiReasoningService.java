package org.apache.stanbol.reasoners.owlapi;

import java.util.List;
import java.util.Set;

import org.apache.stanbol.reasoners.servicesapi.InconsistentInputException;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;

/**
 * Interface for any OWLApi based reasoning service
 */
public interface OWLApiReasoningService extends ReasoningService<OWLOntology,SWRLRule,OWLAxiom> {

    public abstract Set<OWLAxiom> run(OWLOntology ontology,
                                      List<SWRLRule> rules,
                                      List<InferredAxiomGenerator<? extends OWLAxiom>> generators) throws ReasoningServiceException,
                                                                                                  InconsistentInputException;

    public abstract Set<OWLAxiom> run(OWLOntology input,
                                      List<InferredAxiomGenerator<? extends OWLAxiom>> generators) throws ReasoningServiceException,
                                                                                                  InconsistentInputException;
    
}
