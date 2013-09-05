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
package org.apache.stanbol.ontologymanager.sources.owlapi;

import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologySpaceSource extends AbstractOWLOntologyInputSource {

    private Logger log = LoggerFactory.getLogger(getClass());

    protected OntologySpace space;

    public OntologySpaceSource(OntologySpace space) {
        this(space, null);
    }

    public OntologySpaceSource(OntologySpace space, Set<OntologyInputSource<?>> subtrees) {
        this.space = space;
        if (subtrees != null) try {
            for (OntologyInputSource<?> st : subtrees)
                appendSubtree(st);
        } catch (UnmodifiableOntologyCollectorException e) {
            log.error(
                "Could not add subtrees to unmodifiable ontology space {}. Input source will have no additions.",
                e.getOntologyCollector());
        }
        bindRootOntology(space.export(OWLOntology.class, false));
    }

    protected void appendSubtree(OntologyInputSource<?> subtree) throws UnmodifiableOntologyCollectorException {
        space.addOntology(subtree);
    }

    public OntologySpace asOntologySpace() {
        return space;
    }

    @Override
    public String toString() {
        return "SCOPE_ONT_IRI<" + getOrigin() + ">";
    }

}
