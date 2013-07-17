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
package org.apache.stanbol.enhancer.nlp.model.tag;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotated;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

/**
 * An TagSet used for tagging {@link Annotated} resources like {@link Token}s,
 * {@link Chunk}s or even whole {@link Sentence}s and 
 * {@link AnalysedText Texts}s.<p>
 * A TagSet defines a set of {@link Tag} and can be usd for one or more
 * {@link #getLanguages() languages}.<p>
 * {@link TagSet} uses generics to allow the specification of more specific 
 * TagSets e.g. for {@link PosTag} or {@link PhraseTag}s.<p>
 */
public class TagSet<T extends Tag<T>> implements Iterable<T>{


    private final String name;
    private final Set<String> languages;
    
    private final Map<String,T> tag2PosTag = new HashMap<String,T>();
    
    private final Map<String,Object> properties = new HashMap<String,Object>();
    
    /**
     * Creates an AnnotationModel for Tags of a specific type (e.g.
     * {@link PosTag} or {@link PhraseTag}) that can be used for the parsed
     * Languages.<p>
     * In addition AnnotationModels allow to add additional properties.
     * Those can be used to assign information such as the 
     *  In addition this constructor allows to parse
     * URIs for Ontologies that define the model and the linking to the
     * <a herf="http://nlp2rdf.lod2.eu/olia/">nlp2rdf OLIA</a> annotation and
     * linking models.<p>
     * In the future those metadata might even be used by components to 
     * automatically create Annotation models.<p>
     * NOTE that the parsed name us used as unique criteria. TODO this should
     * be evaluated. 
     * @param name the unique name (is used for {@link #hashCode()} and 
     * @param languages the languages
     */
    public TagSet(String name, String...languages) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL!");
        }
        this.name = name;
        if(languages != null && languages.length > 0){
            Set<String> langSet = new HashSet<String>(Arrays.asList(languages));
            langSet.remove(null);
            this.languages = Collections.unmodifiableSet(langSet);
        }else {
            this.languages = Collections.emptySet();
                    
        }
    }
    
    /**
     * Getter for the properties of this AnnotationModel
     * @return
     */
    public Map<String,Object> getProperties(){
        return properties;
    }
    
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return the languages
     */
    public Set<String> getLanguages() {
        return languages;
    }
    /**
     * Adds an PosTag. This will replace any existing
     * Tag with the same {@link Tag#getTag()} value! 
     * @param tag the tag to add to this TagSet
     */
    public void addTag(T tag){
        if(tag != null){
            if(tag.getAnnotationModel() != null || this.equals(tag.getAnnotationModel())){
                throw new IllegalStateException("Unable to add "+tag+" to "+this
                    + "because it is already assigned to "+tag.getAnnotationModel());
            }
            tag.setAnnotationModel(this);
            T oldTag = tag2PosTag.put(tag.getTag(), tag);
            if(oldTag != null && this.equals(oldTag.getAnnotationModel())){
                oldTag.setAnnotationModel(null);
            }
        }
    }
    
    public T getTag(String tag){
        return tag2PosTag.get(tag);
    }
    
    @Override
    public Iterator<T> iterator() {
        return tag2PosTag.values().iterator();
    }
    
    @Override
    public String toString() {
        return String.format("AnnotationModel [name: %s |lanuages: %s]", 
            getName(), languages);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof TagSet && name.equals(((TagSet<?>)obj).name);
    }
}
