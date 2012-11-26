package org.apache.stanbol.enhancer.nlp.pos.olia;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.nlp.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

/**
 * Defines {@link TagSet}s for the German language.<p>
 * TODO: this is currently done manually but it should be able to generate this
 * based on the <a herf="http://nlp2rdf.lod2.eu/olia/">OLIA</a> Ontologies
 * 
 * @author Rupert Westenthaler
 *
 */
public final class German {

    private German(){}
    
    public static final TagSet<PosTag> STTS = new TagSet<PosTag>(
        "STTS", "de");

    static {
        //TODO: define constants for annotation model and linking model
        STTS.getProperties().put("olia.annotationModel", 
            new UriRef("http://purl.org/olia/stts.owl"));
        STTS.getProperties().put("olia.linkingModel", 
            new UriRef("http://purl.org/olia/stts-link.rdf"));
        STTS.addTag(new PosTag("ADJA", LexicalCategory.Adjective));
        STTS.addTag(new PosTag("ADJD", LexicalCategory.Adjective));
        STTS.addTag(new PosTag("ADV", LexicalCategory.Adverb));
        STTS.addTag(new PosTag("APPR", LexicalCategory.Adposition));
        STTS.addTag(new PosTag("APPRART", LexicalCategory.Adposition));
        STTS.addTag(new PosTag("APPO", LexicalCategory.Adposition));
        STTS.addTag(new PosTag("APZR", LexicalCategory.Adposition));
        STTS.addTag(new PosTag("ART", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("CARD", LexicalCategory.Quantifier));
        STTS.addTag(new PosTag("FM", LexicalCategory.Noun));
        STTS.addTag(new PosTag("ITJ", LexicalCategory.Interjection));
        STTS.addTag(new PosTag("KOUI", LexicalCategory.Conjuction));
        STTS.addTag(new PosTag("KOUS", LexicalCategory.Conjuction));
        STTS.addTag(new PosTag("KON", LexicalCategory.Conjuction));
        STTS.addTag(new PosTag("KOKOM", LexicalCategory.Conjuction));
        STTS.addTag(new PosTag("NN", LexicalCategory.Noun));
        STTS.addTag(new PosTag("NE", LexicalCategory.Noun));
        STTS.addTag(new PosTag("PDS", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PDAT", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PIS", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PIAT", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PIDAT", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PPER", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PPOSS", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PPOSAT", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PRELS", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PRELAT", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PRF", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PWS", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PWAT", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PWAV", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PAV", LexicalCategory.PronounOrDeterminer));
        //Tiger-STTS for PAV
        STTS.addTag(new PosTag("PROAV", LexicalCategory.PronounOrDeterminer));
        STTS.addTag(new PosTag("PTKA", LexicalCategory.Unique));
        STTS.addTag(new PosTag("PTKANT", LexicalCategory.Unique));
        STTS.addTag(new PosTag("PTKNEG", LexicalCategory.Unique));
        STTS.addTag(new PosTag("PTKVZ", LexicalCategory.Unique));
        STTS.addTag(new PosTag("PTKZU", LexicalCategory.Unique)); //particle "zu"  e.g. "zu [gehen]".
        STTS.addTag(new PosTag("TRUNC", null)); //e.g. An- [und Abreise] 
        STTS.addTag(new PosTag("VVIMP", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VVINF", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VVFIN", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VVIZU", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VVPP", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VAFIN", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VAIMP", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VAINF", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VAPP", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VMFIN", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VMINF", LexicalCategory.Verb));
        STTS.addTag(new PosTag("VMPP", LexicalCategory.Verb));
        STTS.addTag(new PosTag("XY", null)); //non words (e.g. H20, 3:7 ...)
        STTS.addTag(new PosTag("$.", LexicalCategory.Punctuation));
        STTS.addTag(new PosTag("$,", LexicalCategory.Punctuation));
        STTS.addTag(new PosTag("$(", LexicalCategory.Punctuation));
        //Normal nouns in named entities (not in stts 1999)
        STTS.addTag(new PosTag("NNE", LexicalCategory.Noun));
    }
}
