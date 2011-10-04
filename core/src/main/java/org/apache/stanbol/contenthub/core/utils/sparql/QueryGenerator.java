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

package org.apache.stanbol.contenthub.core.utils.sparql;

/**
 * 
 * @author cihan
 * 
 */
public class QueryGenerator {

    public static final String getExternalPlacesQuery() {
        /*
         * String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n" +
         * "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n" +
         * "PREFIX dc:   <http://purl.org/dc/terms/>\n" + "SELECT distinct ?ref \n" + "WHERE {\n" +
         * "  ?enhancement a fise:EntityAnnotation .\n" + "  ?enhancement dc:relation ?textEnh.\n" +
         * "  ?enhancement fise:entity-label ?label.\n" + "  ?textEnh a fise:TextAnnotation .\n" +
         * "  ?enhancement fise:entity-type ?type.\n" + "  ?enhancement2 dc:relation ?textEnh.\n" +
         * "  ?enhancement2 fise:entity-type ?type.\n" + "  ?enhancement fise:entity-reference ?ref.\n" +
         * "FILTER sameTerm(?type, <http://dbpedia.org/ontology/Place>) }\n" +
         * "ORDER BY DESC(?extraction_time)";
         */

        String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
                       + "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n"
                       + "PREFIX dc:   <http://purl.org/dc/terms/>\n" + "SELECT distinct ?ref \n"
                       + "WHERE {\n" + "  ?enhancement a fise:EntityAnnotation .\n"
                       + "  ?enhancement dc:relation ?textEnh.\n"
                       + "  ?enhancement fise:entity-label ?label.\n"
                       + "  ?textEnh a fise:TextAnnotation .\n" + "  ?enhancement fise:entity-type ?type.\n"
                       + "  ?enhancement fise:entity-reference ?ref.\n"
                       + "FILTER sameTerm(?type, <http://dbpedia.org/ontology/Place>) }\n"
                       + "ORDER BY DESC(?extraction_time)";

        return query;
    }
    
    public static final String getEnhancementsOfContent(String contentID) {
        String enhancementQuery = "PREFIX enhancer: <http://fise.iks-project.eu/ontology/> "
                + "SELECT DISTINCT ?enhancement WHERE { "
                + "  ?enhancement enhancer:extracted-from ?enhID . "
                + "  FILTER sameTerm(?enhID, <" + contentID + ">) } ";
        return enhancementQuery;
    }
    
    public static final String getRecentlyEnhancedDocuments(int pageSize, int offset) {
        String query = "PREFIX enhancer: <http://fise.iks-project.eu/ontology/> "
                   + "PREFIX dc:   <http://purl.org/dc/terms/> " + "SELECT DISTINCT ?content WHERE { "
                   + "  ?enhancement enhancer:extracted-from ?content ."
                   + "  ?enhancement dc:created ?extraction_time . } "
                   + "ORDER BY DESC(?extraction_time) LIMIT %d OFFSET %d";
        return String.format(query, pageSize, offset);
    }
}
