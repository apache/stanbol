package org.apache.stanbol.contenthub.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.impl.Util;

/**
 * This class is constructed with an rdf model that will be queried and extracts
 * semantically related entities according to the entity type's
 * 
 * @author srdc
 * 
 */
public class ExploreHelper {

	private static final String DBPEDIA_PLACE = "http://dbpedia.org/ontology/place";

	private OntModel entityModel;
	private static final Logger logger = LoggerFactory
			.getLogger(ExploreHelper.class);

	public ExploreHelper(OntModel model) {
		entityModel = model;
	}

	/**
	 * finds the all rdf:type property value of the entity
	 * 
	 * @return the list of all rdf:type property values
	 */
	public List<String> extractTypes() {
		List<String> types = new ArrayList<String>();

		if (entityModel != null) {

			String queryString = ExploreQueryHelper.entityTypeExtracterQuery();
			ResultSet resultSet = QueryExecutionFactory.create(queryString,
					entityModel).execSelect();

			while (resultSet.hasNext()) {
				QuerySolution solution = resultSet.next();
				RDFNode node = solution.get("type");
				types.add(node.toString());
			}
		} else {
			logger.warn("There is no entity model to query");
		}

		return types;
	}
	
	public Map<String,List<String>> getSuggestedKeywords()
	{
		HashMap<String,List<String>> suggestedKeywords = new HashMap<String, List<String>>();
		
		List<String> place = findRelatedPlaceEntities();
		List<String> person = findRelatedPersonEntities();
		List<String> organization = findRelatedOrganizationEntities();
		
		suggestedKeywords.put("place", place);
		suggestedKeywords.put("organization", organization);
		suggestedKeywords.put("person", person);
		
		return suggestedKeywords;
		
 	}

	public List<String> findRelatedPlaceEntities() {
		List<String> result = new ArrayList<String>();

		if (entityModel != null) {
			String query = ExploreQueryHelper.relatedPlaceQuery();
			ResultSet resultSet = QueryExecutionFactory.create(query,
					entityModel).execSelect();

			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();
				String[] variables = ExploreQueryHelper.placeTypedProperties;

				for (int i = 0; i < variables.length; i++) {
					String variable = variables[i];
					RDFNode resultNode = sol.get(variable);
					if (resultNode != null) {
						String resultURI = resultNode.toString();
						String entityName = resultURI.substring(Util
								.splitNamespace(resultURI));

						if (entityName != null && !entityName.equals("")) {
							result.add(entityName);
						}
					}

				}

			}
		}
		
		return result;
	}
	
	public List<String> findRelatedPersonEntities() {
		List<String> result = new ArrayList<String>();

		if (entityModel != null) {
			String query = ExploreQueryHelper.relatedPersonQuery();
			ResultSet resultSet = QueryExecutionFactory.create(query,
					entityModel).execSelect();

			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();
				String[] variables = ExploreQueryHelper.personTypedProperties;

				for (int i = 0; i < variables.length; i++) {
					String variable = variables[i];
					RDFNode resultNode = sol.get(variable);
					if (resultNode != null) {
						String resultURI = resultNode.toString();
						String entityName = resultURI.substring(Util
								.splitNamespace(resultURI));

						if (entityName != null && !entityName.equals("")) {
							result.add(entityName);
						}
					}

				}

			}
		}
		return result;
	}
	
	public List<String> findRelatedOrganizationEntities() {
		List<String> result = new ArrayList<String>();

		if (entityModel != null) {
			String query = ExploreQueryHelper.relatedOrganizationQuery();
			ResultSet resultSet = QueryExecutionFactory.create(query,
					entityModel).execSelect();

			while (resultSet.hasNext()) {
				QuerySolution sol = resultSet.next();
				String[] variables = ExploreQueryHelper.organizationTypedProperties;

				for (int i = 0; i < variables.length; i++) {
					String variable = variables[i];
					RDFNode resultNode = sol.get(variable);
					if (resultNode != null) {
						String resultURI = resultNode.toString();
						String entityName = resultURI.substring(Util
								.splitNamespace(resultURI));

						if (entityName != null && !entityName.equals("")) {
							result.add(entityName);
						}
					}

				}

			}
		}
		return result;
	}

}
