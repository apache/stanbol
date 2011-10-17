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
package org.apache.stanbol.ontologymanager.ontonet.impl.renderers;

import java.util.LinkedList;
import java.util.List;

import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.LoggerFactory;

@Deprecated
public class SessionRenderer {

    private static OWLDataFactory __factory = OWLManager.getOWLDataFactory();

    private static final IRI _sessionIri = IRI
            .create("http://kres.iks-project.eu/ontology/onm/meta.owl#Session");

    private static final IRI _hasIdIri = IRI.create("http://kres.iks-project.eu/ontology/onm/meta.owl#hasID");

    private static OWLClass cSession = __factory.getOWLClass(_sessionIri);

    private static OWLDataProperty hasId = __factory.getOWLDataProperty(_hasIdIri);

    public static OWLOntology getSessionMetadataRDFasOntology(Session session) {
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        OWLOntology ont = null;
        try {
            ont = mgr.createOntology(IRI.create(session.getID() + "/meta.owl"));
        } catch (OWLOntologyCreationException e) {
            LoggerFactory.getLogger(ScopeSetRenderer.class).error(
                "KReS :: could not create empty ontology for rendering sesion metadata.", e);
            return null;
        }

        List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();

        OWLNamedIndividual iSes = __factory.getOWLNamedIndividual(IRI.create(session.getID()));
        additions.add(new AddAxiom(ont, __factory.getOWLClassAssertionAxiom(cSession, iSes)));
        OWLDatatype anyURI = __factory.getOWLDatatype(IRI.create("http://www.w3.org/2001/XMLSchema#anyURI"));
        OWLLiteral hasIdValue = __factory.getOWLTypedLiteral(session.getID().toString(), anyURI);
        additions.add(new AddAxiom(ont, __factory.getOWLDataPropertyAssertionAxiom(hasId, iSes, hasIdValue)));
        mgr.applyChanges(additions);

        StringDocumentTarget tgt = new StringDocumentTarget();
        try {
            mgr.saveOntology(ont, new RDFXMLOntologyFormat(), tgt);
            return ont;
        } catch (OWLOntologyStorageException e) {
            LoggerFactory.getLogger(ScopeSetRenderer.class).error(
                "KReS :: could not save session metadata ontology.", e);
            return null;
        }

    }

}
