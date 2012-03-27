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
package org.apache.stanbol.cmsadapter.web.utils;

public class RestURIHelper {
	private static final String ONTOLOGY = "ontology";
	private static final String CLASSES = "classes";
	private static final String INDIVIDUALS = "individuals";
	private static final String DATATYPE_PROPERTIES = "datatypeProperties";
	private static final String OBJECT_PROPERTIES = "objectProperties";

	public static String getOntologyHref(String ontologyURI) {
		return ONTOLOGY + "/" + ontologyURI;
	}

	public static String getClassHref(String ontologyURI, String classURI) {
		return getOntologyHref(ontologyURI) + "/" + CLASSES + "/"
				+ expelLastSharp(classURI);
	}

	public static String getObjectPropertyHref(String ontologyURI,
			String objectPropertyURI) {
		return getOntologyHref(ontologyURI) + "/" + OBJECT_PROPERTIES + "/"
				+ expelLastSharp(objectPropertyURI);
	}

	public static String getDatatypePropertyHref(String ontologyURI,
			String datatypePropertyURI) {
		return getOntologyHref(ontologyURI) + "/" + DATATYPE_PROPERTIES + "/"
				+ expelLastSharp(datatypePropertyURI);
	}

	public static String getIndividualHref(String ontologyURI,
			String individualURI) {
		return getOntologyHref(ontologyURI) + "/" + INDIVIDUALS + "/"
				+ expelLastSharp(individualURI);
	}

	private static String expelLastSharp(String targetStr) {
		return targetStr.substring(0, targetStr.lastIndexOf('#')) + "/"
				+ targetStr.substring(targetStr.lastIndexOf('#') + 1);
	}
}
