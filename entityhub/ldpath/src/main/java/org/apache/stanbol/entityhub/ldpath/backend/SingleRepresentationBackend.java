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
package org.apache.stanbol.entityhub.ldpath.backend;

import java.util.Collections;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.query.FieldQueryImpl;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;

/**
 * Allows to execute ldpath on the data of a single Representation. Will not
 * support paths longer that <code>1</code> but might still be very usefull
 * to select/filter specific values of fields.
 * @author Rupert Westenthaler
 *
 */
public class SingleRepresentationBackend extends AbstractBackend {

    
    Representation representation;
    private final ValueFactory valueFactory;
    
    private static final FieldQuery DUMMY_QUERY = new FieldQueryImpl();
    @SuppressWarnings("unchecked")
    private static final QueryResultList<String> EMPTY_RESULT = 
            new QueryResultListImpl<String>(DUMMY_QUERY, Collections.EMPTY_LIST, String.class);
    
    public SingleRepresentationBackend() {
        this(null);
    }
    public SingleRepresentationBackend(ValueFactory vf){
        if(vf == null){
            this.valueFactory = InMemoryValueFactory.getInstance();
        } else {
            this.valueFactory = vf;
        }
    }
    public void setRepresentation(Representation r){
        if(r != null){
            this.representation = r;
        } else {
            throw new IllegalArgumentException("The parsed Representation MUST NOT be NULL!");
        }
    }
    public Representation getRepresentation(){
        return representation;
    }
    @Override
    protected ValueFactory getValueFactory() {
        return valueFactory;
    }

    @Override
    protected Representation getRepresentation(String id) throws EntityhubException {
        return representation.getId().equals(id) ? representation : null;
    }

    @Override
    protected QueryResultList<String> query(FieldQuery query) throws EntityhubException {
        return EMPTY_RESULT;
    }

    @Override
    protected FieldQuery createQuery() {
        return DUMMY_QUERY;
    }

}
