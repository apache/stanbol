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

import org.apache.stanbol.commons.solr.utils.SolrUtil;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.defaults.IndexDataTypeEnum;
import org.apache.stanbol.entityhub.yard.solr.model.FieldMapper;
import org.apache.stanbol.entityhub.yard.solr.model.IndexDataType;
import org.apache.stanbol.entityhub.yard.solr.model.IndexField;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition;
import org.apache.stanbol.entityhub.yard.solr.query.ConstraintTypePosition.PositionType;
import org.apache.stanbol.entityhub.yard.solr.query.EncodedConstraintParts;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEncoder;
import org.apache.stanbol.entityhub.yard.solr.query.IndexConstraintTypeEnum;

public class LangEncoder implements IndexConstraintTypeEncoder<IndexField> {

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
    public void encode(EncodedConstraintParts constraint, IndexField value) {
        //We need to process languages, because one may parse null, or
        //an empty list or a list that contains a single element "null"
        Collection<String> languages;
        if(value == null){
            languages = Collections.emptyList();
        } else {
            languages = value.getLanguages();
        }
        if(value.getDataType().equals(IndexDataTypeEnum.TXT.getIndexType())){
            if (!languages.isEmpty()) {
                for (String prefix : fieldMapper.encodeLanguages(value)) {
                    constraint.addEncoded(PREFIX, SolrUtil.escapeSolrSpecialChars(prefix));
                }
            } else { // default
                // search in the language merger field of the default language
                constraint.addEncoded(PREFIX,
                    SolrUtil.escapeSolrSpecialChars(fieldMapper.getLanguageMergerField(null)));
            }
        } //else no Text field -> do not add language prefixes
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

    @Override
    public Class<IndexField> acceptsValueType() {
        // generic types are ereased anyway!
        return IndexField.class;
    }

}
