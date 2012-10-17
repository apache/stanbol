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
/**
 * 
 */
package org.apache.stanbol.enhancer.engines.keywordextraction.linking;

import java.util.Iterator;

import org.apache.stanbol.commons.opennlp.TextAnalyzer;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Token;

/**
 * Represents the already with NLP tools analysed content to be linked with
 * Entities of an {@link EntitySearcher}.<p>
 * Note that for the linking process it is only required that the text is
 * tokenized. All other features (sentence detection, POS tags and Chunks) are
 * optional but do improve the performance and to an smaller amount also the
 * results of the linking process. <p>
 * TODO: <ul>
 * <li> Find a better Name
 * <li> The API is not optimal. In general the {@link TextAnalyzer} and the
 * {@link AnalysedContent} interface do not play well together :(
 * </ul>
 * @author Rupert Westenthaler
 *
 */
public interface AnalysedContent {

    
    /**
     * Getter for the Iterator over the analysed sentences. This Method
     * is expected to return always the same Iterator instance.
     * @return the iterator over the analysed sentences
     */
    public Iterator<AnalysedText> getAnalysedText();
    /**
     * Called to check if a {@link Token} should be used to search for
     * Concepts within the Taxonomy based on the POS tag of the Token.
     * @param posTag the POS tag to check
     * @param posProb the probability of the POS tag or <code>1.0</code> if not
     * available
     * @return <code>true</code> if Tokens with this POS tag should be
     * included in searches. Otherwise <code>false</code>.  If this information 
     * is not available (e.g. no set of Tags that need to be processed is defined) 
     * this Method MUST return <code>null</code>
     */
    public Boolean processPOS(String posTag, double posProb);
    /**
     * Called to check if a chunk should be used to search for Concepts.
     * @param chunkTag the tag (type) of the chunk
     * @param chunkProb the probability of the chunk tag or <code>1.0</code> if
     * not available
     * @return <code>true</code> if chunks with this tag (type) should be
     * processed (used to search for matches of concepts) and <code>false</code>
     * if not. If this information is not available (e.g. no set of Tags that
     * need to be processed is defined) this Method MUST return <code>null</code>
     */
    public Boolean processChunk(String chunkTag,double chunkProb);
    /**
     * Tokenizes the parsed label
     * @param label the label to tokenize
     * @return the spans of the tokens
     */
    public String[] tokenize(String label);
}