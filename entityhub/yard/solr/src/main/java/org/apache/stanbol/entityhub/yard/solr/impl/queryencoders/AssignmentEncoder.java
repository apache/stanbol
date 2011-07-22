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
import java.util.List;
import java.util.Set;

import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValueFactory;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.QueryUtils;

/**
 * Encodes the Assignment of the field to an value. If a value is parsed, than it encodes that the field must
 * be equals to this value.
 * 
 * @author Rupert Westenthaler
 */
public class AssignmentEncoder implements IndexConstraintTypeEncoder<Object> {

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.assignment);
    private static final String EQ = ":";
    private final IndexValueFactory indexValueFactory;

    public AssignmentEncoder(IndexValueFactory indexValueFactory) {
        if (indexValueFactory == null) {
            throw new IllegalArgumentException("The indexValueFactory MUST NOT be NULL");
        }
        this.indexValueFactory = indexValueFactory;
    }

    @Override
    public void encode(EncodedConstraintParts constraint, Object value) {
        Set<IndexValue> indexValues = QueryUtils.parseIndexValues(indexValueFactory,value);
        // encode the value based on the type
        for(IndexValue indexValue : indexValues){
            String[] queryConstraints = QueryUtils.encodeQueryValue(indexValue, true);
            String[] eqConstraints;
            if (queryConstraints != null) {
                eqConstraints = new String[queryConstraints.length];
                for (int i = 0; i < eqConstraints.length; i++) {
                    eqConstraints[i] = EQ + queryConstraints[i];
                }
            } else {
                eqConstraints = new String[] {EQ};
            }
            constraint.addEncoded(POS, eqConstraints);
        }
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
        return IndexConstraintTypeEnum.EQ;
    }

    @Override
    public Class<Object> acceptsValueType() {
        return Object.class;
    }

}
