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

public final class Spanish {
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
            new IRI("http://purl.org/olia/parole_es_cat.owl"));
// NO linking model
//        PAROLE.getProperties().put("olia.linkingModel", 
//            new IRI("http://purl.org/olia/???"));
        PAROLE.addTag(new PosTag("AO", LexicalCategory.Adjective));
        PAROLE.addTag(new PosTag("AQ", Pos.QualifierAdjective));
        PAROLE.addTag(new PosTag("CC", Pos.CoordinatingConjunction));
        PAROLE.addTag(new PosTag("CS", Pos.SubordinatingConjunction));
        PAROLE.addTag(new PosTag("DA", Pos.Article));
        PAROLE.addTag(new PosTag("DD", Pos.DemonstrativeDeterminer));
        PAROLE.addTag(new PosTag("DE", Pos.ExclamatoryDeterminer));
        PAROLE.addTag(new PosTag("DI", Pos.IndefiniteDeterminer));
        PAROLE.addTag(new PosTag("DN", Pos.Numeral,Pos.Determiner));
        PAROLE.addTag(new PosTag("DP", Pos.PossessiveDeterminer));
        PAROLE.addTag(new PosTag("DT", Pos.InterrogativeDeterminer));
        PAROLE.addTag(new PosTag("Faa", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("Fat", Pos.ExclamativePoint));
        PAROLE.addTag(new PosTag("Fc", Pos.Comma));
        PAROLE.addTag(new PosTag("Fd", Pos.Colon));
        PAROLE.addTag(new PosTag("Fe", Pos.Quote));
        PAROLE.addTag(new PosTag("Fg", Pos.Hyphen));
        PAROLE.addTag(new PosTag("Fh", Pos.Slash));
        PAROLE.addTag(new PosTag("Fia", Pos.InvertedQuestionMark));
        PAROLE.addTag(new PosTag("Fit", Pos.QuestionMark));
        PAROLE.addTag(new PosTag("Fp", Pos.Point));
        PAROLE.addTag(new PosTag("Fpa", Pos.OpenParenthesis));
        PAROLE.addTag(new PosTag("Fpt", Pos.CloseParenthesis));
        PAROLE.addTag(new PosTag("Fs", Pos.SuspensionPoints));
        PAROLE.addTag(new PosTag("Fx", Pos.SemiColon));
        PAROLE.addTag(new PosTag("Fz", LexicalCategory.Punctuation));
        PAROLE.addTag(new PosTag("I", LexicalCategory.Interjection));
        PAROLE.addTag(new PosTag("NC", Pos.CommonNoun));
        PAROLE.addTag(new PosTag("NP", Pos.ProperNoun));
        PAROLE.addTag(new PosTag("P0", Pos.Pronoun)); //TODO: CliticPronoun is missing
        PAROLE.addTag(new PosTag("PD", Pos.DemonstrativePronoun));
        PAROLE.addTag(new PosTag("PE", Pos.ExclamatoryPronoun));
        PAROLE.addTag(new PosTag("PI", Pos.IndefinitePronoun));
        PAROLE.addTag(new PosTag("PN", Pos.Pronoun)); //TODO: NumeralPronoun is missing
        PAROLE.addTag(new PosTag("PP", Pos.PersonalPronoun));
        PAROLE.addTag(new PosTag("PR", Pos.RelativePronoun));
        PAROLE.addTag(new PosTag("PT", Pos.InterrogativePronoun));
        PAROLE.addTag(new PosTag("PX", Pos.PossessivePronoun));
        PAROLE.addTag(new PosTag("RG", LexicalCategory.Adverb));
        PAROLE.addTag(new PosTag("RN", Pos.NegativeAdverb));
        PAROLE.addTag(new PosTag("SP", Pos.Preposition));
        PAROLE.addTag(new PosTag("VAG", Pos.StrictAuxiliaryVerb, Pos.Gerund));
        PAROLE.addTag(new PosTag("VAI", Pos.StrictAuxiliaryVerb, Pos.IndicativeVerb));
        PAROLE.addTag(new PosTag("VAM", Pos.StrictAuxiliaryVerb, Pos.ImperativeVerb));
        PAROLE.addTag(new PosTag("VAN", Pos.StrictAuxiliaryVerb, Pos.Infinitive));
        PAROLE.addTag(new PosTag("VAP", Pos.StrictAuxiliaryVerb, Pos.Participle));
        PAROLE.addTag(new PosTag("VAS", Pos.StrictAuxiliaryVerb, Pos.SubjunctiveVerb));
        PAROLE.addTag(new PosTag("VMG", Pos.MainVerb, Pos.Gerund));
        PAROLE.addTag(new PosTag("VMI", Pos.MainVerb, Pos.IndicativeVerb));
        PAROLE.addTag(new PosTag("VMM", Pos.MainVerb, Pos.ImperativeVerb));
        PAROLE.addTag(new PosTag("VMN", Pos.MainVerb, Pos.Infinitive));
        PAROLE.addTag(new PosTag("VMP", Pos.MainVerb, Pos.Participle));
        PAROLE.addTag(new PosTag("VMS", Pos.MainVerb, Pos.SubjunctiveVerb));
        PAROLE.addTag(new PosTag("VSG", Pos.ModalVerb, Pos.Gerund));
        PAROLE.addTag(new PosTag("VSI", Pos.ModalVerb, Pos.IndicativeVerb));
        PAROLE.addTag(new PosTag("VSM", Pos.ModalVerb, Pos.ImperativeVerb));
        PAROLE.addTag(new PosTag("VSN", Pos.ModalVerb, Pos.Infinitive));
        PAROLE.addTag(new PosTag("VSP", Pos.ModalVerb, Pos.Participle));
        PAROLE.addTag(new PosTag("VSS", Pos.ModalVerb, Pos.SubjunctiveVerb));
        PAROLE.addTag(new PosTag("W", Pos.Date)); //date times
        PAROLE.addTag(new PosTag("X")); //unknown
        PAROLE.addTag(new PosTag("Y", Pos.Abbreviation)); //abbreviation
        PAROLE.addTag(new PosTag("Z", Pos.Image)); //Figures
        PAROLE.addTag(new PosTag("Zm", Pos.Symbol)); //currency
        PAROLE.addTag(new PosTag("Zp", Pos.Symbol)); //percentage
        
        
    }
}
