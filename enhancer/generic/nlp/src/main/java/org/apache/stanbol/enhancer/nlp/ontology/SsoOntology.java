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
package org.apache.stanbol.enhancer.nlp.ontology;

import org.apache.clerezza.commons.rdf.IRI;

public enum SsoOntology {
    /**
     * A string that can be considered a phrase consists of at least 2 strings that are words. 
     */
    Phrase,
    /**
     * A string that can be considered a sentence. Sentences with only one word are typed as Word and Sentence and have no disjointness.
     */
    Sentence,
    /**
     * A string that can be considered a Stop Word, i.e. it does not contain usefull information for search and other tasks.
     */
    StopWord,
    /**
     * A string that can be considered a word or a punctuation mark, the sentence 'He enters the room.' for example has 5 words. In general, the division into :Words is done by an NLP Tokenizer. Instances of this class should be a string, that is a 'meaningful' unit of characters. The class has not been named 'Token' as the NLP definition of 'Token' is more similar to our definition of :String . 
     */
    Word,
    child,
    firstWord,
    lastWord,
    /**
     * This property (and the others) can be used to traverse :Sentences, it can not be assumed that no gaps between Sentences exist, i.e. string adjacency is not mandatory. 
     */
    nextSentence,
    nextSentenceTrans,
    /**
     * This property (and the others) can be used to traverse words, it can not be assumed that no gaps between words exist, i.e. string adjacency is not mandatory. 
     */
    nextWord,
    nextWordTrans,
    /**
     * The link to the OLiA Annotation model.
     */
    oliaLink,
    parent,
    previousSentence,
    previousSentenceTrans,
    previousWord,
    previousWordTrans,
    sentence,
    /**
     * The lemma of a Word. 
     */
    lemma,
    /**
     * The pos tag of a Word.
     */
    posTag,
    /**
     * The stem of a Word.
     */
    stem
    ;
    public final static String NAMESPACE = "http://nlp2rdf.lod2.eu/schema/sso/";

    IRI uri;
    
    private SsoOntology() {
        uri = new IRI(NAMESPACE+name());
    }
    
    public String getLocalName(){
        return name();
    }
    
    public IRI getUri(){
        return uri;
    }
    
    @Override
    public String toString() {
        return uri.getUnicodeString();
    }

}
