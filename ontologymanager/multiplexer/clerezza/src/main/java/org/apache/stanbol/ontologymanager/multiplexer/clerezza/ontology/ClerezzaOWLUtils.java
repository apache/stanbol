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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A set of utilities for handling OWL2 ontologies in Clerezza.
 * 
 * @author alexdma
 * 
 */
public final class ClerezzaOWLUtils {

    /**
     * Restrict instantiation
     */
    private ClerezzaOWLUtils() {}

    private static Logger log = LoggerFactory.getLogger(ClerezzaOWLUtils.class);

    public static Graph createOntology(String id, TcManager tcm) {
        IRI name = new IRI(id);
        Graph ont = tcm.createGraph(name);
        ont.add(new TripleImpl(name, RDF.type, OWL.Ontology));
        return ont;
    }

    public static Graph createOntology(String id) {
        return createOntology(id, TcManager.getInstance());
    }

}
