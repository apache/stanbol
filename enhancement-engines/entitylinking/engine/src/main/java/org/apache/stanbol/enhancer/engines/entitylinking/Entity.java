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
package org.apache.stanbol.enhancer.engines.entitylinking;

import java.util.Iterator;

import org.apache.clerezza.rdf.core.LiteralFactory;

import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.TransformIterator;

/**
 * An Entity as returned by the {@link EntitySearcher} interface.
 * {@link EntitySearcher} implementations that do support rankings for
 * entities SHOULD override the {@link #getEntityRanking()} method.
 */
public class Entity implements Comparable<Entity>{

    protected static final LiteralFactory lf = LiteralFactory.getInstance();
    
    protected static final Transformer TRIPLE2OBJECT = new Transformer() {
        @Override
        public Object transform(Object input) {
            return ((Triple)input).getObject();
        }
    };
    protected static final Predicate PLAIN_LITERALS = PredicateUtils.instanceofPredicate(Literal.class);
    //protected static final Predicate TYPED_LITERALS = PredicateUtils.instanceofPredicate(TypedLiteral.class);
    protected static final Predicate REFERENCES = PredicateUtils.instanceofPredicate(IRI.class);
    /**
     * The URI of the Entity
     */
     protected final IRI uri;
    /**
     * The data of the Entity. The graph is expected to contain all information
     * of the entity by containing {@link Triple}s that use the {@link #uri} as
     * {@link Triple#getSubject() subject}
     */
    protected final Graph data;
    
    /**
     * Constructs a new Entity
     * @param uri
     * @param data
     */
    public Entity(IRI uri, Graph data) {
        this.uri = uri;
        this.data = data;
    }
    public final IRI getUri() {
        return uri;
    }
    public final String getId(){
        return uri.getUnicodeString();
    }
    public final Graph getData() {
        return data;
    }
    @SuppressWarnings("unchecked")
    public Iterator<Literal> getText(IRI field) {
        return new FilterIterator(new TransformIterator(data.filter(uri, field, null), TRIPLE2OBJECT), PLAIN_LITERALS);
    }
    @SuppressWarnings("unchecked")
    public Iterator<IRI> getReferences(IRI field){
        return new FilterIterator(new TransformIterator(data.filter(uri, field, null), TRIPLE2OBJECT), REFERENCES);
    }
    
    /**
     * The ranking for the entity in the range [0..1] or <code>null</code> 
     * if not support.<p>
     * This default implementation will returns <code>null</code>
     * @return returns <code>null</code> as this default implementation
     * does not support entity rankings
     */
    public Float getEntityRanking(){
        return null;
    }
    /**
     * Uses the hascode of the {@link #getUri() URI}
     */
    @Override
    public int hashCode() {
        return uri.hashCode();
    }
    /**
     * Checks if the two Entities do have the same {@link #getUri() URI}
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Entity && ((Entity)other).uri.equals(uri);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Entity[uri: ").append(uri.getUnicodeString());
        Float entityRanking = getEntityRanking();
        if(entityRanking != null){
            sb.append(" | ranking: ").append(entityRanking);
        }
        sb.append("]");
        return sb.toString();
    }
    /**
     * Compares Entities based on their {@link #getUri()}
     */
    @Override
    public int compareTo(Entity other) {
        return uri.getUnicodeString().compareTo(other.uri.getUnicodeString());
    }
}