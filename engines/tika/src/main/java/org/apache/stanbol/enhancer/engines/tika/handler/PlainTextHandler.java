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
package org.apache.stanbol.enhancer.engines.tika.handler;

import java.io.Writer;

import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
/**
 * Small extensions to the default {@link ToTextContentHandler}. This allows
 * to <ul>
 * <li>skip ignoreable whitespaces
 * <li>skip linebreaks within literals
 * </ul>
 * 
 * @author Rupert Westenthaler
 *
 */
public class PlainTextHandler extends ToTextContentHandler {

    
    
    private final boolean skipWhitespaces;
    private final boolean skipLinebreakes;
    public PlainTextHandler(Writer writer, boolean skipIgnoreableWhitespaces, boolean skipLinebreaksWithinLiterals) {
        super(writer);
        this.skipWhitespaces = skipIgnoreableWhitespaces;
        this.skipLinebreakes = skipLinebreaksWithinLiterals;
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if(!skipWhitespaces){
            super.ignorableWhitespace(ch, start, length);
        } //else ignore
    }
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        int in;
        if(skipLinebreakes){
            //use an in(serte) and an it(erator) index to avoid copying
            //the data to a new char[].
            in = start;
            for(int it = start; it<length;it++){
                if(ch[it] != '\n'){
                    ch[in] = ch[it];
                    in++;
                } //ignore line breaks
            }
            if(in == start){ //only line breaks 
                return; // -> nothing to add
            }
        } else {
            in = length;
        }
        super.characters(ch, start, in);
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
    }
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        // TODO Auto-generated method stub
        super.endElement(uri, localName, qName);
    }
}
