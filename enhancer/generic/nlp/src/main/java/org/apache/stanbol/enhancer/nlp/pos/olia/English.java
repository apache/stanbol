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
            new IRI("http://purl.org/olia/penn.owl"));
        PENN_TREEBANK.getProperties().put("olia.linkingModel", 
            new IRI("http://purl.org/olia/penn-link.rdf"));

        PENN_TREEBANK.addTag(new PosTag("CC", Pos.CoordinatingConjunction));
        PENN_TREEBANK.addTag(new PosTag("CD",Pos.CardinalNumber));
        PENN_TREEBANK.addTag(new PosTag("DT",Pos.Determiner));
        PENN_TREEBANK.addTag(new PosTag("EX",Pos.ExistentialParticle)); //TODO: unsure mapping
        PENN_TREEBANK.addTag(new PosTag("FW",Pos.Foreign));
        PENN_TREEBANK.addTag(new PosTag("IN",Pos.Preposition, Pos.SubordinatingConjunction));
        PENN_TREEBANK.addTag(new PosTag("JJ",LexicalCategory.Adjective));
        PENN_TREEBANK.addTag(new PosTag("JJR",LexicalCategory.Adjective, Pos.ComparativeParticle));
        PENN_TREEBANK.addTag(new PosTag("JJS",LexicalCategory.Adjective, Pos.SuperlativeParticle));
        PENN_TREEBANK.addTag(new PosTag("LS",Pos.ListMarker));
        PENN_TREEBANK.addTag(new PosTag("MD",Pos.ModalVerb));
        PENN_TREEBANK.addTag(new PosTag("NN",Pos.CommonNoun, Pos.SingularQuantifier));
        PENN_TREEBANK.addTag(new PosTag("NNP",Pos.ProperNoun, Pos.SingularQuantifier));
        PENN_TREEBANK.addTag(new PosTag("NNPS",Pos.ProperNoun, Pos.PluralQuantifier));
        PENN_TREEBANK.addTag(new PosTag("NNS",Pos.CommonNoun, Pos.PluralQuantifier));
        PENN_TREEBANK.addTag(new PosTag("PDT",Pos.Determiner)); //TODO should be Pre-Determiner
        PENN_TREEBANK.addTag(new PosTag("POS")); //TODO: map Possessive Ending (e.g., Nouns ending in 's)
        PENN_TREEBANK.addTag(new PosTag("PP",Pos.PersonalPronoun));
        PENN_TREEBANK.addTag(new PosTag("PP$",Pos.PossessivePronoun));
        PENN_TREEBANK.addTag(new PosTag("PRP",Pos.PersonalPronoun));
        PENN_TREEBANK.addTag(new PosTag("PRP$",Pos.PossessivePronoun));
        PENN_TREEBANK.addTag(new PosTag("RB",LexicalCategory.Adverb));
        PENN_TREEBANK.addTag(new PosTag("RBR",LexicalCategory.Adverb,Pos.ComparativeParticle));
        PENN_TREEBANK.addTag(new PosTag("RBS",LexicalCategory.Adverb,Pos.SuperlativeParticle));
        PENN_TREEBANK.addTag(new PosTag("RP",Pos.Participle));
        PENN_TREEBANK.addTag(new PosTag("SYM",Pos.Symbol));
        PENN_TREEBANK.addTag(new PosTag("TO",LexicalCategory.Adposition));
        PENN_TREEBANK.addTag(new PosTag("UH",LexicalCategory.Interjection));
        PENN_TREEBANK.addTag(new PosTag("VB",Pos.Infinitive)); //TODO check a Verb in the base form should be Pos.Infinitive
        PENN_TREEBANK.addTag(new PosTag("VBD",Pos.PastParticiple)); //TODO check
        PENN_TREEBANK.addTag(new PosTag("VBG",Pos.PresentParticiple,Pos.Gerund));
        PENN_TREEBANK.addTag(new PosTag("VBN",Pos.PastParticiple));
        PENN_TREEBANK.addTag(new PosTag("VBP",Pos.PresentParticiple));
        PENN_TREEBANK.addTag(new PosTag("VBZ",Pos.PresentParticiple));
        PENN_TREEBANK.addTag(new PosTag("WDT",Pos.WHDeterminer));
        PENN_TREEBANK.addTag(new PosTag("WP",Pos.WHPronoun));
        PENN_TREEBANK.addTag(new PosTag("WP$",Pos.PossessivePronoun, Pos.WHPronoun));
        PENN_TREEBANK.addTag(new PosTag("WRB",Pos.WHTypeAdverbs));
        PENN_TREEBANK.addTag(new PosTag("´´",Pos.CloseQuote));
        PENN_TREEBANK.addTag(new PosTag(":",Pos.Colon));
        PENN_TREEBANK.addTag(new PosTag(",",Pos.Comma));
        PENN_TREEBANK.addTag(new PosTag("$",LexicalCategory.Residual));
        PENN_TREEBANK.addTag(new PosTag("\"",Pos.Quote));
        PENN_TREEBANK.addTag(new PosTag("``",Pos.OpenQuote));
        PENN_TREEBANK.addTag(new PosTag(".",Pos.Point));
        PENN_TREEBANK.addTag(new PosTag("{",Pos.OpenCurlyBracket));
        PENN_TREEBANK.addTag(new PosTag("}",Pos.CloseCurlyBracket));
        PENN_TREEBANK.addTag(new PosTag("[",Pos.OpenSquareBracket));
        PENN_TREEBANK.addTag(new PosTag("]",Pos.CloseSquareBracket));
        PENN_TREEBANK.addTag(new PosTag("(",Pos.OpenParenthesis));
        PENN_TREEBANK.addTag(new PosTag(")",Pos.CloseParenthesis));
    }
    
}
