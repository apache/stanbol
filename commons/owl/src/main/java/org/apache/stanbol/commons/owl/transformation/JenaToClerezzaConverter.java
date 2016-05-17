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
import java.util.Iterator;
import java.util.List;

import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.ParsingProvider;
import org.apache.clerezza.rdf.core.serializedform.SerializingProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * This class provides static methods to convert:
 * 
 * <ul>
 * <li> a Jena Model (see {@link Model}) to a list of Clerezza triples (see {@link Triple})
 * <li> a Jena Model to a Clerezza Graph (see {@link Graph})
 * <li> a Clerezza Graph a Jena Model
 * <li> a Clerezza Graph a Jena ImmutableGraph (see {@link ImmutableGraph}}
 * </ul>
 * 
 * 
 * @author andrea.nuzzolese
 *
 */

public final class JenaToClerezzaConverter {

    /**
     * Restrict instantiation
     */
    private JenaToClerezzaConverter() {}

   /**
	 * 
	 * Converts a Jena {@link Model} to an {@link ArrayList} of Clerezza triples (instances of class {@link Triple}).
	 * 
	 * @param model {@link Model}
	 * @return an {@link ArrayList} that contains the generated Clerezza triples (see {@link Triple}) 
	 */
	public static List<Triple> jenaModelToClerezzaTriples(Model model){
		
		ArrayList<Triple> clerezzaTriples = new ArrayList<Triple>();
		
		org.apache.clerezza.commons.rdf.Graph mGraph = jenaModelToClerezzaGraph(model);
		
		Iterator<Triple> tripleIterator = mGraph.iterator();
		while(tripleIterator.hasNext()){
			Triple triple = tripleIterator.next();
			clerezzaTriples.add(triple);
		}
		
		return clerezzaTriples;
		
	}
	
	/**
	 * 
	 * Converts a Jena {@link Model} to Clerezza {@link Graph}.
	 * 
	 * @param model {@link Model}
	 * @return the equivalent Clerezza {@link Graph}.
	 */
	
	public static org.apache.clerezza.commons.rdf.Graph jenaModelToClerezzaGraph(Model model){
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		model.write(out);
		
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		
		ParsingProvider parser = new JenaParserProvider();		
		
		org.apache.clerezza.commons.rdf.Graph mGraph = new SimpleGraph();
		parser.parse(mGraph,in, SupportedFormat.RDF_XML, null);
		
		return mGraph;
		
	}
	
	
	/**
	 * Converts a Clerezza {@link Graph} to a Jena {@link Model}.
	 * 
	 * @param mGraph {@link Graph}
	 * @return the equivalent Jena {@link Model}.
	 */
	public static Model clerezzaGraphToJenaModel(org.apache.clerezza.commons.rdf.Graph mGraph){
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		SerializingProvider serializingProvider = new JenaSerializerProvider();
		
		serializingProvider.serialize(out, mGraph, SupportedFormat.RDF_XML);
		
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		
		Model jenaModel = ModelFactory.createDefaultModel();
		
		jenaModel.read(in, null);
		
		return jenaModel;
		
	}
	
	
	/**
	 * Converts a Clerezza {@link Graph} to a Jena {@link ImmutableGraph}.
	 * 
	 * @param mGraph {@link Graph}
	 * @return the equivalent Jena {@link ImmutableGraph}.
	 */
	public static com.hp.hpl.jena.graph.Graph clerezzaGraphToJenaGraph(org.apache.clerezza.commons.rdf.Graph mGraph){
		
		Model jenaModel = clerezzaGraphToJenaModel(mGraph);
		if(jenaModel != null){
			return jenaModel.getGraph();
		}
		else{
			return null;
		}
		
	}
	
}
