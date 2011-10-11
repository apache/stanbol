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
package org.apache.stanbol.reengineer.base.api;

import java.util.Collection;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * 
 * The {@code ReengineerManager} is responsible of the coordination of all the tasks performed by Stanbol
 * Reengineer.
 * 
 * @author andrea.nuzzolese
 * 
 */

public interface ReengineerManager {

    /**
     * The {@link ReengineerManager} can add a new reengineer to the list of available reengineers. This is
     * performed through the method {@cod bindReengineer}.
     * 
     * @param semionReengineer
     *            {@link Reengineer}
     * @return true if the reengineer is bound, false otherwise
     */
    boolean bindReengineer(Reengineer semionReengineer);

    /**
     * Gets the number of active reengineers.
     * 
     * @return the number of active reengineers.
     */
    int countReengineers();

    /**
     * Gets the active reengineers of KReS.
     * 
     * @return the {@link Collection< Reengineer >} of active reengineers.
     */
    Collection<Reengineer> listReengineers();

    // /**
    // * The {@link ReengineerManager} can register a single instance of {@link SemionRefactorer}.
    // *
    // * @param semionRefactorer {@link SemionRefactorer}
    // */
    // public void registerRefactorer(SemionRefactorer semionRefactorer);
    //
    // /**
    // * Unregisters the instance of {@link SemionRefactorer}. After the call of this method Semion has no
    // refactorer.
    // */
    // public void unregisterRefactorer();
    //
    // /**
    // * The instance of the refactored is returned back if it exists.
    // *
    // * @return the active {@link SemionRefactorer}
    // */
    // public SemionRefactorer getRegisteredRefactorer();

    OWLOntology performDataReengineering(String graphNS,
                                         IRI outputIRI,
                                         DataSource dataSource,
                                         OWLOntology schemaOntology) throws ReengineeringException;

    OWLOntology performReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException;

    OWLOntology performSchemaReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException;

    /**
     * The {@link ReengineerManager} can remove a reengineer from the list of available reengineers. This is
     * performed through the method {@cod unbindReengineer}.
     * 
     * @param reenginnerType
     *            {@code int}
     * @return true if the reengineer is unbound, false otherwise
     */
    boolean unbindReengineer(int reenginnerType);

    /**
     * The {@link ReengineerManager} can remove a reengineer from the list of available reengineers. This is
     * performed through the method {@cod unbindReengineer}.
     * 
     * @param semionReengineer
     *            {@link Reengineer}
     * @return true if the reengineer is unbound, false otherwise
     */
    boolean unbindReengineer(Reengineer semionReengineer);

}
