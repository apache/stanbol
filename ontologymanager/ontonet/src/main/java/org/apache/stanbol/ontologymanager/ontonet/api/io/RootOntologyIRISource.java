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
package org.apache.stanbol.ontologymanager.ontonet.api.io;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * An input source that provides the OWL Ontology loaded from the supplied physical IRI, as well as the
 * physical IRI itself for consumers that need to load the ontology themselves. This means that in order to
 * use the {@link RootOntologyIRISource}, <i>one</i> the following requirements must be met:
 * <ol>
 * <li>Stanbol is in online mode and all imports can be resolved recursively;
 * <li>Stanbol is in offline mode and the ontology has no recursive import statements.
 * </ol>
 * If Stanbol is in offline mode and it is known that all imported ontologies are in the same local directory
 * as the root ontology, one should use a {@link ParentPathInputSource} instead. <br>
 * <br>
 * For convenience, an existing OWL ontology manager can be supplied for loading the ontology.
 * 
 * @author alexdma
 * 
 */
public class RootOntologyIRISource extends AbstractOWLOntologyInputSource {

    public RootOntologyIRISource(IRI rootOntologyIri) throws OWLOntologyCreationException {
        this(rootOntologyIri, OWLManager.createOWLOntologyManager());
    }

    public RootOntologyIRISource(IRI rootPhysicalIri, OWLOntologyManager manager) throws OWLOntologyCreationException {
        this(rootPhysicalIri, manager, false);
    }

    public RootOntologyIRISource(IRI rootPhysicalIri, OWLOntologyManager manager, boolean ignoreIriMappers) throws OWLOntologyCreationException {
        bindPhysicalOrigin(Origin.create(rootPhysicalIri));
        bindRootOntology(ignoreIriMappers ? manager.loadOntologyFromOntologyDocument(rootPhysicalIri)
                : manager.loadOntology(rootPhysicalIri));
    }

    @Override
    public String toString() {
        return "ROOT_ONT_IRI<" + getOrigin() + ">";
    }

}
