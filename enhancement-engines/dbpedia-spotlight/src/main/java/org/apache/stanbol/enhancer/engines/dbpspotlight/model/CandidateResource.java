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

import java.util.Collection;
import java.util.HashSet;

import org.apache.clerezza.commons.rdf.IRI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import org.apache.clerezza.commons.rdf.RDFTerm;

/**
 * Stores the candidate ressources given by DBPedia Spotlight Candidates.
 * 
 * @author <a href="mailto:iavor.jelev@babelmonkeys.com">Iavor Jelev</a>
 */
public class CandidateResource {

	public String label;
	public String localName;
	public double contextualScore;
	public double percentageOfSecondRank;
	public double support;
	public double priorScore;
	public double finalScore;

	public String toString() {
		return String
				.format("[label=%s, uri=%s, contextualScore=%d, percentageOfSecondRank=%d, contextualScore=%d, "
						+ "percentageOfSecondRank=%d, contextualScore=%d]",
						label, localName, contextualScore, percentageOfSecondRank,
						support, priorScore, finalScore);
	}
	
	public IRI getUri(){
	    return new IRI(new StringBuilder("http://dbpedia.org/resource/")
	    .append(localName).toString());
	}
	
	/**
	 * This method creates the Collection of surface forms, which the method
	 * <code>createEnhancement</code> adds to the meta data of the content item
	 * as TextAnnotations.
	 * 
	 * @param nList
	 *            NodeList of all Resources contained in the XML response from
	 *            DBpedia Spotlight
	 * @return a Collection<DBPSLSurfaceForm> with all annotations
	 */
	public static Collection<SurfaceForm> parseCandidates(Document xmlDoc) {
		NodeList nList = getElementsByTagName(xmlDoc,"surfaceForm");
		Collection<SurfaceForm> dbpslAnnos = new HashSet<SurfaceForm>();

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Element node = (Element) nList.item(temp);
			SurfaceForm dbpslann = SurfaceForm.parseSerfaceForm(node);

			NodeList resources = node.getChildNodes();

			for (int count = 0; count < resources.getLength(); count++) {
				Node n = resources.item(count);
				if (n instanceof Element) {
					Element r = (Element) n;
					CandidateResource resource = new CandidateResource();
					resource.label = r.getAttribute("label");
					resource.localName = r.getAttribute("uri");
					resource.contextualScore = (new Double(
							r.getAttribute("contextualScore"))).doubleValue();
					resource.percentageOfSecondRank = (new Double(
							r.getAttribute("percentageOfSecondRank")))
							.doubleValue();
					resource.support = (new Double(r.getAttribute("support")))
							.doubleValue();
					resource.priorScore = (new Double(
							r.getAttribute("priorScore"))).doubleValue();
					resource.finalScore = (new Double(
							r.getAttribute("finalScore"))).doubleValue();
					dbpslann.resources.add(resource);
				}

				// Element r = (Element) resources.item(count);
			}

			dbpslAnnos.add(dbpslann);
		}

		return dbpslAnnos;
	}
}
