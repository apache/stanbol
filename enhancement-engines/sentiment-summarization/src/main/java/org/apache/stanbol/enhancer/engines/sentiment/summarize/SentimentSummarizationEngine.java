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

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.SENTIMENT_ANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.createTextEnhancement;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTED_TEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_SELECTION_CONTEXT;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
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
    @Property(name=EnhancementEngine.PROPERTY_NAME,value=SentimentSummarizationEngine.DEFAULT_ENGINE_NAME),
    @Property(name=SentimentSummarizationEngine.PROPERTY_DOCUMENT_SENTIMENT_STATE, boolValue=true),
    @Property(name=SentimentSummarizationEngine.PROPERTY_SENTENCE_SENTIMENT_STATE, boolValue=true),
    @Property(name=SentimentSummarizationEngine.PROPERTY_PHRASE_SENTIMENT_STATE, boolValue=true),
    @Property(name=Constants.SERVICE_RANKING,intValue=-100) //give the default instance a ranking < 0
})
public class SentimentSummarizationEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException> implements ServiceProperties {

    public static final String PROPERTY_PHRASE_SENTIMENT_STATE = "enhancer.engine.sentiment.summarization.phraseSentimentState";
    public static final boolean DEFAULT_PHRASE_SENTIMENT_STATE = true;
    public static final String PROPERTY_SENTENCE_SENTIMENT_STATE = "enhancer.engine.sentiment.summarization.sentenceSentimentState";
    public static final boolean DEFAULT_SENTENCE_SENTIMENT_STATE = true;
    public static final String PROPERTY_DOCUMENT_SENTIMENT_STATE = "enhancer.engine.sentiment.summarization.documentSentimentState";
    public static final boolean DEFAULT_DOCUMENT_SENTIMENT_STATE = true;
//    public static final String PROPERTY_NOUN_CONTEXT_SIZE = "enhancer.engine.sentiment.summarization.nounContextSize";
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final EnumSet<Pos> DEFAULT_SECTION_BORDER_TAGS  = EnumSet.of(
        Pos.SentenceMedialPunctuation);
    
    private static final EnumSet<Pos> DEFAULT_NEGATION_TAGS = EnumSet.of(
        Pos.NegativeAdverb,Pos.NegativeDeterminer, Pos.NegativeParticle,
        Pos.NegativePronoun);
    
    private static final EnumSet<LexicalCategory> DEFAULT_COUNT_LEXICAL_CATEGORIES = EnumSet.of(
        LexicalCategory.Noun,LexicalCategory.Verb,LexicalCategory.Adjective);
    
    
    private static final Double ZERO = Double.valueOf(0.0);
    
    public static final String DEFAULT_ENGINE_NAME = "sentiment-summarization";
    
    //TODO: change this to a real sentiment ontology
    /**
     * The property used to write the sum of all positive classified words
     */
    public static final IRI POSITIVE_SENTIMENT_PROPERTY = new IRI(NamespaceEnum.fise+"positive-sentiment");
    /**
     * The property used to write the sum of all negative classified words
     */
    public static final IRI NEGATIVE_SENTIMENT_PROPERTY = new IRI(NamespaceEnum.fise+"negative-sentiment");
    /**
     * The sentiment of the section (sum of positive and negative classifications)
     */
    public static final IRI SENTIMENT_PROPERTY = new IRI(NamespaceEnum.fise+"sentiment");
    /**
     * The dc:type value used for fise:TextAnnotations indicating a Sentiment
     */
    public static final IRI SENTIMENT_TYPE = new IRI(NamespaceEnum.fise+"Sentiment");
    /**
     * The dc:Type value sued for the sentiment annotation of the whole document
     */
    public static final IRI DOCUMENT_SENTIMENT_TYPE = new IRI(NamespaceEnum.fise+"DocumentSentiment");


    private static final int DEFAULT_NEGATION_CONTEXT = 2;
    private static final int DEFAULT_CONJUCTION_CONTEXT = 1;

    private static final int DEFAULT_NOUN_CONTEXT = 4;

    boolean writeSentimentPhrases = true;
    boolean writeSentencesSentimet = true;
    boolean writeDocumentSentiment = true;
    boolean writeSentimentData = false;
    
    private EnumSet<Pos> negativePosTags = DEFAULT_NEGATION_TAGS;
    private EnumSet<Pos> sectionBorderPosTags = DEFAULT_SECTION_BORDER_TAGS;
    private EnumSet<LexicalCategory> countableLexCats = DEFAULT_COUNT_LEXICAL_CATEGORIES;
    
