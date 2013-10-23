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

import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class moved to {@link org.apache.stanbol.entityhub.query.sparql.SparqlFieldQueryFactory}.
 * This forwards to the new implementation.
 * @author Rupert Westenthaler
 * @see org.apache.stanbol.entityhub.query.sparql.SparqlFieldQueryFactory
 */
@Deprecated
public final class SparqlFieldQueryFactory implements FieldQueryFactory  {
    protected static final Logger logger = LoggerFactory.getLogger(SparqlFieldQueryFactory.class);

    private static SparqlFieldQueryFactory instance;

    @Deprecated
    public static SparqlFieldQueryFactory getInstance() {
        if (instance == null) {
            instance = new SparqlFieldQueryFactory();
        }
        return instance;
    }

    private SparqlFieldQueryFactory() {
        super();
    }

    @Deprecated
    @Override
    public SparqlFieldQuery createFieldQuery() {
        return new SparqlFieldQuery();
    }

    /**
     * Utility Method to create an {@link SparqlFieldQuery} based on the parse {@link FieldQuery}
     * 
     * @param parsedQuery
     *            the parsed Query
     * @see org.apache.stanbol.entityhub.query.sparql.SparqlFieldQueryFactory#getSparqlFieldQuery(FieldQuery)
     */
    @Deprecated
    public static SparqlFieldQuery getSparqlFieldQuery(FieldQuery parsedQuery) {

        if (parsedQuery == null) {
            logger.trace("Parsed query is null.");
            return null;
        } else if (parsedQuery instanceof SparqlFieldQuery) {
            logger.trace("Parsed query is a [SparqlFieldQuery].");
            return (SparqlFieldQuery) parsedQuery;
        } else {
            logger.trace("Parsed query is a [{}].", parsedQuery.getClass().toString());
            return parsedQuery.copyTo(new SparqlFieldQuery());
        }
    }

}
