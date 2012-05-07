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

import java.util.Collection;
import java.util.Collections;

import org.apache.stanbol.commons.solr.utils.SolrUtil;
import org.apache.stanbol.entityhub.yard.solr.model.FieldMapper;
import org.apache.stanbol.entityhub.yard.solr.model.IndexField;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;

public class FieldEncoder implements IndexConstraintTypeEncoder<IndexField> {

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.field);
    private final FieldMapper fieldMapper;

    public FieldEncoder(FieldMapper fieldMapper) {
        if (fieldMapper == null) {
            throw new IllegalArgumentException("The parsed FieldMapper instance MUST NOT be NULL!");
        }
        this.fieldMapper = fieldMapper;
    }

    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Collections.emptySet();
    }

    @Override
    public void encode(EncodedConstraintParts constraint, IndexField value) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException("This encoder does not support the NULL value");
        }
        constraint.addEncoded(POS, SolrUtil.escapeSolrSpecialChars(fieldMapper.encodePath(value)));
    }

    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.FIELD;
    }

    @Override
    public boolean supportsDefault() {
        return false;
    }

    @Override
    public Class<IndexField> acceptsValueType() {
        // NOTE: Generic types are erased at compile time anyways!
        return IndexField.class;
    }

}
