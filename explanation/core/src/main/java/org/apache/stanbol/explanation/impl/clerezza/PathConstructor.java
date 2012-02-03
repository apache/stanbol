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
package org.apache.stanbol.explanation.impl.clerezza;

import java.util.Set;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.apache.stanbol.explanation.api.CompatibilityMapping;

public class PathConstructor {

    public static Set<CompatibilityMapping> computePathCompatibility(UriRef from,
                                                                     UriRef to,
                                                                     Set<TripleCollection> kb,

                                                                     int maxLength) {

        if (maxLength < 1) throw new IllegalArgumentException("Maximum length cannot be lower than 1.");

        StringBuffer sparql = new StringBuffer("SELECT ");

        sparql.append(from == null ? "?y " : "");
        sparql.append(" ?r0 ");

        for (int i = 1; i < maxLength; i++) {
            sparql.append("?x");
            sparql.append(i - 1);
            sparql.append(" ");

            sparql.append(" ?r");
            sparql.append(i);
            sparql.append(" ");
        }

        sparql.append(to == null ? "?z" : "");
        sparql.append(" ");
        

        sparql.append("WHERE { ");

         sparql.append("{ ");

        sparql.append(from == null ? "?y " : from);
        sparql.append(" ");

        sparql.append("?r0 ");

        // We already did it once.
        for (int i = 1; i < maxLength; i++) {
            sparql.append("?x");
            sparql.append(i - 1);
            if (i > 1) sparql.append(" } ");
            sparql.append(" OPTIONAL { ?x");
            sparql.append(i - 1);
            sparql.append(" ?r");
            sparql.append(i);
            sparql.append(" ");
        }

        sparql.append(to == null ? "?z" : to);

        sparql.append(maxLength > 1 ? " } " : " ");

         sparql.append(" } UNION { ");
        
         sparql.append(to == null ? "?z" : to);
        
         // We already did it once.
         for (int i = maxLength - 1; i > 0; i--) {
        
         sparql.append(" ?r");
         sparql.append(i);
         sparql.append(" ");
        
         sparql.append("?x");
         sparql.append(i - 1);
         sparql.append(" . ?x");
         sparql.append(i - 1);
         }
        
         sparql.append(" ?r0 ");
         sparql.append(from == null ? "?y " : from);
        
         sparql.append(" }");

        sparql.append(" }");
        //
        // sparql = new StringBuffer();
        // sparql.append("SELECT ?r ?y WHERE { { <http://semanticweb.org/dumps/people/teddypolar> ?r ?y } UNION { ?y ?r <http://semanticweb.org/dumps/people/teddypolar> } }");
        //
        System.out.println(sparql);
        Query q;
        try {
            q = QueryParser.getInstance().parse(sparql.toString());

            for (TripleCollection g : kb) {
                Object o = TcManager.getInstance().executeSparqlQuery(q, g);
                if (o instanceof ResultSet) {
                    ResultSet result = (ResultSet) o;
                    while (result.hasNext()) {
                        System.out.println();
                        SolutionMapping sm = result.next();
                        for (Variable key : sm.keySet())
                            System.out.println("##### " + key.getName() + " **** " + sm.get(key));
                    }

                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

}
