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
import java.util.HashSet;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


//import org.apache.clerezza.commons.rdf.RDFTerm;

/**
 * Stores the surface forms given by DBPedia Spotlight Candidates.
 * 
 * @author <a href="mailto:iavor.jelev@babelmonkeys.com">Iavor Jelev</a>
 */
public class SurfaceForm {

	public final String name;
	public String type;
	public final Integer offset;
	public final List<CandidateResource> resources = new ArrayList<CandidateResource>();

	public SurfaceForm(Integer offset, String name){
	    if(name == null || offset == null){
	        throw new IllegalArgumentException("Offset and name MUST NOT be NULL");
	    }
	    this.name = name;
	    this.offset = offset;
	}
	
	@Override
	public int hashCode() {
	    return name.hashCode()+offset.hashCode();
	}
	@Override
	public boolean equals(Object o) {
	    if(o instanceof SurfaceForm && name.equals(((SurfaceForm)o).name) &&
	            offset.equals(((SurfaceForm)o).offset)){
	        return (type == null && ((SurfaceForm)o).type == null) ||
	                (type != null && type.equals(((SurfaceForm)o).type));
	    } else {
	        return false;
	    }
	}
	
	public String toString() {
		return String.format("[name=%s, offset=%i, type=%s]", name, offset,
				type);
	}
	
	/**
	 * Parses all {@link SurfaceForm} data from the parsed XML document
	 * 
	 * @param xmlDoc
	 *            The XML Document containing the surface forms
	 * @return a Collection<DBPSLSurfaceForm> with all annotations
	 */
	public static Collection<SurfaceForm> parseSurfaceForm(Document xmlDoc) {
		NodeList nList = getElementsByTagName(xmlDoc,"surfaceForm");
		Collection<SurfaceForm> dbpslAnnos = new HashSet<SurfaceForm>();

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Element node = (Element) nList.item(temp);
			SurfaceForm dbpslann = parseSerfaceForm(node);

			dbpslAnnos.add(dbpslann);
		}

		return dbpslAnnos;
	}

	protected static SurfaceForm parseSerfaceForm(Element node) {
		SurfaceForm dbpslann = new SurfaceForm(
		    new Integer(node.getAttribute("offset")),
		    node.getAttribute("name"));
		dbpslann.type = node.getAttribute("type");
		return dbpslann;
	}
	
	
}
