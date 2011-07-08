package org.apache.stanbol.commons.opennlp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.util.Span;

public class TextAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(TextAnalyzer.class);
    
    private final OpenNLP openNLP;
    private final String language;
    private boolean forceSimpleTokenizer = true;
    private boolean enablePosTagger = true;
    private boolean enableChunker = true;
    private boolean enableSentenceDetector = true;
    private boolean enablePosTypeChunker = true;
    private boolean forcePosTypeChunker = true;
    
    private POSTaggerME posTagger;
    private SentenceDetector sentenceDetector;
    private ChunkerME chunker;
    private PosTypeChunker posTypeChunker;
    private Tokenizer tokenizer;

    
    public TextAnalyzer(OpenNLP openNLP, String language){
        if(openNLP == null){
            throw new IllegalArgumentException("The OpenNLP component MUST NOT be NULL");
        }
        if(language == null || language.isEmpty()){
            throw new IllegalArgumentException("The parsed language MUST NOT be NULL nor empty");
        }
        this.openNLP = openNLP;
        this.language = language;
    }

    protected final POSTaggerME getPosTagger() {
        if(!enablePosTagger){
            return null;
        }
        if(posTagger == null){
            posTagger = initTagger();
        }
        return posTagger;
    }
    protected final Tokenizer getTokenizer(){
        if(tokenizer == null){
            tokenizer = initTokenizer();
        }
        return tokenizer;
    }
    protected final ChunkerME getChunker(){
        if(!enableChunker || forcePosTypeChunker){
            return null;
        }
        if(chunker == null){
            chunker = initChunker();
        }
        return chunker;
    }
    protected final PosTypeChunker getPosTypeChunker(){
        if(!enableChunker || !enablePosTagger){
            return null;
        }
        if(posTypeChunker == null){
            posTypeChunker = new PosTypeChunker();
        }
        return posTypeChunker;
    }

    protected final SentenceDetector getSentenceDetector() {
        if(!enableSentenceDetector){
            return null;
        }
        if(sentenceDetector == null){
            sentenceDetector = initSentence();
        }
        return sentenceDetector;
    }

    public final boolean isSimpleTokenizerForced() {
        return forceSimpleTokenizer;
    }

    public final void forceSimpleTokenizer(boolean useSimpleTokenizer) {
        this.forceSimpleTokenizer = useSimpleTokenizer;
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

    public final OpenNLP getOpenNLP() {
        return openNLP;
    }
    /**
     * The language used to analyse the parsed texts
     * @return
     */
    public final String getLanguage() {
        return language;
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
     * @param language
     * @return
     */
    private Tokenizer initTokenizer() {
        Tokenizer tokenizer;
        if(forceSimpleTokenizer ){
            tokenizer = SimpleTokenizer.INSTANCE;
        } else {
            tokenizer = openNLP.getTokenizer(language);
        }
        return tokenizer;
    }

    /**
     * @param language
     * @return
     */
    private POSTaggerME initTagger() {
        POSTaggerME posTagger;
        try {
            POSModel posModel = openNLP.getPartOfSpeachModel(language);
            if(posModel != null){
                posTagger = new POSTaggerME(posModel);
            } else {
                log.debug("No POS Model for language {}",language);
                posTagger = null;
            }
        } catch (IOException e) {
            log.info("Unable to load POS Model for language "+language,e);
            posTagger = null;
        }
        return posTagger;
    }
    /**
     * @param language
     * @return
     */
    private SentenceDetector initSentence() {
        SentenceDetector sentDetect;
        try {
            SentenceModel sentModel = openNLP.getSentenceModel(language);
            if(sentModel != null){
                sentDetect = new SentenceDetectorME(sentModel);
            } else {
                log.debug("No Sentence Detection Model for language {}",language);
                sentDetect = null;
            }
        } catch (IOException e) {
            log.info("Unable to load Sentence Detection Model for language "+language,e);
            sentDetect = null;
        }
        return sentDetect;
    }
    /**
     * @param language
     */
    private ChunkerME initChunker() {
        ChunkerME chunker;
        try {
            ChunkerModel chunkerModel = openNLP.getChunkerModel(language);
            if(chunkerModel != null){
                chunker = new ChunkerME(chunkerModel);
            } else {
                log.debug("No Chunker Model for language {}",language);
                chunker = null;
            }
        } catch (IOException e) {
            log.info("Unable to load Chunker Model for language "+language,e);
            chunker = null;
        }
        return chunker;
    }
    /**
     * Analyses the parsed text in a single chunk. No sentence detector is used
     * @param sentence the sentence (text) to analyse
     * @return the Analysed text
     */
    public AnalysedText analyseSentence(String sentence){
        return new AnalysedText(sentence);
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
        return new TextAnalysisIterator(text);
    }
    
    private final class TextAnalysisIterator implements Iterator<AnalysedText> {
        private final String text;
        private final Span[] sentenceSpans;
        private int current = 0;
        private TextAnalysisIterator(String text){
            this.text = text;
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
            String sentence = sentenceSpans[current].getCoveredText(text).toString();
            current++;
            return new AnalysedText(sentence);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                "Removal of Sentences of the prsed Text is not supported!");
        }
    }

    public class AnalysedText {
        //NOTE: Members are protected to allow the JVM direct access
        
        protected String sentence;
        
        protected List<Token> tokens;
        protected List<Chunk> chunks;
         
        private AnalysedText(String sentence){
            this.sentence = sentence;
            Span[] tokenSpans = getTokenizer().tokenizePos(sentence);
            POSTaggerME tagger = getPosTagger();
            ChunkerME chunker = getChunker();
            PosTypeChunker posTypeChunker = getPosTypeChunker();
            String[] tokens = new String[tokenSpans.length];
            for(int ti = 0; ti<tokenSpans.length;ti++) {
                tokens[ti] = tokenSpans[ti].getCoveredText(sentence).toString();
            }
            String[] pos;
            double[] posProbs;
            Span[] chunkSpans;
            double[] chunkProps;
            if(tagger != null){
                pos = tagger.tag(tokens);
                posProbs = tagger.probs();
                if(chunker != null){
                    chunkSpans = chunker.chunkAsSpans(tokens, pos);
                    chunkProps = chunker.probs();
                } else if(posTypeChunker != null){
                    //TODO: move initialisation to main class
                    chunkSpans = posTypeChunker.chunkAsSpans(tokens, pos);
                    chunkProps = new double[chunkSpans.length];
                    Arrays.fill(chunkProps, 1.0);
                } else {
                    chunkSpans = null;
                    chunkProps = null;
                }
            } else {
                pos = null;
                posProbs = null;
                chunkSpans = null;
                chunkProps = null;
            }
            this.tokens = new ArrayList<Token>(tokenSpans.length);
            for(int i=0;i<tokenSpans.length;i++){
                this.tokens.add(new Token(tokenSpans[i], tokens[i],
                    pos!=null?pos[i]:null, pos!=null?posProbs[i]:-1));
            }
            if(chunkSpans != null){
                chunks = new ArrayList<Chunk>(chunkSpans.length);
                for(int i=0;i<chunkSpans.length;i++){
                    chunks.add(new Chunk(chunkSpans[i], chunkProps[i]));
                }
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
        
        public class Token {
            //NOTE: Members are protected to allow the JVM direct access
            protected final Span span;
            protected String token;
            protected final String pos;
            protected final double posProbability;

            private Token(Span span,String token,String pos,double posProbability){
                this.span = span;
                this.pos = pos;
                this.token = token;
                this.posProbability = posProbability;
            }

            public int getStart(){
                return span.getStart();
            }
            public int getEnd(){
                return span.getEnd();
            }
            public String getPosTag(){
                return pos;
            }
            /**
             * @return the POS probability
             */
            public double getPosProbability() {
                return posProbability;
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
            @Override
            public String toString() {
                return getText()+(pos != null?'_'+pos:"");
            }
        }
        public class Chunk {
            //NOTE: Members are protected to allow the JVM direct access
            protected final Span span;
            protected final Span chunkSpan;
            protected final double probability;
            protected String text;

            private Chunk(Span chunkSpan,double probability){
                this.chunkSpan = chunkSpan;
                this.span =  new Span(tokens.get(chunkSpan.getStart()).getStart(), 
                    tokens.get(chunkSpan.getEnd()).getEnd());
                this.probability = probability;
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
                if(text == null){
                    text = span.getCoveredText(sentence).toString();
                }
                return text;
            }
            @Override
            public String toString() {
                return getText();
            }
        }
    }    
}
