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