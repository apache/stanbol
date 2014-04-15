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
package org.apache.stanbol.commons.opennlp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Chunk;

/**
 * Enumeration with pre-configured sets of POS tags for finding nouns, verbs ...
 * in different languages
 * @author Rupert Westenthaler
 * @deprecated replaced by STANBOL-733 (stanbol nlp processing module
 */
public enum PosTagsCollectionEnum {

    /**
     * Nouns related POS types for English based on the 
     * <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">
     * Penn Treebank</a> tag set.
     * <p>
     * NOTE the "``" tag is also added as noun, because it can not be found in
     * the official tag set and is sometimes used to tag nouns.
     */
    EN_NOUN("en",PosTypeCollectionType.NOUN,"NN","NNP","NNPS","NNS","FW","CD","``"),
    /**
     * Verb related POS types for English based on the 
     * <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">
     * Penn Treebank</a> tag set
     */
    EN_VERB("en",PosTypeCollectionType.VERB,"VB","VBD","VBG","VBN","VBP","VBZ"),
    /**
     * POS types one needs typically to follow to build {@link Chunk}s over 
     * Nouns (e.g. "University_NN of_IN Otago_NNP" or "Geneva_NNP ,_, Ohio_NNP").
     * For English and based on the 
     * <a href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">
     * Penn Treebank</a> tag set
     */
    EN_FOLLOW("en",PosTypeCollectionType.FOLLOW,"#","$"," ","(",")",",",".",":","POS","IN","JJ"),
    /**
     * Noun related POS types for German based on the 
     * <a href="http://www.ims.uni-stuttgart.de/projekte/corplex/TagSets/stts-table.html">
     * STTS Tag Set</a> 
     */
    DE_NOUN("de",PosTypeCollectionType.NOUN,"NN","NE","FM","XY"),
    /**
     * Verb related POS types for German based on the 
     * <a href="http://www.ims.uni-stuttgart.de/projekte/corplex/TagSets/stts-table.html">
     * STTS Tag Set</a> 
     */
    DE_VERB("de",PosTypeCollectionType.VERB,"VVFIN","VVIMP","VVINF","VVIZU","VVPP","VAFIN","VAIMP","VAINF",
        "VAPP","VMFIN","VMINF","VMPP"),
    /**
     * POS types one needs typically to follow to build {@link Chunk}s over 
     * Nouns (e.g. "University_NN of_IN Otago_NNP" or "Geneva_NNP ,_, Ohio_NNP").
     * For German based on the 
     * <a href="http://www.ims.uni-stuttgart.de/projekte/corplex/TagSets/stts-table.html">
     * STTS Tag Set</a> 
     */
    DE_FOLLOW("de",PosTypeCollectionType.FOLLOW,"$.","$,","$(","APPR"),
    /**
     * POS types representing Nouns for Danish based on the PAROLE Tagset as
     * described by <a href="http://korpus.dsl.dk/paroledoc_en.pdf">this paper</a>
     * <p>
     * TODO: Someone who speaks Danish should check this List
     * NOTES:<ul>
     * <li> included also "XX" and "XR" because the examples in the
     * training data for OpenNLP seam to be good candidates for processing
     * <li> "AC" is included because it refers to numbers
     * </ul>
     */
    DA_NOUN("da",PosTypeCollectionType.NOUN,"N","NP","NC","AC","XX","XR"),
    /**
     * POS types representing Verbs for Danish based on the PAROLE Tagset as
     * described by <a href="http://korpus.dsl.dk/paroledoc_en.pdf">this paper</a>
     * <p>
     * TODO: Someone who speaks Danish should check this List
     */
    DA_VERB("da",PosTypeCollectionType.VERB,"V","VA","VE"),
    /**
     * POS types that are followd to extend chunks for Danish based on the PAROLE Tagset as
     * described by <a href="http://korpus.dsl.dk/paroledoc_en.pdf">this paper</a>
     * <p>
     * TODO: Someone who speaks Danish should check this List<p>
     * NOTES:<ul>
     * <li> included also "U" for unknown, because most of the examples in the
     * training data for OpenNLP seam to be good candidates for following
     * <li> "XA" is included because the examples include units of 
     * <li> "XP" stands for punctuation and such
     * </ul>
     */
    DA_FOLLOW("da",PosTypeCollectionType.FOLLOW,"XP","XA","SP","CS","CC","U"),
    /**
     * POS types for Nouns based on the
     * <a href="http://beta.visl.sdu.dk/visl/pt/symbolset-floresta.html">PALAVRAS tag set</a>
     * for Portuguese.<p>
     * TODO: Someone who speaks this language should check this List<p>
     * NOTES: Currently this includes nouns, proper nouns and numbers.
     * In addition I added "vp". "vp" is not part of the POS tag set 
     * documentation but in the training set there is a single occurrence 
     * therefore the POS tagger sometimes do tag words with this tag.
     */
    PT_NOUN("pt",PosTypeCollectionType.NOUN,"n","num","prop","vp"),
    /**
     * POS types for Verbs based on the
     * <a href="http://beta.visl.sdu.dk/visl/pt/symbolset-floresta.html">PALAVRAS tag set</a>
     * for Portuguese.<p>
     * TODO: Someone who speaks this language should check this List<p>
     */
    PT_VERB("pt",PosTypeCollectionType.VERB,"v-pcp","v-fin","v-inf","v-ger"),
    /**
     * POS types followed to build Chunks based on the
     * <a href="http://beta.visl.sdu.dk/visl/pt/symbolset-floresta.html">PALAVRAS tag set</a>
     * for Portuguese.<p>
     * TODO: Someone who speaks this language should check this List<p>
     * NOTES: Currently this pubctations and prepositions. 
     */
    PT_FOLLOW("pt",PosTypeCollectionType.FOLLOW,"punc", "prp"),
    /**
     * POS types for Nouns based on the WOTAN tagset for Dutch (as used with 
     * Mbt).<p>
     * TODOO: Someone who speaks this language should checkthis List<p>
     * NOTES: This includes now Nouns, Numbers and "others".
     */
    NL_NOUN("nl",PosTypeCollectionType.NOUN,"N","Num","Misc"),
    /**
     * POS types for Verbs based on the WOTAN tagset for Dutch (as used with 
     * Mbt).<p>
     * The tagger does not distinguish the different forms fo verbs. Therefore
     * it is enough so include "V"
     */
    NL_VERB("nl",PosTypeCollectionType.VERB,"V"),
    /**
     * POS types followed to build Chunks based on the WOTAN tagset for Dutch 
     * (as used with Mbt).<p>
     * NOTES: THis includes only prepositions and punctuations
     * 
     */
    NL_FOLLOW("nl",PosTypeCollectionType.FOLLOW,"Punc","Prep"),
    /**
     * POS types for Nouns for Swedish language based on 
     * <a href="http://w3.msi.vxu.se/users/nivre/research/MAMBAlex.html">
     * Lexical categories in MAMBA</a>
     * NOTE: <ul>
     * <li> This includes all typical noun categories as defined by MAMBA
     * <li> Unclassifiable part-of-speech and
     * <li> Numerical "RO"
     * <li> EN is excluded 
     * </ul>
     */
    SV_NOUN("sv",PosTypeCollectionType.NOUN,"NN","PN","AN","MN","VN","XX","RO"),
    /**
     * POS types for Verbs of the Swedish language based on the
     * <a href="http://w3.msi.vxu.se/users/nivre/research/MAMBAlex.html">
     * Lexical categories in MAMBA</a>
     */
    SV_VERB("sv",PosTypeCollectionType.VERB,"MV","AV","BV","FV","GV","HV","KV","QV","SV","VV","WV"),
    /**
     * POS types followed to build Chunks based on the TODO
     * <p>
     * NOTES: this includes  prepositions, Part of idiom, Infinitive marker
     *  as well as all kinds of punctuations
     */
    SV_FOLLOW("sv",PosTypeCollectionType.FOLLOW,"PR","ID","IM","I?","IC","IG","IK","IP","IQ","IR","IS","IT","IU"),
    /**
     * Nouns related POS types for Spanish language.
     * The description of the Tagset is available at
     * http://www.lsi.upc.edu/~nlp/SVMTool/parole.html
     */
    ES_NOUN("es",PosTypeCollectionType.NOUN,"NC","NP","Z"),
    /**
     * Verb related POS types for Spanish language.
     * I was not able to find the list, so POS tag results where used to 
     * create this configuration 
     */
    ES_VERB("es",PosTypeCollectionType.VERB,"VMI", "VMS", "VMM", "VMC", "VMN",
        "VMG", "VMP", "VAI", "VAS","VAM", "VAC", "VAN", "VAG", "VAP"),
    /**
     * POS types one needs typically to follow to build {@link Chunk}s over 
     * Nouns (e.g. "University_NN of_IN Otago_NNP" or "Geneva_NNP ,_, Ohio_NNP").
     * I was not able to find the list, so POS tag results where used to 
     * create this configuration.<p>
     * For now "SP" and all "F*" tokens referring to '.', ';', ...
     */
    ES_FOLLOW("es",PosTypeCollectionType.FOLLOW,"AQ","SP","Fc","Ft","Fp","Fe","Fd","Fx","Fat","Fit","Fpa","Fpt","Fg","Faa","Ft");
    Set<String> tags;
    private String language;
    private PosTypeCollectionType type;
    private PosTagsCollectionEnum(String lang, PosTypeCollectionType type, String...tags) {
        this.tags = new HashSet<String>(Arrays.asList(tags));
        this.language = lang;
        this.type = type;
    }
    /**
     * Getter for the set of POS tags
     * @return the tags
     */
    public final Set<String> getTags() {
        return tags;
    }
    /**
     * @return the language
     */
    public final String getLanguage() {
        return language;
    }
    /**
     * @return the type
     */
    public final PosTypeCollectionType getType() {
        return type;
    }
    
