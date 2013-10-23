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

import org.apache.stanbol.commons.solr.utils.SolrUtil;
import org.apache.stanbol.entityhub.yard.solr.model.FieldMapper;
import org.apache.stanbol.entityhub.yard.solr.model.IndexField;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValueFactory;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;

/**
 * Encodes the DataType to the field name.
 * 
 * @author Rupert Westenthaler
 */
public class DataTypeEncoder implements IndexConstraintTypeEncoder<IndexField> {

    private static final ConstraintTypePosition PREFIX = new ConstraintTypePosition(PositionType.prefix);
    private static final ConstraintTypePosition SUFFIX = new ConstraintTypePosition(PositionType.suffux);

    private final FieldMapper fieldMapper;
//    private final IndexValueFactory indexValueFactory;

    public DataTypeEncoder(IndexValueFactory indexValueFactory, FieldMapper fieldMapper) {
        if (fieldMapper == null) {
            throw new IllegalArgumentException("The FieldMapper MUST NOT be NULL!");
        }
//        if (indexValueFactory == null) {
//            throw new IllegalArgumentException("The IndexValueFactory MUST NOT be NULL!");
//        }
        this.fieldMapper = fieldMapper;
//        this.indexValueFactory = indexValueFactory;
    }

    @Override
    public void encode(EncodedConstraintParts constraint, IndexField value) throws IllegalArgumentException {
//        IndexDataType indexDataType = value.getDataType();
//        if (value == null) {
//            indexDataType = null;
//        } else if (value instanceof IndexDataType) {
//            indexDataType = (IndexDataType) value;
//        } else if (value instanceof IndexField) {
//            indexDataType = ((IndexField) value).getDataType();
//        } else if (value instanceof IndexValue) {
//            indexDataType = ((IndexValue) value).getType();
//        } else {
//            indexDataType = indexValueFactory.createIndexValue(value).getType();
//        }
        if (value != null) {
            String[] prefixSuffix = fieldMapper.encodeDataType(value);

            if (prefixSuffix[0] != null) {
                constraint.addEncoded(PREFIX, SolrUtil.escapeSolrSpecialChars(prefixSuffix[0]));
            }
            if (prefixSuffix[1] != null) {
                constraint.addEncoded(SUFFIX, SolrUtil.escapeSolrSpecialChars(prefixSuffix[1]));
            }
        } // else nothing todo!
    }

    @Override
    public boolean supportsDefault() {
        return true;
    }

    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Arrays.asList(IndexConstraintTypeEnum.FIELD);
    }

    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.DATATYPE;
    }

    @Override
    public Class<IndexField> acceptsValueType() {
        return IndexField.class;
    }

}
