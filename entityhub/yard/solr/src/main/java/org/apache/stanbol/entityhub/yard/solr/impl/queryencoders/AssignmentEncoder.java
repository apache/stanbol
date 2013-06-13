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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint.MODE;
import org.apache.stanbol.entityhub.yard.solr.defaults.QueryConst;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrQueryFactory.ConstraintValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValue;
import org.apache.stanbol.entityhub.yard.solr.model.IndexValueFactory;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.QueryUtils;
import org.apache.stanbol.entityhub.yard.solr.query.QueryUtils.QueryTerm;

/**
 * Encodes the Assignment of the field to an value. If a value is parsed, than it encodes that the field must
 * be equals to this value.
 * 
 * @author Rupert Westenthaler
 */
public class AssignmentEncoder implements IndexConstraintTypeEncoder<ConstraintValue> {

    public static final ConstraintTypePosition POS = new ConstraintTypePosition(PositionType.assignment);
    public static final String EQ = ":";
//    private final IndexValueFactory indexValueFactory;

    public AssignmentEncoder(IndexValueFactory indexValueFactory) {
//        if (indexValueFactory == null) {
//            throw new IllegalArgumentException("The indexValueFactory MUST NOT be NULL");
//        }
//        this.indexValueFactory = indexValueFactory;
    }

    @Override
    public void encode(EncodedConstraintParts constraint, ConstraintValue value) {
        if(value == null){ //if no value is parsed
            constraint.addEncoded(POS,EQ); // add the default
            return; //and return
        } //else encode the values and add them depending on the MODE
        Set<String> queryConstraints = new HashSet<String>();
        Collection<String> phraseTerms = new ArrayList<String>();
        for(IndexValue indexValue : value){
            QueryTerm[] qts = QueryUtils.encodeQueryValue(indexValue, true);
            if (qts != null) {
                for (QueryTerm qt : qts) {
                    StringBuilder sb = new StringBuilder(qt.term.length() + 
                        (qt.needsQuotes ? 3 : 1));
                    sb.append(EQ);
                    if(qt.needsQuotes){
                        sb.append('"').append(qt.term).append('"');
                    } else {
                        sb.append(qt.term);
                    }
                    if(value.getBoost() != null){
                        sb.append("^").append(value.getBoost());
                    }
                    queryConstraints.add(sb.toString());
                    if(!qt.hasWildcard && qt.isText) {
                        phraseTerms.add(qt.term);
                    }
                }
            } else {
                queryConstraints.add(EQ);
            }
            if(value.getMode() == MODE.any){
                //in any mode we need to add values separately
                constraint.addEncoded(POS, queryConstraints);
                //addEncoded copies the added values so we can clear and reuse
                queryConstraints.clear(); 
            }
        }
        if(value.getMode() == MODE.all){
            //in all mode we need to add all values in a single call
            constraint.addEncoded(POS, queryConstraints);
            //NOTE also that for ALL mode Phrase queries do not make sense, as
            //     they would weaken the selection criteria
        } else {
            if(phraseTerms.size() > 1){
                Boolean state = (Boolean) value.getProperty(QueryConst.PHRASE_QUERY_STATE);
                if(state != null && state.booleanValue()){
                    StringBuilder sb = encodePhraseQuery(phraseTerms);
                    sb.insert(0, EQ);
                    if(value.getBoost() != null){
                        sb.append("^").append(value.getBoost());
                    }
                    constraint.addEncoded(POS, sb.toString());
                }//phrase query deactivated
            } //else for less than two terms we can not build a phrase query
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
    public Class<ConstraintValue> acceptsValueType() {
        return ConstraintValue.class;
    }

}
