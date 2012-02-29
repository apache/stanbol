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

package org.apache.stanbol.contenthub.store.solr.util;

import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary.SolrFieldName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.sinaci
 * 
 */
public class QueryGenerator {

	private static final Logger logger = LoggerFactory
			.getLogger(QueryGenerator.class);

	public static final String getFieldQuery(SolrFieldName fieldName) {
		switch (fieldName) {
		case PLACES:
			return getPlaceEntities();
		case PEOPLE:
			return getPersonEntities();
		case ORGANIZATIONS:
			return getOrganizationEntities();
		case COUNTRIES:
			return getCountries();
		case IMAGECAPTIONS:
			return getImageCaptions();
		case REGIONS:
			return getRegions();
		case GOVERNORS:
			return getGovernors();
		case CAPITALS:
			return getCapitals();
		case LARGESTCITIES:
			return getLargestCities();
		case LEADERNAMES:
			return getLeaderNames();
		case GIVENNAMES:
			return getGivenNames();
		case KNOWNFORS:
			return getKnownFors();
		case BIRTHPLACES:
			return getBirthPlaces();
		case PLACEOFBIRTHS:
			return getPlacesOfBirths();
		case WORKINSTITUTIONS:
			return getWorkInstitutions();
		case CAPTIONS:
			return getCaptions();
		case SHORTDESCRIPTIONS:
			return getShortDescriptions();
		case FIELDS:
			return getFields();
		default:
			logger.error("No SPARQL query for the fieldName: {}",
					fieldName.toString());
			return null;
		}
	}

