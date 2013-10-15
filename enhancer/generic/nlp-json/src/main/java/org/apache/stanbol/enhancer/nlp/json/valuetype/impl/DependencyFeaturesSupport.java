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

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.dependency.DependencyFeatures;
import org.apache.stanbol.enhancer.nlp.dependency.DependencyRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelation;
import org.apache.stanbol.enhancer.nlp.dependency.GrammaticalRelationTag;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Span.SpanTypeEnum;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value={ValueTypeParser.class,ValueTypeSerializer.class})
@Property(name=ValueTypeParser.PROPERTY_TYPE, value=DependencyFeaturesSupport.TYPE_VALUE)
public class DependencyFeaturesSupport implements ValueTypeParser<DependencyFeatures>, ValueTypeSerializer<DependencyFeatures> {

	public static final String TYPE_VALUE = "org.apache.stanbol.enhancer.nlp.dependency.DependencyFeatures";
	
	private static final String RELATION_TYPE_TAG = "tag";
	private static final String RELATION_STANBOL_TYPE_TAG = "relationType";
	private static final String RELATION_IS_DEPENDEE_TAG = "isDependent";
	private static final String RELATION_PARTNER_TYPE_TAG = "type";
	private static final String RELATION_PARTNER_START_TAG = "start";
	private static final String RELATION_PARTNER_END_TAG = "end";
	private static final String RELATIONS_TAG = "relations";
	private static final String ROOT_TAG = "ROOT";
	
	@Override
	public ObjectNode serialize(ObjectMapper mapper, DependencyFeatures value) {
		ObjectNode jDependencyFeature = mapper.createObjectNode();
		
        Set<DependencyRelation> relations = value.getRelations();
        
        if(!relations.isEmpty()) {
            ArrayNode jRelations = mapper.createArrayNode();
            
            for(DependencyRelation relation : relations) {
                ObjectNode jRelation = mapper.createObjectNode();
                
                GrammaticalRelationTag gramRelTag = relation.getGrammaticalRelationTag();
                jRelation.put(RELATION_TYPE_TAG, gramRelTag.getTag());
                jRelation.put(RELATION_STANBOL_TYPE_TAG, gramRelTag.getGrammaticalRelation().ordinal());
                jRelation.put(RELATION_IS_DEPENDEE_TAG, (relation.isDependent() ? "true" : "false"));
                
                Span partner = relation.getPartner();
                if (partner != null) {
	                jRelation.put(RELATION_PARTNER_TYPE_TAG, partner.getType().toString());
	                jRelation.put(RELATION_PARTNER_START_TAG, partner.getStart());
	                jRelation.put(RELATION_PARTNER_END_TAG, partner.getEnd());
                } else {
                	jRelation.put(RELATION_PARTNER_TYPE_TAG, "ROOT");
	                jRelation.put(RELATION_PARTNER_START_TAG, 0);
	                jRelation.put(RELATION_PARTNER_END_TAG, 0);
                }
                
                jRelations.add(jRelation);
            }
            
            jDependencyFeature.put(RELATIONS_TAG, jRelations);
        }
        
		return jDependencyFeature;
	}

	@Override
	public Class<DependencyFeatures> getType() {
		return DependencyFeatures.class;
	}

	@Override
	public DependencyFeatures parse(ObjectNode jDependencyFeature, AnalysedText at) {
		DependencyFeatures dependencyFeature = new DependencyFeatures();
		
		JsonNode node = jDependencyFeature.path(RELATIONS_TAG);
        
		if(node.isArray()) {
            ArrayNode jRelations = (ArrayNode)node;
            
            for(int i=0;i<jRelations.size();i++) {
                JsonNode member = jRelations.get(i);
                
                if(member.isObject()) {
                    ObjectNode jRelation = (ObjectNode)member;
                    
                    JsonNode tag = jRelation.path(RELATION_TYPE_TAG);
                    
                    if(!tag.isTextual()){
                        throw new IllegalStateException("Unable to parse GrammaticalRelationTag. The value of the "
                                +"'tag' field MUST have a textual value (json: " + jDependencyFeature + ")");
                    }
                    
                    GrammaticalRelation grammaticalRelation = GrammaticalRelation.class.getEnumConstants()[jRelation.path(RELATION_STANBOL_TYPE_TAG).asInt()];
                    GrammaticalRelationTag gramRelTag = new GrammaticalRelationTag(tag.getTextValue(), grammaticalRelation);
                    
                    JsonNode isDependent = jRelation.path(RELATION_IS_DEPENDEE_TAG);
                    
                    if (!isDependent.isBoolean()) {
                    	throw new IllegalStateException("Field 'isDependent' must have a true/false format");
                    }
                    
                    Span partnerSpan = null;
                    String typeString = jRelation.path(RELATION_PARTNER_TYPE_TAG).getTextValue();
                    
                    if (!typeString.equals(ROOT_TAG)) {
	                    SpanTypeEnum spanType = SpanTypeEnum.valueOf(jRelation.path(RELATION_PARTNER_TYPE_TAG).getTextValue());
	                    int spanStart = jRelation.path(RELATION_PARTNER_START_TAG).asInt();
	                    int spanEnd = jRelation.path(RELATION_PARTNER_END_TAG).asInt();
	                    
	                    
	                    switch (spanType) {
							case Chunk:
								partnerSpan = at.addChunk(spanStart, spanEnd);
								break;
							case Sentence:
							case Text:
							case TextSection:
								break;
							case Token:
								partnerSpan = at.addToken(spanStart, spanEnd);
								break;
	                    }
                    }    
                    
                    DependencyRelation relation = new DependencyRelation(gramRelTag, isDependent.asBoolean(), partnerSpan);
                    dependencyFeature.addRelation(relation);
                }
            }
		}    
                
		return dependencyFeature;
	}
}
