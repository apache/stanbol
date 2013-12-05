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
package org.apache.stanbol.enhancer.engines.opennlp.pos.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.commons.opennlp.PosTagsCollectionEnum;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
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
        DANISH.addTag(new PosTag("NP",Pos.ProperNoun));
        DANISH.addTag(new PosTag("NC",Pos.CommonNoun));
        DANISH.addTag(new PosTag("AC",Pos.CardinalNumber)); //numbers
        DANISH.addTag(new PosTag("AO",Pos.OrdinalNumber)); //numbers
        DANISH.addTag(new PosTag("AN",LexicalCategory.Adjective));
        DANISH.addTag(new PosTag("XX",Pos.Typo)); //non-words (incl typos ..)
        DANISH.addTag(new PosTag("XF",Pos.Foreign)); //foreign word
        DANISH.addTag(new PosTag("XR",Pos.Symbol)); //symbol letters
        DANISH.addTag(new PosTag("XA",LexicalCategory.Noun)); //abbreviations
        DANISH.addTag(new PosTag("XS",Pos.Abbreviation)); //abbreviations
        DANISH.addTag(new PosTag("V",LexicalCategory.Verb)); 
        DANISH.addTag(new PosTag("VA", Pos.MainVerb)); 
        DANISH.addTag(new PosTag("VAD", Pos.MainVerb, Pos.IndicativeVerb)); 
        DANISH.addTag(new PosTag("VAF", Pos.MainVerb, Pos.Infinitive)); 
        DANISH.addTag(new PosTag("VAG", Pos.MainVerb, Pos.Gerund)); 
        DANISH.addTag(new PosTag("VAPR", Pos.MainVerb, Pos.PresentParticiple)); 
        DANISH.addTag(new PosTag("VAPA", Pos.MainVerb, Pos.PastParticiple)); 
        DANISH.addTag(new PosTag("VE", LexicalCategory.Verb)); //TODO MedialVerb is missing 
        DANISH.addTag(new PosTag("VED",Pos.IndicativeVerb)); //TODO MedialVerb is missing 
        DANISH.addTag(new PosTag("VEF",Pos.Infinitive)); //TODO MedialVerb is missing 
        DANISH.addTag(new PosTag("XP",LexicalCategory.Punctuation)); 
        DANISH.addTag(new PosTag("CC",Pos.CoordinatingConjunction)); 
        DANISH.addTag(new PosTag("SC",Pos.SubordinatingConjunction)); 
        DANISH.addTag(new PosTag("U")); //unmarked for degree
        DANISH.addTag(new PosTag("SP",Pos.Preposition)); 
        DANISH.addTag(new PosTag("R",Pos.AdjectivalAdverb));
        DANISH.addTag(new PosTag("RG",LexicalCategory.Adverb));
        DANISH.addTag(new PosTag("PD",Pos.DemonstrativePronoun)); 
        DANISH.addTag(new PosTag("PI",Pos.IndefinitePronoun)); 
        DANISH.addTag(new PosTag("PT",Pos.InterrogativePronoun,Pos.RelativePronoun));
        DANISH.addTag(new PosTag("PP",Pos.PersonalPronoun)); //unsure
        DANISH.addTag(new PosTag("PO",Pos.PossessivePronoun)); //unsure
        DANISH.addTag(new PosTag("PC",Pos.ReciprocalPronoun)); //unsure
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
        PORTUGUESE.addTag(new PosTag("n",Pos.CommonNoun));
        PORTUGUESE.addTag(new PosTag("prop",Pos.ProperNoun));
        PORTUGUESE.addTag(new PosTag("adj",LexicalCategory.Adjective));
        PORTUGUESE.addTag(new PosTag("v-fin",Pos.FiniteVerb));
        PORTUGUESE.addTag(new PosTag("v-inf",Pos.Infinitive));
        PORTUGUESE.addTag(new PosTag("v-pcp",Pos.Participle));
        PORTUGUESE.addTag(new PosTag("v-ger",Pos.Gerund));
        PORTUGUESE.addTag(new PosTag("art",Pos.Article));
        PORTUGUESE.addTag(new PosTag("pron",Pos.Pronoun));
        PORTUGUESE.addTag(new PosTag("pron-pers",Pos.PersonalPronoun));
        PORTUGUESE.addTag(new PosTag("pron-det",Pos.DeterminalPronoun));
        PORTUGUESE.addTag(new PosTag("pron-indp",Pos.Pronoun)); //TODO: missing independent pronoun 
        PORTUGUESE.addTag(new PosTag("adv",LexicalCategory.Adverb));
        PORTUGUESE.addTag(new PosTag("num",Pos.Numeral));
        PORTUGUESE.addTag(new PosTag("prp",Pos.Preposition));
        PORTUGUESE.addTag(new PosTag("in",LexicalCategory.Interjection));
        PORTUGUESE.addTag(new PosTag("conj-s",Pos.SubordinatingConjunction));
        PORTUGUESE.addTag(new PosTag("conj-c",Pos.CoordinatingConjunction));
        PORTUGUESE.addTag(new PosTag("punc",LexicalCategory.Punctuation)); //missing on the webpage ^
        getInstance().add(PORTUGUESE);
    }
    /**
     * POS tags used by the Dutch POS model of OpenNLP for Dutch.<p>
     * Source: J.T. Berghmans, "WOTAN: Een automatische grammatikale tagger 
     * voor het Nederlands", doctoral dissertation, Department of language & 
     * Speech, Nijmegen University (renamed to Radboud University), 
     * december 1994.<p>
     * <b>NOTE:</b> This {@link TagSet} DOES NOT distinquish beteen Proper- and
     * Common- Nouns!<p>
     */
    public static final TagSet<PosTag> DUTCH = new TagSet<PosTag>("WOTAN Dutch","nl");
    
    static {
        DUTCH.addTag(new PosTag("Adj",LexicalCategory.Adjective));
        DUTCH.addTag(new PosTag("Adv",LexicalCategory.Adverb));
        DUTCH.addTag(new PosTag("Art",Pos.Article));
        DUTCH.addTag(new PosTag("Conj",LexicalCategory.Conjuction));
        DUTCH.addTag(new PosTag("Int",LexicalCategory.Interjection));
        DUTCH.addTag(new PosTag("N",LexicalCategory.Noun));
        DUTCH.addTag(new PosTag("Num",Pos.Numeral));
        DUTCH.addTag(new PosTag("Misc"));
        DUTCH.addTag(new PosTag("Prep",Pos.Preposition));
        DUTCH.addTag(new PosTag("Pron",Pos.Pronoun));
        DUTCH.addTag(new PosTag("Punc",LexicalCategory.Punctuation));
        DUTCH.addTag(new PosTag("V",LexicalCategory.Verb));
        getInstance().add(DUTCH);
    }
    /**
     * POS tags used by the Swedish POS model of OpenNLP for Swedish based on the
     * <a href="http://w3.msi.vxu.se/users/nivre/research/MAMBAlex.html">
     * Lexical categories in MAMBA</a>
     * Most of the <i>'interesting'</i> {@link Pos} mappings would be defined
     * as "Features" of MABAS.
     */
    public static final TagSet<PosTag> SWEDISH = new TagSet<PosTag>("MAMBA Swedish","sv");
    
    static {
        SWEDISH.addTag(new PosTag("PN",Pos.ProperNoun));
        SWEDISH.addTag(new PosTag("MN",Pos.CommonNoun)); //TODO: missing Meta-Nouns
        SWEDISH.addTag(new PosTag("AN",Pos.CommonNoun)); //TODO: missing Adjectival noun
        SWEDISH.addTag(new PosTag("VN",Pos.VerbalNoun));
        SWEDISH.addTag(new PosTag("NN",Pos.CommonNoun));
        SWEDISH.addTag(new PosTag("PO",Pos.Pronoun));
        SWEDISH.addTag(new PosTag("EN",Pos.IndefiniteArticle, Pos.Numeral));
        SWEDISH.addTag(new PosTag("RO",Pos.Numeral));
        SWEDISH.addTag(new PosTag("AJ",LexicalCategory.Adjective));
        SWEDISH.addTag(new PosTag("AV",LexicalCategory.Verb)); //"vara" (be)
        SWEDISH.addTag(new PosTag("BV",LexicalCategory.Verb)); //"bli(va)" (become)
        SWEDISH.addTag(new PosTag("HV",LexicalCategory.Verb)); //"ha(va)" (have)
        SWEDISH.addTag(new PosTag("WV",LexicalCategory.Verb)); //"vilja" (want)
        SWEDISH.addTag(new PosTag("QV",LexicalCategory.Verb)); //"kunna" (can)
        SWEDISH.addTag(new PosTag("MV",LexicalCategory.Verb)); //"m��ste" (must)
        SWEDISH.addTag(new PosTag("KV",LexicalCategory.Verb)); // locution "komma att" (periphrastic future)
        SWEDISH.addTag(new PosTag("SV",LexicalCategory.Verb)); //"skola" (will, shall)
        SWEDISH.addTag(new PosTag("GV",LexicalCategory.Verb)); //"g��ra" (do, make)
        SWEDISH.addTag(new PosTag("FV",LexicalCategory.Verb)); //f��" (get)
        SWEDISH.addTag(new PosTag("VV",LexicalCategory.Verb)); //all other verbs
        SWEDISH.addTag(new PosTag("TP",Pos.PastParticiple)); //PerfectParticle
        SWEDISH.addTag(new PosTag("SP",Pos.PresentParticiple));
        SWEDISH.addTag(new PosTag("AB",LexicalCategory.Adverb));
        SWEDISH.addTag(new PosTag("PR",Pos.Preposition));
        SWEDISH.addTag(new PosTag("IM",Pos.Infinitive));
        SWEDISH.addTag(new PosTag("++",Pos.CoordinatingConjunction));
        SWEDISH.addTag(new PosTag("UK",Pos.SubordinatingConjunction));
        SWEDISH.addTag(new PosTag("IK",Pos.Comma));
        SWEDISH.addTag(new PosTag("IP",Pos.Point));
        SWEDISH.addTag(new PosTag("I?",Pos.QuestionMark));
        SWEDISH.addTag(new PosTag("IU",Pos.ExclamativePoint));
        SWEDISH.addTag(new PosTag("IQ",Pos.Colon));
        SWEDISH.addTag(new PosTag("IS",Pos.SemiColon));
        SWEDISH.addTag(new PosTag("IT",Pos.Hyphen));
        SWEDISH.addTag(new PosTag("IR",Pos.ParentheticalPunctuation));
        SWEDISH.addTag(new PosTag("IC",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("PU",Pos.ListMarker));
        SWEDISH.addTag(new PosTag("IG",LexicalCategory.Punctuation));
        SWEDISH.addTag(new PosTag("YY",Pos.Interjection));
        SWEDISH.addTag(new PosTag("ID",LexicalCategory.Noun));
        SWEDISH.addTag(new PosTag("XX"));
        getInstance().add(SWEDISH);
    }
    /**
     * POS tags used by the French Treebank as described in 
     * <a href="http://alpage.inria.fr/statgram/frdep/Publications/crabbecandi-taln2008-final.pdf">
     * Expériences d’analyse syntaxique statistique du français</a> page 8.<p>
     * Note that this Tagset was originally introduced by Crabb ́e & Candito, 2008
     * but the linked paper contains a nice tabular overview of it.
     */
    public static final TagSet<PosTag> FRENCH = new TagSet<PosTag>("Treebank+ French","fr");
    
    static {
        //Cat C
        FRENCH.addTag(new PosTag("CS",Pos.SubordinatingConjunction));
        FRENCH.addTag(new PosTag("CC",Pos.CoordinatingConjunction));
        //Cat CL
        FRENCH.addTag(new PosTag("CLO", Pos.PersonalPronoun)); //Clitic
        FRENCH.addTag(new PosTag("CLS", Pos.PersonalPronoun)); //Clitic
        FRENCH.addTag(new PosTag("CLR", Pos.PersonalPronoun)); //Clitic
        //Cat P
        FRENCH.addTag(new PosTag("P",Pos.Preposition));
        FRENCH.addTag(new PosTag("P+D")); //no cat
        FRENCH.addTag(new PosTag("P+PRO")); //no cat
        //Cat I
        FRENCH.addTag(new PosTag("I", LexicalCategory.Interjection)); //no cat
        //Cat PONCT
        FRENCH.addTag(new PosTag("PONCT",LexicalCategory.Punctuation));
        //Cat ET
        FRENCH.addTag(new PosTag("ET", Pos.Foreign));
        //Cat A
        FRENCH.addTag(new PosTag("ADJ",LexicalCategory.Adjective));
        FRENCH.addTag(new PosTag("ADJWH",LexicalCategory.Adjective));
        //Cat ADV
        FRENCH.addTag(new PosTag("ADV",LexicalCategory.Adverb));
        FRENCH.addTag(new PosTag("ADVWH",LexicalCategory.Adverb));
        //Cat PRO
        FRENCH.addTag(new PosTag("PRO",Pos.StrongPersonalPronoun)); //Strong Pronoun
        FRENCH.addTag(new PosTag("PROWH",Pos.StrongPersonalPronoun)); //Strong Pronoun
        FRENCH.addTag(new PosTag("PROREL",Pos.StrongPersonalPronoun)); //Strong Pronoun
        //Cat D
        FRENCH.addTag(new PosTag("DET",Pos.Determiner));
        FRENCH.addTag(new PosTag("DETWH",Pos.Determiner));
        //Cat N
        FRENCH.addTag(new PosTag("NC", Pos.CommonNoun));
        FRENCH.addTag(new PosTag("NPP", Pos.ProperNoun));
        //Cat V
        FRENCH.addTag(new PosTag("V",Pos.IndicativeVerb));
        FRENCH.addTag(new PosTag("VIMP",Pos.ImperativeVerb));
        FRENCH.addTag(new PosTag("VINF",Pos.Infinitive));
        FRENCH.addTag(new PosTag("VS",Pos.SubjunctiveVerb));
        FRENCH.addTag(new PosTag("VPP",Pos.PastParticiple));
        FRENCH.addTag(new PosTag("VPR", Pos.PresentParticiple)); //Verb Present?
        getInstance().add(FRENCH);
    }
}
