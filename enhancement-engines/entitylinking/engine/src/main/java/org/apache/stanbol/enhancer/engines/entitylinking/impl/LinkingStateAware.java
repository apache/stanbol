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
package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;

/**
 * Provides callbacks form the {@link EntityLinker} about the currently
 * processed Tokens. 
 * @author Rupert Westenthaler
 *
 */
public interface LinkingStateAware {
    /**
     * Callback notifying that the {@link EntityLinker} has completed the
     * linking for the parsed {@link Section} (as {@link Sentence} in case
     * sentence annotations are present in the {@link AnalysedText}).
     * @param sentence the completed section
     */
    void endSection(Section sentence);

    /**
     * Callback notifying that the {@link EntityLinker} has started to link a
     * new section of the text
     * @param sentence the completed section
     */
    void startSection(Section sentence);
    /**
     * The next {@link Token} to be processed by the {@link EntityLinker}
     * @param token the token that will be processed next
     */
    void startToken(Token token);
    /**
     * The next {@link Token} to be processed by the {@link EntityLinker}
     * @param token the token that will be processed next
     */
    void endToken(Token token);

}
