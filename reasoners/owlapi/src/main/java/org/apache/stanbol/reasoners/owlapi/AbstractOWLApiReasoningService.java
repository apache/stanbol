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
package org.apache.stanbol.reasoners.owlapi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.reasoners.servicesapi.InconsistentInputException;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException;
import org.apache.stanbol.reasoners.servicesapi.UnsupportedTaskException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportEvent;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener.LoadingFinishedEvent;
import org.semanticweb.owlapi.model.OWLOntologyLoaderListener.LoadingStartedEvent;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredDataPropertyCharacteristicAxiomGenerator;
import org.semanticweb.owlapi.util.InferredDisjointClassesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentDataPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredEquivalentObjectPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredInverseObjectPropertiesAxiomGenerator;
import org.semanticweb.owlapi.util.InferredObjectPropertyCharacteristicAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.InferredPropertyAssertionGenerator;
import org.semanticweb.owlapi.util.InferredSubClassAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubDataPropertyAxiomGenerator;
import org.semanticweb.owlapi.util.InferredSubObjectPropertyAxiomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class implements basic methods for a reasoning service based on OWLApi
 */
public abstract class AbstractOWLApiReasoningService implements OWLApiReasoningService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * An OWLOntologyManager to be used to place a target ontology. We don't want to use the manager bound to
     * the input ontologies for that, we don't want to interfere with it.
     * 
     * Other {@see OWLApiReasoningService}s may want to change this.
     * 
     * @return
     */
    protected OWLOntologyManager createOWLOntologyManager() {
        log.info("createOWLOntologyManager()");
        // We want a single instance here
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        log.info("manager: {}", manager);
        // FIXME Which is the other way of doing this?
        // Maybe -> OWLOntologyManagerProperties();
        manager.setSilentMissingImportsHandling(true);
        // Listening for missing imports
        manager.addMissingImportListener(new MissingImportListener() {
            @Override
            public void importMissing(MissingImportEvent arg0) {
                log.warn("Missing import {} ", arg0.getImportedOntologyURI());
            }
        });
        manager.addOntologyLoaderListener(new OWLOntologyLoaderListener() {

            @Override
            public void finishedLoadingOntology(LoadingFinishedEvent arg0) {
                log.info("Finished loading {} (imported: {})", arg0.getOntologyID(), arg0.isImported());
            }

            @Override
            public void startedLoadingOntology(LoadingStartedEvent arg0) {
                log.info("Started loading {} (imported: {}) ...", arg0.getOntologyID(), arg0.isImported());
                log.info(" ... from {}", arg0.getDocumentIRI().toString());
            }
        });
        return manager;
    }

    /**
     * Method to be implemented by subclasses.
     * 
     * @param ontology
     * @return
     */
    protected abstract OWLReasoner getReasoner(OWLOntology ontology);

    /**
     * Generic method for running the reasoner
     * 
     * @param input
     * @param generators
     * @return
     */
    @Override
    public Set<OWLAxiom> run(OWLOntology input, List<InferredAxiomGenerator<? extends OWLAxiom>> generators) throws ReasoningServiceException,
                                                                                                            InconsistentInputException {
        log.debug("run(OWLOntology input, List<InferredAxiomGenerator<? extends OWLAxiom>> generators)");
        try {
            // Get the manager
            OWLOntologyManager manager = createOWLOntologyManager();

            // Get the reasoner
            OWLReasoner reasoner = getReasoner(input);
            log.info("Running {} reasoner on {} ", reasoner.getClass(), input.getOntologyID());

            // To generate inferred axioms
            InferredOntologyGenerator inferred = new InferredOntologyGenerator(reasoner, generators);

            // We fill an anonymous ontology with the result, the return the
            // axiom set
            Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
            try {
                OWLOntology output = manager.createOntology();
                log.debug("Created output ontology: {}", output);
                try {
                    inferred.fillOntology(manager, output);
                } catch (InconsistentOntologyException i) {
                    throw i;
                } catch (Throwable t) {
                    log.error("Some problem occurred:\n {}", t.getStackTrace());
                    throw new ReasoningServiceException();
                }
                log.debug("Filled ontology: {}", output);
                log.debug("Temporary ID is {}", output.getOntologyID());
                axioms = manager.getOntology(output.getOntologyID()).getAxioms();
                // IMPORTANT We remove the ontology from the manager
                manager.removeOntology(output);
            } catch (OWLOntologyCreationException e) {
                log.error("An exception have been thrown when instantiating the ontology");
                throw new ReasoningServiceException();
            }

            return axioms;
        } catch (InconsistentOntologyException inconsistent) {
            /**
             * TODO Add report. Why it is inconsistent?
             */
            throw new InconsistentInputException();
        } catch (Exception exception) {
            log.error("An exception have been thrown while executing method run()", exception);
            throw new ReasoningServiceException();
        }
    }

    /**
     * Merges the SWRL rules in the input ontology, then calls run(OWLOntology,List<InferredAxiomGenerator<?
     * extends OWLAxiom>>)
     * 
     * @param ontology
     * @param rules
     * @param generators
     * @return
     */
    @Override
    public Set<OWLAxiom> run(OWLOntology ontology,
                             List<SWRLRule> rules,
                             List<InferredAxiomGenerator<? extends OWLAxiom>> generators) throws ReasoningServiceException,
                                                                                         InconsistentInputException {
        log.debug("Called method run(OWLOntology,List<SWRLRule>,List)");
        OWLOntologyManager manager = ontology.getOWLOntologyManager();

        log.debug("Adding SWRL rules to the input ontology.");
        Set<SWRLRule> ruleSet = new HashSet<SWRLRule>();
        ruleSet.addAll(rules);
        
        manager.addAxioms(ontology, ruleSet);
        if(log.isDebugEnabled())
            for(OWLAxiom a:ontology.getAxioms()){
                log.debug("Axiom {}",a);
            }
        log.debug("Calling the run method.");
        return run(ontology, generators);

    }

    /**
     * This method provides the default implementation for executing one of the default tasks with no
     * additional arguments.
     * 
     * TODO: Add support for the filtered parameter on task 'classify';
     */
    @Override
    public Set<OWLAxiom> runTask(String taskID, OWLOntology data) throws UnsupportedTaskException,
                                                                 ReasoningServiceException,
                                                                 InconsistentInputException {
        if (taskID.equals(ReasoningService.Tasks.CLASSIFY)) {
            return classify(data);
        } else if (taskID.equals(ReasoningService.Tasks.ENRICH)) {
            return enrich(data);
        } else throw new UnsupportedTaskException();
    }

    /**
     * This method provides the default implementation for executing one of the default tasks.
     * 
     * TODO: Add support for the filtered parameter on task 'classify';
     */
    @Override
    public Set<OWLAxiom> runTask(String taskID,
                                 OWLOntology data,
                                 List<SWRLRule> rules,
                                 boolean filtered,
                                 Map<String,List<String>> parameters) throws UnsupportedTaskException,
                                                                     ReasoningServiceException,
                                                                     InconsistentInputException {
        log.info("Called task {} with data {}", taskID, data);
        if (taskID.equals(ReasoningService.Tasks.CLASSIFY)) {
            if (rules != null) {

                return classify(data, rules);
            } else {
                log.warn("No rules attached");
                return classify(data);
            }
        } else if (taskID.equals(ReasoningService.Tasks.ENRICH)) {
            if (rules != null) {
                return enrich(data, rules, filtered);
            } else {
                return enrich(data, filtered);
            }
        } else throw new UnsupportedTaskException();
    }

    /**
     * {@see InferredAxiomGenerator}s to use for the classify() reasoning method.
     * 
     * Subclasses may want to change this.
     * 
     * @return
     */
    protected List<InferredAxiomGenerator<? extends OWLAxiom>> getClassifyAxiomGenerators() {
        List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
        gens.add(new InferredClassAssertionAxiomGenerator());
        gens.add(new InferredSubClassAxiomGenerator());
        gens.add(new InferredEquivalentClassAxiomGenerator());
        gens.add(new InferredDisjointClassesAxiomGenerator());
        return gens;
    }

    /**
     * {@see InferredAxiomGenerator}s to use for the enrich() reasoning method.
     * 
     * Subclasses may want to change this.
     * 
     * @return
     */
    protected List<InferredAxiomGenerator<? extends OWLAxiom>> getEnrichAxiomGenerators() {
        List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
        // Classes
        gens.add(new InferredClassAssertionAxiomGenerator());
        gens.add(new InferredSubClassAxiomGenerator());
        gens.add(new InferredEquivalentClassAxiomGenerator());
        gens.add(new InferredDisjointClassesAxiomGenerator());
        // data properties
        gens.add(new InferredDataPropertyCharacteristicAxiomGenerator());
        gens.add(new InferredEquivalentDataPropertiesAxiomGenerator());
        gens.add(new InferredSubDataPropertyAxiomGenerator());
        // object properties
        gens.add(new InferredEquivalentObjectPropertyAxiomGenerator());
        gens.add(new InferredInverseObjectPropertiesAxiomGenerator());
        gens.add(new InferredObjectPropertyCharacteristicAxiomGenerator());
        gens.add(new InferredSubObjectPropertyAxiomGenerator());
        // individuals
        gens.add(new InferredClassAssertionAxiomGenerator());
        gens.add(new InferredPropertyAssertionGenerator());
        return gens;
    }

    /**
     * Classify, returns only axioms about classes and instances
     * 
     * This is the default implementation of task {@see ReasoningService.Tasks.CLASSIFY}. Subclasses may want
     * to change it.
     * 
     * @param ontology
     * @return
     */
    protected Set<OWLAxiom> classify(OWLOntology ontology) throws ReasoningServiceException,
                                                          InconsistentInputException {
        log.info("classify(OWLOntology ontology)");
        return run(ontology, getClassifyAxiomGenerators());
    }

    /**
     * Classify, merge SWRL rules in the input ontology, before
     * 
     * This is the default implementation of task {@see ReasoningService.Tasks.CLASSIFY} when rules are given.
     * Subclasses may want to change it.
     * 
     * @param ontology
     * @param rules
     * @return
     * @throws InconsistentInputException
     * @throws ReasoningServiceException
     */
    protected Set<OWLAxiom> classify(OWLOntology ontology, List<SWRLRule> rules) throws ReasoningServiceException,
                                                                                InconsistentInputException {
        log.debug("Calling classify(OWLOntology ontology, List<SWRLRule> rules) ");
        return run(ontology, rules, getClassifyAxiomGenerators());
    }

    /**
     * Enrich, return all inferences. This is the same as enrich(ontology,false);
     * 
     * This is the default implementation of task {@see ReasoningService.Tasks.ENRICH}. Subclasses may want to
     * change it.
     * 
     * @param ontology
     * @return
     * @throws ReasoningServiceException
     * @throws InconsistentInputException
     */
    protected Set<OWLAxiom> enrich(OWLOntology ontology) throws ReasoningServiceException,
                                                        InconsistentInputException {
        return run(ontology, getEnrichAxiomGenerators());
    }

    /**
     * Enrich, return all inferences. If filtered = false, then merge the inferences with the input
     * 
     * This is the default implementation of task {@see ReasoningService.Tasks.ENRICH}. Subclasses may want to
     * change it.
     * 
     * @param ontology
     * @param filtered
     * @return
     * @throws ReasoningServiceException
     * @throws InconsistentInputException
     */
    protected Set<OWLAxiom> enrich(OWLOntology ontology, boolean filtered) throws ReasoningServiceException,
                                                                          InconsistentInputException {
        log.debug("Calling enrich(OWLOntology ontology, filtered) ");
        // If filtered = false, then we merge the output with the input
        if (filtered) {
            return run(ontology, getEnrichAxiomGenerators());
        } else {
            Set<OWLAxiom> output = ontology.getAxioms();
            output.addAll(run(ontology, getEnrichAxiomGenerators()));
            return output;
        }
    }

    /**
     * Enrich, merge SWRL rules and return all inferences. This is the same as enrich(ontology,rules,false)
     * 
     * This is the default implementation of task {@see ReasoningService.Tasks.ENRICH}. Subclasses may want to
     * change it.
     * 
     * @param ontology
     * @param rules
     * @return
     * @throws ReasoningServiceException
     * @throws InconsistentInputException
     */
    protected Set<OWLAxiom> enrich(OWLOntology ontology, List<SWRLRule> rules) throws ReasoningServiceException,
                                                                              InconsistentInputException {
        log.debug("Calling enrich(OWLOntology ontology, List<SWRLRule> rules) ");
        return run(ontology, rules, getEnrichAxiomGenerators());
    }

    /**
     * Enrich, merge SWRL rules and return all inferences. If filtered = false, then merge the inferences with
     * the input.
     * 
     * This is the default implementation of task {@see ReasoningService.Tasks.ENRICH}. Subclasses may want to
     * change it.
     * 
     * @param ontology
     * @param rules
     * @param filtered
     * @return
     * @throws ReasoningServiceException
     * @throws InconsistentInputException
     */
    protected Set<OWLAxiom> enrich(OWLOntology ontology, List<SWRLRule> rules, boolean filtered) throws ReasoningServiceException,
                                                                                                InconsistentInputException {
        log.debug("Calling enrich(OWLOntology ontology, List<SWRLRule> rules) ");
        if (filtered) {
            return run(ontology, rules, getEnrichAxiomGenerators());
        } else {
            Set<OWLAxiom> output = ontology.getAxioms();
            output.addAll(run(ontology, rules, getEnrichAxiomGenerators()));
            return output;
        }
    }

    /**
     * Only check consistency.
     * 
     * Subclasses may want to change how.
     * 
     * @param ontology
     * @return
     * @throws ReasoningServiceException
     */
    @Override
    public boolean isConsistent(OWLOntology ontology) throws ReasoningServiceException {
        try {
            return getReasoner(ontology).isConsistent();
        } catch (Exception e) {
            log.error("An error have been thrown while attempting to check consistency. Message was: {}",
                e.getLocalizedMessage());
            // TODO Add explanation of this exception
            throw new ReasoningServiceException();
        }
    }

    /**
     * Only check consistency.
     * 
     * Subclasses may want to change how.
     * 
     * @param ontology
     * @param rules
     * @return
     * @throws ReasoningServiceException
     */
    @Override
    public boolean isConsistent(OWLOntology ontology, List<SWRLRule> rules) throws ReasoningServiceException {
        log.debug("Create a input ontology to merge rules in.");
        OWLOntology input;
        try {
            OWLOntologyManager manager = createOWLOntologyManager();
            input = manager.createOntology();
            Set<SWRLRule> ruleSet = new HashSet<SWRLRule>();
            ruleSet.addAll(rules);
            manager.addAxioms(input, ruleSet);
            input = manager.getOntology(input.getOntologyID());
            log.debug("Created ontology: {}", input);
            return getReasoner(ontology).isConsistent();

        } catch (OWLOntologyCreationException e) {
            log.error("An error have been thrown while attempting to create ontology. Message was: {}",
                e.getLocalizedMessage());
            // TODO Add explanation of this exception
            throw new ReasoningServiceException();
        }
    }

    /**
     * The abstract implementation of an OWLApi based reasoning service supports all default {@see
     * ReasoningService.Tasks}. Subclasses may want to extend this.
     */
    @Override
    public List<String> getSupportedTasks() {
        return ReasoningService.Tasks.DEFAULT_TASKS;
    }

    @Override
    public boolean supportsTask(String taskID) {
        return getSupportedTasks().contains(taskID);
    }

    /**
     * OWLApi based reasoning services have type {@see OWLOntology} as input type
     */
    public Class<OWLOntology> getModelType() {
        return OWLOntology.class;
    }

    /**
     * OWLApi based reasoning services have type {@see SWRLRule} as rule type
     */
    public Class<SWRLRule> getRuleType() {
        return SWRLRule.class;
    }

    /**
     * OWLApi based reasoning services have type {@see OWLAxiom} as statement type
     */
    public Class<OWLAxiom> getStatementType() {
        return OWLAxiom.class;
    }
}
