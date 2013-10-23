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
package org.apache.stanbol.ontologymanager.servicesapi.collector;

import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * Objects that react to the addition or removal of ontologies to or from an ontology collector will implement
 * this interface.<br>
 * <br>
 * TODO add "before" methods and provide a way to abort the corresponding operation.
 * 
 * @author alexdma
 * 
 */
public interface OntologyCollectorListener {

    /**
     * Fired <i>after</i> an ontology was successfully added to an ontology collector.
     * 
     * @param collectorId
     *            the ontology collector identifier.
     * @param addedOntology
     *            the added ontology identifier.
     */
    void onOntologyAdded(OntologyCollector collector, OWLOntologyID addedOntology);

    /**
     * Fired <i>after</i> an ontology was successfully removed from an ontology collector.
     * 
     * @param collectorId
     *            the ontology collector identifier.
     * @param removedOntology
     *            the removed ontology identifier.
     */
    void onOntologyRemoved(OntologyCollector collector, OWLOntologyID removedOntology);

}
