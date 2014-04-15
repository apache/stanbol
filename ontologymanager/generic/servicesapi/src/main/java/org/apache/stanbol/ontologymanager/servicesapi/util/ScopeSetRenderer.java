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
package org.apache.stanbol.ontologymanager.servicesapi.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.Vocabulary;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.LoggerFactory;

/**
 * Just an attempt. If we like it, make an API out of it.
 * 
 * XXX deprecate it in favor of {@link Vocabulary} ?
 * 
 * @author alexdma
 * 
 */
public final class ScopeSetRenderer {

    /**
     * Restrict instantiation
     */
    private ScopeSetRenderer() {}

    private static OWLDataFactory __factory = OWLManager.getOWLDataFactory();

    private static IRI _scopeIri = IRI.create(Vocabulary.SCOPE_URIREF.getUnicodeString());

    private static OWLClass cScope = __factory.getOWLClass(_scopeIri);

    public static OWLOntology getScopes(Set<Scope> scopes) {

        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLOntology ont = null;
        try {
            ont = mgr.createOntology();
        } catch (OWLOntologyCreationException e) {
            LoggerFactory.getLogger(ScopeSetRenderer.class).error(
                "KReS :: could not create empty ontology for rendering scopes.", e);
            return null;
        }
        List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
        // The ODP metadata vocabulary is always imported.
        // TODO : also import the ONM meta when it goes online.
        additions.add(new AddImport(ont, __factory.getOWLImportsDeclaration(IRI
                .create("http://www.ontologydesignpatterns.org/schemas/meta.owl"))));
        for (Scope scope : scopes) {
            OWLNamedIndividual iScope = __factory.getOWLNamedIndividual(IRI.create(scope
                    .getDefaultNamespace() + scope.getID()));
            OWLAxiom ax = __factory.getOWLClassAssertionAxiom(cScope, iScope);
            additions.add(new AddAxiom(ont, ax));
        }
        mgr.applyChanges(additions);

        return ont;
    }

}
