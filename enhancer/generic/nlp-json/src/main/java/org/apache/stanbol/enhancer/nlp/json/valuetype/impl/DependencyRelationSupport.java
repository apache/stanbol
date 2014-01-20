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
 */package org.apache.stanbol.enhancer.nlp.json.valuetype.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelationTag;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Component(immediate = true, policy = ConfigurationPolicy.IGNORE)
@Service(value = {ValueTypeParser.class, ValueTypeSerializer.class})
@Property(name = ValueTypeParser.PROPERTY_TYPE, value = DependencyRelationSupport.TYPE_VALUE)
public class DependencyRelationSupport implements ValueTypeParser<DependencyRelation>,
        ValueTypeSerializer<DependencyRelation> {

    public static final String TYPE_VALUE = "org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation";

    private static final String RELATION_TYPE_TAG = "tag";
    private static final String RELATION_STANBOL_TYPE_TAG = "relationType";
    private static final String RELATION_IS_DEPENDENT_TAG = "isDependent";
    private static final String RELATION_PARTNER_TYPE_TAG = "partnerType";
    private static final String RELATION_PARTNER_START_TAG = "partnerStart";
    private static final String RELATION_PARTNER_END_TAG = "partnerEnd";
    private static final String ROOT_TAG = "ROOT";

    @Override
    public ObjectNode serialize(ObjectMapper mapper, DependencyRelation relation) {
        ObjectNode jDependencyRelation = mapper.createObjectNode();

        GrammaticalRelationTag gramRelTag = relation.getGrammaticalRelationTag();
        jDependencyRelation.put(RELATION_TYPE_TAG, gramRelTag.getTag());
        jDependencyRelation.put(RELATION_STANBOL_TYPE_TAG, gramRelTag.getGrammaticalRelation().ordinal());
        jDependencyRelation.put(RELATION_IS_DEPENDENT_TAG, (relation.isDependent()));

        Span partner = relation.getPartner();
        if (partner != null) {
            jDependencyRelation.put(RELATION_PARTNER_TYPE_TAG, partner.getType().toString());
            jDependencyRelation.put(RELATION_PARTNER_START_TAG, partner.getStart());
            jDependencyRelation.put(RELATION_PARTNER_END_TAG, partner.getEnd());
        } else {
            jDependencyRelation.put(RELATION_PARTNER_TYPE_TAG, ROOT_TAG);
            jDependencyRelation.put(RELATION_PARTNER_START_TAG, 0);
            jDependencyRelation.put(RELATION_PARTNER_END_TAG, 0);
        }

        return jDependencyRelation;
    }

    @Override
    public Class<DependencyRelation> getType() {
        return DependencyRelation.class;
    }

    @Override
    public DependencyRelation parse(ObjectNode jDependencyRelation, AnalysedText at) {
        JsonNode tag = jDependencyRelation.path(RELATION_TYPE_TAG);

        if (!tag.isTextual()) {
            throw new IllegalStateException("Unable to parse GrammaticalRelationTag. The value of the "
                                            + "'tag' field MUST have a textual value (json: "
                                            + jDependencyRelation + ")");
        }

        GrammaticalRelation grammaticalRelation = GrammaticalRelation.class.getEnumConstants()[jDependencyRelation
                .path(RELATION_STANBOL_TYPE_TAG).asInt()];
        GrammaticalRelationTag gramRelTag = new GrammaticalRelationTag(tag.getTextValue(),
                grammaticalRelation);

        JsonNode isDependent = jDependencyRelation.path(RELATION_IS_DEPENDENT_TAG);

        if (!isDependent.isBoolean()) {
            throw new IllegalStateException("Field 'isDependent' must have a true/false format");
        }

        Span partnerSpan = null;
        String typeString = jDependencyRelation.path(RELATION_PARTNER_TYPE_TAG).getTextValue();

        if (!typeString.equals(ROOT_TAG)) {
            SpanTypeEnum spanType = SpanTypeEnum.valueOf(jDependencyRelation.path(RELATION_PARTNER_TYPE_TAG)
                    .getTextValue());
            int spanStart = jDependencyRelation.path(RELATION_PARTNER_START_TAG).asInt();
            int spanEnd = jDependencyRelation.path(RELATION_PARTNER_END_TAG).asInt();

            switch (spanType) {
                case Chunk:
                    partnerSpan = at.addChunk(spanStart, spanEnd);
                    break;
                // unused types
                // case Sentence:
                // case Text:
                // case TextSection:
                // break;
                case Token:
                    partnerSpan = at.addToken(spanStart, spanEnd);
                    break;
            }
        }

        return new DependencyRelation(gramRelTag, isDependent.asBoolean(), partnerSpan);
    }
}
