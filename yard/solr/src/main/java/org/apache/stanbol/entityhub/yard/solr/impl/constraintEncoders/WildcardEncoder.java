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
package org.apache.stanbol.entityhub.yard.solr.impl.constraintEncoders;

import java.util.Arrays;
import java.util.Collection;

import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;


public class WildcardEncoder implements IndexConstraintTypeEncoder<String>{

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.value);

    @Override
    public void encode(EncodedConstraintParts constraint, String value) {
        if(value == null){
            throw new IllegalArgumentException("This encoder does not support the NULL IndexValue!");
        } else {
            //TODO: Use toLoverCase here, because I had problems with Solr that
            //     Queries where not converted to lower case even that the
            //     LowerCaseFilterFactory was present in the query analyser :(
            value = value.toLowerCase();
            /* NOTE:
             *   When searching for multiple words we assume that we need to find
             *   the exact pattern e.g. "best pract*" because of that we replace
             *   spaces with '+'
             */
            value = value.replace(' ', '+');
            constraint.addEncoded(POS, value);
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
    public Class<String> acceptsValueType() {
        return String.class;
    }

}
