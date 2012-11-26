package org.apache.stanbol.enhancer.engines.opennlp.pos.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.commons.opennlp.PosTagsCollectionEnum;
import org.apache.stanbol.enhancer.nlp.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.nlp.pos.olia.English;
import org.apache.stanbol.enhancer.nlp.pos.olia.German;
import org.apache.stanbol.enhancer.nlp.pos.olia.Spanish;

/**
 * {@link TagSet}s for known <a herf="http://opennlp.apache.org/">OpenNLP</a>
 * POS models.<p>
 * When available this refers to models defined by the 
 * <a herf="http://nlp2rdf.lod2.eu/olia/">OLIA</a> Ontologies. Other TagSets
 * are - for now - directly defined in this class.
 * <p>
 * Specifications in this class are based on {@link PosTagsCollectionEnum}.
 *  Links/defines to the POS {@link TagSet}s used by
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
     * Links to the Penn Treebank model as defined by the 
     * <a herf="http://nlp2rdf.lod2.eu/olia/">OLIA</a> Ontology.
     * @see English#PENN_TREEBANK
     */
    public static final TagSet<PosTag> ENGLISH = English.PENN_TREEBANK;
    
    static { //adds the English model to the getInstance()
        getInstance().add(ENGLISH);
    }
    /**
     * Links to the STTS model as defined by the 
     * <a herf="http://nlp2rdf.lod2.eu/olia/">OLIA</a> Ontology.
     * @see German#STTS
     */
    public static final TagSet<PosTag> GERMAN = German.STTS;
    
    static { //adds the English model to the getInstance()
        getInstance().add(GERMAN);
    }
    /**
     * Links to the PAROLE model as defined by the 
     * <a herf="http://nlp2rdf.lod2.eu/olia/">OLIA</a> Ontology.
     * @see Spanish#PAROLE
     */
    public static final TagSet<PosTag> SPANISH = Spanish.PAROLE;
    
    static { //adds the Spanish model to the getInstance()
        getInstance().add(SPANISH);
    }
    /**
     * POS types representing Nouns for Danish based on the PAROLE Tagset as
     * described by <a href="http://korpus.dsl.dk/paroledoc_en.pdf">this paper</a>
     */
    public static final TagSet<PosTag> DANISH = new TagSet<PosTag>("PAROLE Danish","da");
    
    static {
        DANISH.addTag(new PosTag("N",LexicalCategory.Noun));
        DANISH.addTag(new PosTag("NP",LexicalCategory.Noun));
        DANISH.addTag(new PosTag("NC",LexicalCategory.Noun));
        DANISH.addTag(new PosTag("AC",LexicalCategory.Quantifier)); //numbers
        DANISH.addTag(new PosTag("AO",LexicalCategory.Quantifier)); //numbers
        DANISH.addTag(new PosTag("XX",LexicalCategory.Noun)); //unsure
        DANISH.addTag(new PosTag("XF",LexicalCategory.Noun)); //foreign word
        DANISH.addTag(new PosTag("XR",LexicalCategory.Quantifier)); //number letters
        DANISH.addTag(new PosTag("XR",LexicalCategory.Quantifier)); //symbol letters
        DANISH.addTag(new PosTag("XA",LexicalCategory.Noun)); //abbreviations
        DANISH.addTag(new PosTag("XX",LexicalCategory.Quantifier)); //tokenizer errors
        DANISH.addTag(new PosTag("V",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VA",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VAD",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VAF",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VAG",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VAPR",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VAPA",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VE",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VED",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VEF",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("XP",LexicalCategory.Punctuation)); 
        DANISH.addTag(new PosTag("CC",LexicalCategory.Conjuction)); 
        DANISH.addTag(new PosTag("CS",LexicalCategory.Conjuction)); 
        DANISH.addTag(new PosTag("U",LexicalCategory.Noun)); //unknown tokens
        DANISH.addTag(new PosTag("SP",LexicalCategory.Adposition)); 
        DANISH.addTag(new PosTag("AN",LexicalCategory.Adjective)); //unsure
        DANISH.addTag(new PosTag("R",LexicalCategory.Adverb)); //unsure
        DANISH.addTag(new PosTag("RG",LexicalCategory.Adverb)); //unsure
        DANISH.addTag(new PosTag("PD",LexicalCategory.PronounOrDeterminer)); //unsure
        DANISH.addTag(new PosTag("PI",LexicalCategory.PronounOrDeterminer)); //unsure
        DANISH.addTag(new PosTag("PT",LexicalCategory.PronounOrDeterminer)); //unsure
        DANISH.addTag(new PosTag("PP",LexicalCategory.PronounOrDeterminer)); //unsure
        DANISH.addTag(new PosTag("PO",LexicalCategory.PronounOrDeterminer)); //unsure
        DANISH.addTag(new PosTag("PC",LexicalCategory.PronounOrDeterminer)); //unsure
        DANISH.addTag(new PosTag("U=",LexicalCategory.Unique)); //unsure
        DANISH.addTag(new PosTag("I=",LexicalCategory.Interjection)); //unsure
        getInstance().add(DANISH);
    }
    
    /**
     * POS tags for the Portuguese POS model of OpenNLP based the
     * <a href="http://beta.visl.sdu.dk/visl/pt/symbolset-floresta.html">PALAVRAS tag set</a>
     * <p>
     */
    public static final TagSet<PosTag> PORTUGUESE = new TagSet<PosTag>("PALAVRAS Portuguese","pt");
    
    static {
        PORTUGUESE.addTag(new PosTag("n",LexicalCategory.Noun));
        PORTUGUESE.addTag(new PosTag("prop",LexicalCategory.Noun));
        PORTUGUESE.addTag(new PosTag("v-fin",LexicalCategory.Verb));
        PORTUGUESE.addTag(new PosTag("v-inf",LexicalCategory.Verb));
        PORTUGUESE.addTag(new PosTag("v-pcp",LexicalCategory.Verb));
        PORTUGUESE.addTag(new PosTag("v-ger",LexicalCategory.Verb));
        PORTUGUESE.addTag(new PosTag("art",LexicalCategory.PronounOrDeterminer));
        PORTUGUESE.addTag(new PosTag("pron-pers",LexicalCategory.PronounOrDeterminer));
        PORTUGUESE.addTag(new PosTag("pron-det",LexicalCategory.PronounOrDeterminer));
        PORTUGUESE.addTag(new PosTag("pron-indp",LexicalCategory.PronounOrDeterminer));
        PORTUGUESE.addTag(new PosTag("adv",LexicalCategory.Adverb));
        PORTUGUESE.addTag(new PosTag("num",LexicalCategory.Quantifier));
        PORTUGUESE.addTag(new PosTag("prp",LexicalCategory.Adposition));
        PORTUGUESE.addTag(new PosTag("in",LexicalCategory.Interjection));
        PORTUGUESE.addTag(new PosTag("conj-s",LexicalCategory.Conjuction));
        PORTUGUESE.addTag(new PosTag("conj-c",LexicalCategory.Conjuction));
        PORTUGUESE.addTag(new PosTag("punc",LexicalCategory.Punctuation)); //missing on the webpage ^
        getInstance().add(PORTUGUESE);
    }
    /**
     * POS tags used by the Dutch POS model of OpenNLP for Dutch.<p>
     * Source: J.T. Berghmans, "WOTAN: Een automatische grammatikale tagger 
     * voor het Nederlands", doctoral dissertation, Department of language & 
     * Speech, Nijmegen University (renamed to Radboud University), 
     * december 1994.<p>
     * 
     */
    public static final TagSet<PosTag> DUTCH = new TagSet<PosTag>("WOTAN Dutch","nl");
    
    static {
        DUTCH.addTag(new PosTag("Adj",LexicalCategory.Adjective));
        DUTCH.addTag(new PosTag("Adv",LexicalCategory.Adverb));
        DUTCH.addTag(new PosTag("Art",LexicalCategory.PronounOrDeterminer));
        DUTCH.addTag(new PosTag("Conj",LexicalCategory.Conjuction));
        DUTCH.addTag(new PosTag("Int",LexicalCategory.Interjection));
        DUTCH.addTag(new PosTag("N",LexicalCategory.Noun));
        DUTCH.addTag(new PosTag("Num",LexicalCategory.Quantifier));
        DUTCH.addTag(new PosTag("Misc",null));
        DUTCH.addTag(new PosTag("Prep",LexicalCategory.Adposition));
        DUTCH.addTag(new PosTag("Pron",LexicalCategory.PronounOrDeterminer));
        DUTCH.addTag(new PosTag("Punc",LexicalCategory.Punctuation));
        DUTCH.addTag(new PosTag("V",LexicalCategory.Verb));
        getInstance().add(DUTCH);
    }
    /**
     * POS tags used by the Swedish POS model of OpenNLP for Swedish based on the
     * <a href="http://w3.msi.vxu.se/users/nivre/research/MAMBAlex.html">
     * Lexical categories in MAMBA</a>
     */
    public static final TagSet<PosTag> SWEDISH = new TagSet<PosTag>("MAMBA Swedish","sv");
    
    static {
        SWEDISH.addTag(new PosTag("PN",LexicalCategory.Noun));
        SWEDISH.addTag(new PosTag("MN",LexicalCategory.Noun));
        SWEDISH.addTag(new PosTag("AN",LexicalCategory.Noun));
        SWEDISH.addTag(new PosTag("VN",LexicalCategory.Noun));
        SWEDISH.addTag(new PosTag("NN",LexicalCategory.Noun));
        SWEDISH.addTag(new PosTag("PO",LexicalCategory.PronounOrDeterminer));
        SWEDISH.addTag(new PosTag("EN",LexicalCategory.Quantifier));
        SWEDISH.addTag(new PosTag("RO",LexicalCategory.Quantifier));
        SWEDISH.addTag(new PosTag("AJ",LexicalCategory.Adjective));
        SWEDISH.addTag(new PosTag("AV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("BV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("HV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("WV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("QV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("MV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("KV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("SV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("GV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("FV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("VV",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("TP",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("SP",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("AB",LexicalCategory.Adverb));
        SWEDISH.addTag(new PosTag("PR",LexicalCategory.Adposition));
        SWEDISH.addTag(new PosTag("IM",LexicalCategory.Verb));
        SWEDISH.addTag(new PosTag("++",LexicalCategory.Conjuction));
        SWEDISH.addTag(new PosTag("UK",LexicalCategory.Conjuction));
        SWEDISH.addTag(new PosTag("IK",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("IP",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("I?",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("IU",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("IQ",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("IS",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("IT",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("IR",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("IC",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("PU",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("IG",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("YY",LexicalCategory.Conjuction));
        SWEDISH.addTag(new PosTag("ID",LexicalCategory.Noun));
        SWEDISH.addTag(new PosTag("XX",null));
        getInstance().add(SWEDISH);
    }

}
