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

package org.apache.stanbol.rules.adapters.clerezza;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.sparql.query.Expression;
import org.apache.clerezza.rdf.core.sparql.query.TriplePattern;

/**
 * 
 * This object represents either a {@link TriplePattern} or an {@link Expression} or a {@link IRI}
 * internally to the Clerezza adpter.
 * 
 * @author anuzzolese
 * 
 */
public class ClerezzaSparqlObject {

    private TriplePattern triplePattern;
    private Expression expression;
    private IRI uriRef;

    public ClerezzaSparqlObject(TriplePattern triplePattern) {
        this.triplePattern = triplePattern;
    }

    public ClerezzaSparqlObject(Expression expression) {
        this.expression = expression;
    }

    public ClerezzaSparqlObject(IRI uriRef) {
        this.uriRef = uriRef;
    }

    /**
     * It returns the actual Clerezza value.<br/>
     * 
     * It can be:
     * <ul>
     * <li>a {@link TriplePattern}
     * <li>an {@link Expression}
     * <li>a {@link IRI}
     * 
     * @return the object that can be in turn a {@link TriplePattern}, an {@link Expression}, and a
     *         {@link IRI}
     */
    public Object getClerezzaObject() {
        if (triplePattern != null) {
            return triplePattern;
        } else if (expression != null) {
            return expression;
        } else {
            return uriRef;
        }
    }
}
