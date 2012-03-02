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
package org.apache.stanbol.ontologymanager.ontonet.api.scope;

import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * An ontology scope for application use. There exists exactly one scope for each live (active or halted) KReS
 * session. <br>
 * <br>
 * This is the only type of ontology scope that allows public access to its OWL ontology manager.
 * 
 * @deprecated Session ontology spaces should no longer be created. Session data should be loaded in session
 *             objects instead.
 * 
 * @author alexdma
 * 
 */
public interface SessionOntologySpace extends OntologySpace {

    /**
     * Returns the OWL ontology manager associated to this scope.
     * 
     * @return the associated ontology manager
     */
    OWLOntologyManager getOntologyManager();

    /**
     * @deprecated space linking is performed by the parent scope at OWL export time. Implementations do
     *             nothing.
     * @param space
     * @param skipRoot
     * @throws UnmodifiableOntologyCollectorException
     */
    void attachSpace(OntologySpace space, boolean skipRoot) throws UnmodifiableOntologyCollectorException;

}