    private final LiteralFactory lf = LiteralFactory.getInstance();


    private int negationContext = DEFAULT_NEGATION_CONTEXT;
    private int nounContext = DEFAULT_NOUN_CONTEXT;
 
    private int conjuctionContext = DEFAULT_CONJUCTION_CONTEXT;
    
    /**
     * Used to sort {@link Sentiment}s before merging them to {@link SentimentPhrase}s
     */
    private static final Comparator<Sentiment> sentimentComparator = new Comparator<Sentiment>(){

        @Override
        public int compare(Sentiment s1, Sentiment s2) {
            if(s1.getStart() == s2.getStart()){
                return s1.getEnd() > s2.getEnd() ? -1 : s1.getEnd() == s2.getEnd() ?  0 : -1;
            } else {
                return s1.getStart() < s2.getStart() ? -1 : 1;
            }
        }
        
    };
    
    @Override
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        log.info(" activate {} with config {}",getClass().getSimpleName(),ctx.getProperties());
        super.activate(ctx);
        //should we write sentiment values for the document
        Object value = ctx.getProperties().get(PROPERTY_DOCUMENT_SENTIMENT_STATE);
        this.writeDocumentSentiment = value == null ? DEFAULT_DOCUMENT_SENTIMENT_STATE :
            value instanceof Boolean ? ((Boolean)value).booleanValue() : 
                Boolean.parseBoolean(value.toString());
        //should we write sentiment values for sentences
        value = ctx.getProperties().get(PROPERTY_SENTENCE_SENTIMENT_STATE);
        this.writeSentencesSentimet = value == null ? DEFAULT_SENTENCE_SENTIMENT_STATE :
            value instanceof Boolean ? ((Boolean)value).booleanValue() : 
                Boolean.parseBoolean(value.toString());
        //should we write sentiment values for phrases
        value = ctx.getProperties().get(PROPERTY_PHRASE_SENTIMENT_STATE);
        this.writeSentimentPhrases = value == null ? DEFAULT_PHRASE_SENTIMENT_STATE :
            value instanceof Boolean ? ((Boolean)value).booleanValue() : 
                Boolean.parseBoolean(value.toString());
    }
    
    @Override
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        super.deactivate(ctx);
    }
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        return NlpEngineHelper.getAnalysedText(this, ci, false) != null &&
                NlpEngineHelper.getLanguage(this, ci, false) != null ?
               ENHANCE_ASYNC : CANNOT_ENHANCE; 
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        String language = NlpEngineHelper.getLanguage(this, ci, true);
        AnalysedText at = NlpEngineHelper.getAnalysedText(this, ci, true);
        //configure the spanTypes based on the configuration
