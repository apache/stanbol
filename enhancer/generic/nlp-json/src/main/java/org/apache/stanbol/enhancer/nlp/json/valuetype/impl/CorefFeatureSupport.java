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

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.coref.CorefFeature;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser;
import org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value={ValueTypeParser.class,ValueTypeSerializer.class})
@Property(name=ValueTypeParser.PROPERTY_TYPE, value=CorefFeatureSupport.TYPE_VALUE)
public class CorefFeatureSupport implements ValueTypeParser<CorefFeature>, ValueTypeSerializer<CorefFeature> {
	
	public static final String TYPE_VALUE = "org.apache.stanbol.enhancer.nlp.coref.CorefFeature";
	
	private static final String IS_REPRESENTATIVE_TAG = "isRepresentative";
	private static final String MENTIONS_TAG = "mentions";
	private static final String MENTION_TYPE_TAG = "type";
	private static final String MENTION_START_TAG = "start";
	private static final String MENTION_END_TAG = "end";
	
	@Override
	public ObjectNode serialize(ObjectMapper mapper, CorefFeature coref) {
		ObjectNode jCoref = mapper.createObjectNode();
		
		jCoref.put(IS_REPRESENTATIVE_TAG, coref.isRepresentative());
		
        Set<Span> mentions = coref.getMentions(); 
        ArrayNode jMentions = mapper.createArrayNode();
        
        for(Span mention : mentions) {
            ObjectNode jMention = mapper.createObjectNode();
            
            jMention.put(MENTION_TYPE_TAG, mention.getType().toString());
            jMention.put(MENTION_START_TAG, mention.getStart());
            jMention.put(MENTION_END_TAG, mention.getEnd());
            
            jMentions.add(jMention);
        }
        
        jCoref.put(MENTIONS_TAG, jMentions);
        
		return jCoref;
	}

	@Override
	public Class<CorefFeature> getType() {
		return CorefFeature.class;
	}

	@Override
	public CorefFeature parse(ObjectNode jCoref, AnalysedText at) {
		JsonNode jIsRepresentative = jCoref.path(IS_REPRESENTATIVE_TAG);
		
		if (!jIsRepresentative.isBoolean()) {
			throw new IllegalStateException("Field 'isRepresentative' must have a true/false format");
		}
		
		JsonNode node = jCoref.path(MENTIONS_TAG);
		Set<Span> mentions = new HashSet<Span>();
		
		if(node.isArray()) {
            ArrayNode jMentions = (ArrayNode)node;
            
            for(int i = 0;i < jMentions.size();i++) {
                JsonNode member = jMentions.get(i);
                
                if(member.isObject()) {
                    ObjectNode jMention = (ObjectNode)member;
                    SpanTypeEnum spanType = SpanTypeEnum.valueOf(jMention.path(MENTION_TYPE_TAG).getTextValue());
                    int spanStart = jMention.path(MENTION_START_TAG).asInt();
                    int spanEnd = jMention.path(MENTION_END_TAG).asInt();
                    Span mentionedSpan = null;
                    
                    switch (spanType) {
						case Chunk:
							mentionedSpan = at.addChunk(spanStart, spanEnd);
							break;
						case Sentence:
						case Text:
						case TextSection:
							break;
						case Token:
							mentionedSpan = at.addToken(spanStart, spanEnd);
							break;
                    
                    }
                    
                    mentions.add(mentionedSpan);
                }
            }
		}    
        
		return new CorefFeature(jIsRepresentative.asBoolean(), mentions);
	}	
}