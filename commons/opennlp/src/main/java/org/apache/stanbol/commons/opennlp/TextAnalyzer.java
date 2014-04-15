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
package org.apache.stanbol.commons.opennlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Sequence;
import opennlp.tools.util.Span;

import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @deprecated replaced by STANBOL-733 (stanbol nlp processing module
 */
public class TextAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(TextAnalyzer.class);
    @Reference
    private final OpenNLP openNLP;
    
    private final TextAnalyzerConfig config;
    
    /**
     * @deprecated replaced by STANBOL-733 (stanbol nlp processing module
     */
    public static final class TextAnalyzerConfig {
        protected boolean forceSimpleTokenizer = false; //default to false
        protected boolean forceKeywordTokenizer = false; //default to false
        protected boolean enablePosTagger = true;
        protected boolean enableChunker = true;
        protected boolean enableSentenceDetector = true;
        protected boolean enablePosTypeChunker = true;
        protected boolean forcePosTypeChunker = true;
        /**
         * The minimum POS type probability used by the PosTypeChunker
         */
        private double minPosTagProbability = 0.75;
        public final boolean isSimpleTokenizerForced() {
            return forceSimpleTokenizer;
        }
    
        public final void forceSimpleTokenizer(boolean useSimpleTokenizer) {
            this.forceSimpleTokenizer = useSimpleTokenizer;
            if(useSimpleTokenizer){
                this.forceKeywordTokenizer = false;
            }
        }
        public final boolean isKeywordTokenizerForced() {
            return forceKeywordTokenizer;
        }
    
        public final void forceKeywordTokenizer(boolean useKeywordTokenizer) {
            this.forceKeywordTokenizer = useKeywordTokenizer;
            if(useKeywordTokenizer){
                this.forceSimpleTokenizer = false;
            }
        }
    
        public final boolean isPosTaggerEnable() {
            return enablePosTagger;
        }
    
        public final void enablePosTagger(boolean enablePosTagger) {
            this.enablePosTagger = enablePosTagger;
        }
    
        public final boolean isChunkerEnabled() {
            return enableChunker;
        }
    
        public final void enableChunker(boolean enableChunker) {
            this.enableChunker = enableChunker;
        }
    
        public final boolean isSentenceDetectorEnabled() {
            return enableSentenceDetector;
        }
    
        public final void enableSentenceDetector(boolean enableSentenceDetector) {
            this.enableSentenceDetector = enableSentenceDetector;
        }
        public final boolean isPosTypeChunkerEnabled() {
            return enablePosTypeChunker;
        }
        /**
         * Enables the used of the {@link PosTypeChunker} if no {@link Chunker} for
         * the current {@link #getLanguage() language} is available.
         * @param enablePosTypeChunker
         */
        public final void enablePosTypeChunker(boolean enablePosTypeChunker) {
            this.enablePosTypeChunker = enablePosTypeChunker;
            if(!enablePosTypeChunker){
                forcePosTypeChunker(enablePosTypeChunker);
            }
        }
    
        public final boolean isPosTypeChunkerForced() {
            return forcePosTypeChunker;
        }
        /**
         * Forces the use of the {@link PosTypeChunker} even if a {@link Chunker}
         * for the current language would be available
         * @param forcePosTypeChunker
         */
        public final void forcePosTypeChunker(boolean forcePosTypeChunker) {
            this.forcePosTypeChunker = forcePosTypeChunker;
            if(forcePosTypeChunker) {
                enablePosTypeChunker(true);
            }
        }
    
        /**
         * Getter for the minimum POS tag probability so that the
         * {@link PosTypeChunker} processes a POS tag.
         * @return the minPosTypeProbability
         */
        public final double getMinPosTypeProbability() {
            return minPosTagProbability;
        }
    
        /**
         * Setter for the minimum POS tag probability so that the
         * {@link PosTypeChunker} processes a POS tag.
         * @param minPosTagProbability The probability [0..1] or value < 0 to 
         * deactivate this feature
         * @throws IllegalArgumentException if values > 1 are parsed as probability
         */
        public final void setMinPosTagProbability(double probability) {
            if(probability > 1){
                throw new IllegalArgumentException("The minimum POS tag probability MUST be set to a value <= 1 (parsed:"+minPosTagProbability+"");
            }
            this.minPosTagProbability = probability;
        }

    }
    
    private POSTaggerME posTagger;
    /**
     * used to ensure that {@link #openNLP} is only ask once for the {@link POSTaggerME}
     * of the parsed {@link #language}
     */
    private boolean posTaggerNotAvailable;
    private SentenceDetector sentenceDetector;
    /**
     * used to ensure that {@link #openNLP} is only ask once for the {@link SentenceDetector}
     * of the parsed {@link #language}
     */
    private boolean sentenceDetectorNotAvailable;
    private ChunkerME chunker;
    /**
     * used to ensure that {@link #openNLP} is only ask once for the {@link ChunkerME}
     * of the parsed {@link #language}
     */
    private boolean chunkerNotAvailable;
    private PosTypeChunker posTypeChunker;
    /**
     * used to ensure only a single try to init a {@link PosTypeChunker} for 
     * the parsed {@link #language}
     */
    private boolean posTypeChunkerNotAvailable;
    /**
     * The Tokenizer
     */
    private Tokenizer tokenizer;
    /**
     * The language
     */
    private final String language;


    /**
     * Creates a TextAnalyzer based on the OpenNLP and the given language and the
     * default {@link TextAnalyzerConfig configuration}.<p>
     * If <code>null</code> is parsed as language, than a minimal configuration
     * that tokenizes the text using the {@link SimpleTokenizer} is used. 
     * @param openNLP The openNLP configuration to be used to analyze the text
     * @param language the language or <code>null</code> if not known.
     */
    public TextAnalyzer(OpenNLP openNLP,String language){
        this(openNLP,language,null);
    }
    /**
     * Creates a TextAnalyzer based on the OpenNLP and the given language.<p>
     * If <code>null</code> is parsed as language, than a minimal configuration
     * that tokenizes the text using the {@link SimpleTokenizer} is used. 
     * @param openNLP The openNLP configuration to be used to analyze the text
     * @param language the language or <code>null</code> if not known.
     */
    public TextAnalyzer(OpenNLP openNLP,String language, TextAnalyzerConfig config){
        if(openNLP == null){
            throw new IllegalArgumentException("The OpenNLP component MUST NOT be NULL");
        }
        this.config = config == null ? new TextAnalyzerConfig() : config;
        this.openNLP = openNLP;
        this.language = language;
    }

    protected final POSTaggerME getPosTagger() {
        if(!config.enablePosTagger){
            return null;
        }
        if(posTagger == null && !posTaggerNotAvailable){
            try {
                POSModel posModel = openNLP.getPartOfSpeechModel(language);
                if(posModel != null){
                    posTagger = new POSTaggerME(posModel);
                } else {
                    log.debug("No POS Model for language '{}'",language);
                    posTaggerNotAvailable = true;
                }
            } catch (IOException e) {
                log.info("Unable to load POS Model for language '"+language+"'",e);
                posTaggerNotAvailable = true;
            }
        }
        return posTagger;
    }
    /**
     * Getter for the Tokenizer of a given language
     * @param language the language
     * @return the Tolenizer
     */
    public final Tokenizer getTokenizer(){
        if(tokenizer == null){
            if(config.forceSimpleTokenizer){
                tokenizer = SimpleTokenizer.INSTANCE;
            } else if(config.forceKeywordTokenizer){
                tokenizer = KeywordTokenizer.INSTANCE;
            } else {
                tokenizer = openNLP.getTokenizer(language);
                if(tokenizer == null){
                    log.debug("No Tokenizer for Language '{}': fall back to SimpleTokenizer!",language);
                    tokenizer = SimpleTokenizer.INSTANCE;
                }
            }
        }
        return tokenizer;
    }
    protected final ChunkerME getChunker(){
        if(!config.enableChunker || config.forcePosTypeChunker){
            return null;
        }
        if(chunker == null && !chunkerNotAvailable) {
            try {
                ChunkerModel chunkerModel = openNLP.getChunkerModel(language);
                if(chunkerModel != null){
                    chunker = new ChunkerME(chunkerModel);
                } else {
                    log.debug("No Chunker Model for language {}",language);
                    chunkerNotAvailable = true;
                }
            } catch (IOException e) {
                log.info("Unable to load Chunker Model for language "+language,e);
                chunkerNotAvailable = true;
            }
        }
        return chunker;
    }
    protected final PosTypeChunker getPosTypeChunker(){
        if(!config.enableChunker || !config.enablePosTagger){
            return null;
        }
        if(posTypeChunker == null && !posTypeChunkerNotAvailable){
            posTypeChunker = PosTypeChunker.getInstance(language,config.minPosTagProbability);
            posTypeChunkerNotAvailable = posTypeChunker == null;
        }
        return posTypeChunker;
    }

    protected final SentenceDetector getSentenceDetector() {
        if(!config.enableSentenceDetector){
            return null;
        }
        if(sentenceDetector == null && !sentenceDetectorNotAvailable){
            try {
                SentenceModel sentModel = openNLP.getSentenceModel(language);
                if(sentModel != null){
                    sentenceDetector = new SentenceDetectorME(sentModel);
                } else {
                    log.debug("No Sentence Detection Model for language '{}'",language);
                    sentenceDetectorNotAvailable = true;
                }
            } catch (IOException e) {
                log.info("Unable to load Sentence Detection Model for language '"+language+"'",e);
                sentenceDetectorNotAvailable = true;
            }
        }
        return sentenceDetector;
    }

    public final OpenNLP getOpenNLP() {
        return openNLP;
    }
    /**
     * @return the config
     */
    public final TextAnalyzerConfig getConfig() {
        return config;
    }
    /**
     * @return the language
     */
    public final String getLanguage() {
        return language;
    }


    /**
     * Analyses the parsed text in a single chunk. No sentence detector is used
     * @param sentence the sentence (text) to analyse
     * @return the Analysed text
     */
    public AnalysedText analyseSentence(String sentence){
        return new AnalysedText(sentence,language);
    }
    /**
     * Analyses sentence by sentence when {@link Iterator#next()} is called on
     * the returned Iterator. Changes to the configuration of this class will
     * have an effect on the analysis results of this iterator.<p>
     * if no sentence detector is available the whole text is parsed at once. 
     * @param text The text to analyse
     * @return Iterator the analyses the parsed text sentence by sentence on
     * calls to {@link Iterator#next()}.
     */
    public Iterator<AnalysedText> analyse(String text){
        return new TextAnalysisIterator(text, language);
    }
    
    /**
     * @deprecated replaced by STANBOL-733 (stanbol nlp processing module
     */
    private final class TextAnalysisIterator implements Iterator<AnalysedText> {
        private final String text;
        private final Span[] sentenceSpans;
        private int current = 0;
        private final String language;
        private TextAnalysisIterator(String text,String language){
            this.text = text;
            this.language = language;
            if(text == null || text.isEmpty()){
                sentenceSpans = new Span[]{};
            } else {
                SentenceDetector sd = getSentenceDetector();
                if(sd != null){
                    sentenceSpans = sd.sentPosDetect(text);
                } else {
                    sentenceSpans = new Span[]{new Span(0, text.length())};
                }
            }
        }
        @Override
        public boolean hasNext() {
            return sentenceSpans.length > current;
        }

        @Override
        public AnalysedText next() {
            Span sentenceSpan = sentenceSpans[current];
            String sentence = sentenceSpan.getCoveredText(text).toString();
            current++; //mark this as consumed and navigate to the next
            return new AnalysedText(sentence,language,sentenceSpan.getStart());
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                "Removal of Sentences of the prsed Text is not supported!");
        }
    }

    /**
     * @deprecated replaced by STANBOL-733 (stanbol nlp processing module
     */
    public final class AnalysedText {
        //NOTE: Members are protected to allow the JVM direct access
        /**
         * The analysed sentence
         */
        protected final String sentence;
        /**
         * Final and {@link Collections#unmodifiableList(List) unmodifiable list}
         * with the tokens of the analysed {@link #sentence}.
         */
        protected final List<Token> tokens;
        /**
         * Final and {@link Collections#unmodifiableList(List) unmodifiable list}
         * with the chunks of the analysed {@link #sentence} or <code>null</code>
         * of no chunks are available
         */
        protected final List<Chunk> chunks;
        /**
         * The offset of the sentence with respect to the whole text. Note that
         * {@link AnalysedText this class} only holds the offset and no reference
         * to the whole text. <code>0</code> indicates that this represents the
         * start of the text (this may also indicate that the {@link #sentence} 
         * represents the whole analysed text).
         */
        private final int offset;
        /**
         * The language of the analyzed text
         */
        protected String language;
         
        private AnalysedText(String sentence, String language){
            this(sentence,language,0);
        }
        private AnalysedText(String sentence,String language, int offset){
            if(sentence == null || sentence.isEmpty()){
                throw new IllegalArgumentException(
                    "The parsed Sentence MUST NOT be NULL nor empty!");
            }
            this.sentence = sentence;
            if(language == null || language.isEmpty()){
                throw new IllegalArgumentException("The parsed language MUST NOT be NULL nor empty");
            }
            this.language = language;
            if(offset < 0){
                throw new IllegalArgumentException(
                    "The parsed offset MUST NOT be a negative number (offset="+offset+")");
            }
            this.offset = offset;
            Span[] tokenSpans = getTokenizer().tokenizePos(sentence);
            POSTaggerME tagger = getPosTagger();
            ChunkerME chunker = getChunker();
            PosTypeChunker posTypeChunker = getPosTypeChunker();
            String[] tokens = new String[tokenSpans.length];
            for(int ti = 0; ti<tokenSpans.length;ti++) {
                tokens[ti] = tokenSpans[ti].getCoveredText(sentence).toString();
            }
            String[][] posTags;
            double[][] posProbs;
            Span[] chunkSpans;
            double[] chunkProps;
            if(tagger != null){
                posTags = new String[tokens.length][];
                posProbs = new double[tokens.length][];
                //get the topK POS tags and props and copy it over to the 2dim Arrays
                Sequence[] posSequences = tagger.topKSequences(tokens);
                //extract the POS tags and props for the current token from the
                //posSequences.
                //NOTE: Sequence includes always POS tags for all Tokens. If
                //      less then posSequences.length are available it adds the
                //      best match for all followings.
                //      We do not want such copies.
                String[] actPos = new String[posSequences.length];
                double[] actProp = new double[posSequences.length];
                for(int i=0;i<tokenSpans.length;i++){
                    boolean done = false;
                    int j = 0;
                    while( j < posSequences.length && !done){
                        String p = posSequences[j].getOutcomes().get(i);
                        done = j > 0 && p.equals(actPos[0]);
                        if(!done){
                            actPos[j] = p;
                            actProp[j] = posSequences[j].getProbs()[i];
                            j++;
                        }
                    }
                    posTags[i] = new String[j];
                    System.arraycopy(actPos, 0, posTags[i], 0, j);
                    posProbs[i] = new double[j];
                    System.arraycopy(actProp, 0, posProbs[i], 0, j);
                }
                //posProbs = tagger.probs();
                if(chunker != null){
                    //we still need the Array of the best ranked POS tags for the chunker
                    String[] pos = posSequences[0].getOutcomes().toArray(new String[tokens.length]);
                    chunkSpans = chunker.chunkAsSpans(tokens, pos);
                    chunkProps = chunker.probs();
                } else if(posTypeChunker != null){
                    chunkSpans = posTypeChunker.chunkAsSpans(tokens, posTags, posProbs);
                    chunkProps = new double[chunkSpans.length];
                    Arrays.fill(chunkProps, 1.0);
                } else {
                    chunkSpans = null;
                    chunkProps = null;
                }
            } else {
                posTags = null;
                posProbs = null;
                chunkSpans = null;
                chunkProps = null;
            }
            List<Token> tokenList = new ArrayList<Token>(tokenSpans.length);
            for(int i=0;i<tokenSpans.length;i++){
                tokenList.add(new Token(tokenSpans[i], tokens[i],
                    posTags == null ? null: posTags[i], 
                            posProbs == null ? null : posProbs[i]));
            }
            //assign the list to the member var but make unmodifiable!
            this.tokens = Collections.unmodifiableList(tokenList);
            if(chunkSpans != null){
                List<Chunk> chunkList = new ArrayList<Chunk>(chunkSpans.length);
                for(int i=0;i<chunkSpans.length;i++){
                    chunkList.add(new Chunk(chunkSpans[i], chunkProps[i]));
                }
                this.chunks = Collections.unmodifiableList(chunkList);
            } else {
                chunks = null;
            }
            
        }
        public List<Token> getTokens(){
            return tokens;
        }
        public List<Chunk> getChunks(){
            return chunks;
        }
        public String getText(){
            return sentence;
        }
        public String getLanguage(){
            return language;
        }
        
        /**
         * Getter for the Offset of this Sentence relative to the whole analysed
         * Text. <code>0</code> if there is no offset this analysed text represents
         * the whole content
         * @return the offset
         */
        public int getOffset() {
            return offset;
        }

        /**
         * @deprecated replaced by STANBOL-733 (stanbol nlp processing module
         */
        public final class Token {
            //NOTE: Members are protected to allow the JVM direct access
            protected final Span span;
            protected String token;
            protected final String[] posTags;
            protected final double[] posProbabilities;
            protected final boolean hasAlphaNumeric;

            private Token(Span span,String token,String pos,double posProbability){
                this(span,token,new String[]{pos},new double[] {posProbability});
            }
            private Token(Span span,String token,String[] posTags, double[] posProbabilities){
                this.span = span;
                if(posTags == null || posTags.length < 1){
                    this.posTags = null;
                } else {
                    this.posTags = posTags;
                }
                this.token = token;
                if(this.posTags == null){
                    this.posProbabilities = null;
                } else if(posTags.length != posProbabilities.length){
                    throw new IllegalStateException("POS Tag array and POS probability array MUST BE of the same size!");
                } else {
                    this.posProbabilities = posProbabilities;
                }
                boolean foundAlphaNumericCahr = false;
                for(int i = 0;!foundAlphaNumericCahr &&i<token.length();i++){
                    foundAlphaNumericCahr = Character.isLetterOrDigit(token.charAt(i));
                }
                hasAlphaNumeric = foundAlphaNumericCahr;
            }

            public int getStart(){
                return span.getStart();
            }
            public int getEnd(){
                return span.getEnd();
            }
            /**
             * Getter for the best ranked POS tag for this token
             * @return
             */
            public String getPosTag(){
                return posTags == null ? null : posTags[0];
            }
            /**
             * Getter for all the POS tags of this Token. The one with the
             * highest probability is at index 0.
             * @return All POS tags assigned to this Token
             */
            public String[] getPosTags(){
                return posTags;
            }
            /**
             * Getter for the probability of the top ranked POS tag
             * @return the POS probability
             */
            public double getPosProbability() {
                return posProbabilities == null ? -1 : posProbabilities[0];
            }
            /**
             * Getter for the probabilities of all {@link #getPosTags() POS tags}
             * @return the probabilities of the POS tags returned by
             * {@link #getPosTags()}
             */
            public double[] getPosProbabilities(){
                return posProbabilities;
            }
            /**
             * Getter for the value of this token
             * @return
             */
            public String getText(){
                if(token == null){
                    token = span.getCoveredText(sentence).toString();
                }
                return token;
            }
            public boolean hasAplhaNumericChar(){
                return hasAlphaNumeric;
            }
            @Override
            public String toString() {
                return getText()+(posTags != null?
                        '_'+(posTags.length == 1 ?
                                posTags[0] :
                                    Arrays.toString(posTags)):"");
            }
        }
        /**
         * @deprecated replaced by STANBOL-733 (stanbol nlp processing module
         */
        public final class Chunk {
            //NOTE: Members are protected to allow the JVM direct access
            /**
             * The span over the char offset of this chunk within the 
             * {@link AnalysedText#sentence}
             */
            protected final Span span;
            /**
             * Span over the {@link AnalysedText#tokens} as used by the
             * {@link #getStart()} and {@link #getEnd()} methods 
             */
            protected final Span chunkSpan;
            protected final double probability;
            /**
             * DO NOT DIRECTLY ACCESS - lazy initialisation in {@link #getText()}
             */
            private String __text;
            /**
             * DO NOT DIRECTYL ACCESS - lazy initialisation in {@link #getTokens()}
             */
            private List<Token> __chunkTokens;

            private Chunk(Span chunkSpan,double probability){
                this.chunkSpan = chunkSpan;
                this.span =  new Span(tokens.get(chunkSpan.getStart()).getStart(), 
                    tokens.get(chunkSpan.getEnd()).getEnd());
                this.probability = probability;
            }
            public List<Token> getTokens(){
                if(__chunkTokens == null){
                    __chunkTokens = tokens.subList(chunkSpan.getStart(), chunkSpan.getEnd());
                }
                return __chunkTokens;
            }
            /**
             * @return the span
             */
            public int getStart() {
                return chunkSpan.getStart();
            }
            public int getEnd(){
                return chunkSpan.getEnd();
            }
            public int getSize(){
                return chunkSpan.length();
            }
            /**
             * @return the probability
             */
            public double getProbability() {
                return probability;
            }
            /**
             * The text of this chunk
             * @return
             */
            public String getText(){
                if(__text == null){
                    __text = span.getCoveredText(sentence).toString();
                }
                return __text;
            }
            @Override
            public String toString() {
                return getText();
            }
        }
    }    
}
