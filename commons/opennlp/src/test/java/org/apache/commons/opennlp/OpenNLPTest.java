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
package org.apache.commons.opennlp;

import java.io.IOException;
import java.util.Arrays;


import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.namefind.TokenNameFinder;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerModel;

import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test loading of OpenNLP models. This was previously implicitly tested by the
 * in the meantime removed TextAnalyzer test.
 * @author Rupert Westenthaler
 *
 */
public class OpenNLPTest {
    
    
    private static OpenNLP openNLP;    
    @BeforeClass
    public static void init(){
        openNLP = new OpenNLP(new ClasspathDataFileProvider("DUMMY"));
    }

    @Test
    public void testLoadEnTokenizer() throws IOException{
        TokenizerModel model = openNLP.getTokenizerModel("en");
        Assert.assertNotNull(model);
        Tokenizer tokenizer = openNLP.getTokenizer("en");
        Assert.assertNotNull(tokenizer);
    }
    
    @Test
    public void testLoadMissingTokenizerModel() throws IOException{
        TokenizerModel model = openNLP.getTokenizerModel("ru");
        //there is not Russian model ...
        //so it is expected that the model is NULL
        Assert.assertNull(model);
    }
    @Test
    public void testFallbackToSimpleTokenizer() throws IOException{
        //however for the tokenizer it is expected that a fallback to the
        //SimpleTokenizer is made
        Tokenizer tokenizer = openNLP.getTokenizer("ru");
        Assert.assertNotNull(tokenizer);
        Assert.assertEquals(SimpleTokenizer.INSTANCE, tokenizer);
    }
    
    @Test
    public void testLoadEnSentence() throws IOException{
        SentenceModel model = openNLP.getSentenceModel("en");
        Assert.assertNotNull(model);
        SentenceDetector sentDetector = openNLP.getSentenceDetector("en");
        Assert.assertNotNull(sentDetector);
    }
    @Test
    public void testLoadMissingSentence() throws IOException{
        SentenceModel model = openNLP.getSentenceModel("ru");
        Assert.assertNull(model);
        SentenceDetector sentDetector = openNLP.getSentenceDetector("ru");
        Assert.assertNull(sentDetector);
    }
    @Test
    public void testLoadEnPOS() throws IOException{
        POSModel model = openNLP.getPartOfSpeechModel("en");
        Assert.assertNotNull(model);
        POSTagger posTagger = openNLP.getPartOfSpeechTagger("en");
        Assert.assertNotNull(posTagger);
    }
    @Test
    public void testLoadMissingPOS() throws IOException{
        POSModel model = openNLP.getPartOfSpeechModel("ru");
        Assert.assertNull(model);
        POSTagger posTagger = openNLP.getPartOfSpeechTagger("ru");
        Assert.assertNull(posTagger);
    }
    @Test
    public void testLoadEnChunker() throws IOException{
        ChunkerModel model = openNLP.getChunkerModel("en");
        Assert.assertNotNull(model);
        Chunker chunker = openNLP.getChunker("en");
        Assert.assertNotNull(chunker);
    }
    @Test
    public void testLoadMissingChunker() throws IOException{
        ChunkerModel model = openNLP.getChunkerModel("ru");
        Assert.assertNull(model);
        Chunker chunker = openNLP.getChunker("ru");
        Assert.assertNull(chunker);
    }    
    @Test
    public void testLoadEnNER() throws IOException{
        for(String type : Arrays.asList("person","organization","location")){
            TokenNameFinderModel model = openNLP.getNameModel(type, "en");
            Assert.assertNotNull(model);
            TokenNameFinder ner = openNLP.getNameFinder(type, "en");
            Assert.assertNotNull(ner);
        }
    }
    @Test
    public void testLoadMissingNER() throws IOException{
        //first unknown type
        TokenNameFinderModel model = openNLP.getNameModel("person2", "en");
        Assert.assertNull(model);
        TokenNameFinder ner = openNLP.getNameFinder("person2", "en");
        Assert.assertNull(ner);
        //unknown language
        model = openNLP.getNameModel("person", "ru");
        Assert.assertNull(model);
        ner = openNLP.getNameFinder("person", "ru");
        Assert.assertNull(ner);
    }
    @Test
    public void testLoadModelByName() throws IOException{
        TokenizerModel tokenModel = openNLP.getModel(TokenizerModel.class, "en-token.bin", null);
        Assert.assertNotNull(tokenModel);
        SentenceModel sentModel = openNLP.getModel(SentenceModel.class, "en-sent.bin", null);
        Assert.assertNotNull(sentModel);
        POSModel posModel = openNLP.getModel(POSModel.class, "en-pos-maxent.bin", null);
        Assert.assertNotNull(posModel);
        ChunkerModel chunkModel = openNLP.getModel(ChunkerModel.class, "en-chunker.bin", null);
        Assert.assertNotNull(chunkModel);
        TokenNameFinderModel nerModel = openNLP.getModel(TokenNameFinderModel.class, "en-ner-person.bin", null);
        Assert.assertNotNull(nerModel);
        //unavailable model
        tokenModel = openNLP.getModel(TokenizerModel.class, "ru-token.bin", null);
        Assert.assertNull(tokenModel);
    }
    
    @Test(expected=IllegalStateException.class)
    public void testLoadIncompatibleModelByName() throws IOException{
        SentenceModel sentModel = openNLP.getModel(SentenceModel.class, "en-token.bin", null);
        Assert.assertNotNull(sentModel);
    }
    
}
