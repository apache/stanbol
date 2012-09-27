package org.apache.stanbol.enhancer.nlp.pos.olia;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;


/**
 * Defines {@link TagSet}s for the English language.<p>
 * TODO: this is currently done manually but it should be able to generate this
 * based on the <a herf="http://nlp2rdf.lod2.eu/olia/">OLIA</a> Ontologies
 * @author Rupert Westenthaler
 *
 */
public final class English {
    
    private English(){}

    public static final TagSet<PosTag> PENN_TREEBANK = new TagSet<PosTag>(
        "Penn Treebank", "en");
    
    static {
        //TODO: define constants for annotation model and linking model
        PENN_TREEBANK.getProperties().put("olia.annotationModel", 
            new UriRef("http://purl.org/olia/penn.owl"));
        PENN_TREEBANK.getProperties().put("olia.linkingModel", 
            new UriRef("http://purl.org/olia/penn-link.rdf"));

        PENN_TREEBANK.addTag(new PosTag("CC", LexicalCategory.Conjuction));
        PENN_TREEBANK.addTag(new PosTag("CD",LexicalCategory.Quantifier));
        PENN_TREEBANK.addTag(new PosTag("DT",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("EX",null)); //existential there
        PENN_TREEBANK.addTag(new PosTag("FW",LexicalCategory.Noun)); //TODO check
        PENN_TREEBANK.addTag(new PosTag("IN",LexicalCategory.Adjective));
        PENN_TREEBANK.addTag(new PosTag("JJ",LexicalCategory.Adjective));
        PENN_TREEBANK.addTag(new PosTag("JJR",LexicalCategory.Adjective));
        PENN_TREEBANK.addTag(new PosTag("JJS",LexicalCategory.Adjective));
        PENN_TREEBANK.addTag(new PosTag("LS",null));
        PENN_TREEBANK.addTag(new PosTag("MD",LexicalCategory.Noun));
        PENN_TREEBANK.addTag(new PosTag("NN",LexicalCategory.Noun));
        PENN_TREEBANK.addTag(new PosTag("NNP",LexicalCategory.Noun));
        PENN_TREEBANK.addTag(new PosTag("NNPS",LexicalCategory.Noun));
        PENN_TREEBANK.addTag(new PosTag("NNS",LexicalCategory.Noun));
        PENN_TREEBANK.addTag(new PosTag("PDT",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("POS",null));
        PENN_TREEBANK.addTag(new PosTag("PP",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("PP$",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("PRP",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("PRP$",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("RB",LexicalCategory.Adverb));
        PENN_TREEBANK.addTag(new PosTag("RBR",LexicalCategory.Adverb));
        PENN_TREEBANK.addTag(new PosTag("RBS",LexicalCategory.Adverb));
        PENN_TREEBANK.addTag(new PosTag("RP",null));
        PENN_TREEBANK.addTag(new PosTag("SYM",LexicalCategory.Residual));
        PENN_TREEBANK.addTag(new PosTag("TO",LexicalCategory.Adposition));
        PENN_TREEBANK.addTag(new PosTag("UH",LexicalCategory.Interjection));
        PENN_TREEBANK.addTag(new PosTag("VB",LexicalCategory.Verb));
        PENN_TREEBANK.addTag(new PosTag("VBD",LexicalCategory.Verb));
        PENN_TREEBANK.addTag(new PosTag("VBG",LexicalCategory.Verb));
        PENN_TREEBANK.addTag(new PosTag("VBN",LexicalCategory.Verb));
        PENN_TREEBANK.addTag(new PosTag("VBP",LexicalCategory.Verb));
        PENN_TREEBANK.addTag(new PosTag("VBZ",LexicalCategory.Verb));
        PENN_TREEBANK.addTag(new PosTag("WDT",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("WP",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("WP$",LexicalCategory.PronounOrDeterminer));
        PENN_TREEBANK.addTag(new PosTag("WRB",LexicalCategory.Adverb));
        PENN_TREEBANK.addTag(new PosTag("´´",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag(":",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag(",",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag("$",LexicalCategory.Residual));
        PENN_TREEBANK.addTag(new PosTag("\"",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag("``",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag(".",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag("{",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag("}",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag("[",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag("]",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag("(",LexicalCategory.Punctuation));
        PENN_TREEBANK.addTag(new PosTag(")",LexicalCategory.Punctuation));
    }
    
}
