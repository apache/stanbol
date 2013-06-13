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

import static org.apache.stanbol.entityhub.yard.solr.query.QueryUtils.encodePhraseQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint.MODE;
import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.defaults.QueryConst;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrQueryFactory.ConstraintValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexDataType;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValueFactory;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.QueryUtils;
import org.apache.stanbol.entityhub.yard.solr.query.QueryUtils.QueryTerm;

public class WildcardEncoder implements IndexConstraintTypeEncoder<ConstraintValue> {

    private static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.value);

    private static final Set<IndexDataType> SUPPORTED_TYPES;
    static {
        Set<IndexDataType> types = new HashSet<IndexDataType>();
        types.add(IndexDataTypeEnum.TXT.getIndexType());
        types.add(IndexDataTypeEnum.STR.getIndexType());
        SUPPORTED_TYPES = Collections.unmodifiableSet(types);
    }
//    private final IndexValueFactory indexValueFactory;

    public WildcardEncoder(IndexValueFactory indexValueFactory) {
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
        //the query constraints used for the phrase constraint
        Collection<String> phraseTerms = new ArrayList<String>();
        for(IndexValue indexValue : value){
            if (indexValue != null) {
                if (!SUPPORTED_TYPES.contains(indexValue.getType())) {
                    throw new IllegalArgumentException(String.format(
                        "This encoder does not support the IndexDataType %s (supported: %s)", indexValue.getType(),
                        SUPPORTED_TYPES));
                } else {
                    for(QueryTerm qt : QueryUtils.encodeQueryValue(indexValue, false)){
                        StringBuilder sb = new StringBuilder(qt.needsQuotes ? 
                                qt.term.length()+2 : 0);
                        if(qt.needsQuotes){
                            sb.append('"').append(qt.term).append('"');
                            queryConstraints.add(sb.toString());
                        } else {
                           queryConstraints.add(qt.term);
                        }
                        if(value.getBoost() != null){
                            sb.append("^").append(value.getBoost());
                        }
                        if(!qt.hasWildcard && qt.isText) {
                            //phrases do not work with wildcard and are only
                            //relevant for texts
                            phraseTerms.add(qt.term);
                        }
                    }
                }
                if(value.getMode() == MODE.any){ //in any mode
                    //we need to add constraints separately (to connect them with OR)
                    constraint.addEncoded(POS, queryConstraints);
                    queryConstraints.clear();
                }
            } // else ignore null value
        }
        if(value.getMode() == MODE.all){ // an all mode we need to add all
            //constraint in a single call (to connect them with AND)
            constraint.addEncoded(POS, queryConstraints);
        } else {
            if(phraseTerms.size() > 1){
                Boolean state = (Boolean) value.getProperty(QueryConst.PHRASE_QUERY_STATE);
                if(state != null && state.booleanValue()){
                    StringBuilder sb = encodePhraseQuery(phraseTerms);
                    if(value.getBoost() != null){
                        sb.append("^").append(value.getBoost());
                    }
                    constraint.addEncoded(POS, sb.toString());
                }
            }
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
    public Class<ConstraintValue> acceptsValueType() {
        return ConstraintValue.class;
    }

}
