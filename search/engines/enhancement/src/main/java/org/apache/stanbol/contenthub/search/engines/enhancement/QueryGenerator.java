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

package org.apache.stanbol.contenthub.search.engines.enhancement;

/**
 * 
 * @author cihan
 * 
 */
public class QueryGenerator {

    public static final String keywordBasedAnnotationQuery(String keyword) {
        String query = "PREFIX fise: <http://fise.iks-project.eu/ontology/>\n"
                       + "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#>\n"
                       + "PREFIX dc:   <http://purl.org/dc/terms/>\n" + "PREFIX crv: <http://cms.item#>\n"
                       + "SELECT distinct ?label ?score ?document ?type ?text ?ref ?refscore ?path\n"
                       + "WHERE {\n" + "  ?enhancement a fise:EntityAnnotation .\n"
                       + "  ?enhancement dc:relation ?textEnh.\n"
                       + "  ?enhancement fise:entity-label ?label.\n"
                       + "  ?textEnh a fise:TextAnnotation .\n"
                       + "  ?textEnh fise:extracted-from ?document .\n"
                       + "  ?enhancement fise:entity-type ?type.\n"
                       + "  ?enhancement fise:entity-reference ?ref.\n"
                       + "  ?enhancement fise:confidence ?refscore.\n"
                       + "  ?textEnh fise:selection-context ?text.\n" + "  (?label ?score)  pf:textMatch '+"
                       + normalizeKeyword(keyword) + "'. \n" + "OPTIONAL {?enhancement crv:path ?path} \n"
                       + "}\n" + "ORDER BY DESC(?extraction_time)";
        return query;
    }

    private static String normalizeKeyword(String keyword) {
        return keyword.replace("'", "");
    }

}
