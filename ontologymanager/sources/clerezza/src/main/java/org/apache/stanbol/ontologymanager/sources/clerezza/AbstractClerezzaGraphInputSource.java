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
package org.apache.stanbol.ontologymanager.sources.clerezza;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.servicesapi.io.AbstractGenericInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of an {@link OntologyInputSource} that returns {@link Graph} objects as
 * ontologies.
 * 
 * Subclasses must implement the {@link #getImports(boolean)} method, as the availability of imported
 * ontologies might depend on the input source being able to access the {@link TcManager} where they are
 * stored.
 * 
 * @author alexdma
 * 
 */
public abstract class AbstractClerezzaGraphInputSource extends AbstractGenericInputSource<Graph> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void bindRootOntology(Graph ontology) {
        super.bindRootOntology(ontology);
    }

}