//        EnumSet<Span.SpanTypeEnum> spanTypes = EnumSet.noneOf(SpanTypeEnum.class);
//        if(writeSentimentPhrases){
//            spanTypes.add(SpanTypeEnum.Chunk);
//        }
//        if(writeSentencesSentimet){
//            spanTypes.add(SpanTypeEnum.Sentence);
//        }
//        if(writeTextSectionSentiments){
//            spanTypes.add(SpanTypeEnum.TextSection);
//        }
//        if(writeTextSentiments ){
//            spanTypes.add(SpanTypeEnum.Text);
//        }
        
        List<SentimentPhrase> sentiments = extractSentiments(at, language);
        String detectedLang = EnhancementEngineHelper.getLanguage(ci);
        ci.getLock().writeLock().lock();
        try {
            writeSentimentEnhancements(ci,sentiments,at,
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
     * Extracts {@link Sentiment}s for words with a {@link NlpAnnotations#SENTIMENT_ANNOTATION}.
     * The {@link NlpAnnotations#POS_ANNOTATION}s are used to link those words with
     * {@link LexicalCategory#Noun}s.
     * @param at the AnalyzedText to process
     * @return the {@link Sentiment} instances organised along {@link Sentence}s. If
     * no {@link Sentence}s are present on the parsed {@link AnalysedText}, than all
     * {@link Sentiment}s are added to the {@link AnalysedText}. Otherwise only 
     * {@link Sentiment}s not contained within a {@link Sentence} are added to the
     * {@link AnalysedText} key.
     */
    private List<SentimentPhrase> extractSentiments(AnalysedText at, String language) {
        //we do use Sentences (optional) and Tokens (required)
        Iterator<Span> tokenIt = at.getEnclosed(EnumSet.of(
            SpanTypeEnum.Sentence, SpanTypeEnum.Token));
        List<Sentiment> sentimentTokens = new ArrayList<Sentiment>(32);
        NavigableMap<Integer,Token> negations = new TreeMap<Integer,Token>();
        NavigableMap<Integer,Token> nounsAndPronouns = new TreeMap<Integer,Token>();
        NavigableMap<Integer,Token> verbs = new TreeMap<Integer,Token>();
        NavigableMap<Integer,Token> conjuctions = new TreeMap<Integer,Token>();
        NavigableMap<Integer,Token> sectionBorders = new TreeMap<Integer,Token>();
        boolean firstTokenInSentence = true;
        Sentence sentence = null;
        final List<SentimentPhrase> sentimentPhrases = new ArrayList<SentimentPhrase>();
        while(tokenIt.hasNext()){
            Span span = tokenIt.next();
            switch (span.getType()) {
                case Token:
                    Token word = (Token)span;
                    Integer wordIndex = sentimentTokens.size();
                    Value<Double> sentimentAnnotation = span.getAnnotation(SENTIMENT_ANNOTATION);
                    boolean addToList = false;
                    Sentiment sentiment = null;
                    if(sentimentAnnotation != null && sentimentAnnotation.value() != null &&
                            !sentimentAnnotation.value().equals(ZERO)){
                        sentiment = new Sentiment(word, sentimentAnnotation.value(),
                            sentence == null || word.getEnd() > sentence.getEnd() ?
                                    null : sentence);
                        addToList = true;
                    }
                    if(isNegation((Token)span, language)){
                        addToList = true;
                        negations.put(wordIndex, word);
                    } else if(isNoun(word, firstTokenInSentence, language) ||
                            isPronoun(word,language)){
                        addToList = true;
                        nounsAndPronouns.put(wordIndex, word);
                    } else if(isSectionBorder(word, language)){
                        addToList = true;
                        sectionBorders.put(wordIndex, word);
                    } else if(isVerb(word, language)){
                        addToList = true;
                        verbs.put(wordIndex, word);
                    } else if(isCoordinatingConjuction(word,language)){
                        addToList = true;
                        conjuctions.put(wordIndex, word);
                    } else if(isCountable(word, language)){ 
                        addToList = true;
                    }
                    if(log.isDebugEnabled()){
                        Value<PosTag> pos = word.getAnnotation(NlpAnnotations.POS_ANNOTATION);
                        log.debug(" [{}] '{}' pos: {}, sentiment {}", new Object[]{
                                addToList ? sentimentTokens.size() : "-", 
                                word.getSpan(),pos.value().getCategories(), 
                                sentiment == null ? "none" : sentiment.getValue()});
                    }
                    if(addToList){
                        sentimentTokens.add(sentiment); //add the token
                    }
                    firstTokenInSentence = false;
                    break;
                case Sentence:
                    //cleanup the previous sentence
                    sentimentPhrases.addAll(summarizeSentence(sentimentTokens, 
                        negations, nounsAndPronouns, verbs, conjuctions, sectionBorders));
                    negations.clear();
                    nounsAndPronouns.clear();
                    sentimentTokens.clear();
                    verbs.clear();
                    sectionBorders.clear();
                    firstTokenInSentence = true;
                    sentence = (Sentence)span;
                    break;
                case TextSection:
                    break;
                default:
                    break;
            }
        }
        sentimentPhrases.addAll(summarizeSentence(sentimentTokens, negations, 
            nounsAndPronouns, verbs, conjuctions, sectionBorders));
        return sentimentPhrases;
    }

    /**
     * @param sentimentTokens
     * @param negations
     * @param nounsAndPronouns
     * @param verbs
     * @param sectionBorders
     */
    private List<SentimentPhrase> summarizeSentence(List<Sentiment> sentimentTokens, NavigableMap<Integer,Token> negations,
            NavigableMap<Integer,Token> nounsAndPronouns, NavigableMap<Integer,Token> verbs, NavigableMap<Integer,Token> conjunctions,
            NavigableMap<Integer,Token> sectionBorders) {
        List<Sentiment> processedSentiments = new ArrayList<Sentiment>();
        Integer[] searchSpan = new Integer[]{-1,-1};
        for(int i = 0; i < sentimentTokens.size(); i++){
            Integer index = Integer.valueOf(i);
            Sentiment sentiment = sentimentTokens.get(i);
            if(sentiment != null){
                //check for a new section
                if(index.compareTo(searchSpan[1]) > 0) {
                    searchSpan[0] = sectionBorders.floorKey(index);
                    if(searchSpan[0] == null) {
                        searchSpan[0] = Integer.valueOf(0);
                    }
                    searchSpan[1] = sectionBorders.ceilingKey(index);
                    if(searchSpan[1] == null) {
                        searchSpan[1] = Integer.valueOf(sentimentTokens.size()-1);
                    }
                }
                //for negation use the negation context
                Integer[] context = getNegationContext(index, conjunctions, searchSpan);
                for(Token negationToken : negations.subMap(context[0] , true, context[1], true).values()){
                    sentiment.addNegate(negationToken);
                }
                //for nouns use the sentiment context
                context = getSentimentContext(index, sentiment, verbs, conjunctions, nounsAndPronouns, searchSpan);
                for(Token word : nounsAndPronouns.subMap(context[0] , true, context[1], true).values()){
                    sentiment.addAbout(word);
                }
                processedSentiments.add(sentiment);
            }
        }
        //now combine the processed sentiments to SentimentPhrases
        Collections.sort(processedSentiments, sentimentComparator);
        List<SentimentPhrase> sentimentPhrases = new ArrayList<SentimentPhrase>();
        SentimentPhrase phrase = null;
        for(Sentiment sentiment : processedSentiments){
            if(phrase == null || sentiment.getStart() > phrase.getEndIndex()){
                phrase = new SentimentPhrase(sentiment);
                sentimentPhrases.add(phrase);
            } else {
                phrase.addSentiment(sentiment);
            }
        }
        return sentimentPhrases;
    }

    private Integer[] getNegationContext(Integer index, NavigableMap<Integer,Token> conjunctions, Integer[] sectionSpan) {
        Integer[] context = new Integer[]{
                Integer.valueOf(Math.max(index-negationContext,sectionSpan[0])),
                Integer.valueOf(Math.min(index+negationContext,sectionSpan[1]))};
        Integer floorConjunction = conjunctions.floorKey(index);
        //consider conjuction "The helmet is not comfortable and easy to use"
        //the "not" refers both to "comfortable" and  "easy"
        if(floorConjunction != null && floorConjunction.compareTo(index-conjuctionContext) >= 0){
            context[0] = Integer.valueOf(Math.max(floorConjunction-negationContext-1,sectionSpan[0]));
        }
        return context;
    }
    private Integer[] getSentimentContext(Integer index, Sentiment sentiment, NavigableMap<Integer,Token> verbs, NavigableMap<Integer,Token> conjunctions, NavigableMap<Integer,Token> nouns, Integer[] sectionSpan) {
        Integer[] context;
        PosTag pos = sentiment.getPosTag();
        boolean isPredicative;
        if(pos != null && pos.getPosHierarchy().contains(Pos.PredicativeAdjective)){
            isPredicative = true;
        } else if(pos != null && pos.hasCategory(LexicalCategory.Adjective) && 
                //Adjective that are not directly in front of a Noun
                nouns.get(Integer.valueOf(index+1)) == null){ 
          isPredicative = true;
        } else {
            isPredicative = false;
        }
        if(isPredicative){
//            Integer floorConjunction = conjunctions.floorKey(index);
//            if(floorConjunction != null && floorConjunction.compareTo(
//                Integer.valueOf(Math.max(index-conjuctionContext,sectionSpan[0]))) >= 0){
//                lowIndex = Integer.valueOf(floorConjunction-1);
//            }
//            Integer ceilingConjunction = conjunctions.ceilingKey(index);
//            if(ceilingConjunction != null && ceilingConjunction.compareTo(
//                Integer.valueOf(Math.min(index+conjuctionContext,sectionSpan[1]))) <= 0){
//                highIndex = Integer.valueOf(ceilingConjunction+1);
//            }
            //use the verb as context
            Integer floorNoun = nouns.floorKey(index);
            Entry<Integer,Token> floorVerb = verbs.floorEntry(index);
            Integer ceilingNoun = nouns.ceilingKey(index);
            Entry<Integer,Token> ceilingVerb = verbs.ceilingEntry(index);
            floorVerb = floorVerb == null || floorVerb.getKey().compareTo(sectionSpan[0]) < 0 ||
                    //do not use verbs with an noun in-between
                    (floorNoun != null && floorVerb.getKey().compareTo(floorNoun) < 0) ? 
                            null : floorVerb;
            ceilingVerb = ceilingVerb == null || ceilingVerb.getKey().compareTo(sectionSpan[1]) > 0 ||
                    //do not use verbs with an noun in-between
                    (ceilingNoun != null && ceilingVerb.getKey().compareTo(ceilingNoun) > 0) ? 
                            null : ceilingVerb;
            Entry<Integer,Token> verb;
            if(ceilingVerb != null && floorVerb != null){
                verb = (index - floorVerb.getKey()) < (ceilingVerb.getKey()-index) ? floorVerb : ceilingVerb;
            } else if(ceilingVerb != null){
                verb =  ceilingVerb;
            } else if(floorVerb != null){
                verb = floorVerb;
            } else { //no verb that can be used as context ... return an area around the current pos.
                verb = null;
            }
            if(verb != null){
                if(verb.getKey().compareTo(index) < 0){
                    Integer floorConjunction = conjunctions.floorKey(verb.getKey());
                    if(floorConjunction != null && floorConjunction.compareTo(
                        Integer.valueOf(Math.max(verb.getKey()-conjuctionContext,sectionSpan[0]))) >= 0){
                        //search an other verb in the same direction
                        floorVerb = verbs.floorEntry(floorConjunction);
                        if(floorVerb != null && floorVerb.getKey().compareTo(sectionSpan[0]) >= 0 &&
                                //do not step over an noun
                                (floorNoun == null || floorVerb.getKey().compareTo(floorNoun) >= 0)){
                          verb = floorVerb;
                        }
                    }
                } else if(verb.getKey().compareTo(index) > 0){
                    Integer ceilingConjunction = conjunctions.ceilingKey(verb.getKey());
                    if(ceilingConjunction != null && ceilingConjunction.compareTo(
                        Integer.valueOf(Math.min(verb.getKey()+conjuctionContext,sectionSpan[1]))) >= 0){
                        //search an other verb in the same direction
                        ceilingVerb = verbs.floorEntry(ceilingConjunction);
                        if(ceilingVerb != null && ceilingVerb.getKey().compareTo(sectionSpan[1]) <= 0 &&
                                //do not step over an noun
                                (ceilingNoun == null || ceilingVerb.getKey().compareTo(ceilingNoun) <= 0)){
                            verb = ceilingVerb;
                        }
                    }
                }
                context = new Integer[]{Integer.valueOf(verb.getKey()-nounContext),
                        Integer.valueOf(verb.getKey()+nounContext)};
                sentiment.setVerb(verb.getValue());
            } else {
                context = new Integer[]{Integer.valueOf(index-nounContext),
                        Integer.valueOf(index+nounContext)};
            }
        } else if(pos != null && pos.hasCategory(LexicalCategory.Adjective)){
            //for all other adjective the affected noun is expected directly
            //after the noun
            context = new Integer[]{index,Integer.valueOf(index+1)};
        } else if(pos != null && pos.hasCategory(LexicalCategory.Noun)){
            //a noun with an sentiment
            context = new Integer[]{index,index};
        } else { //else (includes pos == null) return default
            context = new Integer[]{Integer.valueOf(index-nounContext),
                    Integer.valueOf(index+nounContext)};
        }
        //ensure the returned context does not exceed the parsed sectionSpan 
        if(context[0].compareTo(sectionSpan[0]) < 0){
            context[0] = sectionSpan[0];
        }
        if(context[1].compareTo(sectionSpan[1]) > 0) {
            context[1] = sectionSpan[1];
        }
        return context;
        }

    private boolean isPronoun(Token token, String language) {
        Value<PosTag> posAnnotation = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        return posAnnotation == null ? false : posAnnotation.value().getPosHierarchy().contains(Pos.Pronoun);
    }

    private boolean isVerb(Token token, String language) {
        Value<PosTag> posAnnotation = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        return posAnnotation == null ? false : posAnnotation.value().hasCategory(LexicalCategory.Verb);
    }
    
    private boolean isCoordinatingConjuction(Token token, String language) {
        Value<PosTag> posAnnotation = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        return posAnnotation == null ? false : posAnnotation.value().getPosHierarchy().contains(Pos.CoordinatingConjunction);
    }

    private boolean isSectionBorder(Token token, String language) {
        Value<PosTag> posAnnotation = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        if(posAnnotation != null && !Collections.disjoint(sectionBorderPosTags, posAnnotation.value().getPosHierarchy())){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the parsed {@link Token} represents an negation
     * @param token the word
     * @param language the language
     * @return <code>true</code> if the {@link Token} represents a negation.
     * Otherwise <code>false</code>
     */
    private boolean isNegation(Token token, String language) {
        Value<PosTag> posAnnotation = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        if(posAnnotation != null && !Collections.disjoint(negativePosTags, posAnnotation.value().getPosHierarchy())){
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Checks if the parsed {@link Token} represents an negation
     * @param token the word
     * @param index the index of the token relative to the sentence | section
     * @param language the language
     * @return <code>true</code> if the {@link Token} represents a negation.
     * Otherwise <code>false</code>
     */
    private boolean isNoun(Token token, boolean firstTokenInSentence, String language) {
        String word = token.getSpan();
        if(!firstTokenInSentence && !word.isEmpty() && Character.isUpperCase(word.charAt(0))){
            return true; //assume all upper case tokens are Nouns
        }
        Value<PosTag> posAnnotation = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        if(posAnnotation != null && (posAnnotation.value().hasCategory(LexicalCategory.Noun)
                || posAnnotation.value().getPosHierarchy().contains(Pos.CardinalNumber))){
            return true;
        }
        return false;
    }
    /**
     * If the current Token should be considered for counting distances to
     * negations and nouns
     * @param token
     * @param language
     * @return
     */
    private boolean isCountable(Token token, String language){
        Value<PosTag> posAnnotation = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
        if(posAnnotation != null && !Collections.disjoint(countableLexCats, posAnnotation.value().getCategories())){
            return true;
        } else {
            return false;
        }
    }
    
    
    private void writeSentimentEnhancements(ContentItem ci, List<SentimentPhrase> sentimentPhrases, AnalysedText at, Language lang) {
        // TODO Auto-generated method stub
        Graph metadata = ci.getMetadata();
        Sentence currentSentence = null;
        final List<SentimentPhrase> sentencePhrases = new ArrayList<SentimentPhrase>();
        for(SentimentPhrase sentPhrase : sentimentPhrases){
            Sentence sentence = sentPhrase.getSentence();
            if(log.isDebugEnabled()){ //debug sentiment info
                CharSequence phraseText = at.getText().subSequence(sentPhrase.getStartIndex(), sentPhrase.getEndIndex());
                log.debug("Write SentimentPhrase for {} (sentence: {})", phraseText,
                    sentence == null ? "none" : sentence.getSpan().length() > 17 ? (sentence.getSpan().subSequence(0,17) + "...") : sentence.getSpan());
                List<Sentiment> sentiments = sentPhrase.getSentiments();
                log.debug(" > {} Sentiments:",sentiments.size());
                for(int i = 0; i < sentiments.size(); i++){
                    log.debug("    {}. {}",i+1,sentiments.get(i));
                }
            }
            if(writeSentimentPhrases){
                IRI enh = createTextEnhancement(ci, this);
                String phraseText = at.getSpan().substring(sentPhrase.getStartIndex(), sentPhrase.getEndIndex());
                metadata.add(new TripleImpl(enh, ENHANCER_SELECTED_TEXT, 
                    new PlainLiteralImpl(phraseText, lang)));
                if(sentPhrase.getSentence() == null){
                    metadata.add(new TripleImpl(enh, ENHANCER_SELECTION_CONTEXT, 
                        new PlainLiteralImpl(getSelectionContext(
                            at.getSpan(), phraseText, sentPhrase.getStartIndex()),lang)));
                } else {
                    metadata.add(new TripleImpl(enh, ENHANCER_SELECTION_CONTEXT, 
                        new PlainLiteralImpl(sentPhrase.getSentence().getSpan(),lang)));
                }
                metadata.add(new TripleImpl(enh, ENHANCER_START, 
                    lf.createTypedLiteral(sentPhrase.getStartIndex())));
                metadata.add(new TripleImpl(enh, ENHANCER_END, 
                    lf.createTypedLiteral(sentPhrase.getEndIndex())));
                if(sentPhrase.getPositiveSentiment() != null){
                    metadata.add(new TripleImpl(enh, POSITIVE_SENTIMENT_PROPERTY, 
                        lf.createTypedLiteral(sentPhrase.getPositiveSentiment())));
                }
                if(sentPhrase.getNegativeSentiment() != null){
                    metadata.add(new TripleImpl(enh, NEGATIVE_SENTIMENT_PROPERTY, 
                        lf.createTypedLiteral(sentPhrase.getNegativeSentiment())));
                }
                metadata.add(new TripleImpl(enh, SENTIMENT_PROPERTY, 
                    lf.createTypedLiteral(sentPhrase.getSentiment())));               
                //add the Sentiment type as well as the type of the SSO Ontology
                metadata.add(new TripleImpl(enh, DC_TYPE, SENTIMENT_TYPE));
                IRI ssoType = NIFHelper.SPAN_TYPE_TO_SSO_TYPE.get(SpanTypeEnum.Chunk);
                if(ssoType != null){
                    metadata.add(new TripleImpl(enh, DC_TYPE, ssoType));
                }
            }
            if(writeSentencesSentimet && sentence != null){
                if(sentence.equals(currentSentence)){
                    sentencePhrases.add(sentPhrase);
                } else {
                    writeSentiment(ci, currentSentence,sentencePhrases);
                    //reset
                    currentSentence = sentence;
                    sentencePhrases.clear();
                    sentencePhrases.add(sentPhrase);
                }
            }
        }
        if(!sentencePhrases.isEmpty()){
            writeSentiment(ci, currentSentence,sentencePhrases);
        }
        if(writeDocumentSentiment){
            writeSentiment(ci, at,sentimentPhrases);
        }

    }
    
    
    private void writeSentiment(ContentItem ci, Section section, List<SentimentPhrase> sectionPhrases) {
        if(section == null || sectionPhrases == null || sectionPhrases.isEmpty()){
            return; //nothing to do
        }
        IRI enh = createTextEnhancement(ci, this);
        Graph metadata = ci.getMetadata();
        if(section.getType() == SpanTypeEnum.Sentence){
            //TODO use the fise:TextAnnotation new model for 
            //add start/end positions
            metadata.add(new TripleImpl(enh, ENHANCER_START, 
                lf.createTypedLiteral(section.getStart())));
            metadata.add(new TripleImpl(enh, ENHANCER_END, 
                lf.createTypedLiteral(section.getEnd())));
        }
        //TODO: Summarize the sentiments of this section
        //add the sentiment information
        double positiveSent = 0.0;
        int positiveCount = 0;
        double negativeSent = 0.0;
        int negativeCount = 0;
        for(SentimentPhrase sentPhrase : sectionPhrases){
            if(sentPhrase.getNegativeSentiment() != null){
                double neg = sentPhrase.getNegativeSentiment();
                negativeSent = negativeSent+(neg*neg);
                negativeCount++;
            }
            if(sentPhrase.getPositiveSentiment() != null){
                double pos = sentPhrase.getPositiveSentiment();
                positiveSent = positiveSent+(pos*pos);
                positiveCount++;
            }
        }
        if(positiveCount > 0){
            positiveSent = Math.sqrt(positiveSent/(double)positiveCount);
            metadata.add(new TripleImpl(enh, POSITIVE_SENTIMENT_PROPERTY, 
                lf.createTypedLiteral(Double.valueOf(positiveSent))));
        }
        if(negativeCount > 0){
            negativeSent = Math.sqrt(negativeSent/(double)negativeCount)*-1;
            metadata.add(new TripleImpl(enh, NEGATIVE_SENTIMENT_PROPERTY, 
                lf.createTypedLiteral(Double.valueOf(negativeSent))));
        }
        metadata.add(new TripleImpl(enh, SENTIMENT_PROPERTY, 
            lf.createTypedLiteral(Double.valueOf(negativeSent+positiveSent))));

        //add the Sentiment type as well as the type of the SSO Ontology
        metadata.add(new TripleImpl(enh, DC_TYPE, SENTIMENT_TYPE));
        IRI ssoType = NIFHelper.SPAN_TYPE_TO_SSO_TYPE.get(section.getType());
        if(ssoType != null){
            metadata.add(new TripleImpl(enh, DC_TYPE, ssoType));
        }
        if(section.getType() == SpanTypeEnum.Text){
            metadata.add(new TripleImpl(enh, DC_TYPE, DOCUMENT_SENTIMENT_TYPE));
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
