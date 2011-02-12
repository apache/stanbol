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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.stanbol.entityhub.core.utils.ModelUtils;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;


public class RdfQueryResultList implements QueryResultList<Representation> {

    private final FieldQuery query;
    private final Collection<RdfRepresentation> results;
    private final MGraph resultGraph;

    public RdfQueryResultList(FieldQuery query,MGraph resultGraph) {
        if(query == null){
            throw new IllegalArgumentException("Parameter Query MUST NOT be NULL!");
        }
        if(resultGraph == null){
            throw new IllegalArgumentException("Parameter \"MGraph resultGraph\" MUST NOT be NULL");
        }
        this.query = query;
        this.resultGraph = resultGraph;
        this.results = Collections.unmodifiableCollection(
                ModelUtils.asCollection(
                        SparqlQueryUtils.parseQueryResultsFromMGraph(resultGraph)));
    }
    @Override
    public FieldQuery getQuery() {
        return query;
    }

    @Override
    public Set<String> getSelectedFields() {
        return query.getSelectedFields();
    }

    @Override
    public boolean isEmpty() {
        return results.isEmpty();
    }

    @Override
    public Iterator<Representation> iterator() {
        return new Iterator<Representation>() {
            Iterator<RdfRepresentation> it = results.iterator();
            @Override
            public boolean hasNext() { return it.hasNext(); }
            @Override
            public Representation next() { return it.next(); }
            @Override
            public void remove() { it.remove(); }
        };
    }

    @Override
    public int size() {
        return results.size();
    }
    /**
     * Getter for the RDF Graph holding the Results of the Query
     * @return the RDF Graph with the Results
     */
    public MGraph getResultGraph() {
        return resultGraph;
    }
    @Override
    public Class<Representation> getType() {
        return Representation.class;
    }

}
