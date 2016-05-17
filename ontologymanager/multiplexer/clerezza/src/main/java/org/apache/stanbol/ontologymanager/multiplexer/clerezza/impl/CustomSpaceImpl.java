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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * Default implementation of custom ontology space.
 * 
 * @author alexdma
 * 
 */
public class CustomSpaceImpl extends AbstractOntologySpaceImpl {

    public static final String SUFFIX = SpaceType.CUSTOM.getIRISuffix();

    protected static String buildId(String scopeID) {
        return (scopeID != null ? scopeID : "") + "/" + SUFFIX;
    }

    public CustomSpaceImpl(String scopeID, org.semanticweb.owlapi.model.IRI namespace, OntologyProvider<?> ontologyProvider) {
        super(buildId(scopeID), namespace, SpaceType.CUSTOM, ontologyProvider);
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null) return false;
        if (!(arg0 instanceof OntologySpace)) return false;
        if (this == arg0) return true;
        log.warn(
            "{} only implements weak equality, i.e. managed ontologies are only checked by public key, not by content.",
            getClass());
        OntologySpace sp = (OntologySpace) arg0;
        if (!super.equals(arg0)) return false;
        if (!this.getConnectivityPolicy().equals(sp.getConnectivityPolicy())) return false;
        return true;
    }

    @Override
    protected Graph getOntologyAsGraph(OWLOntologyID ontologyId, boolean merge, org.semanticweb.owlapi.model.IRI universalPrefix) {
        Graph o = super.getOntologyAsGraph(ontologyId, merge, universalPrefix);
        switch (getConnectivityPolicy()) {
            case LOOSE:
                break;
            case TIGHT:
                Set<BlankNodeOrIRI> onts = new HashSet<BlankNodeOrIRI>(); // Expected to be a singleton
                synchronized (o) {
                    Iterator<Triple> it = o.filter(null, RDF.type, OWL.Ontology);
                    while (it.hasNext())
                        onts.add(it.next().getSubject());
                }
                String s = getID();
                s = s.substring(0, s.indexOf(SUFFIX)); // strip "custom"
                s += SpaceType.CORE.getIRISuffix(); // concatenate "core"
                IRI target = new IRI(universalPrefix + s);
                for (BlankNodeOrIRI subject : onts)
                    o.add(new TripleImpl(subject, OWL.imports, target));
                break;
            default:
                break;
        }
        return o;

    }

    @Override
    protected OWLOntology getOntologyAsOWLOntology(OWLOntologyID ontologyId,
                                                   boolean merge,
                                                   org.semanticweb.owlapi.model.IRI universalPrefix) {
        OWLOntology o = super.getOntologyAsOWLOntology(ontologyId, merge, universalPrefix);
        switch (getConnectivityPolicy()) {
            case LOOSE:
                break;
            case TIGHT:
                String s = getID();
                s = s.substring(0, s.indexOf(SUFFIX)); // strip "custom"
                s += SpaceType.CORE.getIRISuffix(); // concatenate "core"
                org.semanticweb.owlapi.model.IRI target = org.semanticweb.owlapi.model.IRI.create(universalPrefix + s);
                o.getOWLOntologyManager().applyChange(
                    new AddImport(o, OWLManager.getOWLDataFactory().getOWLImportsDeclaration(target)));
                break;
            default:
                break;
        }
        return o;
    }

    /**
     * Once it is set up, a custom space is write-locked.
     */
    @Override
    public synchronized void setUp() {
        locked = true;
    }

    @Override
    public synchronized void tearDown() {
        locked = false;
    }

}
