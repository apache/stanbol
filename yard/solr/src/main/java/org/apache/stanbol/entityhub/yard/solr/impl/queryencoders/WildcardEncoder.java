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
package org.apache.stanbol.entityhub.yard.solr.impl.queryencoders;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.model.IndexDataType;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValueFactory;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.QueryUtils;

public class WildcardEncoder implements IndexConstraintTypeEncoder<Object> {

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.value);

    private static final Set<IndexDataType> SUPPORTED_TYPES;
    static {
        Set<IndexDataType> types = new HashSet<IndexDataType>();
        types.add(IndexDataTypeEnum.TXT.getIndexType());
        types.add(IndexDataTypeEnum.STR.getIndexType());
        SUPPORTED_TYPES = Collections.unmodifiableSet(types);
    }
    private final IndexValueFactory indexValueFactory;

    public WildcardEncoder(IndexValueFactory indexValueFactory) {
        if (indexValueFactory == null) {
            throw new IllegalArgumentException("The indexValueFactory MUST NOT be NULL");
        }
        this.indexValueFactory = indexValueFactory;
    }

    @Override
    public void encode(EncodedConstraintParts constraint, Object value) {
        Set<IndexValue> indexValues = QueryUtils.parseIndexValues(indexValueFactory,value);
        if(indexValues.size() == 1 && indexValues.iterator().next() == null){
            throw new IllegalArgumentException("This encoder does not support the NULL IndexValue!");
        }
        // encode the value based on the type
        for(IndexValue indexValue : indexValues){
            if (indexValue != null) {
                if (!SUPPORTED_TYPES.contains(indexValue.getType())) {
                    throw new IllegalArgumentException(String.format(
                        "This encoder does not support the IndexDataType %s (supported: %s)", indexValue.getType(),
                        SUPPORTED_TYPES));
                } else {
                    constraint.addEncoded(POS, QueryUtils.encodeQueryValue(indexValue, false));
                }
            } // else ignore null value
        }
    }

    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Arrays.asList(IndexConstraintTypeEnum.EQ);
    }

    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.WILDCARD;
    }

    @Override
    public boolean supportsDefault() {
        return false;
    }

    @Override
    public Class<Object> acceptsValueType() {
        return Object.class;
    }

}
