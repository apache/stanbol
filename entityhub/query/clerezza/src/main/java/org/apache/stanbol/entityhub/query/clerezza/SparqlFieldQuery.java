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

import org.apache.stanbol.entityhub.query.clerezza.SparqlQueryUtils.EndpointTypeEnum;
import org.apache.stanbol.entityhub.query.sparql.SparqlEndpointTypeEnum;

/**
 * This class moved to {@link org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery}.
 * and now extends the new one. NOTE that calls to {@link #setEndpointType(EndpointTypeEnum)}
 * and {@link #getEndpointType()} will translate {@link EndpointTypeEnum} instances
 * to {@link SparqlEndpointTypeEnum}.
 * @author Rupert Westenthaler
 * @see org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery
 */
@Deprecated
public class SparqlFieldQuery extends org.apache.stanbol.entityhub.query.sparql.SparqlFieldQuery {

    protected SparqlFieldQuery() {
        this(null);
    }

    protected SparqlFieldQuery(EndpointTypeEnum endpointType) {
        super(endpointType == null ? null :
            org.apache.stanbol.entityhub.query.sparql.SparqlEndpointTypeEnum.valueOf(endpointType.name()));
    }

    @Deprecated
    public final EndpointTypeEnum getEndpointType() {
        EndpointTypeEnum type;
        try {
            type = EndpointTypeEnum.valueOf(super.getSparqlEndpointType().name());
        } catch (IllegalArgumentException e) {
            type = null;
        }
        return type;
    }

    @Deprecated
    public final void setEndpointType(EndpointTypeEnum endpointType) {
        SparqlEndpointTypeEnum type = endpointType == null ? null :
            SparqlEndpointTypeEnum.valueOf(endpointType.name());
        setSparqlEndpointType(type);
    }

    /**
     * Clones the query (including the field to var name mapping)
     */
    @Override
    public SparqlFieldQuery clone() {
        return clone(new SparqlFieldQuery());
    }


    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof SparqlFieldQuery;
    }

}
