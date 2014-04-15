/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreemnets.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import java.util.ArrayList;
import java.util.List;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;
import opennlp.tools.util.StringUtil;

/**
 * Performs tokenization using the character class whitespace. Will create
 * seperate tokens for punctation at the end of words. 
 * Intended to be used to extract alphanumeric
 * keywords from texts
 * 
 * @author Rupert Westenthaler
 */
public final class KeywordTokenizer implements Tokenizer {

    public static final KeywordTokenizer INSTANCE;

    static {
        INSTANCE = new KeywordTokenizer();
    }

    private KeywordTokenizer() {}

    public String[] tokenize(String s) {
        return Span.spansToStrings(tokenizePos(s), s);
    }

    public Span[] tokenizePos(String s) {
        boolean isWhitespace;
        List<Span> tokens = new ArrayList<Span>();
        int sl = s.length();
        int start = -1;
        char pc = 0;
        for (int ci = 0; ci <= sl; ci++) {
            char c = ci < sl ? s.charAt(ci) : ' ';
            isWhitespace = StringUtil.isWhitespace(c);
            if (!isWhitespace & start < 0) { // new token starts
                start = ci;
            }
            if (isWhitespace && start >= 0) { // end of token
                // limited support for punctations at the end of words
                if (start < ci - 1 && (pc == '.' || pc == ',' || 
                        pc == '!' || pc == '?' || pc == ';' || pc == ':')) {
                    tokens.add(new Span(start, ci - 1));
                    tokens.add(new Span(ci - 1, ci));
                } else {
                    tokens.add(new Span(start, ci));
                }
                start = -1;
            }
        }
        return tokens.toArray(new Span[tokens.size()]);
    }
}
