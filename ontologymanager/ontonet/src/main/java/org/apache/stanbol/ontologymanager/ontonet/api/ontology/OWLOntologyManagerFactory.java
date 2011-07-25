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
package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import java.util.List;

import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Generates new instances of {@link OWLOntologyManager} with optional offline support already enabled for
 * each of them.
 * 
 * @author alessandro
 * @deprecated use the static methods of {@link org.apache.stanbol.owl.OWLOntologyManagerFactory} instead.
 */
public interface OWLOntologyManagerFactory {

    /**
     * Returns the list of local IRI mappers that are automatically bound to a newly created
     * {@link OWLOntologyManager} if set to support offline mode. The IRI mappers are typically applied in
     * order, therefore mappers at the end of the list may supersede those at its beginning.
     * 
     * @return the list of local IRI mappers, in the order they are applied.
     */
    List<OWLOntologyIRIMapper> getLocalIRIMapperList();

    /**
     * Creates a new instance of {@link OWLOntologyManager}, with optional offline support.
     * 
     * @param withOfflineSupport
     *            if true, the local IRI mappers obtained by calling {@link #getLocalIRIMapperList()} will be
     *            applied to the new ontology manager.
     * @return a new OWL ontology manager.
     */
    OWLOntologyManager createOntologyManager(boolean withOfflineSupport);
}
