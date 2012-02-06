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
package org.apache.stanbol.entityhub.jersey.writers;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.entityhub.query.clerezza.RdfQueryResultList;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;


final class QueryResultsToRDF {

    private QueryResultsToRDF() { /* do not create instances of utility classes */}

    /**
     * The URI used for the query result list (static for all responses)
     */
    static final UriRef QUERY_RESULT_LIST = new UriRef(RdfResourceEnum.QueryResultSet.getUri());
    /**
     * The property used for all results
     */
    static final UriRef QUERY_RESULT = new UriRef(RdfResourceEnum.queryResult.getUri());
    /**
     * The property used for the JSON serialised FieldQuery (STANBOL-298)
     */
    static final UriRef FIELD_QUERY = new UriRef(RdfResourceEnum.query.getUri());
    
    /**
     * The LiteralFactory retrieved from {@link EntityToRDF#literalFactory}
     */
    static final LiteralFactory literalFactory = EntityToRDF.literalFactory;

    static MGraph toRDF(QueryResultList<?> resultList) {
        final MGraph resultGraph;
        Class<?> type = resultList.getType();
        if (String.class.isAssignableFrom(type)) {
            resultGraph = new IndexedMGraph(); //create a new Graph
            for (Object result : resultList) {
                //add a triple to each reference in the result set
                resultGraph.add(new TripleImpl(QUERY_RESULT_LIST, QUERY_RESULT, new UriRef(result.toString())));
            }
        } else {
            //first determine the type of the resultList
            final boolean isSignType;
            if (Representation.class.isAssignableFrom(type)) {
                isSignType = false;
            } else if (Representation.class.isAssignableFrom(type)) {
                isSignType = true;
            } else {
                //incompatible type -> throw an Exception
                throw new IllegalArgumentException("Parsed type " + type + " is not supported");
            }
            //special treatment for RdfQueryResultList for increased performance
            if (resultList instanceof RdfQueryResultList) {
                resultGraph = ((RdfQueryResultList) resultList).getResultGraph();
                if (isSignType) { //if we build a ResultList for Signs, that we need to do more things
                    //first remove all triples representing results
                    Iterator<Triple> resultTripleIt = resultGraph.filter(QUERY_RESULT_LIST, QUERY_RESULT, null);
                    while (resultTripleIt.hasNext()) {
                        resultTripleIt.next();
                        resultTripleIt.remove();
                    }
                    //now add the Sign specific triples and add result triples
                    //to the Sign IDs
                    for (Object result : resultList) {
                        UriRef signId = new UriRef(((Entity) result).getId());
                        EntityToRDF.addEntityTriplesToGraph(resultGraph, (Entity) result);
                        resultGraph.add(new TripleImpl(QUERY_RESULT_LIST, QUERY_RESULT, signId));
                    }
                }
            } else { //any other implementation of the QueryResultList interface
                resultGraph = new IndexedMGraph(); //create a new graph
                if (Representation.class.isAssignableFrom(type)) {
                    for (Object result : resultList) {
                        UriRef resultId;
                        if (!isSignType) {
                            EntityToRDF.addRDFTo(resultGraph, (Representation) result);
                            resultId = new UriRef(((Representation) result).getId());
                        } else {
                            EntityToRDF.addRDFTo(resultGraph, (Entity) result);
                            resultId = new UriRef(((Entity) result).getId());
                        }
                        //Note: In case of Representation this Triple points to
                        //      the representation. In case of Signs it points to
                        //      the sign.
                        resultGraph.add(new TripleImpl(QUERY_RESULT_LIST, QUERY_RESULT, resultId));
                    }
                }
            }
        }
        return resultGraph;
    }

}
