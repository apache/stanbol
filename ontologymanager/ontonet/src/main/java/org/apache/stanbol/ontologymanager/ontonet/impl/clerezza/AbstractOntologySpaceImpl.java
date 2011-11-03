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
package org.apache.stanbol.ontologymanager.ontonet.impl.clerezza;

import java.util.LinkedList;
import java.util.List;

import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OWLExportable;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.owl.util.URIUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Clerezza-native implementation of {@link OntologySpace}.
 * 
 * @author alexdma
 * 
 */
public abstract class AbstractOntologySpaceImpl extends AbstractOntologyCollectorImpl implements
        OntologySpace {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected SpaceType type;

    public AbstractOntologySpaceImpl(String spaceID, IRI namespace, SpaceType type, TcProvider tcProvider) {
        super(spaceID, namespace, tcProvider);
        this.type = type;
    }

    @Override
    public OWLOntology asOWLOntology() {
        return this.asOWLOntology(false);
    }

    /**
     * FIXME not merging yet
     * 
     * @see OWLExportable#asOWLOntology(boolean)
     */
    @Override
    public OWLOntology asOWLOntology(boolean merge) {
        if (merge) throw new UnsupportedOperationException(
                "Ontology merging not implemented yet. Please set merge parameter to false.");
        OWLOntology root;
        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        IRI iri = IRI.create(namespace + _id);
        try {
            root = ontologyManager.createOntology(iri);
        } catch (OWLOntologyAlreadyExistsException e) {
            ontologyManager.removeOntology(ontologyManager.getOntology(iri));
            try {
                root = ontologyManager.createOntology(iri);
            } catch (OWLOntologyAlreadyExistsException e1) {
                root = ontologyManager.getOntology(iri);
            } catch (OWLOntologyCreationException e1) {
                log.error("Failed to assemble root ontology for scope " + iri, e);
                root = null;
            }
        } catch (OWLOntologyCreationException e) {
            log.error("Failed to assemble root ontology for scope " + _id, e);
            root = null;
        }

        // Add the import declarations for directly managed ontologies.
        if (root != null) {
            List<OWLOntologyChange> changes = new LinkedList<OWLOntologyChange>();
            OWLDataFactory df = ontologyManager.getOWLDataFactory();
            for (OWLOntology o : getOntologies(false)) {
                if (o == null) continue;

                String base = URIUtils.upOne(IRI.create(namespace + getID())) + "/";

                IRI ontologyIri;

                if (o.isAnonymous()) try {
                    ontologyIri = ontologyManager.getOntologyDocumentIRI(o);
                } catch (Exception ex) {
                    ontologyIri = o.getOWLOntologyManager().getOntologyDocumentIRI(o);
                }
                else {
                    ontologyIri = o.getOntologyID().getDefaultDocumentIRI();
                }

                IRI physIRI = IRI.create(base + ontologyIri);

                changes.add(new AddImport(root, df.getOWLImportsDeclaration(physIRI)));
            }
            ontologyManager.applyChanges(changes);
        }
        return root;
    }

    /**
     * 
     * @param id
     *            The ontology space identifier. This implementation only allows non-null and non-empty
     *            alphanumeric sequences, case-sensitive and preferably separated by a single slash character,
     *            with optional dashes or underscores.
     */
    @Override
    protected void setID(String id) {
        if (id == null) throw new IllegalArgumentException("Space ID cannot be null.");
        id = id.trim();
        if (id.isEmpty()) throw new IllegalArgumentException("Space ID cannot be empty.");
        if (id.matches("[\\w-]+")) log.warn(
            "Space ID {} is a single alphanumeric sequence, with no separating slash."
                    + " This is legal but strongly discouraged. Please consider using"
                    + " space IDs of the form [scope_id]/[space_type], e.g. Users/core .", id);
        else if (!id.matches("[\\w-]+/[\\w-]+")) throw new IllegalArgumentException(
                "Illegal space ID " + id + " - Must be an alphanumeric sequence, (preferably two, "
                        + " slash-separated), with optional underscores or dashes.");
        this._id = id;
    }

}
