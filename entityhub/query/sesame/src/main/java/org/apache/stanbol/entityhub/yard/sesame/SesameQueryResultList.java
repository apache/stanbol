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
package org.apache.stanbol.entityhub.yard.sesame;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.openrdf.model.Model;

/**
 * {@link QueryResultList} implementation for Sesame. This provides
 * access to the Sesame {@link Model} holding the data. Mainly for the use of
 * Sesame specific RDF serializer.
 * 
 * @author Rupert Westenthaler
 *
 * @param <T>
 */
public class SesameQueryResultList implements QueryResultList<Representation> {

    protected final Model model;
    protected final Collection<Representation> representations;
    protected final SparqlFieldQuery query;

    
    public SesameQueryResultList(Model model, SparqlFieldQuery query, List<Representation> representations){
        this.model = model;
        this.representations = Collections.unmodifiableCollection(representations);
        this.query = query;
    }
    
    @Override
    public SparqlFieldQuery getQuery() {
        return query;
    }

    @Override
    public Set<String> getSelectedFields() {
        return query.getSelectedFields();
    }

    @Override
    public Class<Representation> getType() {
        return Representation.class;
    }

    @Override
    public boolean isEmpty() {
        return representations.isEmpty();
 
    }

    @Override
    public Iterator<Representation> iterator() {
        return representations.iterator();
    }

    @Override
    public Collection<Representation> results() {
        return representations;
    }

    @Override
    public int size() {
        return representations.size();
    }

    /**
     * The model holding all query results
     * @return
     */
    public Model getModel() {
        return model;
    }
    
}
