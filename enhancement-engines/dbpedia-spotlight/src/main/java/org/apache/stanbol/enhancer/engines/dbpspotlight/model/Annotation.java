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
package org.apache.stanbol.enhancer.engines.dbpspotlight.model;

import static org.apache.stanbol.enhancer.engines.dbpspotlight.utils.XMLParser.getElementsByTagName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Contains a result given by DBPedia Spotlight..
 * 
 * 
 * @author <a href="mailto:iavor.jelev@babelmonkeys.com">Iavor Jelev</a>
 */
public class Annotation {

	/*
	 * TODO (Note by rwesten 2012-08-22) 
	 * 
	 * Added here functionality to extract DBpedia
	 * Ontoloty types for Annotations. This is mainly to
	 * choose the best dc:type for fise:TextAnnotations
	 * created for Annotation.
	 * 
	 * This is based on the assumption that the most generic
	 * dbpedia type is always the last one in the returned list.
	 * 
	 * In addition "DBpedia:TopicalConcept" is ignored first
	 * as it seams not to be used by dbpedia.org and second
	 * because it is always parsed last (even after schema
	 * and freebase types) and would therefore be considered
	 * as the most generic dbpedia type.
	 * 
	 * I do not like this solution and would like to find
	 * a better solution for that
	 */
	/**
	 * Allows to add DBpedia Ontology types that should be
	 * ignored by {@link #getDbpediaTypeNames()}.<p>
	 * Introduced this to ignore the "TopicalConcept"
	 * type.
	 */
	public static final Set<String> IGNORED_DBP_TYPES;
	static {
		Set<String> ignored = new HashSet<String>();
		ignored.add("DBpedia:TopicalConcept");
		IGNORED_DBP_TYPES = Collections.unmodifiableSet(ignored);
	}
	
	public RDFTerm uri;
	//TODO: change this to a list with the parsed types
	//      Processing of XML results should be done during parsing
	public String types;
	public Integer support;
	//NOTE rwesten: changed this to embed a SurfaceFrom so that i
	//     can reuse code for creating fise:TextAnnotations
	public SurfaceForm surfaceForm;
	public Double similarityScore;
	public Double percentageOfSecondRank;

	public List<String> getTypeNames() {
		if (types != null) {
			List<String> t = new ArrayList<String>();
			String[] typex = types.split(",");
			for (String type : typex) {
				// make the returned types referenceable
				String deref = type.replace("DBpedia:", "http://dbpedia.org/ontology/")
						.replace("Freebase:", "http://www.freebase.com/schema")
						.replace("Schema:", "http://www.schema.org/");
				if(!deref.isEmpty()){
					t.add(deref);
				}
			}
			return t;
		}
		return Collections.emptyList();
	}
	
	/**
	 * Getter for the dbpedia ontology types excluding {@link #IGNORED_DBP_TYPES}
	 * @return the types or an empty list if none
	 */
	public List<String> getDbpediaTypeNames(){
		if (types != null) {
			List<String> t = new ArrayList<String>();
			String[] typex = types.split(",");
			for (String type : typex) {
				if(!IGNORED_DBP_TYPES.contains(type) && type.startsWith("DBpedia:")){
					t.add(type.replace("DBpedia:", "http://dbpedia.org/ontology/"));
				}
			}
			return t;
		}
		return Collections.emptyList();
	}

	public String toString() {
		return String
				.format("[uri=%s, support=%i, types=%s, surfaceForm=\"%s\", similarityScore=%d, percentageOfSecondRank=%d]",
						uri, support, types, surfaceForm,
						similarityScore, percentageOfSecondRank);
	}

	/**
	 * This method parses allAnnotations from the parsed XML {@link Document}
	 * 
	 * @param xmlDoc
	 *            A XML document containing annotations.
	 * @return a Collection<DBPSLAnnotation> with all annotations
	 */
	public static Collection<Annotation> parseAnnotations(Document xmlDoc) {
		NodeList nList = getElementsByTagName(xmlDoc, "RDFTerm");
		Collection<Annotation> dbpslAnnos = new HashSet<Annotation>();

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Annotation dbpslann = new Annotation();
			Element node = (Element) nList.item(temp);
			dbpslann.uri = new IRI(node.getAttribute("URI"));
			dbpslann.support = (new Integer(node.getAttribute("support")))
					.intValue();
			dbpslann.types = node.getAttribute("types");
			dbpslann.surfaceForm = new SurfaceForm(
			    new Integer(node.getAttribute("offset")),
			    node.getAttribute("surfaceForm"));
			//set the type of the surface form
			List<String> dbpediaTypes = dbpslann.getDbpediaTypeNames();
			if(!dbpediaTypes.isEmpty()){
				//set the last type in the list - the most general one - as type
				//for the surface form
				dbpslann.surfaceForm.type = dbpediaTypes.get(dbpediaTypes.size()-1);
			}
			dbpslann.similarityScore = (new Double(
					node.getAttribute("similarityScore"))).doubleValue();
			dbpslann.percentageOfSecondRank = (new Double(
					node.getAttribute("percentageOfSecondRank"))).doubleValue();

			dbpslAnnos.add(dbpslann);
		}

		return dbpslAnnos;
	}

}
