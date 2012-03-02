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

import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.NamedResource;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.Lockable;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OWLExportable;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;

/**
 * Represents an ontology network that is used for modelling a given knowledge component or domain, e.g.
 * workflows, organisations, devices, content or business domain.<br>
 * <br>
 * Each ontology scope comprises in turn a number of ontology spaces of three kinds.
 * <ul>
 * <li>Exactly one core space, which defines the immutable components of the scope.
 * <li>At most one custom space, which contains user-defined components.
 * <li>Zero or more session spaces, which contains (potentially volatile) components specific for user
 * sessions.
 * </ul>
 * An ontology scope can thus be seen as a fa&ccedil;ade for ontology spaces.
 * 
 * 
 * @author alexdma
 * 
 */
public interface OntologyScope extends NamedResource, Lockable, ScopeOntologyListenable, OWLExportable {

    /**
     * Adds a new ontology space to the list of user session spaces for this scope.
     * 
     * @deprecated as session ontology spaces are obsolete, so is this method. Please refer directly to the
     *             session identified by <code>sessionID</code>.
     * 
     * @param sessionSpace
     *            the ontology space to be added.
     * @throws UnmodifiableOntologyCollectorException
     */
    void addSessionSpace(OntologySpace sessionSpace, String sessionID) throws UnmodifiableOntologyCollectorException;

    /**
     * Returns the core ontology space for this ontology scope. The core space should never be null for any
     * scope.
     * 
     * @return the core ontology space
     */
    OntologySpace getCoreSpace();

    /**
     * Returns the custom ontology space for this ontology scope.
     * 
     * @return the custom ontology space, or null if no custom space is registered for this scope.
     */
    OntologySpace getCustomSpace();

    /**
     * Returns the ontology space for this scope that is identified by the supplied IRI.
     * 
     * @deprecated as session ontology spaces are obsolete, so is this method. Please refer directly to the
     *             session identified by <code>sessionID</code>.
     * 
     * @param sessionID
     *            the unique identifier of the KReS session.
     * @return the ontology space identified by <code>sessionID</code>, or null if no such space is registered
     *         for this scope and session.
     */
    SessionOntologySpace getSessionSpace(String sessionID);

    /**
     * Returns all the active ontology spaces for this scope.
     * 
     * @deprecated as session ontology spaces are obsolete, so is this method. Please reroute all
     *             session-related queries to {@link Session} objects directly.
     * 
     * @return a set of active ontology spaces for this scope.
     */
    Set<OntologySpace> getSessionSpaces();

    /**
     * Sets an ontology space as the custom space for this scope.
     * 
     * @param customSpace
     *            the custom ontology space.
     * @throws UnmodifiableOntologyCollectorException
     *             if either the scope or the supplied space are locked.
     */
    void setCustomSpace(OntologySpace customSpace) throws UnmodifiableOntologyCollectorException;

    /**
     * Performs the operations required for activating the ontology scope. It should be possible to perform
     * them <i>after</i> the constructor has been invoked.<br>
     * <br>
     * When the core ontology space is created for this scope, this should be set in the scope constructor. It
     * can be changed in the <code>setUp()</code> method though.
     */
    void setUp();

    /**
     * Performs whatever operations are required for making sure the custom space of this scope is aware of
     * changes occurring in its core space, that all session spaces are aware of changes in the custom space,
     * and so on. Typically, this includes updating all import statements in the top ontologies for each
     * space.<br>
     * <br>
     * This method is not intended for usage by ontology managers. Since its invocation is supposed to be
     * automatic, it should be invoked by whatever classes are responsible for listening to changes in an
     * ontology scope/space. In the default implementation, it is the scope itself, yet the method is left
     * public in order to allow for external controllers.
     * 
     * @deprecated synchronization is managed internally, therefore this method has no effect.
     */
    void synchronizeSpaces();

    /**
     * Performs the operations required for deactivating the ontology scope. In general, this is not
     * equivalent to finalizing the object for garbage collection. It should be possible to activate the same
     * ontology scope again if need be.
     */
    void tearDown();

}
