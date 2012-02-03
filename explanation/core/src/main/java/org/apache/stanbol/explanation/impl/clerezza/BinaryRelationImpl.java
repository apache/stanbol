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
package org.apache.stanbol.explanation.impl.clerezza;

import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.explanation.api.BinaryRelation;

public class BinaryRelationImpl implements BinaryRelation {

    private Resource object;

    private String predicate;

    private NonLiteral subject;

    public BinaryRelationImpl(NonLiteral subject, String predicate, Resource object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public BinaryRelationImpl(Triple triple) {
        this.subject = triple.getSubject();
        this.predicate = triple.getPredicate().getUnicodeString();
        this.object = triple.getObject();
    }

    @Override
    public String getFrom() {
        if (subject instanceof UriRef) return ((UriRef) subject).getUnicodeString();
        return subject.toString();
    }

    @Override
    public String getProperty() {
        return predicate;
    }

    @Override
    public String getTo() {
        if (object instanceof UriRef) return ((UriRef) object).getUnicodeString();
        return object.toString();
    }

}
