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
package org.apache.stanbol.entityhub.model.clerezza.utils;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator.Adapter;

/**
 * Needed because IRIs and Literals use the RDF representation for the
 * toString Method
 *
 * @author Rupert Westenthaler
 *
 * @param <T>
 */
public class Resource2StringAdapter<T extends RDFTerm> implements Adapter<T, String> {

    @Override
    public final String adapt(T value, Class<String> type) {
        if (value == null) {
            return null;
        } else if (value instanceof IRI) {
            return ((IRI) value).getUnicodeString();
        } else if (value instanceof Literal) {
            return ((Literal) value).getLexicalForm();
        } else {
            return value.toString();
        }
    }

}
