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

/**
 * Includes static methods that returns SPARQL query strings Queries are executed on graph of entities to find
 * their types and extract semantic information according to entity type's
 * 
 * @author srdc
 * 
 */

public class ExploreQueryHelper {

    /**
     * dbpedia-owl:place ranged properties for related places
     */
    public final static String[] placeTypedProperties = {"country", "largestCity", "city", "state",
                                                         "capital", "isPartOf", "part", "deathPlace",
                                                         "birthPlace", "location"};

    /**
     * dbpedia-owl:person ranged properties for related persons
     */
    public final static String[] personTypedProperties = {"leader", "leaderName", "child", "spouse",
                                                          "partner", "president"};

    /**
     * dbpedia-owl:organization ranged properties for related organizations
     */
    public final static String[] organizationTypedProperties = {"leaderParty", "affiliation", "team",
                                                                "party", "otherParty", "associatedBand"};

    /**
     * Used to find all rdf:type's of the entity
     * 
     * @return is SPARQL query finds rdf:type's of an entity
     */
    public final static String entityTypeExtracterQuery() {
        String query = "PREFIX j.3:<http://www.iks-project.eu/ontology/rick/model/>\n"
                       + "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                       + "SELECT DISTINCT ?type\n" + "WHERE {\n" + "?entity j.3:about ?description.\n"
                       + "?description rdf:type ?type\n" + "}\n";
        return query;
    }

    /**
     * Creates a query which finds place type entities; <br>
     * country <br>
     * capital <br>
     * largestCity <br>
     * isPartOf <br>
     * part <br>
     * birthPlace <br>
     * deathPlace <br>
     * location <br>
     * ... optionally
     * 
     * @return resulted query
     */
    public final static String relatedPlaceQuery() {
        StringBuilder query = new StringBuilder("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        query.append("PREFIX dbp.ont: <http://dbpedia.org/ontology/>\n");
        query.append("PREFIX about.ns: <http://www.iks-project.eu/ontology/rick/model/>\n");
        query.append("SELECT DISTINCT ");

        for (int i = 0; i < placeTypedProperties.length; i++) {
            query.append(" ?" + placeTypedProperties[i]);
        }
        query.append(" \n" + "WHERE {\n ?entity about.ns:about ?description .\n");

        for (int i = 0; i < placeTypedProperties.length; i++) {
            String var = placeTypedProperties[i];
            query.append("OPTIONAL { ?description dbp.ont:" + var + " ?" + var + " }\n");
        }

        query.append("}\n");
        return query.toString();
    }

    /**
     * creates a query that finds the person typed entities; <br>
     * president <br>
     * spouse <br>
     * leader <br>
     * ... optionally
     * 
     * @return resulted query string
     */
    public final static String relatedPersonQuery() {
        StringBuilder query = new StringBuilder("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        query.append("PREFIX dbp.ont: <http://dbpedia.org/ontology/>\n");
        query.append("PREFIX about.ns: <http://www.iks-project.eu/ontology/rick/model/>\n");
        query.append("SELECT DISTINCT ");

        for (int i = 0; i < personTypedProperties.length; i++) {
            query.append(" ?" + personTypedProperties[i]);
        }
        query.append(" \n" + "WHERE {\n ?entity about.ns:about ?description .\n");

        for (int i = 0; i < personTypedProperties.length; i++) {
            String var = personTypedProperties[i];
            query.append("OPTIONAL { ?description dbp.ont:" + var + " ?" + var + " }\n");
        }

        query.append("}\n");
        return query.toString();

    }

    /**
     * creates a query that finds organization typed related entities; <br>
     * associatedBand <br>
     * team <br>
     * party <br>
     * ... optionally
     * 
     * @return resulted query String
     */
    public final static String relatedOrganizationQuery() {
        StringBuilder query = new StringBuilder("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n");
        query.append("PREFIX dbp.ont: <http://dbpedia.org/ontology/>\n");
        query.append("PREFIX about.ns: <http://www.iks-project.eu/ontology/rick/model/>\n");
        query.append("SELECT DISTINCT ");

        for (int i = 0; i < organizationTypedProperties.length; i++) {
            query.append(" ?" + organizationTypedProperties[i]);
        }
        query.append(" \n" + "WHERE {\n ?entity about.ns:about ?description .\n");

        for (int i = 0; i < organizationTypedProperties.length; i++) {
            String var = organizationTypedProperties[i];
            query.append("OPTIONAL { ?description dbp.ont:" + var + " ?" + var + " }\n");
        }

        query.append("}\n");
        return query.toString();

    }

    /**
     * finds and returns the index of the location of <br>
     * - last occurence of # , if fails <br>
     * - last occurence of / , if fails <br>
     * - last occurence of : , if fails length of the string, if string is null, then returns -1;
     * 
     * @param URI
     *            is the URI that whose namespace will be splitted
     * @return is the index of valid splitter
     */
    public static int splitNameSpaceFromURI(String URI) {
        int index = -1;

        index = URI.lastIndexOf("#");
        if (index != -1) return index + 1;

        index = URI.lastIndexOf("/");
        if (index != -1) return index + 1;

        index = URI.lastIndexOf(":");
        if (index != -1) return index + 1;

        index = URI.length();
        return index;
    }

}
