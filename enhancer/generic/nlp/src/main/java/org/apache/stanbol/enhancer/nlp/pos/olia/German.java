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
package org.apache.stanbol.enhancer.nlp.pos.olia;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
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
            new IRI("http://purl.org/olia/stts.owl"));
        STTS.getProperties().put("olia.linkingModel", 
            new IRI("http://purl.org/olia/stts-link.rdf"));
        STTS.addTag(new PosTag("ADJA", Pos.AttributiveAdjective));
        STTS.addTag(new PosTag("ADJD", Pos.PredicativeAdjective));
        STTS.addTag(new PosTag("ADV", LexicalCategory.Adverb));
        STTS.addTag(new PosTag("APPR", Pos.Preposition));
        STTS.addTag(new PosTag("APPRART", Pos.FusedPrepArt));
        STTS.addTag(new PosTag("APPO", Pos.Postposition));
        STTS.addTag(new PosTag("APZR", Pos.Circumposition));
        STTS.addTag(new PosTag("ART", Pos.Article));
        STTS.addTag(new PosTag("CARD", Pos.CardinalNumber));
        STTS.addTag(new PosTag("FM", Pos.Foreign));
        STTS.addTag(new PosTag("ITJ", LexicalCategory.Interjection));
        STTS.addTag(new PosTag("KOUI", Pos.SubordinatingConjunction));
        STTS.addTag(new PosTag("KOUS", Pos.SubordinatingConjunctionWithFiniteClause));
        STTS.addTag(new PosTag("KON", Pos.CoordinatingConjunction));
        STTS.addTag(new PosTag("KOKOM", Pos.ComparativeParticle));
        STTS.addTag(new PosTag("NN", Pos.CommonNoun));
        STTS.addTag(new PosTag("NE", Pos.ProperNoun));
        STTS.addTag(new PosTag("PDS", Pos.DemonstrativePronoun,Pos.SubstitutivePronoun));
        STTS.addTag(new PosTag("PDAT", Pos.DemonstrativePronoun, Pos.AttributivePronoun));
        STTS.addTag(new PosTag("PIS", Pos.SubstitutivePronoun, Pos.IndefinitePronoun));
        STTS.addTag(new PosTag("PIAT",  Pos.AttributivePronoun, Pos.IndefinitePronoun));
        STTS.addTag(new PosTag("PIDAT", Pos.AttributivePronoun, Pos.IndefinitePronoun));
        STTS.addTag(new PosTag("PPER", Pos.PersonalPronoun));
        STTS.addTag(new PosTag("PPOSS", Pos.SubstitutivePronoun, Pos.PossessivePronoun));
        STTS.addTag(new PosTag("PPOSAT", Pos.AttributivePronoun, Pos.PossessivePronoun));
        STTS.addTag(new PosTag("PRELS", Pos.SubstitutivePronoun, Pos.RelativePronoun));
        STTS.addTag(new PosTag("PRELAT", Pos.AttributivePronoun, Pos.RelativePronoun));
        STTS.addTag(new PosTag("PRF", Pos.ReflexivePronoun));
        STTS.addTag(new PosTag("PWS", Pos.SubstitutivePronoun, Pos.InterrogativePronoun));
        STTS.addTag(new PosTag("PWAT", Pos.AttributivePronoun, Pos.InterrogativePronoun));
        STTS.addTag(new PosTag("PWAV", LexicalCategory.Adverb, Pos.RelativePronoun, Pos.InterrogativePronoun));
        STTS.addTag(new PosTag("PAV", Pos.PronominalAdverb));
        //Tiger-STTS for PAV
        STTS.addTag(new PosTag("PROAV", Pos.PronominalAdverb));
        STTS.addTag(new PosTag("PTKA", Pos.AdjectivalParticle));
        STTS.addTag(new PosTag("PTKANT", Pos.Particle));
        STTS.addTag(new PosTag("PTKNEG", Pos.NegativeParticle));
        STTS.addTag(new PosTag("PTKVZ", Pos.VerbalParticle));
        STTS.addTag(new PosTag("PTKZU", Pos.Particle)); //particle "zu"  e.g. "zu [gehen]".
        STTS.addTag(new PosTag("TRUNC", Pos.Abbreviation)); //e.g. An- [und Abreise] 
        STTS.addTag(new PosTag("VVIMP", Pos.ImperativeVerb));
        STTS.addTag(new PosTag("VVINF", Pos.Infinitive));
        STTS.addTag(new PosTag("VVFIN", Pos.FiniteVerb));
        STTS.addTag(new PosTag("VVIZU", Pos.Infinitive));
        STTS.addTag(new PosTag("VVPP", Pos.PastParticiple));
        STTS.addTag(new PosTag("VAFIN", Pos.FiniteVerb, Pos.AuxiliaryVerb));
        STTS.addTag(new PosTag("VAIMP", Pos.AuxiliaryVerb, Pos.ImperativeVerb));
        STTS.addTag(new PosTag("VAINF", Pos.AuxiliaryVerb, Pos.Infinitive));
        STTS.addTag(new PosTag("VAPP", Pos.PastParticiple, Pos.AuxiliaryVerb));
        STTS.addTag(new PosTag("VMFIN", Pos.FiniteVerb, Pos.ModalVerb));
        STTS.addTag(new PosTag("VMINF", Pos.Infinitive, Pos.ModalVerb));
        STTS.addTag(new PosTag("VMPP", Pos.PastParticiple, Pos.ModalVerb));
        STTS.addTag(new PosTag("XY")); //non words (e.g. H20, 3:7 ...)
        STTS.addTag(new PosTag("$.", Pos.Point));
        STTS.addTag(new PosTag("$,", Pos.Comma));
        STTS.addTag(new PosTag("$(", Pos.ParentheticalPunctuation));
        //Normal nouns in named entities (not in stts 1999)
        STTS.addTag(new PosTag("NNE", Pos.ProperNoun)); //TODO maybe map to common non
    }
}
