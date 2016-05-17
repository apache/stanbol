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
package org.apache.stanbol.enhancer.engines.entitycomention.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.engines.entitycomention.CoMentionConstants;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkingStateAware;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentItemMentionBuilder extends InMemoryEntityIndex implements LinkingStateAware{

    private static final Logger log = LoggerFactory.getLogger(ContentItemMentionBuilder.class);
    private static final LiteralFactory lf = LiteralFactory.getInstance();
    
    /**
     * The last index notified via {@link #startToken(Token)}
     */
    private Integer lastIndex = 0; 
    
    private SortedMap<Integer,Collection<EntityMention>> mentionIndex = new TreeMap<Integer,Collection<EntityMention>>();
    
    public ContentItemMentionBuilder(LabelTokenizer labelTokenizer, String...languages){
        super(labelTokenizer,CoMentionConstants.CO_MENTION_LABEL_FIELD, languages);
    }

    public void registerTextAnnotation(IRI textAnnotation, Graph metadata){
        String selectedText = EnhancementEngineHelper.getString(metadata, textAnnotation, ENHANCER_SELECTED_TEXT);
        if(selectedText != null){
            //NOTE: Typically it is not possible to find co-mentions for Entities with a
            //      single Token, so can ignore those.
            //      The only exception would be to use proper-nouns for initial linking and
            //      Nouns for the co-mention resolution. In such cases this might result
            //      in additional extractions.
            String[] tokens = tokenizer.tokenize(selectedText, language);
            if(tokens != null && tokens.length > 1){ //TODO make configurable
                Double confidence = EnhancementEngineHelper.get(metadata,textAnnotation,ENHANCER_CONFIDENCE,Double.class,lf);
                if(confidence == null || confidence > 0.85){ //TODO make configurable
                    Integer start = EnhancementEngineHelper.get(metadata,textAnnotation,ENHANCER_START,Integer.class,lf);
                    Integer end = EnhancementEngineHelper.get(metadata,textAnnotation,ENHANCER_END,Integer.class,lf);
                    registerMention(new EntityMention(textAnnotation,metadata, ENHANCER_SELECTED_TEXT, DC_TYPE, 
                        start != null && end != null ? new Integer[]{start,end} : null));
                } // else confidence to low
            } else if(tokens == null){
                log.warn("Unable to tokenize \"{}\"@{} via tokenizer {} (class: {})!", new Object []{
                    selectedText,language,tokenizer, tokenizer.getClass().getName()});
            } //else ignore Tokens with a single token
        } // else no selected text
    }

    private void registerMention(EntityMention entityMention){
        log.debug(" > register {} ",entityMention);
        if(entityMention.getStart() == null || entityMention.getStart() < 0){
            addEntity(entityMention);
        } else {
            Collection<EntityMention> mentions = mentionIndex.get(entityMention.getEnd());
            if(mentions == null){
                mentions = new ArrayList<EntityMention>();
                mentionIndex.put(entityMention.getEnd(), mentions);
            }
            mentions.add(entityMention);
        }
    }

    /**
     * Everytime the entityLinker starts to process a token we need to check
     * if we need to add additional contextual information from the {@link ContentItem}
     * to the {@link InMemoryEntityIndex}
     */
    @Override
    public void startToken(Token token) {
        log.debug(" > start token: {}",token);
        final Integer actIndex = token.getStart();
        if(actIndex > lastIndex){
            for(Collection<EntityMention> mentions : mentionIndex.subMap(lastIndex, actIndex).values()){
                for(EntityMention mention : mentions){
                    addEntity(mention);
                }
            }
            lastIndex = actIndex;
        } else if(lastIndex > actIndex){
            log.warn("Token {} has earlier start index as the last one {}!", token, lastIndex);
        } // else the same index ... ignore
    }

    @Override
    public void startSection(Section sentence) {/* not used */}
    @Override
    public void endSection(Section sentence) {/* not used */}
    @Override
    public void endToken(Token token) {/* not used */}
    
        
}
