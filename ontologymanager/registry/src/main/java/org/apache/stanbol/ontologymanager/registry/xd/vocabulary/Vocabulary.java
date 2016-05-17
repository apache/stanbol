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
package org.apache.stanbol.ontologymanager.registry.xd.vocabulary;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.semanticweb.owlapi.model.IRI;

/**
 * Class Vocabulary
 * 
 * @author Enrico Daga
 * 
 */
public enum Vocabulary {

	ODP("odp", "http://www.ontologydesignpatterns.org/schemas/repository.owl",
			""), XD_QUERY("xdq",
			"http://www.ontologydesignpatterns.org/xd/selection/query.owl", ""),
	/**
	 * Ontology Design Patterns Metadata Vocabulary
	 */
	ODPM("odpm", "http://www.ontologydesignpatterns.org/schemas/meta.owl", ""),
	/**
	 * Ontology Metadata Vocabulary
	 */
	OMV("omv", "http://omv.ontoware.org/2005/05/ontology", ""),
	/**
	 * Friend-Of-A-Friend
	 */
	FOAF("foaf", "http://xmlns.com/foaf/0.1/", ""),
	/**
	 * The Web Ontology Language
	 */
	OWL("owl", "http://www.w3.org/2002/07/owl", ""),
	/**
	 * Simple Knowledge Organization System
	 */
	SKOS("skos", "http://www.w3.org/2008/05/skos", ""),
	/**
	 * eXtensible Markup Language
	 */
	XML("xml", "http://www.w3.org/XML/1998/namespace", ""),
	/**
	 * XML Schema Definition
	 */
	XSD("xsd", "http://www.w3.org/2001/XMLSchema", ""),
	/**
	 * RDFTerm Description Framework
	 */
	RDF("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns", ""),
	/**
	 * RDF Schema
	 */
	RDFs("rdfs", "http://www.w3.org/2000/01/rdf-schema", ""),
	/**
	 * Dublin Core
	 */
	DC("dc", "http://purl.org/dc/elements/1.1/", ""),
	/**
	 * DC Terms
	 */
	DT("dterm", "http://purl.org/dc/terms/", ""),
	/**
	 * Content Pattern Annotation Schema
	 */
	CPA(
			"cpa",
			"http://www.ontologydesignpatterns.org/schemas/cpannotationschema.owl",
			""),
	/**
	 * C-ODO core module
	 */
	CODK("codkernel",
			"http://www.ontologydesignpatterns.org/cpont/codo/codkernel.owl",
			""),
	/**
	 * C-ODO Data module
	 */
	CODD("coddata",
			"http://www.ontologydesignpatterns.org/cpont/codo/coddata.owl", ""),
	/**
	 * C-ODO Solutions module
	 */
	CODS(
			"cods",
			"http://www.ontologydesignpatterns.org/cpont/codo/codsolutions.owl",
			""),
	/**
	 * C-ODO Light root
	 */
	CODL("codlight",
			"http://www.ontologydesignpatterns.org/cpont/codo/codolight.owl",
			""),
	/**
	 * C-ODO Projects module
	 */
	CODP("codprojects",
			"http://www.ontologydesignpatterns.org/cpont/codo/codprojects.owl",
			""),
	/**
	 * C-ODO Tools module
	 */
	CODT("codtools",
			"http://www.ontologydesignpatterns.org/cpont/codo/codtools.owl", ""),
	/**
	 * C-ODO Workflows module
	 */
	CODW(
			"codworkflows",
			"http://www.ontologydesignpatterns.org/cpont/codo/codworkflows.owl",
			""),
	/**
	 * Part-Of content pattern
	 */
	PARTOF("partof", "http://www.ontologydesignpatterns.org/cp/owl/partof.owl",
			""),
	/**
	 * Descriptions and Situations content pattern
	 */
	DESCASIT(
			"descriptionandsituation",
			"http://www.ontologydesignpatterns.org/cp/owl/descriptionandsituation.owl",
			""),
	/**
	 * Intension/Extension content pattern
	 */
	INTEXT(
			"intensionextension",
			"http://www.ontologydesignpatterns.org/cp/owl/intensionextension.owl",
			""),

	/**
	 * Information Objects and Representation Languages content pattern
	 */
	REPRESENTATION(
			"representation",
			"http://www.ontologydesignpatterns.org/cp/owl/informationobjectsandrepresentationlanguages.owl",
			"");

	// This is the preferred prefix
	public final String prefix;
	// This is the standard URI
	public final String uri;
	// This is the location
	public final String url;

	/**
	 * 
	 * @param prefix
	 * @param sUri
	 * @param sUrl
	 */
	Vocabulary(String prefix, String sUri, String sUrl) {
		this.prefix = prefix;
		this.uri = sUri;
		this.url = sUrl.equals("") ? sUri : sUrl;
	}

	/**
	 * 
	 * @return URL
	 */
	public URL getURL() {
		try {
			if (this.url.equals(""))
				return new URL(this.uri);
			return new URL(this.url);
		} catch (MalformedURLException e) {
			// This cannot happen!
			return null;
		}
	}

	/**
	 * 
	 * @return URI
	 */
	public URI getURI() {
		return URI.create(this.uri);
	}

	/**
	 * Default separator is '#'
	 * 
	 * @return URI
	 */
	public URI getURIWithElement(String element) {
		return URI.create(this.uri + "#" + element);
	}

	@Override
	public String toString() {
		return this.prefix;
	}

	/**
	 * 
	 * @param uri
	 * @return String
	 */
	public static String getPrefix(URI uri) {
		for (Vocabulary vocabulary : Vocabulary.values()) {
			if (vocabulary.getURI().equals(uri))
				return vocabulary.toString();
		}
		return "";
	}

	public IRI getIRI() {
		return IRI.create(this.uri);
	}

	/**
	 * @param string
	 * @return
	 */
	public IRI getIRIWithElement(String element) {
		// TODO Auto-generated method stub
		return IRI.create(this.uri + "#" + element);
	}

}
