package org.apache.stanbol.enhancer.engines.opennlp.chunker.model;

import java.util.HashMap;
import java.util.Map;

import opennlp.tools.chunker.Chunker;

import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;

/**
 * Registry for {@link PhraseTag} {@link TagSet}s used by OpenNLP
 * {@link Chunker}.<p>
 * TODO: consider to add a {@link TagSet}Registry feature to the
 * org.apache.stanbol.enhancer.nlp module. Maybe even register TagSets to 
 * the OSGI Environment.
 * @author Rupert Westenthaler
 *
 */
public class PhraseTagSetRegistry {
    private static PhraseTagSetRegistry instance = new PhraseTagSetRegistry();
    
    private PhraseTagSetRegistry(){}
    
    private final Map<String, TagSet<PhraseTag>> models = new HashMap<String,TagSet<PhraseTag>>();
    
    public static PhraseTagSetRegistry getInstance(){
        return instance;
    }
    
    private void add(TagSet<PhraseTag> model) {
        for(String lang : model.getLanguages()){
            if(models.put(lang, model) != null){
                throw new IllegalStateException("Multiple TagSets for Language '"
                    + lang+"'! This is an error in the static confituration of "
                    + "this class. Please report this to the stanbol-dev mailing"
                    + "list!");
            }
        }
    }
    /**
     * Getter for the TagSet used by an {@link Chunker} of the parsed Language.
     * If no {@link TagSet} is available for an Language this will return 
     * <code>null</code>
     * @param language the language
     * @return the AnnotationModel or <code>null</code> if non is defined
     */
    public TagSet<PhraseTag> getTagSet(String language){
        return models.get(language);
    }

    public static final TagSet<PhraseTag> DEFAULT = new TagSet<PhraseTag>(
            "OpenNLP Default Chunker TagSet", "en","de");
    
    static {
        DEFAULT.addTag(new PhraseTag("NP", LexicalCategory.Noun));
        DEFAULT.addTag(new PhraseTag("VP",LexicalCategory.Verb));
        DEFAULT.addTag(new PhraseTag("PP", LexicalCategory.PronounOrDeterminer));
        getInstance().add(DEFAULT);
    }
}
