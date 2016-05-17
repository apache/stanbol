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
package org.apache.stanbol.enhancer.nlp.model;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.utils.NIFHelper;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class added as ContentPart to the contentItem
 * @author westei
 *
 */
public class AnalysedTextTest {

    private static Logger log = LoggerFactory.getLogger(AnalysedTextTest.class);

    public static final String text = "The Stanbol enhancer can detect famous " +
            "cities such as Paris and people such as Bob Marley. With " +
            "disambiguation it would even be able to detect the Comedian " +
            "Bob Marley trafeling to Paris in Texas.";
    
    public static final Annotation<Number> testAnnotation = 
            new Annotation<Number>("test", Number.class);
    
    /* -----
     * Test data creates within the BeforeClass
     * -----
     */
    /**
     * AnalysedText instance filled in {@link #setup()} with test dats
     */
    private static AnalysedText analysedTextWithData;
    private static LinkedHashMap<Sentence,String> expectedSentences = new LinkedHashMap<Sentence,String>();
    private static LinkedHashMap<Chunk,String> expectedChunks = new LinkedHashMap<Chunk,String>();
    private static LinkedHashMap<Token,String> expectedTokens = new LinkedHashMap<Token,String>();
    
    /* -----
     * Test data creates before every single test
     * -----
     */
    /**
     * Empty AnalysedText instance created before each test
     */
    private static AnalysedText at;

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static final AnalysedTextFactory atFactory = AnalysedTextFactory.getDefaultInstance();
    
    private static ContentItem ci;
    
    @BeforeClass
    public static final void setup() throws IOException {
        analysedTextWithData = createAnalysedText();
        int sentence = text.indexOf('.')+1;
        Sentence sent1 = analysedTextWithData.addSentence(0, sentence);
        expectedSentences.put(sent1, "The Stanbol enhancer can detect famous " +
            "cities such as Paris and people such as Bob Marley.");
        
        Sentence sent2 = analysedTextWithData.addSentence(sentence+1, text.length());
        expectedSentences.put(sent2, "With disambiguation it would even be able " +
        		"to detect the Comedian Bob Marley trafeling to Paris in Texas.");
        
        Token the = sent1.addToken(0, 3);
        expectedTokens.put(the, "The");
        Token stanbol = sent1.addToken(4,11);
        expectedTokens.put(stanbol, "Stanbol");
        //use index to create Tokens
        int enhancerStart = sent1.getSpan().indexOf("enhancer");
        Token enhancer = sent1.addToken(enhancerStart,enhancerStart+"enhancer".length());
        expectedTokens.put(enhancer, "enhancer");

        //create a chunk
        Chunk stanbolEnhancer = analysedTextWithData.addChunk(stanbol.getStart(), enhancer.getEnd());
        expectedChunks.put(stanbolEnhancer, "Stanbol enhancer");
        
        int parisStart = sent1.getSpan().indexOf("Paris");
        Token paris = sent1.addToken(parisStart, parisStart+5);
        expectedTokens.put(paris, "Paris");

        int bobMarleyStart = sent1.getSpan().indexOf("Bob Marley");
        Chunk bobMarley = sent1.addChunk(bobMarleyStart, bobMarleyStart+10);
        expectedChunks.put(bobMarley, "Bob Marley");
        Token bob = bobMarley.addToken(0, 3);
        expectedTokens.put(bob, "Bob");
        Token marley = bobMarley.addToken(4, 10);
        expectedTokens.put(marley, "Marley");

        Token with = sent2.addToken(0, 4);
        expectedTokens.put(with, "With");
        Token disambiguation = sent2.addToken(5, 5+"disambiguation".length());
        expectedTokens.put(disambiguation, "disambiguation");
        
        int comedianBobMarleyIndex = sent2.getSpan().indexOf("Comedian");
        Chunk comedianBobMarley = sent2.addChunk(comedianBobMarleyIndex, 
            comedianBobMarleyIndex+"Comedian Bob Marley".length());
        expectedChunks.put(comedianBobMarley, "Comedian Bob Marley");
        Token comedian = comedianBobMarley.addToken(0, "Comedian".length());
        expectedTokens.put(comedian, "Comedian");
        Token bobSent2 = comedianBobMarley.addToken(9,9+"Bob".length());
        expectedTokens.put(bobSent2, "Bob");
        Token marleySent2 = comedianBobMarley.addToken(13, 13+"Marley".length());
        expectedTokens.put(marleySent2, "Marley");

        int parisIndex = sent2.getSpan().indexOf("Paris");
        Chunk parisInTexas = sent2.addChunk(parisIndex, parisIndex+"Paris in Texas".length());
        expectedChunks.put(parisInTexas, "Paris in Texas");
        Token parisSent2 = parisInTexas.addToken(0, "Paris".length());
        expectedTokens.put(parisSent2, "Paris");
        int inIndex = parisInTexas.getSpan().indexOf("in");
        Token in = parisInTexas.addToken(inIndex,
            inIndex+2);
        expectedTokens.put(in, "in");
        Token texasSent2 = parisInTexas.addToken(parisInTexas.getSpan().indexOf("Texas"),
                parisInTexas.getSpan().indexOf("Texas")+"Texas".length());
        expectedTokens.put(texasSent2, "Texas");
        
    }


