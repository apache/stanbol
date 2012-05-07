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
package org.apache.stanbol.entityhub.yard.solr.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.stanbol.entityhub.yard.solr.model.IndexField;

/**
 * Interface used to encode constraints for a logical {@link IndexField}.
 * <p>
 * Typically a single constraint in a query needs to converted to several constraints in the index. Do give
 * some examples let's assume, that we are interested in documents that contains "life" in there English
 * title. This constraint would be represented by the following constraints in the index
 * <ul>
 * <li>A {@link IndexConstraintTypeEnum#LANG} set to be English
 * <li>A {@link IndexConstraintTypeEnum#FIELD} set to "dc:title" (dublin core)
 * <li>A {@link IndexConstraintTypeEnum#WILDCARD} set to life* or alternatively
 * <li> {@link IndexConstraintTypeEnum#EQ} set to life.
 * </ul>
 * In addition it can be the case that for encoding one kind of constraint one needs also an other constraint
 * to be present. e.g. when encoding a range constraint one needs always to have both the upper and lower
 * bound to be present. Because of that implementations of this interface can define there
 * {@link #dependsOn()} other {@link IndexConstraintTypeEnum} to be present. Such types are than added with
 * the default constraint <code>null</code> if missing. Implementations can indicate with
 * {@link #supportsDefault()} if they are able to encode the constraint type when set to the default value
 * <code>null</code>.
 * <p>
 * Finally different constraints need different types of values to be parsed. Therefore this interface uses a
 * generic type and the {@link #acceptsValueType()} method can be used to check if the type of the parsed
 * value is compatible with the implementation registered for the current {@link IndexConstraintTypeEnum}.
 * <p>
 * Please note that implementations of this interface need to be thread save, because typically there will
 * only be one instance that is shared for all encoding taks.
 * <p>
 * TODO: Currently there is no generic implementation that implements the processing steps described here.
 * However the SolrQueryFactory provides a Solr specific implementation.
 * 
 * @author Rupert Westenthaler
 * @param T
 *            the type of supported values!
 */
public interface IndexConstraintTypeEncoder<T> {
    /**
     * Encodes the parsed value and adds it to the IndexQueryConstraint.
     * <p>
     * If the encoder supports default values (meaning that {@link #supportsDefault()} returns
     * <code>true</code>, than parsing <code>null</code> as value MUST result in returning the default value.
     * Otherwise the encode method might throw an {@link IllegalArgumentException} if <code>null</code> is
     * parsed.
     * <p>
     * Please note that <code>null</code> might also be a valid parameter even if an Encoder does not support
     * a default value!
     * @param constraint the encoded constraint parts
     * @param value
     *            the value for the constraint
     * @throws IllegalArgumentException
     *             if the parsed value is not supported. Especially if <code>null</code> is parsed and the
     *             implementation does not {@link #supportsDefault()}!
     */
    void encode(EncodedConstraintParts constraint, T value) throws IllegalArgumentException;

    /**
     * Returns <code>true</code> if this Encoder supports a default value. This would be e.g. an open ended
     * upper or lower bound for range constraints or any language for text constraints.
     * 
     * @return if the encoder supports a default encoding if no constraint of that type is provided by the
     *         query.
     */
    boolean supportsDefault();

    /**
     * A set of other Constraints that need to be present, that the encoding provided by this encoder results
     * in a valid query constraint. e.g. A range constraint always need a lower and an upper bound. So even if
     * the query only defines an upper bound, than there MUST BE also a open ended lower bound encoded to
     * result in a valid query constraint.
     * 
     * @return set of other index constraint types that must be present. If none return
     *         {@link Collections#emptySet()}. This Method should return an unmodifiable collection (e.g.
     *         {@link Arrays#asList(Object...)} or using
     *         {@link Collections#unmodifiableCollection(Collection)})
     */
    Collection<IndexConstraintTypeEnum> dependsOn();

    /**
     * The type of the constraint this implementation encodes
     * 
     * @return the type of the constraints encoded by this implementation
     */
    IndexConstraintTypeEnum encodes();

    /**
     * Getter for the type of values this encoder accepts.
     * 
     * @return the generic type of the index constraint type encoder
     */
    Class<T> acceptsValueType();
}
