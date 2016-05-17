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
package org.apache.stanbol.commons.owl.transformation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.commons.owl.PhonyIRIMapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * This class provides static methods to convert:
 * 
 * <ul>
 * <li>a Jena Model (see {@link Model}) to a list of Clerezza triples (see {@link Triple})
 * <li>a Jena Model to a Clerezza Graph (see {@link Graph})
 * <li>a Clerezza Graph a Jena Model
 * <li>a Clerezza Graph a Jena ImmutableGraph (see {@link ImmutableGraph}
 * </ul>
 * 
 * 
 * @author andrea.nuzzolese
 * 
 */

public final class OWLAPIToClerezzaConverter {

    /**
     * Restrict instantiation
     */
    private OWLAPIToClerezzaConverter() {}

   private static Logger log = LoggerFactory.getLogger(OWLAPIToClerezzaConverter.class);

    /**
     * 
     * Converts an OWL API {@link OWLOntology} to an {@link ArrayList} of Clerezza triples (instances of class
     * {@link Triple}).
     * 
     * @param ontology
     *            {@link OWLOntology}
     * @return an {@link ArrayList} that contains the generated Clerezza triples (see {@link Triple})
     */
    public static List<Triple> owlOntologyToClerezzaTriples(OWLOntology ontology) {
        ArrayList<Triple> clerezzaTriples = new ArrayList<Triple>();
        org.apache.clerezza.commons.rdf.Graph mGraph = owlOntologyToClerezzaGraph(ontology);
        Iterator<Triple> tripleIterator = mGraph.iterator();
        while (tripleIterator.hasNext()) {
            Triple triple = tripleIterator.next();
            clerezzaTriples.add(triple);
        }
        return clerezzaTriples;
    }

    /**
     * 
     * Converts a OWL API {@link OWLOntology} to Clerezza {@link Graph}.
     * 
     * @param ontology
     *            {@link OWLOntology}
     * @return the equivalent Clerezza {@link Graph}.
     */

    public static org.apache.clerezza.commons.rdf.Graph owlOntologyToClerezzaGraph(OWLOntology ontology) {
        org.apache.clerezza.commons.rdf.Graph mGraph = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        try {
            manager.saveOntology(ontology, new RDFXMLOntologyFormat(), out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            ParsingProvider parser = new JenaParserProvider();
            mGraph = new SimpleGraph();
            parser.parse(mGraph, in, SupportedFormat.RDF_XML, null);
        } catch (OWLOntologyStorageException e) {
            log.error("Failed to serialize OWL Ontology " + ontology + "for conversion", e);
        }
        return mGraph;

    }

    /**
     * Converts a Clerezza {@link Graph} to an OWL API {@link OWLOntology}.
     * 
     * @param mGraph
     *            {@link org.apache.clerezza.commons.rdf.Graph}
     * @return the equivalent OWL API {@link OWLOntology}.
     */
    public static OWLOntology clerezzaGraphToOWLOntology(org.apache.clerezza.commons.rdf.Graph graph) {
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        // Never try to import
        mgr.addIRIMapper(new PhonyIRIMapper(Collections.<IRI> emptySet()));
        return clerezzaGraphToOWLOntology(graph, mgr);
    }

    public static OWLOntology clerezzaGraphToOWLOntology(org.apache.clerezza.commons.rdf.Graph graph,
                                                         OWLOntologyManager ontologyManager) {

        /*
         * The root graph can be serialized and de-serialized, but before that we should decide what to do
         * with imports. We can proceed as follows:
         * 
         * for each import statement, - check if the ontology manager has (1) an ontology or (2) a mapping. -
         * if (1), just get it and add it to the merge pool - if (2), do nothing. the ontology manager should
         * handle it when loading the root, - if neither, fetch the graph and call the procideure on it
         * 
         * Alternatively, construct the whole reverse imports stack, then traverse it again, get the
         * OWLOntology version for each (with the phony mapper set) and add it to the merge pool
         * 
         * If it works, just add all the triples to a Graph, but no, we don't want to store that
         * change.
         */

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SerializingProvider serializingProvider = new JenaSerializerProvider();
        serializingProvider.serialize(out, graph, SupportedFormat.RDF_XML);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        OWLOntology ontology = null;
        try {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(in);
        } catch (OWLOntologyAlreadyExistsException e) {
            ontology = ontologyManager.getOntology(e.getOntologyID());
        } catch (OWLOntologyCreationException e) {
            log.error("Failed to serialize OWL Ontology " + ontology + "for conversion", e);
        }
        return ontology;
    }

}
