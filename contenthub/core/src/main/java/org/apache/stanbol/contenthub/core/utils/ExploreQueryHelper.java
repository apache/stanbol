package org.apache.stanbol.contenthub.core.utils;

import java.util.List;

/**
 * Includes static methods that returns SPARQL query strings Queries are
 * executed on graph of entities to find their types and extract semantic
 * information according to entity type's
 * 
 * @author srdc
 * 
 */

public class ExploreQueryHelper {

	public final static String[] placeTypedProperties = { "country",
			"largestCity", "city", "state", "capital", "isPartOf", "part",
			"deathPlace", "birthPlace", "location" };

	public final static String[] personTypedProperties = { "leader",
			"leaderName", "child", "spouse", "partner", "president" };

	public final static String[] organizationTypedProperties = { "leaderParty",
			"affiliation", "team", "party", "otherParty", "associatedBand" };

	/**
	 * Used to find all rdf:type's of the entity
	 * 
	 * @return is SPARQL query finds rdf:type's of an entity
	 */
	public final static String entityTypeExtracterQuery() {
		String query = "PREFIX j.3:<http://www.iks-project.eu/ontology/rick/model/>\n"
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "SELECT DISTINCT ?type\n"
				+ "WHERE {\n"
				+ "?entity j.3:about ?description.\n"
				+ "?description rdf:type ?type\n" + "}\n";
		return query;
	}

	/**
	 * Creates a query which finds place type entities; <br> country <br> capital <br>
	 * largestCity <br> isPartOf <br> part <br> birthPlace <br> deathPlace <br> location <br> ...
	 * optionally
	 * 
	 * @return resulted query
	 */
	public final static String relatedPlaceQuery() {
		StringBuilder query = new StringBuilder(
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
		query.append("PREFIX dbp.ont: <http://dbpedia.org/ontology/>\n");
		query.append("PREFIX about.ns: <http://www.iks-project.eu/ontology/rick/model/>\n");
		query.append("SELECT DISTINCT ");

		for (int i = 0; i < placeTypedProperties.length; i++) {
			query.append(" ?" + placeTypedProperties[i]);
		}
		query.append(" \n"
				+ "WHERE {\n ?entity about.ns:about ?description .\n");

		for (int i = 0; i < placeTypedProperties.length; i++) {
			String var = placeTypedProperties[i];
			query.append("OPTIONAL { ?description dbp.ont:" + var + " ?" + var
					+ " }\n");
		}

		query.append("}\n");
		return query.toString();
	}
	
	/**
	 * creates a query that finds the person typed entities;
	 * <br> president
	 * <br> spouse
	 * <br> leader
	 * <br> ... optionally
	 * @return resulted query string
	 */
	public final static String relatedPersonQuery() {
		StringBuilder query = new StringBuilder(
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
		query.append("PREFIX dbp.ont: <http://dbpedia.org/ontology/>\n");
		query.append("PREFIX about.ns: <http://www.iks-project.eu/ontology/rick/model/>\n");
		query.append("SELECT DISTINCT ");

		for (int i = 0; i < personTypedProperties.length; i++) {
			query.append(" ?" + personTypedProperties[i]);
		}
		query.append(" \n"
				+ "WHERE {\n ?entity about.ns:about ?description .\n");

		for (int i = 0; i < personTypedProperties.length; i++) {
			String var = personTypedProperties[i];
			query.append("OPTIONAL { ?description dbp.ont:" + var + " ?" + var
					+ " }\n");
		}

		query.append("}\n");
		return query.toString();

	}
	
	/**
	 * creates a query that finds organization typed related entities;
	 * <br> associatedBand
	 * <br> team
	 * <br> party
	 * <br> ... optionally
	 * @return resulted query String
	 */
	public final static String relatedOrganizationQuery() {
		StringBuilder query = new StringBuilder(
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
		query.append("PREFIX dbp.ont: <http://dbpedia.org/ontology/>\n");
		query.append("PREFIX about.ns: <http://www.iks-project.eu/ontology/rick/model/>\n");
		query.append("SELECT DISTINCT ");

		for (int i = 0; i < personTypedProperties.length; i++) {
			query.append(" ?" + personTypedProperties[i]);
		}
		query.append(" \n"
				+ "WHERE {\n ?entity about.ns:about ?description .\n");

		for (int i = 0; i < personTypedProperties.length; i++) {
			String var = personTypedProperties[i];
			query.append("OPTIONAL { ?description dbp.ont:" + var + " ?" + var
					+ " }\n");
		}

		query.append("}\n");
		return query.toString();

	}

}
