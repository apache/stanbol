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

import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint.MODE;
import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrQueryFactory.ConstraintValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexDataType;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValueFactory;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;

/**
 * TODO: This encoder is not functional! It would need to convert the REGEX Pattern to the according WildCard
 * search! Need to look at
 * http://lucene.apache.org/java/2_4_0/api/org/apache/lucene/search/regex/RegexQuery.html
 * 
 * @author Rupert Westenthaler
 * 
 */
public class RegexEncoder implements IndexConstraintTypeEncoder<ConstraintValue> {

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.value);

    private static final Set<IndexDataType> SUPPORTED_TYPES;
    static {
        Set<IndexDataType> types = new HashSet<IndexDataType>();
        types.add(IndexDataTypeEnum.TXT.getIndexType());
        types.add(IndexDataTypeEnum.STR.getIndexType());
        SUPPORTED_TYPES = Collections.unmodifiableSet(types);
    }
//    private final IndexValueFactory indexValueFactory;

    public RegexEncoder(IndexValueFactory indexValueFactory) {
//        if (indexValueFactory == null) {
//            throw new IllegalArgumentException("The indexValueFactory MUST NOT be NULL");
//        }
//        this.indexValueFactory = indexValueFactory;
    }

    @Override
    public void encode(EncodedConstraintParts constraint, ConstraintValue value) {
        if(value == null || value.getValues().isEmpty()){
            throw new IllegalArgumentException("This encoder does not support the NULL IndexValue!");
        }
        // encode the value based on the type
        Set<String> queryConstraints = new HashSet<String>();
        for(IndexValue indexValue : value){
            if (value != null) {
                if (!SUPPORTED_TYPES.contains(indexValue.getType())) {
                    throw new IllegalArgumentException(String.format(
                        "This encoder does not support the IndexDataType %s (supported: %s)", indexValue.getType(),
                        SUPPORTED_TYPES));
                } else {
                    // NOTE that not all regex queries can be supported by Solr
                    // see https://issues.apache.org/jira/browse/LUCENE-2604
                    StringBuilder sb = new StringBuilder(indexValue.getValue().length()+2);
                    sb.append('/').append(indexValue.getValue()).append('/');
                    if(value.getBoost() != null){
                        sb.append('^').append(value.getBoost());
                    }
                    queryConstraints.add(sb.toString());
                }
                if(value.getMode() == MODE.any){ //in any mode
                    //we need to add constraints separately (to connect them with OR)
                    constraint.addEncoded(POS, queryConstraints);
                    queryConstraints.clear();
                }
            } //else ignore null element
        }
        if(value.getMode() == MODE.all){ // an all mode we need to add all
            //constraint in a single call (to connect them with AND)
            constraint.addEncoded(POS, queryConstraints);
        }
    }

    @Override
    public Collection<IndexConstraintTypeEnum> dependsOn() {
        return Arrays.asList(IndexConstraintTypeEnum.EQ);
    }

    @Override
    public IndexConstraintTypeEnum encodes() {
        return IndexConstraintTypeEnum.REGEX;
    }

    @Override
    public boolean supportsDefault() {
        return false;
    }

    @Override
    public Class<ConstraintValue> acceptsValueType() {
        return ConstraintValue.class;
    }

}
