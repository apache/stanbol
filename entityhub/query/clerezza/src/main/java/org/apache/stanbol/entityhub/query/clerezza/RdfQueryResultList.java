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

import static org.apache.stanbol.entityhub.servicesapi.util.ModelUtils.RESULT_SCORE_COMPARATOR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;


public class RdfQueryResultList implements QueryResultList<Representation> {

    private final FieldQuery query;
    private final List<RdfRepresentation> results;
    private final Graph resultGraph;

    public RdfQueryResultList(FieldQuery query,Graph resultGraph) {
        if(query == null){
            throw new IllegalArgumentException("Parameter Query MUST NOT be NULL!");
        }
        if(resultGraph == null){
            throw new IllegalArgumentException("Parameter \"Graph resultGraph\" MUST NOT be NULL");
        }
        this.query = query;
        this.resultGraph = resultGraph;
        List<RdfRepresentation> results = (List<RdfRepresentation>)ModelUtils.addToCollection(
            ClerezzaQueryUtils.parseQueryResultsFromGraph(resultGraph),
            new ArrayList<RdfRepresentation>());
        //sort the list based on the score
        Collections.sort(results,RESULT_SCORE_COMPARATOR);
        this.results = Collections.unmodifiableList(results);
                
    }
    @Override
    public final FieldQuery getQuery() {
        return query;
    }

    @Override
    public final Set<String> getSelectedFields() {
        return query.getSelectedFields();
    }

    @Override
    public final boolean isEmpty() {
        return results.isEmpty();
    }

    @Override
    public final Iterator<Representation> iterator() {
        return new Iterator<Representation>() {
            private Iterator<RdfRepresentation> it = results.iterator();
            @Override
            public boolean hasNext() { return it.hasNext(); }
            @Override
            public Representation next() { return it.next(); }
            @Override
            public void remove() { it.remove(); }
        };
    }
    @Override
    public Collection<RdfRepresentation> results() {
        return results;
    }
    @Override
    public final int size() {
        return results.size();
    }
    /**
     * Getter for the RDF ImmutableGraph holding the Results of the Query
     * @return the RDF ImmutableGraph with the Results
     */
    public final Graph getResultGraph() {
        return resultGraph;
    }
    @Override
    public final Class<Representation> getType() {
        return Representation.class;
    }

}
