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
