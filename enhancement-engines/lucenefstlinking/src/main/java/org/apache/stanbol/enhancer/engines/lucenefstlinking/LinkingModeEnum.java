package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;

public enum LinkingModeEnum {
    /**
     * Links every token in the parsed text
     */
    PLAIN,
    /**
     * Links only Linkable Tokens (typically all {@link LexicalCategory#Noun}s
     * or even only {@link Pos#ProperNoun} - depending on the 
     * {@link TextProcessingConfig} 
     */
    LINKABLE_TOKEN //,
//    /**
//     * Only {@link NerTag}s are linked with the vocabualry
//     */
//    NER

}
