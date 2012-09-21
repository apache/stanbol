package org.apache.stanbol.enhancer.engines.celi;

import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.CeliLemmatizerEnhancementEngine;
import org.apache.stanbol.enhancer.nlp.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.pos.olia.English;
import org.apache.stanbol.enhancer.nlp.pos.olia.German;
import org.apache.stanbol.enhancer.nlp.pos.olia.Spanish;

/**
 * {@link TagSet}s for known CELI (linguagrid.org) POS models.<p>
 * 
 * @author Rupert Westenthaler
 *
 */
public final class PosTagSetRegistry {
    
    private static PosTagSetRegistry instance = new PosTagSetRegistry();
    
    private PosTagSetRegistry(){}
    
    private final Map<String, TagSet<PosTag>> models = new HashMap<String,TagSet<PosTag>>();
    
    public static PosTagSetRegistry getInstance(){
        return instance;
    }
    
    private void add(TagSet<PosTag> model) {
        for(String lang : model.getLanguages()){
            if(models.put(lang, model) != null){
                throw new IllegalStateException("Multiple Models for Language '"
                    + lang+"'! This is an error in the static confituration of "
                    + "this class. Please report this to the stanbol-dev mailing"
                    + "list!");
            }
        }
    }
    /**
     * Getter for the {@link TagSet} by language. If no {@link TagSet}
     * is available for an Language this will return <code>null</code>
     * @param language the language
     * @return the AnnotationModel or <code>null</code> if non is defined
     */
    public TagSet<PosTag> getTagSet(String language){
        return models.get(language);
    }
    
    /**
     * TODO: create correct POS TagSets for the Languages supported by CELI
     * This creates a default set for all languages supported by the
     * CELI lemmatizer Engine
     */
    public static final TagSet<PosTag> ITALIEN = new TagSet<PosTag>("CELI Italien","it");
    
    static {
        ITALIEN.addTag(new PosTag("ADJ",LexicalCategory.Adjective));
        ITALIEN.addTag(new PosTag("ADV",LexicalCategory.Adverb));
        ITALIEN.addTag(new PosTag("ART",LexicalCategory.PronounOrDeterminer));
        ITALIEN.addTag(new PosTag("CLI")); //mapping ??
        ITALIEN.addTag(new PosTag("CONJ",LexicalCategory.Conjuction));
        ITALIEN.addTag(new PosTag("PREP",LexicalCategory.Adposition));
        ITALIEN.addTag(new PosTag("NF",LexicalCategory.Noun));
        ITALIEN.addTag(new PosTag("NM",LexicalCategory.Noun));
        ITALIEN.addTag(new PosTag("V",LexicalCategory.Verb));
        //add the PosSet to the registry
        getInstance().add(ITALIEN);
    }

}
