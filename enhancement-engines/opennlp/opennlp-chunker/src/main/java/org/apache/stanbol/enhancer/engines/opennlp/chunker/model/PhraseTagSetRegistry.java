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
package org.apache.stanbol.enhancer.engines.opennlp.chunker.model;

import java.util.HashMap;
import java.util.Map;

import opennlp.tools.chunker.Chunker;

import org.apache.stanbol.enhancer.nlp.model.tag.TagSet;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;

/**
 * Registry for {@link PhraseTag} {@link TagSet}s used by OpenNLP
 * {@link Chunker}.<p>
 * TODO: consider to add a {@link TagSet}Registry feature to the
 * org.apache.stanbol.enhancer.nlp module. Maybe even register TagSets to 
 * the OSGI Environment.
 * @author Rupert Westenthaler
 *
 */
public final class PhraseTagSetRegistry {
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

    public static final TagSet<PhraseTag> FRENCH = new TagSet<PhraseTag>(
            "French Treebank+ Phrase TagSet", "fr");
    
    static {
        FRENCH.addTag(new PhraseTag("AP", LexicalCategory.Adjective));
        FRENCH.addTag(new PhraseTag("AdP",LexicalCategory.Adverb));
        FRENCH.addTag(new PhraseTag("COORD",LexicalCategory.Conjuction));
        FRENCH.addTag(new PhraseTag("NP",LexicalCategory.Noun));
        FRENCH.addTag(new PhraseTag("PP", LexicalCategory.PronounOrDeterminer));
        FRENCH.addTag(new PhraseTag("VN",LexicalCategory.Verb));
        FRENCH.addTag(new PhraseTag("VPinf",LexicalCategory.Verb));
        FRENCH.addTag(new PhraseTag("VPpart",LexicalCategory.Verb));
        FRENCH.addTag(new PhraseTag("Ssub"));
        FRENCH.addTag(new PhraseTag("Srel"));
        FRENCH.addTag(new PhraseTag("Sint"));
        getInstance().add(FRENCH);
    }
}
