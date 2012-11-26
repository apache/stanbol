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
package org.apache.stanbol.enhancer.engines.sentiment.summarize;

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.PHRASE_ANNOTATION;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.SENTIMENT_ANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.ServiceProperties.ENHANCEMENT_ENGINE_ORDERING;
import static org.apache.stanbol.enhancer.servicesapi.ServiceProperties.ORDERING_EXTRACTION_ENHANCEMENT;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.createTextEnhancement;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Span.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.utils.NIFHelper;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link EnhancementEngine} that summarizes {@link Token} level
 * Sentiment tags for NounPhraces, Sentences and the whole
 * Content.
 * @author Rupert Westenthaler
 *
 */
@Component(immediate = true, metatype = true, 
    policy=ConfigurationPolicy.OPTIONAL,
    configurationFactory=true) //allow multiple instances to be configured
@Service
@Properties(value={
    @Property(name= EnhancementEngine.PROPERTY_NAME,value=SentimentSummarizationEngine.DEFAULT_ENGINE_NAME),
    @Property(name=Constants.SERVICE_RANKING,intValue=-100) //give the default instance a ranking < 0
})
public class SentimentSummarizationEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String DEFAULT_ENGINE_NAME = "sentiment-summarization";
    
    //TODO: change this to a real sentiment ontology
    /**
     * The property used to write the sum of all positive classified words
     */
    public static final UriRef POSITIVE_SENTIMENT_PROPERTY = new UriRef(NamespaceEnum.fise+"positive-sentiment");
    /**
     * The property used to write the sum of all negative classified words
     */
    public static final UriRef NEGATIVE_SENTIMENT_PROPERTY = new UriRef(NamespaceEnum.fise+"negative-sentiment");
    /**
     * The sentiment of the section (sum of positive and negative classifications)
     */
    public static final UriRef SENTIMENT_PROPERTY = new UriRef(NamespaceEnum.fise+"sentiment");
    /**
     * The dc:type value used for fise:TextAnnotations indicating a Sentiment
     */
    public static final UriRef SENTIMENT_TYPE = new UriRef(NamespaceEnum.fise+"Sentiment");
    boolean writeNounPhraseSentiments = true;
    boolean writeSentenceSentimets = true;
    boolean writeTextSectionSentiments = true;
    boolean wirteDocumentSentiments = true;
    boolean writeTextSentiments = true;
    
    private final LiteralFactory lf = LiteralFactory.getInstance();
    
    @Override
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        log.info(" activate {} with config {}",getClass().getSimpleName(),ctx.getProperties());
        super.activate(ctx);
    }
    
    @Override
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        super.deactivate(ctx);
    }
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        return NlpEngineHelper.getAnalysedText(this, ci, false) != null ?
               ENHANCE_ASYNC : CANNOT_ENHANCE; 
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at = NlpEngineHelper.getAnalysedText(this, ci, true);
        //configure the spanTypes based on the configuration
        EnumSet<Span.SpanTypeEnum> spanTypes = EnumSet.noneOf(SpanTypeEnum.class);
        if(writeNounPhraseSentiments){
            spanTypes.add(SpanTypeEnum.Chunk);
        }
        if(writeSentenceSentimets){
            spanTypes.add(SpanTypeEnum.Sentence);
        }
        if(writeTextSectionSentiments){
            spanTypes.add(SpanTypeEnum.TextSection);
        }
        if(writeTextSentiments ){
            spanTypes.add(SpanTypeEnum.Text);
        }
        
        List<SentimentInfo> sentimentInfos = summarizeSentiments(at, spanTypes);
        String detectedLang = EnhancementEngineHelper.getLanguage(ci);
        ci.getLock().writeLock().lock();
        try {
            writeSentimentEnhancements(ci,sentimentInfos,at,
                detectedLang == null ? null : new Language(detectedLang));
        } finally {
            ci.getLock().writeLock().unlock();
        }
        
    }
    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object)ORDERING_EXTRACTION_ENHANCEMENT);
    }
    /**
     * 
     * @param at
     * @return
     */
    private List<SentimentInfo> summarizeSentiments(AnalysedText at, EnumSet<SpanTypeEnum> spanTypes) {
        spanTypes.add(SpanTypeEnum.Token);
        Iterator<Span> tokenIt = at.getEnclosed(spanTypes);
        // use double array of length 1 as value to avoid final double values
        //List with the section that contain sentiments
        List<SentimentInfo> sentimentInfos = new ArrayList<SentimentInfo>();
        NavigableMap<Span,SentimentInfo> activeSpans = new TreeMap<Span,SentimentInfo>();
        if(spanTypes.contains(SpanTypeEnum.Text)){
            activeSpans.put(at, new SentimentInfo(at));
        }
        while(tokenIt.hasNext()){
            Span span = tokenIt.next();
            switch (span.getType()) {
                case Token:
                    Value<Double> sentiment = span.getAnnotation(SENTIMENT_ANNOTATION);
                    Iterator<Entry<Span,SentimentInfo>> entries = activeSpans.entrySet().iterator();
                    if(sentiment != null){
                        while(entries.hasNext()){
                            Entry<Span,SentimentInfo> entry = entries.next();
                            //if(span.getEnd() > entry.getKey().getEnd()){ //fully enclosed
                            if(entry.getKey().getEnd() > span.getStart()){ //partly enclosed
                                entry.getValue().addSentiment(sentiment.value());
                            } else { // span has completed
                                if(entry.getValue().hasSentiment()){ //if a sentiment was found
                                    //add it to the list
                                    sentimentInfos.add(entry.getValue());
                                }
                                entries.remove(); // remove completed
                            }
                        }
                    }
                    break;
                case Chunk:
                    Value<PhraseTag> phraseTag = span.getAnnotation(PHRASE_ANNOTATION);
                    if(phraseTag.value().getCategory() == LexicalCategory.Noun){
                        //noun phrase
                        activeSpans.put(span, new SentimentInfo((Section)span));
                    }
                    break;
                case Sentence:
                    activeSpans.put(span, new SentimentInfo((Section)span));
                    break;
                case TextSection:
                    activeSpans.put(span, new SentimentInfo((Section)span));
                    break;
                default:
                    break;
            }
        }
        //finally cleanup still active Sections
        for(SentimentInfo sentInfo : activeSpans.values()){
            if(sentInfo.hasSentiment()){
                sentimentInfos.add(sentInfo);
            } //else no sentiment in that section
        }
        return sentimentInfos;
    }

    private void writeSentimentEnhancements(ContentItem ci, List<SentimentInfo> sentimentInfos, AnalysedText at, Language lang) {
        // TODO Auto-generated method stub
        MGraph metadata = ci.getMetadata();
        for(SentimentInfo sentInfo : sentimentInfos){
            UriRef enh = createTextEnhancement(ci, this);
            if(sentInfo.getSection().getType() == SpanTypeEnum.Chunk) {
                metadata.add(new TripleImpl(enh, ENHANCER_SELECTED_TEXT, 
                    new PlainLiteralImpl(sentInfo.getSection().getSpan(), lang)));
                metadata.add(new TripleImpl(enh, ENHANCER_SELECTION_CONTEXT, 
                    new PlainLiteralImpl(getSelectionContext(
                        at.getSpan(), 
                        sentInfo.getSection().getSpan(), 
                        sentInfo.getSection().getStart()))));
                //NOTE: fall through intended!
            } else if(sentInfo.getSection().getType() != SpanTypeEnum.Text){ //sentence, textsection
                //For longer selections it does not make sense to include selection context
                //and the selected text.
                //We can add prefix, suffix, selection-start, selection-end
                //as soon as we use the new TextAnnotation model
            }
            //add start/end positions
            if(sentInfo.getSection().getType() != SpanTypeEnum.Text){
                metadata.add(new TripleImpl(enh, ENHANCER_START, 
                    lf.createTypedLiteral(sentInfo.getSection().getStart())));
                metadata.add(new TripleImpl(enh, ENHANCER_END, 
                    lf.createTypedLiteral(sentInfo.getSection().getEnd())));
            } //else do not add start/end pos for sentiment of the whole text
            
            //add the sentiment information
            if(sentInfo.getPositive() != null){
                metadata.add(new TripleImpl(enh, POSITIVE_SENTIMENT_PROPERTY, 
                    lf.createTypedLiteral(sentInfo.getPositive())));
            }
            if(sentInfo.getNegative() != null){
                metadata.add(new TripleImpl(enh, NEGATIVE_SENTIMENT_PROPERTY, 
                    lf.createTypedLiteral(sentInfo.getNegative())));
            }
            metadata.add(new TripleImpl(enh, SENTIMENT_PROPERTY, 
                lf.createTypedLiteral(sentInfo.getSentiment())));

            //add the Sentiment type as well as the type of the SSO Ontology
            metadata.add(new TripleImpl(enh, DC_TYPE, SENTIMENT_TYPE));
            UriRef ssoType = NIFHelper.SPAN_TYPE_TO_SSO_TYPE.get(sentInfo.getSection().getType());
            if(ssoType != null){
                metadata.add(new TripleImpl(enh, DC_TYPE, ssoType));
            }
        }
    }
    
    
    /**
     * The maximum size of the preix/suffix for the selection context
     */
    private static final int DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE = 50;
    /**
     * Extracts the selection context based on the content, selection and
     * the start char offset of the selection
     * @param content the content
     * @param selection the selected text
     * @param selectionStartPos the start char position of the selection
     * @return the context
     */
    public static String getSelectionContext(String content, String selection,int selectionStartPos){
        //extract the selection context
        int beginPos;
        if(selectionStartPos <= DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE){
            beginPos = 0;
        } else {
            int start = selectionStartPos-DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            beginPos = content.indexOf(' ',start);
            if(beginPos < 0 || beginPos >= selectionStartPos){ //no words
                beginPos = start; //begin within a word
            }
        }
        int endPos;
        if(selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE >= content.length()){
            endPos = content.length();
        } else {
            int start = selectionStartPos+selection.length()+DEFAULT_SELECTION_CONTEXT_PREFIX_SUFFIX_SIZE;
            endPos = content.lastIndexOf(' ', start);
            if(endPos <= selectionStartPos+selection.length()){
                endPos = start; //end within a word;
            }
        }
        return content.substring(beginPos, endPos);
    }    
}
