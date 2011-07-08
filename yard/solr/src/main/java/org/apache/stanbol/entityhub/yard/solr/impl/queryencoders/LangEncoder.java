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

import org.apache.stanbol.commons.solr.utils.SolrUtil;
import org.apache.stanbol.entityhub.yard.solr.model.FieldMapper;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.query.QueryUtils;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;

public class LangEncoder implements IndexConstraintTypeEncoder<Collection<String>> {

    private static final ConstraintTypePosition PREFIX = new ConstraintTypePosition(PositionType.prefix);
    // private static final ConstraintTypePosition SUFFIX = new ConstraintTypePosition(PositionType.suffux);
    // deactivated, because xsd:string values are now also included in the language
    // merger field (the name returned by fieldMapper.getLanguageMergerField(null)).
    // private static final IndexDataType STRING_DATATYPE = new IndexDataType(NamespaceEnum.xsd+"string");
    private FieldMapper fieldMapper;

    public LangEncoder(FieldMapper fieldMapper) {
        this.fieldMapper = fieldMapper;
    }

    @Override
    public void encode(EncodedConstraintParts constraint, Collection<String> value) {
        //We need to process languages, because one may parse null, or
        //an empty list or a list that contains a single element "null"
        Collection<String> languages;
        if(value == null || value.isEmpty()){
            languages = Collections.emptyList();
        } else {
            languages = new HashSet<String>();
            for(String lang : value){
                if(lang != null){
                    languages.add(lang);
                }
            }
        }
        if (!languages.isEmpty()) {
            for (String prefix : fieldMapper.encodeLanguages(value)) {
                constraint.addEncoded(PREFIX, SolrUtil.escapeSolrSpecialChars(prefix));
            }
        } else { // default
            // search in the language merger field of the default language
            constraint.addEncoded(PREFIX,
                SolrUtil.escapeSolrSpecialChars(fieldMapper.getLanguageMergerField(null)));
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
        return IndexConstraintTypeEnum.LANG;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Collection<String>> acceptsValueType() {
        // generic types are ereased anyway!
        return (Class<Collection<String>>) (Class<?>) Collection.class;
    }

}