	public static final String getPlaceEntities() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "SELECT DISTINCT ?"
				+ SolrFieldName.PLACES.toString()
				+ " WHERE { \n"
				+ " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-type <http://dbpedia.org/ontology/Place>.\n"
				+ " ?enhancement fise:entity-label ?"
				+ SolrFieldName.PLACES.toString() + ".\n" + "}\n";
		return query;
	}

	public static final String getPersonEntities() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "SELECT DISTINCT ?"
				+ SolrFieldName.PEOPLE.toString()
				+ " WHERE { \n"
				+ " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-type <http://dbpedia.org/ontology/Person>.\n"
				+ " ?enhancement fise:entity-label ?"
				+ SolrFieldName.PEOPLE.toString() + ".\n" + "}\n";
		return query;
	}

	public static final String getOrganizationEntities() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "SELECT DISTINCT ?"
				+ SolrFieldName.ORGANIZATIONS.toString()
				+ " WHERE { \n"
				+ " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-type <http://dbpedia.org/ontology/Organisation>.\n"
				+ " ?enhancement fise:entity-label ?"
				+ SolrFieldName.ORGANIZATIONS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getCountries() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbont: <http://dbpedia.org/ontology/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.COUNTRIES.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbont:country ?"
				+ SolrFieldName.COUNTRIES.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getImageCaptions() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.IMAGECAPTIONS.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:imageCaption ?"
				+ SolrFieldName.IMAGECAPTIONS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getRegions() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.REGIONS.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:region ?" + SolrFieldName.REGIONS.toString()
				+ ".\n" + "}\n";
		return query;
	}

	private static final String getGovernors() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.GOVERNORS.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:governor ?"
				+ SolrFieldName.GOVERNORS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getCapitals() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.CAPITALS.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:capital ?"
				+ SolrFieldName.CAPITALS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getLargestCities() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.LARGESTCITIES.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:largestCity ?"
				+ SolrFieldName.LARGESTCITIES.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getLeaderNames() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.LEADERNAMES.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:leaderName ?"
				+ SolrFieldName.LEADERNAMES.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getGivenNames() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.GIVENNAMES.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity foaf:givenName ?"
				+ SolrFieldName.GIVENNAMES.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getKnownFors() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.KNOWNFORS.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:knownFor ?"
				+ SolrFieldName.KNOWNFORS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getBirthPlaces() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbont: <http://dbpedia.org/ontology/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.BIRTHPLACES.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbont:birthPlace ?"
				+ SolrFieldName.BIRTHPLACES.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getPlacesOfBirths() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.PLACEOFBIRTHS.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:placeOfBirth ?"
				+ SolrFieldName.PLACEOFBIRTHS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getWorkInstitutions() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?"
				+ SolrFieldName.WORKINSTITUTIONS.toString() + " WHERE { \n"
				+ " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:workInstitutions ?"
				+ SolrFieldName.WORKINSTITUTIONS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getCaptions() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.CAPTIONS.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:caption ?"
				+ SolrFieldName.CAPTIONS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getShortDescriptions() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbprop: <http://dbpedia.org/property/>\n"
				+ "SELECT DISTINCT ?"
				+ SolrFieldName.SHORTDESCRIPTIONS.toString() + " WHERE { \n"
				+ " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbprop:shortDescription ?"
				+ SolrFieldName.SHORTDESCRIPTIONS.toString() + ".\n" + "}\n";
		return query;
	}

	private static final String getFields() {
		String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
				+ "PREFIX dc: <http://purl.org/dc/terms/>\n"
				+ "PREFIX dbont: <http://dbpedia.org/ontology/>\n"
				+ "SELECT DISTINCT ?" + SolrFieldName.FIELDS.toString()
				+ " WHERE { \n" + " ?enhancement a fise:EntityAnnotation.\n"
				+ " ?enhancement dc:relation ?textEnh.\n"
				+ " ?textEnh a fise:TextAnnotation.\n"
				+ " ?enhancement fise:entity-reference ?entity.\n"
				+ " ?entity dbont:field ?" + SolrFieldName.FIELDS.toString()
				+ ".\n" + "}\n";
		return query;
	}

	/*
	 * public static final String getExternalPlacesQuery() {
	 * 
	 * String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n" +
	 * "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" +
	 * "PREFIX dc:   <http://purl.org/dc/terms/>\n" + "SELECT distinct ?ref \n"
	 * + "WHERE {\n" + "  ?enhancement a fise:EntityAnnotation .\n" +
	 * "  ?enhancement dc:relation ?textEnh.\n" +
	 * "  ?enhancement fise:entity-label ?label.\n" +
	 * "  ?textEnh a fise:TextAnnotation .\n" +
	 * "  ?enhancement fise:entity-type ?type.\n" +
	 * "  ?enhancement fise:entity-reference ?ref.\n" +
	 * "FILTER sameTerm(?type, <http://dbpedia.org/ontology/Place>) }\n" +
	 * "ORDER BY DESC(?extraction_time)";
	 * 
	 * return query; }
	 */

	public static final String getEnhancementsOfContent(String contentID) {
		String enhancementQuery = "PREFIX fise: <http://fise.iks-project.eu/ontology/> "
				+ "SELECT DISTINCT ?enhID WHERE { "
				+ "  { ?enhID fise:extracted-from ?contentID . } UNION "
				+ "  { ?enhancement fise:extracted-from ?contentID . "
				+ "		 ?enhancement a fise:EntityAnnotation . "
				+ "		 ?enhancement fise:entity-reference ?enhID . } "
				+ "    FILTER sameTerm(?contentID, <" + contentID + ">) " + "}";
		return enhancementQuery;
	}

	/*
	 * public static final String getRecentlyEnhancedDocuments(int pageSize, int
	 * offset) { String query =
	 * "PREFIX enhancer: <http://fise.iks-project.eu/ontology/> " +
	 * "PREFIX dc:   <http://purl.org/dc/terms/> " +
	 * "SELECT DISTINCT ?content WHERE { " +
	 * "  ?enhancement enhancer:extracted-from ?content ." +
	 * "  ?enhancement dc:created ?extraction_time . } " +
	 * "ORDER BY DESC(?extraction_time) LIMIT %d OFFSET %d"; return
	 * String.format(query, pageSize, offset); }
	 */
}
