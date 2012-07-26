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
package org.apache.stanbol.entityhub.core.query;

import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;

/**
 * Simple {@link FieldQueryFactory} implementation that uses the singleton
 * pattern and returns for each call to {@link #createFieldQuery()} a new
 * instance of {@link FieldQueryImpl}.
 *
 * @author Rupert Westenthaler
 */
public class DefaultQueryFactory implements FieldQueryFactory {

    private static final DefaultQueryFactory instance = new DefaultQueryFactory();

    public static FieldQueryFactory getInstance() {
        return instance;
    }

    protected DefaultQueryFactory() {
        super();
    }

    @Override
    public FieldQuery createFieldQuery() {
        return new FieldQueryImpl();
    }

}
