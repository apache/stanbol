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
package org.apache.stanbol.commons.owl;

import java.util.Iterator;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.ontologies.OWL;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.stanbol.commons.owl.util.OWL2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An utility modifiable graph that only accepts triples that deal with naming this graph as an ontology.
 * 
 * @author alexdma
 * 
 */
public class OntologyLookaheadGraph extends SimpleGraph {

    private Logger log = LoggerFactory.getLogger(getClass());

    private IRI ontologyIRI = null, versionIRI = null;

    private int tripleCount = 0, foundIndex = -1;

    private int maxTriples, offset = 10;

    private IRI versionIriProperty = new IRI(OWL2Constants.OWL_VERSION_IRI);

    public OntologyLookaheadGraph() {
        this(-1, -1);
    }

    public OntologyLookaheadGraph(int maxTriples) {
        this(maxTriples, Math.max(10, maxTriples / 10));
    }

    public OntologyLookaheadGraph(int maxTriples, int offset) {
        if (maxTriples > 0 && offset > maxTriples) throw new IllegalArgumentException(
                "Offset cannot be greater than the maximum triples to scan.");
        this.maxTriples = maxTriples;
        this.offset = offset;
    }

    protected void checkOntologyId() {
        for (Iterator<Triple> it = this.filter(null, RDF.type, OWL.Ontology); it.hasNext();) {
            BlankNodeOrIRI s = it.next().getSubject();
            if (s instanceof IRI) {
                ontologyIRI = (IRI) s;
                if (foundIndex <= 0) foundIndex = tripleCount;
                break;
            }
        }
        /*
         * TODO be more tolerant with versionIRI triples with no owl:Ontology typing?
         */
        for (Iterator<Triple> it = this.filter(null, versionIriProperty, null); it.hasNext();) {
            RDFTerm o = it.next().getObject();
            if (o instanceof IRI) {
                versionIRI = (IRI) o;
                if (foundIndex <= 0) foundIndex = tripleCount;
                break;
            }
        }
    }

    /**
     * Returns the maximum distance allowed (in triples) between the ontology IRI and version IRI
     * declarations. A negative value indicates no limit.
     * 
     * @return
     */
    public int getOffset() {
        return offset;
    }

    public IRI getOntologyIRI() {
        return ontologyIRI;
    }

    public int getScannedTripleCount() {
        return tripleCount;
    }

    public IRI getVersionIRI() {
        return versionIRI;
    }

    @Override
    public boolean performAdd(Triple t) {
        // Check if the overall triple limit has been reached.
        if (maxTriples > 0 && tripleCount == maxTriples) {
            log.debug("Triple limit {} reached. Stopping triple addition.", maxTriples);
            throw new RuntimeException();
        }
        // If one IRI was found, check if the allowed offset has been reached.
        // (if foundIndex is positive, then tripleCount must be at least the same.)
        if (foundIndex > 0 && tripleCount - foundIndex == offset) {
            log.debug("Offset reached.");
            log.debug(" ... Triples scanned: {}", tripleCount);
            log.debug(" ... Found at: {}", foundIndex);
            log.debug(" ... Offset: {}", offset);
            throw new RuntimeException();
        }
        boolean b = false;
        tripleCount++;

        // filter the interesting Triples
        if (versionIriProperty.equals(t.getPredicate())
            || (RDF.type.equals(t.getPredicate()) && OWL.Ontology.equals(t.getObject()))) b = super
                .performAdd(t);

        // check the currently available triples for the Ontology ID
        checkOntologyId();
        if (ontologyIRI != null && versionIRI != null) {
            log.debug("Fully qualified OWL Ontology ID found. Exiting.");
            throw new RuntimeException(); // stop importing
        }
        return b;
    }

}
