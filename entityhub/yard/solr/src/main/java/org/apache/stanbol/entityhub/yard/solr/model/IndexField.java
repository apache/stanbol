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
package org.apache.stanbol.entityhub.yard.solr.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.defaults.SpecialFieldEnum;

/**
 * Represents a logical field within the index.
 * <p>
 * A logical field consists of the following parts:
 * <ul>
 * <li>The path, a list of path elements (URIs parsed as String)
 * <li>The {@link IndexDataType}
 * <li>The language
 * </ul>
 * <p>
 * Logical fields are than mapped with an 1..n mapping to actual fields in the Index Documents. This
 * functionality is provided by the {@link FieldMapper}
 * 
 * @author Rupert Westenthaler
 * 
 */
public class IndexField {

    private final List<String> path;
    private final SpecialFieldEnum specialField;
    private final IndexDataType indexType;
    private final Set<String> languages;
    private final int hash;
 
    /**
     * Constructs a new IndexField
     * @param path
     * @param languages
     * @throws IllegalArgumentException
     */
    public IndexField(List<String> path, String... languages) throws IllegalArgumentException {
        this(path,null,languages);
    }

    /**
     * Constructs a new IndexField
     * 
     * @param path
     * @param indexType
     * @param language
     * @throws IllegalArgumentException
     */
    public IndexField(List<String> path, IndexDataType indexType, String... languages) throws IllegalArgumentException {
        this(path,indexType,languages != null ? Arrays.asList(languages) : null);
    }
    /**
     * Constructs a new IndexField
     * @param path
     * @param indexType
     * @param languages
     * @throws IllegalArgumentException
     */
    public IndexField(List<String> path, IndexDataType indexType, Collection<String> languages) throws IllegalArgumentException {
        this.specialField = getSpecialField(path);
        // we need to create a new list, to ensure, that no one can change this member!
        this.path = Collections.unmodifiableList(new ArrayList<String>(path));
        //NOTE: no data types for special fields
        if (indexType == null || specialField != null) {
            this.indexType = IndexDataType.DEFAULT; // the type representing no pre- nor suffix
        } else {
            this.indexType = indexType;
        }
        //NOTE: no languages for special fields
        if (specialField != null || languages == null || languages.isEmpty() ) {
            this.languages = Collections.emptySet();
        } else {
            Set<String> languageSet = new HashSet<String>();
            for (String language : languages) {
                if (language == null || language.isEmpty()) {
                    languageSet.add(null); // interpret empty as default language
                } else {
                    languageSet.add(language);
                }
            }
            this.languages = Collections.unmodifiableSet(languageSet);
        }
        // calculate the hash of is immutable class only once
        hash = this.path.hashCode() + this.indexType.hashCode() + this.languages.hashCode();
        //we do not need to use specialField for the has as those do have an
        //unique hash by {special path, no type, no language}.
    }
    /**
     * Checks if this {@link IndexField} represents a field registered with the
     * {@link SpecialFieldEnum}.
     * @return the state
     */
    public boolean isSpecialField(){
        return specialField != null;
    }
    /**
     * If this path represents a special field registered with the 
     * {@link SpecialFieldEnum} than it will return the according entry of this
     * enumeration. Otherwise <code>null</code> is returned
     * @return the {@link SpecialFieldEnum} or <code>null</code> if this
     * {@link IndexField} is not a special one.
     */
    public SpecialFieldEnum getSpecialField(){
        return specialField;
    }

    /**
     * Checks if the path is not <code>null</code>, empty and does not contain a <code>null</code> or empty
     * element.
     * 
     * @param path
     *            the path to validate
     * @throws IllegalArgumentException
     *             if the parsed path in not valid
     */
    public static void validatePath(List<String> path) throws IllegalArgumentException {
        checkPathElements(path);
    }
    private static SpecialFieldEnum checkPathElements(List<String> path) throws IllegalArgumentException {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Parameter path MUST NOT be NULL nor empty!");
        }
        for(String field : path){
            if(field == null || field.isEmpty()){
                throw new IllegalArgumentException(String.format(
                    "The parsed path MUST NOT contain a NULL value or an empty element (path=%s)!", path));
            }
            SpecialFieldEnum specialField = SpecialFieldEnum.getSpecialField(field);
            if(specialField != null){
                if(path.size() > 1){
                    throw new IllegalArgumentException(String.format(
                        "Special Fields MUST NOT be used on path with a length > 1 " +
                        "(path='%s' | specialField='%s')!",
                        path, specialField.getUri()));
                }
                return specialField;
            }
        }
        return null;
    }
    /**
     * Checks if the parsed path represents a special field. This also validates
     * the parsed path with the same rules as applied by {@link #validatePath(List)}
     * @param path the path
     * @return the {@link SpecialFieldEnum special field} or <code>null</code>.
     * @throws IllegalArgumentException if the parsed path is not valid
     */
    public static SpecialFieldEnum getSpecialField(List<String> path){
        return checkPathElements(path);
    }
    
    /**
     * Getter for the Path
     * 
     * @return the path. Unmodifiable list, guaranteed to contain at lest one element. All elements are
     *         guaranteed NOT <code>null</code> and NOT empty.
     */
    public final List<String> getPath() {
        return path;
    }

    /**
     * Getter for the index data type
     * 
     * @return the index data type. Guaranteed to be NOT <code>null</code>
     */
    public final IndexDataType getDataType() {
        return indexType;
    }

    /**
     * Checks if this field defines any language
     * 
     * @return <code>true</code> if a language is defined for this field. Note that <code>true</code> is
     *         returned if the language is <code>null</code>.
     */
    public final boolean hasLanguage() {
        return !languages.isEmpty();
    }

    /**
     * Getter for the Languages.
     * 
     * @return the languages. Unmodifiable collection, guaranteed to contain at least one element. May contain
     *         the <code>null</code> value (used for the default language).
     */
    public final Collection<String> getLanguages() {
        return languages;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexField && ((IndexField) obj).path.equals(path)
               && ((IndexField) obj).indexType.equals(indexType)
               && ((IndexField) obj).languages.equals(languages);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return String.format("IndexField[path: %s|type: %s", path, indexType)
               + (hasLanguage() ? String.format("|languages: %s]", languages) : "]");
    }

}