    @Before
    public void initAnalysedText() throws Exception {
        at = createAnalysedText();
    }
    /**
     * @throws IOException
     */
    private static AnalysedText createAnalysedText() throws IOException {
        ci = ciFactory.createContentItem(new StringSource(text));
        Entry<IRI,Blob> textBlob = ContentItemHelper.getBlob(ci, Collections.singleton("text/plain"));
        return  atFactory.createAnalysedText(ci, textBlob.getValue());
    }
    
    
    @Test
    public void testSpanFilter(){
        Iterator<Sentence> sentences = analysedTextWithData.getSentences();
        Iterator<Chunk> chunks = analysedTextWithData.getChunks();
        Iterator<Token> tokens = analysedTextWithData.getTokens();
        for(Entry<Sentence,String> sentEntry : expectedSentences.entrySet()){
            Sentence sent = sentences.next();
            Assert.assertEquals(sentEntry.getKey(), sent);
            Assert.assertEquals(sentEntry.getValue(), sent.getSpan());
        }
        for(Entry<Chunk,String> chunkEntry : expectedChunks.entrySet()){
            Chunk chunk = chunks.next();
            Assert.assertEquals(chunkEntry.getKey(), chunk);
            Assert.assertEquals(chunkEntry.getValue(), chunk.getSpan());
        }
        for(Entry<Token,String> tokenEntry : expectedTokens.entrySet()){
            Token token = tokens.next();
            Assert.assertEquals(tokenEntry.getKey(), token);
            Assert.assertEquals(tokenEntry.getValue(), token.getSpan());
        }
    }
    
