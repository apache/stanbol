package org.apache.stanbol.enhancer.nlp.pos;

import org.apache.clerezza.rdf.core.UriRef;

public enum LexicalCategory {
    Noun,
    Verb,
    Adjective,
    Adposition,
    Adverb,
    Conjuction,
    Interjection,
    PronounOrDeterminer,
    Punctuation,
    Quantifier,
    Residual,
    Unique,
    ;
    static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";
    
    UriRef uri;
    
    LexicalCategory(){
        this.uri = new UriRef(OLIA_NAMESPACE+name());
    }
    
    public UriRef getUri(){
        return uri;
    }
    
    @Override
    public String toString() {
        return "olia:"+name();
    }
}
