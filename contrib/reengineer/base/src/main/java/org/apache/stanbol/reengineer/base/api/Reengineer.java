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

import org.apache.stanbol.reengineer.base.api.util.UnsupportedReengineerException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * 
 * A Reengineer provides methods for transforming in KReS both the schema and the data of a non-RDF data
 * source into RDF. <br>
 * <br>
 * Accepted data sources are:
 * <ul>
 * <li>0 - Relational Databases
 * <li>1 - XML
 * <li>2 - iCalendar
 * <li>3 - RSS
 * </ul>
 * 
 * @author andrea.nuzzolese
 * 
 */

public interface Reengineer {

    /**
     * The method returns one of the following values related to a particular data souce: <li>0 - Relational
     * Databases <li>1 - XML <li>2 - iCalendar <li>3 - RSS </ul>
     * 
     * @return {@code int}
     */
    int getReengineerType();

    /**
     * The method enables to test if the Reengineer can perform the reengineering of a particular data source
     * given as input.
     * 
     * @param dataSource
     *            {@link DataSource}
     * @return true if the Reengineer can perform the reengineering, false otherwise
     */
    boolean canPerformReengineering(DataSource dataSource);

    /**
     * The method enables to test if the Reengineer can perform the reengineering of a particular data source
     * type given as input.
     * 
     * @param the
     *            data source type {@code int}
     * @return true if the Reengineer can perform the reengineering, false otherwise
     */
    boolean canPerformReengineering(int dataSourceType);

    boolean canPerformReengineering(OWLOntology schemaOntology);

    /**
     * The method enables to test if the Reengineer can perform the reengineering of a particular data source
     * type given as input.
     * 
     * @param the
     *            data source type {@code String}
     * @return true if the Reengineer can perform the reengineering, false otherwise
     */
    boolean canPerformReengineering(String dataSourceType) throws UnsupportedReengineerException;

    /**
     * The data source (non-RDF) provided is reengineered to RDF. This operation produces an RDF data set that
     * contains information both about the data and about the schema of the original data source.
     * 
     * @param graphNS
     *            {@link String}
     * @param outputIRI
     *            {@link IRI}
     * @param dataSource
     *            {@link DataSource}
     * @return the reengineered data set - {@link OWLOntology}
     */
    OWLOntology reengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException;

    /**
     * The generation of the RDF containing the information about the schema of the data source is obtained
     * passing to this method the data source object as it is represented in Semion (i.e. {@link DataSource}).
     * An {@link OWLOntology} is returned
     * 
     * @param graphNS
     *            {@link String}
     * @param outputIRI
     *            {@link IRI}
     * @param dataSource
     *            {@link DataSource}
     * @return the {@link OWLOntology} of the data source shema
     */
    OWLOntology schemaReengineering(String graphNS, IRI outputIRI, DataSource dataSource) throws ReengineeringException;

    /**
     * The generation of the RDF containing the information about the data of the data source is obtained
     * passing to this method the data source object as it is represented in Semion (i.e. {@link DataSource}).
     * An {@link OWLOntology} is returned
     * 
     * @param graphNS
     *            {@link String}
     * @param outputIRI
     *            {@link IRI}
     * @param dataSource
     *            {@link DataSource}
     * @return the {@link OWLOntology} of the data source shema
     */
    OWLOntology dataReengineering(String graphNS,
                                  IRI outputIRI,
                                  DataSource dataSource,
                                  OWLOntology schemaOntology) throws ReengineeringException;

}
