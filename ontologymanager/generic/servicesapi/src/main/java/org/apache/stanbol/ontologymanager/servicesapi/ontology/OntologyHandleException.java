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
package org.apache.stanbol.ontologymanager.servicesapi.ontology;

import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollector;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * Thrown whenever an operation that tries to change an ontology is denied due to active handles on that
 * ontolgoies that forbid changes.
 * 
 * @author alexdma
 * 
 */
public class OntologyHandleException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 3192943015668440337L;

    private Set<OntologyCollector> collectorHandles;

    private Set<OWLOntologyID> dependents;

    public OntologyHandleException(Set<OntologyCollector> collectorHandles, Set<OWLOntologyID> dependents) {
        super();
        setHandles(collectorHandles, dependents);
    }

    public OntologyHandleException(String message) {
        super(message);
    }

    public OntologyHandleException(String message,
                                   Set<OntologyCollector> collectorHandles,
                                   Set<OWLOntologyID> dependents) {
        this(message);
        setHandles(collectorHandles, dependents);
    }

    public Set<OntologyCollector> getCollectorHandles() {
        return collectorHandles;
    }

    public Set<OWLOntologyID> getDependingOntologies() {
        return dependents;
    }

    private void setHandles(Set<OntologyCollector> collectorHandles, Set<OWLOntologyID> dependents) {
        if (collectorHandles == null) collectorHandles = new HashSet<OntologyCollector>();
        this.collectorHandles = collectorHandles;
        if (dependents == null) dependents = new HashSet<OWLOntologyID>();
        this.dependents = dependents;
    }

}
