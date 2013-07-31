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
package org.apache.stanbol.enhancer.engines.kuromoji.impl;

import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

/**
 * Used as intermediate representation of NER annotations so that one needs
 * not to obtain a write lock on the {@link ContentItem} for each detected 
 * entity
 * @author Rupert Westenthaler
 *
 */
class NerData {
    
    protected final NerTag tag;
    protected final int start;
    protected int end;
    protected String context;
    
    protected NerData(NerTag ner, int start){
        this.tag = ner;
        this.start = start;
    }
    
}