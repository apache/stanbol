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
package org.apache.stanbol.contenthub.core.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * This class is constructed with an rdf model that will be queried and extracts semantically related entities
 * according to the entity type's
 * 
 * @author srdc
 * 
 */
public class ExploreHelper {

    private OntModel entityModel;
    private static final Logger logger = LoggerFactory.getLogger(ExploreHelper.class);

    public ExploreHelper(OntModel model) {
        if (model == null) {
            logger.warn("Given Entity Model is empty, ExploreHelper could NOT be initialized");
        } else entityModel = model;
    }

    /**
     * finds the all rdf:type property value of the entity
     * 
     * @return the list of all rdf:type property values; if there is no, returns an empty set
     */
    public List<String> extractTypes() {
        List<String> types = new ArrayList<String>();

        if (entityModel != null) {

            String queryString = ExploreQueryHelper.entityTypeExtracterQuery();
            ResultSet resultSet = QueryExecutionFactory.create(queryString, entityModel).execSelect();

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

    /**
     * Finds the semantically related entity names, while doing this, categorizes the related entities
     * according to their type, for now finds; <br>
     * - related places <br>
     * - related persons <br>
     * - related organizations
     * 
     * @return the Map of Type Category Name of the Entity - Set of Related Entities of that Type
     */
    public Map<String,Set<String>> getSuggestedKeywords() {
        HashMap<String,Set<String>> suggestedKeywords = new HashMap<String,Set<String>>();

        Set<String> place = findRelatedPlaceEntities();
        Set<String> person = findRelatedPersonEntities();
        Set<String> organization = findRelatedOrganizationEntities();

        suggestedKeywords.put("places", place);
        suggestedKeywords.put("organizations", organization);
        suggestedKeywords.put("persons", person);

        return suggestedKeywords;

    }

    /**
     * finds the semantically related entities of type dbpedia-owl:place
     * 
     * @return the Set of place typed related Entities
     */
    public Set<String> findRelatedPlaceEntities() {
        Set<String> result = new HashSet<String>();

        if (entityModel != null) {
            String query = ExploreQueryHelper.relatedPlaceQuery();
            ResultSet resultSet = QueryExecutionFactory.create(query, entityModel).execSelect();

            while (resultSet.hasNext()) {
                QuerySolution sol = resultSet.next();
                String[] variables = ExploreQueryHelper.placeTypedProperties;

                for (int i = 0; i < variables.length; i++) {
                    String variable = variables[i];
                    RDFNode resultNode = sol.get(variable);
                    if (resultNode != null) {
                        String resultURI;
                        try {
                            resultURI = URLDecoder.decode(resultNode.toString(), "UTF-8");
                            String entityName = resultURI.substring(ExploreQueryHelper
                                    .splitNameSpaceFromURI(resultURI));

                            if (entityName != null && !entityName.equals("")) {
                                result.add(entityName);
                            }
                        } catch (UnsupportedEncodingException e) {
                            logger.error("Unsupported encoding for URLDecoder.decode", e);
                        }

                    } else {
                        logger.debug("No binding for the query variable {}", variable);
                    }
                }
            }
        }

        else {
            logger.debug("There is no entity model, so related places could NOT be found");
        }

        return result;
    }

    /**
     * finds the semantically related entities of type dbpedia-owl:person
     * 
     * @return the Set of person typed related Entities
     */
    public Set<String> findRelatedPersonEntities() {
        Set<String> result = new HashSet<String>();

        if (entityModel != null) {
            String query = ExploreQueryHelper.relatedPersonQuery();
            ResultSet resultSet = QueryExecutionFactory.create(query, entityModel).execSelect();

            while (resultSet.hasNext()) {
                QuerySolution sol = resultSet.next();
                String[] variables = ExploreQueryHelper.personTypedProperties;

                for (int i = 0; i < variables.length; i++) {
                    String variable = variables[i];
                    RDFNode resultNode = sol.get(variable);
                    if (resultNode != null) {
                        String resultURI;
                        try {
                            resultURI = URLDecoder.decode(resultNode.toString(), "UTF-8");
                            String entityName = resultURI.substring(ExploreQueryHelper
                                    .splitNameSpaceFromURI(resultURI));

                            if (entityName != null && !entityName.equals("")) {
                                result.add(entityName);
                            }
                        } catch (UnsupportedEncodingException e) {
                            logger.error("Unsupported encoding for URLDecoder.decode", e);
                        }

                    } else {
                        logger.debug("No binding for the query variable {}", variable);
                    }
                }
            }
        }

        else {
            logger.debug("There is no entity model, so related places could NOT be found");
        }

        return result;
    }

    /**
     * finds the semantically related entities of type dbpedia-owl:organization
     * 
     * @return the Set of organization typed related Entities
     */
    public Set<String> findRelatedOrganizationEntities() {
        Set<String> result = new HashSet<String>();

        if (entityModel != null) {
            String query = ExploreQueryHelper.relatedOrganizationQuery();
            ResultSet resultSet = QueryExecutionFactory.create(query, entityModel).execSelect();

            while (resultSet.hasNext()) {
                QuerySolution sol = resultSet.next();
                String[] variables = ExploreQueryHelper.organizationTypedProperties;

                for (int i = 0; i < variables.length; i++) {
                    String variable = variables[i];
                    RDFNode resultNode = sol.get(variable);
                    if (resultNode != null) {
                        String resultURI;
                        try {
                            resultURI = URLDecoder.decode(resultNode.toString(), "UTF-8");
                            String entityName = resultURI.substring(ExploreQueryHelper
                                    .splitNameSpaceFromURI(resultURI));

                            if (entityName != null && !entityName.equals("")) {
                                result.add(entityName);
                            }
                        } catch (UnsupportedEncodingException e) {
                            logger.error("Unsupported encoding for URLDecoder.decode", e);
                        }

                    } else {
                        logger.debug("No binding for the query variable {}", variable);
                    }
                }
            }
        }

        else {
            logger.debug("There is no entity model, so related places could NOT be found");
        }

        return result;
    }

}