    @Test
    public void testAnalysedText(){
        Assert.assertEquals(text, at.getText());
        Assert.assertEquals(text, at.getSpan());
        Assert.assertEquals(0, at.getStart());
        Assert.assertEquals(text.length(), at.getEnd());
    }
    /**
     * Spans created relative to an other MUST NOT exceed the span of the 
     * other one
     */
    @Test(expected=IllegalArgumentException.class)
    public void testExceedsRelativeSpan(){
        Sentence sent = at.addSentence(0, 10);
        sent.addChunk(5, 15); //Invalid
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNegativeStart(){
        at.addSentence(-1, 10);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testRelativeNegativeStart(){
        Sentence sent = at.addSentence(0, 10);
        sent.addToken(-1, 5);
    }
    @Test
    public void testAnalysedTextaddSpanMethods(){
        Collection<Span> spans = new HashSet<Span>();
        //add some span of different types 
        spans.add(at.addToken(4, 11));
        spans.add(at.addChunk(4,19));
        spans.add(at.addSentence(0, 91));
        Set<Span> atSpans = AnalysedTextUtils.asSet(at.getEnclosed(EnumSet.allOf(SpanTypeEnum.class)));
        Assert.assertTrue(spans.containsAll(atSpans));
        Assert.assertTrue(atSpans.containsAll(spans));
    }
    /**
     * Test relative additions (with relative indexes) as well as iterators
     * over this hierarchy
     */
    @Test
    public void testSpanHierarchy(){
        int[] startPos = new int[]{0,1,2};
        int[] endPos = new int[]{1,2,3};
        int maxVal = endPos[endPos.length-1];
        int tokenLength = 5;
        int chunkLength = tokenLength*maxVal;
        int sentenceLength = tokenLength*maxVal*maxVal;
        List<Sentence> sentences = new ArrayList<Sentence>(startPos.length);
        List<Chunk> chunks = new ArrayList<Chunk>(startPos.length*2);
        List<Token> tokens = new ArrayList<Token>(startPos.length*3);
        int start;
        int end;
        //1. test relative add and absolute start/end
        log.info("--- adding Spans ---");
        for(int s=0;s<startPos.length;s++){
            start = startPos[s]*sentenceLength;
            end = endPos[s]*sentenceLength;
            Sentence sent = at.addSentence(start, end);
            log.info("add {}",sent);
            Assert.assertEquals(start, sent.getStart());
            Assert.assertEquals(end, sent.getEnd());
            sentences.add(sent);
        }
        //1.b iterate over the sentences while adding Chunks and Tokens to
        //    test that returned Iterators MUST NOT throw 
        //    ConcurrentModificationExceptions when adding Spans to the AnalysedText
        Iterator<Sentence> sentenceIt = at.getSentences();
        while(sentenceIt.hasNext()){
            Sentence sent = sentenceIt.next();
            for(int c=0;c<startPos.length;c++){
                start = startPos[c]*chunkLength;
                end = endPos[c]*chunkLength;
                Chunk chunk = sent.addChunk(start, end);
                log.info("  add {}",chunk);
                start = sent.getStart() + start;
                end = sent.getStart() + end;
                Assert.assertEquals(start, chunk.getStart());
                Assert.assertEquals(end, chunk.getEnd());
                chunks.add(chunk);
                for(int t=0;t<startPos.length;t++){
                    start = startPos[t]*tokenLength;
                    end = endPos[t]*tokenLength;
                    Token token = chunk.addToken(start, end);
                    log.info("    add {}",token);
                    start = chunk.getStart() + start;
                    end = chunk.getStart() + end;
                    Assert.assertEquals(start, token.getStart());
                    Assert.assertEquals(end, token.getEnd());
                    tokens.add(token);
                }
            }
        }
        //2. test iterations of enclosed
        int chunksInSentence = startPos.length;
        int tokensInChunk = chunksInSentence;
        int tokensInSentence = chunksInSentence*tokensInChunk;
        Iterator<Sentence> sentIt = at.getSentences();
        int s = 0;
        int c = 0;
        int t = 0;
        log.info("--- iterating over Spans ---");
        log.info("{}",at);
        for(;sentIt.hasNext();s++){
            Assert.assertTrue(sentences.size()+" Sentences Expected (found: "+(s+1)+")",s < sentences.size());
            Sentence sent = sentIt.next();
            log.info("  {}",sent);
            Assert.assertEquals(sentences.get(s), sent);
            Iterator<Chunk> chunkIt = sent.getChunks();
            int foundChunks = 0;
            for(;chunkIt.hasNext();c++){
                Assert.assertTrue(chunks.size()+" Chunks Expected (found: "+(c+1)+")",c < chunks.size());
                Chunk chunk = chunkIt.next();
                log.info("    {}",chunk);
                Assert.assertEquals(chunks.get(c), chunk);
                Iterator<Token> tokenIt = chunk.getTokens();
                int foundTokens = 0;
                for(;tokenIt.hasNext();t++){
                    Assert.assertTrue(tokens.size()+" Tokens Expected (found: "+(t+1)+")",t < tokens.size());
                    Token token = tokenIt.next();
                    log.info("      {}",token);
                    Assert.assertEquals(tokens.get(t), token);
                    foundTokens++;
                }
                Assert.assertEquals(tokensInChunk+" Tokens expected in Chunk", tokensInChunk,foundTokens);
                foundChunks++;
            }
            Assert.assertEquals(chunksInSentence+" Chunks expected in Sentence", chunksInSentence,foundChunks);
            //also iterate over tokens within a sentence
            log.info("  {}",sent);
            Iterator<Token> tokenIt = sent.getTokens();
            int foundTokens = 0;
            for(;tokenIt.hasNext();foundTokens++){
                Token token = tokenIt.next();
                log.info("    {}",token);
                Assert.assertEquals(tokens.get(s*tokensInSentence+foundTokens), token);
            }
            Assert.assertEquals(tokensInSentence+" Tokens expected in Sentence", tokensInSentence,foundTokens);
        }
        Assert.assertEquals(sentences.size()+" Sentences Expected (found: "+s+")", sentences.size(),s);
        Assert.assertEquals(chunks.size()+" Chunks Expected (found: "+c+")", chunks.size(),c);
        Assert.assertEquals(tokens.size()+" Sentences Expected (found: "+t+")", tokens.size(),t);
        //also iterate over Chunks in AnalysedText
        Iterator<Chunk> chunkIt = at.getChunks();
        int foundChunks = 0;
        log.info("{}",at);
        for(;chunkIt.hasNext();foundChunks++){
            Chunk chunk = chunkIt.next();
            log.info("  {}",chunk);
            Assert.assertEquals(chunks.get(foundChunks), chunk);
        }
        Assert.assertEquals(chunks.size()+" Chunks expected in AnalysedText", chunks.size(),foundChunks);
        //also iterate over Tokens in AnalysedText
        Iterator<Token> tokenIt = at.getTokens();
        int foundTokens = 0;
        log.info("{}",at);
        for(;tokenIt.hasNext();foundTokens++){
            Token token = tokenIt.next();
            log.info("  {}",token);
            Assert.assertEquals(tokens.get(foundTokens), token);
        }
        Assert.assertEquals(tokens.size()+" Tokens expected in AnalysedText", tokens.size(),foundTokens);

      //Finally iterate over multiple token types
      Iterator<Span> sentencesAndChunks = at.getEnclosed(
          EnumSet.of(SpanTypeEnum.Sentence,SpanTypeEnum.Chunk));
      s=0;
      c=0;
      log.info("{} >> Iterate over Sentences and Chunks",at);
      while(sentencesAndChunks.hasNext()){
          Span span = sentencesAndChunks.next();
          log.info("  {}",span);
          if(span.getType() == SpanTypeEnum.Chunk){
              Assert.assertEquals(chunks.get(c), span);
              c++;
          } else if(span.getType() == SpanTypeEnum.Sentence){
              Assert.assertEquals(sentences.get(s), span);
              s++;
          } else {
              Assert.fail("Unexpected SpanType '"+span.getType()+" (Span: "+span.getClass()+")");
          }
      }
      Assert.assertEquals(sentences.size()+" Sentences expected in AnalysedText", sentences.size(),s);
      Assert.assertEquals((sentences.size()*chunksInSentence)+" Chunks expected in AnalysedText", 
          (sentences.size()*chunksInSentence),c);
    }
    
    /**
     * Tests the {@link Section#getEnclosed(Set, int, int)} method introduced
     * with <code>0.12.1</code>
     */
    @Test
    public void testSubSectionIteration(){
        log.info("testSubSectionIteration ...");
        List<Span> expectedSpans = new ArrayList<Span>();
        List<Sentence> sentences = new ArrayList<Sentence>();
        Set<SpanTypeEnum> enabledTypes = EnumSet.of(SpanTypeEnum.Sentence, SpanTypeEnum.Token);
        Iterator<Span> allIt = analysedTextWithData.getEnclosed(enabledTypes);
        while(allIt.hasNext()){
            Span s = allIt.next();
            expectedSpans.add(s);
            if(s.getType() == SpanTypeEnum.Sentence) {
                sentences.add((Sentence)s);
            }
        }
        //first test an section that exceeds the end of the text
        int[] testSpan = new int[]{4,90};
        Assert.assertEquals(5, assertSectionIterator(analysedTextWithData, 
            expectedSpans, testSpan, enabledTypes));
        //second test a section relative to an sentence
        Sentence lastSent = sentences.get(sentences.size()-1);
        int [] offsetSpan = new int[]{5,25};
        Assert.assertEquals(1, assertSectionIterator(lastSent, expectedSpans, 
            offsetSpan, enabledTypes));
        
    }


    /**
     * @param span
     * @param testSpan
     */
    private int assertSectionIterator(Section section, List<Span> span, int[] testSpan, Set<SpanTypeEnum> types) {
        log.info("> assert span {} over {}", Arrays.toString(testSpan), section);
        Iterator<Span> sectionIt = section.getEnclosed(
            types, testSpan[0], testSpan[1]);
        int startIdx = section.getStart() + testSpan[0];
        int endIdx = section.getStart() + testSpan[1];
        int count = 0;
        for(Span s : span){
            if(s.getStart() < startIdx){
                log.info(" - asserted {} before section", s);
            } else if(s.getEnd() < endIdx){
                Assert.assertTrue(sectionIt.hasNext());
                Assert.assertEquals(s, sectionIt.next());
                count++;
                log.info(" - asserted section token {}", s);
            } else {
                log.info(" - asserted correct section end", s);
                Assert.assertFalse(sectionIt.hasNext());
                break;
            }
        }
        return count;
    }
    
    @Test
    public void testAnnotation(){
        List<Value<Number>> values = new ArrayList<Value<Number>>();
        values.add(new Value<Number>(26,0.6));
        values.add(new Value<Number>(27l));
        values.add(new Value<Number>(28.0f));
        values.add(new Value<Number>(25.0,0.8));
        at.addAnnotations(testAnnotation, values);
        Value<Number> value = at.getAnnotation(testAnnotation);
        Assert.assertNotNull(value);
        Assert.assertEquals(Double.valueOf(25.0), value.value());
        Assert.assertEquals(0.8d, value.probability(), 0.0d);
        Number prev = Float.valueOf(24f);
        for(Value<Number> v : at.getAnnotations(testAnnotation)){
            Assert.assertNotNull(v);
            Assert.assertTrue(v.value().doubleValue() > prev.doubleValue());
            prev = v.value();
        }
        //check that the order of Annotations without probability is kept
        at.addAnnotation(testAnnotation, new Value<Number>(29));
        prev = Integer.valueOf(24);
        for(Value<Number> v : at.getAnnotations(testAnnotation)){
            Assert.assertNotNull(v);
            Assert.assertTrue(v.value().intValue() > prev.intValue());
            prev = v.value();
        }
        
    }
    
}
