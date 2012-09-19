package org.apache.stanbol.enhancer.nlp.pos.olia;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.nlp.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

public class Spanish {
    private Spanish(){}
    
    /**
     * The PAROLE TagSet for Spanish. This is mainly defined based on this
     * <a herf="http://www.lsi.upc.edu/~nlp/SVMTool/parole.html">description</a>
     * as the Ontology mainly defines REGEX tag matchings that are not very
     * helpful for fast tag lookup needed for processing POS tag results. 
     */
    public static final TagSet<PosTag> PAROLE = new TagSet<PosTag>(
        "PAROLE Spanish", "es");

    static {
        //TODO: define constants for annotation model and linking model
        PAROLE.getProperties().put("olia.annotationModel", 
            new UriRef("http://purl.org/olia/parole_es_cat.owl"));
// NO linking model
//        PAROLE.getProperties().put("olia.linkingModel", 
//            new UriRef("http://purl.org/olia/???"));
        PAROLE.addTag(new PosTag("AO", LexicalCategory.Adjective));
        PAROLE.addTag(new PosTag("AQ", LexicalCategory.Adjective));
        PAROLE.addTag(new PosTag("CC", LexicalCategory.Conjuction));
        PAROLE.addTag(new PosTag("CS", LexicalCategory.Conjuction));
        PAROLE.addTag(new PosTag("DA", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("DD", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("DE", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("DI", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("DN", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("DP", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("DT", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("Faa", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fat", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fc", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fd", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fe", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fg", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fh", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fia", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fit", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fp", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fpa", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fpt", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fs", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fx", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fz", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("I", LexicalCategory.Interjection));
        PAROLE.addTag(new PosTag("NC", LexicalCategory.Noun));
        PAROLE.addTag(new PosTag("NP", LexicalCategory.Noun));
        PAROLE.addTag(new PosTag("P0", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("PD", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("PE", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("PI", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("PN", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("PP", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("PR", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("PT", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("PX", LexicalCategory.PronounOrDeterminer));
        PAROLE.addTag(new PosTag("RG", LexicalCategory.Adverb));
        PAROLE.addTag(new PosTag("RN", LexicalCategory.Adverb));
        PAROLE.addTag(new PosTag("SP", LexicalCategory.Adposition));
        PAROLE.addTag(new PosTag("VAG", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VAI", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VAM", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VAN", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VAP", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VAS", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VMG", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VMI", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VMM", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VMN", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VMP", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VMS", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VSG", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VSI", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VSM", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VSN", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VSP", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("VSS", LexicalCategory.Verb));
        PAROLE.addTag(new PosTag("W", LexicalCategory.Quantifier)); //date times
        PAROLE.addTag(new PosTag("X", null)); //unknown
        PAROLE.addTag(new PosTag("Y", null)); //abbreviation
        PAROLE.addTag(new PosTag("Z", null)); //Figures
        PAROLE.addTag(new PosTag("Zm", LexicalCategory.Quantifier)); //currency
        PAROLE.addTag(new PosTag("Zp", LexicalCategory.Quantifier)); //percentage
        
        
    }
}
