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

import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

public class YardBackend extends AbstractBackend {

    protected final Yard yard;
    
    public YardBackend(Yard yard) {
        this(yard,null);
    }
    public YardBackend(Yard yard,ValueConverterFactory valueConverter) {
        super(valueConverter);
        if(yard == null){
            throw new IllegalArgumentException("The parsed Yard MUST NOT be NULL");
        }
        this.yard = yard;
    }
    @Override
    protected FieldQuery createQuery() {
        return yard.getQueryFactory().createFieldQuery();
    }
    @Override
    protected Representation getRepresentation(String id) throws EntityhubException {
        return yard.getRepresentation(id);
    }
    @Override
    protected ValueFactory getValueFactory() {
        return yard.getValueFactory();
    }
    @Override
    protected QueryResultList<String> query(FieldQuery query) throws EntityhubException {
        return yard.findReferences(query);
    }
}
