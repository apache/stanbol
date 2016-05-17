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
package org.apache.stanbol.entityhub.model.clerezza.impl;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator.Adapter;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;


/**
 * Adapter that converts Clerezza {@link IRI} instances to {@link Reference}s.
 * The {@link RdfValueFactory} is used to create {@link Reference} instances.
 * @author Rupert Westenthaler
 *
 */
public class IRI2ReferenceAdapter implements Adapter<IRI,Reference> {

    private final RdfValueFactory valueFactory = RdfValueFactory.getInstance();

    @Override
    public final Reference adapt(IRI value, Class<Reference> type) {
        return valueFactory.createReference(value);
    }

}
