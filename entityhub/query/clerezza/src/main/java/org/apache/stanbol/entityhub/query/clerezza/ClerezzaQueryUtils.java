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
package org.apache.stanbol.entityhub.query.clerezza;

import java.util.Iterator;

import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.query.sparql.SparqlQueryUtils;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Clerezza specific Query utilities that are in addition to those
 * provided by {@link SparqlQueryUtils}.
 * @author Rupert Westenthaler
 *
 */
public final class ClerezzaQueryUtils {

    /**
     * Restrict instantiation
     */
    private ClerezzaQueryUtils() {}

    private static final Logger log = LoggerFactory.getLogger(ClerezzaQueryUtils.class);

    private static final RdfValueFactory valueFavtory = RdfValueFactory.getInstance();
    /**
     * {@link IRI} constant for {@link RdfResourceEnum#queryResult}
     * 
     * @see RdfResourceEnum.fieldQueryResult
     */
    public static final IRI FIELD_QUERY_RESULT = new IRI(RdfResourceEnum.queryResult.getUri());
    /**
     * {@link IRI} constant for {@link RdfResourceEnum#QueryResultSet}
     * 
     * @see RdfResourceEnum.FieldQueryResultSet
     */
    public static final IRI FIELD_QUERY_RESULT_SET = new IRI(RdfResourceEnum.QueryResultSet.getUri());

    /**
     * @param query
     * @param resultGraph
     * @return
     */
    public static Iterator<RdfRepresentation> parseQueryResultsFromGraph(final Graph resultGraph) {
        Iterator<Triple> resultTripleIterator = resultGraph.filter(FIELD_QUERY_RESULT_SET,
            FIELD_QUERY_RESULT, null);
        Iterator<RdfRepresentation> resultIterator = new AdaptingIterator<Triple,RdfRepresentation>(
                resultTripleIterator, new AdaptingIterator.Adapter<Triple,RdfRepresentation>() {
                    /*
                     * Anonymous implementation of an Adapter that converts the filtered Triples of the
                     * resulting graph to RdfRepresentations
                     */
                    @Override
                    public RdfRepresentation adapt(Triple value, Class<RdfRepresentation> type) {
                        RDFTerm object = value.getObject();
                        if (object == null) {
                            return null;
                        } else if (object instanceof IRI) {
                            return valueFavtory.createRdfRepresentation((IRI) object, resultGraph);
                        } else {
                            log.warn("Unable to create representation for FieldQueryResult " + object
                                     + " because this RDFTerm is not of Type IRI (type: "
                                     + object.getClass() + ") -> result gets ignored");
                            return null;
                        }
                    }
                }, RdfRepresentation.class);
        return resultIterator;
    }
    
}
