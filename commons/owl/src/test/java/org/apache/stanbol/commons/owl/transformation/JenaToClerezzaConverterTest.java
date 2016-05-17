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

import java.util.Collection;
import java.util.Iterator;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.commons.owl.transformation.JenaToClerezzaConverter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * It is a JUnit test class.<br>
 * It tests the methods of the class {@link JenaToClerezzaConverter}.
 * 
 * @author andrea.nuzzolese
 *
 */

public class JenaToClerezzaConverterTest {

	private static Model model;
	private static Graph mGraph;
	private static String ns = "http://incubator.apache.org/stanbol/owl#";
	private static String foaf = "http://xmlns.com/foaf/0.1/";
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	@BeforeClass
	public static void setupClass(){
		
		/*
		 * Set-up the Jena model for the test.
		 * Simply add the triples:
		 * 	AndreaNuzzolese isA Person
		 * 	EnricoDaga isA Person
		 *  AndreaNuzzolese knows EnricoDaga
		 */
		model = ModelFactory.createDefaultModel();
		Resource foafPersonInJena = model.createResource(foaf+"Person");
		
		Property knowsInJena = model.createProperty(foaf+"knows");
		Resource andreaNuzzoleseInJena = model.createResource(ns+"AndreaNuzzolese", foafPersonInJena);
		Resource enricoDagaInJena = model.createResource(ns+"EnricoDaga", foafPersonInJena);
		
		andreaNuzzoleseInJena.addProperty(knowsInJena, enricoDagaInJena);
		
		
		/*
		 * Set-up the Clerezza model for the test.
		 * As before simply add the triples:
		 * 	AndreaNuzzolese isA Person
		 * 	EnricoDaga isA Person
		 *  AndreaNuzzolese knows EnricoDaga
		 */
		mGraph = new SimpleGraph();
		
		IRI knowsInClerezza = new IRI(ns+"knows");
		IRI rdfType = new IRI(RDF.getURI()+"type");
		IRI foafPersonInClerezza = new IRI(foaf+"Person");
		
		BlankNodeOrIRI andreaNuzzoleseInClerezza = new IRI(ns+"AndreaNuzzolese");		
		BlankNodeOrIRI enricoDagaInClerezza = new IRI(ns+"EnricoDaga");
		
		Triple triple = new TripleImpl(andreaNuzzoleseInClerezza, rdfType, foafPersonInClerezza);
		mGraph.add(triple);
		triple = new TripleImpl(enricoDagaInClerezza, rdfType, foafPersonInClerezza);
		mGraph.add(triple);
		triple = new TripleImpl(andreaNuzzoleseInClerezza, knowsInClerezza, enricoDagaInClerezza);
		mGraph.add(triple);
		
		
	}
	
	@Test
	public void testGraphToJenaGraph(){
		/*
		 * Convert the Graph to a Jena ImmutableGraph.
		 */
		com.hp.hpl.jena.graph.Graph jGraph = JenaToClerezzaConverter.clerezzaGraphToJenaGraph(mGraph);
		
		/*
		 * Print all the triples contained in the Jena ImmutableGraph.
		 */
		ExtendedIterator<com.hp.hpl.jena.graph.Triple> tripleIt = jGraph.find(null, null, null);
		while(tripleIt.hasNext()){
			com.hp.hpl.jena.graph.Triple triple = tripleIt.next();
			log.info(triple.toString());
		}
	}
	
	@Test
	public void testGraphToJenaModel(){
		/*
		 * Convert the Graph to a Jena Model.
		 */
		Model model = JenaToClerezzaConverter.clerezzaGraphToJenaModel(mGraph);
		
		/*
		 * Print all the triples contained in the Jena Model.
		 */
		StmtIterator stmtIt = model.listStatements();
		while(stmtIt.hasNext()){
			Statement statement = stmtIt.next();
			log.info(statement.toString());
		}
		
	}
	
	@Test
	public void testModelToGraph(){
		/*
		 * Convert the Jena Model to a Clerezza Graph.
		 */
		Graph mGraph = JenaToClerezzaConverter.jenaModelToClerezzaGraph(model);
		
		/*
		 * Print all the triples contained in the Clerezza Graph.
		 */
		Iterator<Triple> tripleIt = mGraph.iterator();
		while(tripleIt.hasNext()){
			Triple triple = tripleIt.next();
			log.info(triple.toString());
		}
	}
	
	@Test
	public void testModelToClerezzaTriples(){
		/*
		 * Convert the Jena Model into a collection of Clerezza triples.
		 */
		Collection<Triple> triples = JenaToClerezzaConverter.jenaModelToClerezzaTriples(model);
		
		/*
		 * Print the collection of Clerezza triples.
		 */
		for(Triple triple : triples){
			log.info(triple.toString());
		}
	}
}
