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

package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer;

import java.util.ArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.osgi.framework.Constants;
/**
 * Simple Tokenizer that behaves equals as the
 * OpenNLP <code>opennlp.tools.tokenize.SimpleTokenizer</code>
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true)
@Service
@Properties(value={
    @Property(name=Constants.SERVICE_RANKING,intValue=-1000)
})
public class SimpleLabelTokenizer implements LabelTokenizer {

    private enum CT {WHITESPACE,LETTER,NUMBER,OTHER}
    
    @Override
    public String[] tokenize(String label, String language) {
        if(label == null){
            throw new IllegalArgumentException("The parsed Label MUST NOT be NULL!");
        }
        ArrayList<String> tokens = new ArrayList<String>();
        int start = -1;
        int pc = 0;
        CT state = CT.WHITESPACE;
        CT charType = CT.WHITESPACE;
        for (int i = 0; i < label.length(); i++) {
            int c = label.codePointAt(i);
            charType = getType(c);
            if (state == CT.WHITESPACE) {
                if (charType != CT.WHITESPACE) {
                    start = i;
                }
            } else {
                if (charType != state || charType == CT.OTHER && c != pc) {
                    tokens.add(label.substring(start, i));
                    start = i;
                }
            }
            state = charType;
            pc = c;
        }
        if (charType != CT.WHITESPACE) {
            tokens.add(label.substring(start, label.length()));
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    private CT getType(int c){
        if(Character.isLetter(c)){
            return CT.LETTER;
        } else if(Character.isDigit(c)){
            return CT.NUMBER;
        } else if(Character.isWhitespace(c) || 
                Character.getType(c) == Character.SPACE_SEPARATOR){
            return CT.WHITESPACE;
        } else {
            return CT.OTHER;
        }
    }
}
