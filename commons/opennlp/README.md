<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Apache Stanbol OpenNLP Module

This module provides utilities for Apache Stanbol components that uses Apache OpenNLP.

## OpenNLP service

Wrapper over the main OpenNLP functionalities that registers itself as OSGI component. It can be easily injected to a field by using

    :::java
    @Reference
    private OpenNLP openNLP;

The main functionality of this component is to handle the loading of OpenNLP models by using the Apache Stanbol DatafileProvider infrastructure. OpenNLP provides getters for getting sentence detectors, tokenizers, POS taggers and Chunkers for a given language as well as NER models for a language and a type. Loaded models are internally kept and only loaded on the first request for a given combination.

## TextAnalyzer

This utility wraps the functionality provided by sentence detectors, tokenizers, POS taggers and chunkers and provides an API that allows to analyze parsed text sentence wise.

This tool consists of two parts: First the TextAnalyzerConfig describes how text should be processed by OpenNLP. This configuration is typically created once and reused for the processing of multiple texts.

    :::java
    TextAnalyzerConfig config = new TextAnalyzerConfig();
    config.enableChunker(false); //disable the use of chunkers

This configuration also checks for internal dependency. E.g. because chunking requires POS tags the following configuration will ignore the activation of the chunker

    :::java
    config.enablePosTagger(false);
    config.enableChunker(true);//ignored


The second part is the TextAnalyzer class. This is used to analyze a given natural language text. It takes the OpenNLP component the configuration and the language as input and can than be used to analyze parsed text. TextAnalyzer is a light wight component. While it a TextAnalyzer object can be used to analyze several natural language texts it is - performance wise - also save to create a new instance for every text to analyze.

TextAnalyzer has two methods that can be used to analyze text.

* analyze(String text): This method uses the OpenNLP sentence detector to split the parsed text and than returns an Iterator of AnalyzedText objects representing analysis results for each sentence. It is important to note that the actual analyses of an sentence is only perfumed on calling next() on the returned Iterator. This allows to reduce the memory footprint of application by only holding analyses results of the current sentence in memory
* analyzeSentence(String text): Analyses the whole text in a single chunk - as if it would been a single sentence.

The following example shows how to use the TextAnalyzer and how to precess the analyses results.

    :::java
    String text; //the natural language text to precess
    String lang; //the language of the parsed text
    //use the default config for this example
    TextAnalyzerConfig config = new TextAnalyzerConfig();

    TextAnalyzer analyzer = new TextAnalyzer(openNLP, lang, config);
    Iterator<AnalysedText> analysedSentences = analyzer.analyse(text);
    while(analysedSentences.hasNext()){
        AnalysedText sentence = analysedSentences.next();
        //sentence information
        String sentenceText = sentence.getText();
        int offset = sentence.getOffset();
        
        //work with the Tokens
        for(Token token : sentence.getTokens()){
            //Token information
            String text = token.getText();
            int start = token.getStart(); //relative to sentence
            int end = token.getEnd();
            int absoluteStart = sentence.getOffset()+start;
            //POS tagging information
            String posTag = token.getPosTag();
            double posTagPron = token.getPosProbability()
        }
        
        //work with Chunks
        for(Chunk chunk : sentence.getChunks()){
            //Chunk information
            String text = chunk.getText();
            int start = chunk.getStart(); //relative to sentence
            int end = chunk.getEnd();
            int absoluteStart = sentence.getOffset()+start;
            double prob = chunk.getProbability();
            //The tokens part of this chunk
            List<Token> chunkTokens = chunk.getTokens();
        }
    }

