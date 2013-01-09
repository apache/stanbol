package org.apache.stanbol.enhancer.nlp;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

/**
 * Defines the {@link Annotation} constants typically used by NLP components
 */
public interface NlpAnnotations {
    
    /**
     * The POS {@link Annotation} added by POS taggers to {@link Token}s of
     * an {@link AnalysedText}.
     */
    Annotation<PosTag> POS_ANNOTATION = new Annotation<PosTag>(
            "stanbol.enhancer.nlp.pos", PosTag.class);
    /**
     * 
     */
    Annotation<NerTag> NER_ANNOTATION = new Annotation<NerTag>(
            "stanbol.enhancer.nlp.ner", NerTag.class);
    
    /**
     * The Phrase {@link Annotation} added by chunker to a group of
     * [1..*] {@link Token}s.<p>
     * This annotation is typically found on {@link Chunk}s.
     */
    Annotation<PhraseTag> PHRASE_ANNOTATION = new Annotation<PhraseTag>(
            "stanbol.enhancer.nlp.phrase", PhraseTag.class);
    
    /**
     * The Sentiment {@link Annotation} added by a sentiment tagger typically
     * to single {@link Token}s that do carry a positive or negative sentiment.
     */
    Annotation<Double> SENTIMENT_ANNOTATION = new Annotation<Double>(
            "stanbol.enhancer.nlp.sentiment", Double.class);
    /**
     * {@link Annotation} representing the Morphological analysis of a word. 
     * Typically used on {@link Token}s.<p>
     * The {@link MorphoFeatures} defines at least the Lemma and [1..*] POS tags.
     * NOTE that the POS tag information does not assign a Tag to the {@link Token},
     * but rather specifies that if the Token is classified by a {@link #POS_ANNOTATION}
     * to be of one of the Tags the definitions of this {@link MorphoFeatures} can
     * be applied.
     */
    Annotation<MorphoFeatures> MORPHO_ANNOTATION = new Annotation<MorphoFeatures>(
            "stanbol.enhancer.nlp.morpho",MorphoFeatures.class);

    /* 
     * Currently only used as part of MorphoFeatures
     */
//    Annotation<CaseTag> CASE_ANNOTATION = new Annotation<CaseTag>(
//            "stanbol.enhancer.nlp.morpho.case",CaseTag.class);
//
//    Annotation<GenderTag> GENDER_ANNOTATION = new Annotation<GenderTag>(
//            "stanbol.enhancer.nlp.morpho.gender",GenderTag.class);
//
//    Annotation<NumberTag> NUMBER_ANNOTATION = new Annotation<NumberTag>(
//            "stanbol.enhancer.nlp.morpho.number",NumberTag.class);
//    
//    Annotation<PersonTag> PERSON_ANNOTATION = new Annotation<PersonTag>(
//            "stanbol.enhancer.nlp.morpho.person",PersonTag.class);
//
//    Annotation<TenseTag> TENSE_ANNOTATION = new Annotation<TenseTag>(
//            "stanbol.enhancer.nlp.morpho.tense",TenseTag.class);
//
//    Annotation<VerbMoodTag> VERB_MOOD_ANNOTATION = new Annotation<VerbMoodTag>(
//            "stanbol.enhancer.nlp.morpho.verb-mood",VerbMoodTag.class);

}