    private static final Map<CollectionType,PosTagsCollectionEnum> tagCollections;
    
    static {
        Map<CollectionType,PosTagsCollectionEnum> tcm = new HashMap<CollectionType,PosTagsCollectionEnum>();
        for(PosTagsCollectionEnum collection : PosTagsCollectionEnum.values()){
            CollectionType type = new CollectionType(collection.getLanguage(), collection.getType());
            if(tcm.put(type, collection) != null){
                throw new IllegalStateException("The PosTagsCollectionEnum contains" +
                		"Multiple POS tags collections for the Language '"+collection.getLanguage()+
                		"' and POS tag type '"+collection.getType()+"'!");
            }
        }
        tagCollections = Collections.unmodifiableMap(tcm);
    }
    /**
     * Getter for the POS (Part-of-Speech) tag collection for the given language
     * and type
     * @param lang the language
     * @param type the type
     * @return the collection or <code>null</code> if no configuration for the
     * parsed parameters is available.
     */
    public static Set<String> getPosTagCollection(String lang, PosTypeCollectionType type){
        PosTagsCollectionEnum collection = tagCollections.get(
            new CollectionType(lang, type));
        return collection == null ? null : collection.getTags();
    }
    /**
     * Internally used as key for a Map that mapps POS tag collection sets 
     * based on Language and {@link PosTypeCollectionType}
     * @author Rupert Westenthaler
     *
     */
    private static final class CollectionType {
        protected String lang;
        protected PosTypeCollectionType type;

        private CollectionType(String lang,PosTypeCollectionType type) {
            this.lang = lang;
            this.type = type;
        }
        @Override
        public int hashCode() {
            return lang.hashCode()+type.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof CollectionType &&
                ((CollectionType)obj).lang.equals(lang) &&
                ((CollectionType)obj).type == type;
        }
    }
    
}
