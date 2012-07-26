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
package org.apache.stanbol.entityhub.servicesapi.model;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * This interface is used by the Entityhub to define representations. It is used for
 * any kind of CRUD operations on the {@link Yard} (the storage of the Entityhub). <br>
 * The goal if this interface is to allow implementation based on different storage
 * solutions such as CMS, full text indices, triple stores, noSQL data stores ...<br>
 *
 * TODO: handling the differences between "NaturalLanguageText", "References" and
 *       "normal" values feels to complex! Need to reevaluate if this differentiation
 *       is needed or can be done in a more easy way!
 * TODO: add an API that allows to attach Content!
 * TODO: Do we need subNodes or are "references" enough.
 *
 * TODO: Check to use also Wrappers for fields and values (in analogy to
 *       {@link Reference} and {@link Text}. PRO: clearer API CON: more Objects
 *       to be garbage collected.
 *
 * @author Rupert Westenthaler
 */
public interface Representation {
    /**
     * Getter for the identifier.
     * @return the identifier
     */
    String getId();
    /**
     * Getter for a single Value for a field
     * @param <T> the generic type the returned value
     * @param field the field
     * @param type the type of the values
     * @return the (first) value of that field
     * @throws IllegalArgumentException if the type is not supported
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    <T> T getFirst(String field,Class<T> type) throws UnsupportedTypeException, IllegalArgumentException;

    /**
     * Getter for all values of a field
     * @param <T> the generic type of the returned values
     * @param field the field
     * @param type the type
     * @return the values of the field
     * @throws UnsupportedTypeException if the parsed type is not supported
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    <T> Iterator<T> get(String field,Class<T> type) throws UnsupportedTypeException, IllegalArgumentException;
    /**
     * Getter for the (first) value for a field
     * @param field the field
     * @return the first value of a field
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    Object getFirst(String field) throws IllegalArgumentException;
    /**
     * Getter for the first reference value for a field
     * @param field the field
     * @return the reference or null of the field has no reference as value
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    Reference getFirstReference(String field) throws IllegalArgumentException;
    /**
     * Getter for the first natural language text value of a specific language
     * @param field the field
     * @param language the language(s) of the natural language text value
     *             (If <code>null</code> is parsed as language, than also labels
     *             without language tag are included in the Result)
     * @return the first natural language text found for the parsed field
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
    */
    Text getFirst(String field, String...language) throws IllegalArgumentException;
    /**
     * Getter for all values for the requested field
     * @param field the field
     * @return the values of the field
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    Iterator<Object> get(String field) throws IllegalArgumentException;
    /**
     * Getter for all natural language text values of a field
     * @param field the field
     * @return the natural text values
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    Iterator<Text> getText(String field) throws IllegalArgumentException;
    /**
     * Getter for all natural language text values of a field
     * @param field the field
     * @param language the language(s) of the natural language text value
     *             (If <code>null</code> is parsed as language, than also labels
     *             without language tag are included in the Result)
     * @return iterator over all natural language text values in the requested
     *             language.
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    Iterator<Text> get(String field,String...language) throws IllegalArgumentException;
    /**
     * Getter for all reference values of a field
     * @param field the field
     * @return Iterator over all reference values of a field
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    Iterator<Reference> getReferences(String field) throws IllegalArgumentException;
    /**
     * Adds the object as value to the field.
     * <p>The type of the value is inferred based on the type of the Object.<br>
     * Supported Types are:</p>
     * <ul>
     * <li>{@link Reference}
     * <li>URL, URI: {@link Reference} instances are created for such values</li>
     * <li>Boolean, Integer, Long, Double and Float as primitive data types</li>
     * <li>{@link Date} 
     * <li>{@link Text}
     * <li>String[]{text,language}: text and language (further entries are ignored)</li>
     * <li>String: mapped to {@link Text} with the <code>language=null</code></li>
     * <li>{@link Collection}s, {@link Enumeration}s and {@link Iterator}s are 
     *     can be used to parse multiple values
     * <li>For unsupported types it MUSST be assured the the toString() value
     *     of the stored Object is equals to the toString() value of the parsed
     *     object.</li>
     * </ul> 
     * @param field the field
     * @param value the value to add
     * @throws NullPointerException 
     * @throws IllegalArgumentException if <code>null</code> is parsed as field or
     * value and/or if an empty string is parsed as field
     */
    void add(String field, Object value) throws IllegalArgumentException;
    /**
     * Adds an reference to the field.
     * @param field the field
     * @param reference the string representation of the reference. Note that
     * the value will be interpreted as a "reference" so there might apply
     * some rules about the format of the string. Regardless of the implementation
     * any valid URI and URL need to be accepted as a valid reference value
     * @throws NullPointerException 
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field or reference
     */
    void addReference(String field, String reference) throws IllegalArgumentException;
    /**
     * Adds a natural language text as value for one or more languages
     * @param field the field to add the text as value
     * @param text the natural language text
     * @param language the text is set for all the parsed languages. Parse
     *             <code>null</code> to set the text also without any language
     *             information.
     * @throws NullPointerException 
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field; if <code>null</code> is parsed as text. 
     * NOTE that <code>null</code> is supported for languages.
     */
    void addNaturalText(String field,String text, String...languages) throws IllegalArgumentException;
    /**
     * Sets the value of the field to the parsed object. If the parsed value
     * is <code>null</code> than this method removes all values for the given
     * field<br>
     * This Method supports collections as well as value conversions for some
     * types. Please see the documentation of {@link #add(String, Object)} for
     * details.
     * @param field the field
     * @param value the new value for the field
     * @throws NullPointerException 
     * @throws IllegalArgumentException if <code>null</code> or an empty string 
     * is parsed as field
     */
    void set(String field, Object value) throws IllegalArgumentException;
    /**
     * Setter for the reference of a field. If the parsed value
     * is <code>null</code> than this method removes all values for the given
     * field.
     * @param field the field
     * @param reference the string representation of the reference. Note that
     * the value will be interpreted as a "reference" so there might apply
     * some rules about the format of the string. Regardless of the implementation
     * any valid URI and URL need to be accepted as a valid reference value
     * @throws IllegalArgumentException if <code>null</code>or an emtpy string is
     * parsed as field
     */
    void setReference(String field, String reference) throws IllegalArgumentException;
    /**
     * Setter for the natural language text value of a field in the given
     * languages. If <code>null</code> is parsed as text, all present values
     * for the parsed languages are removed (values of other languages are
     * not removed)
     * @param field the field
     * @param text the natural language text
     * @param language the languages of the parsed text. Parse
     *             <code>null</code> to set the text also without any language
     *             information.
     * @throws IllegalArgumentException if <code>null</code>or an emtpy string is
     * parsed as field
     */
    void setNaturalText(String field,String text, String...language) throws IllegalArgumentException;
    /**
     * Removes the parsed value form the field. If <code>null</code> is parsed
     * as value than the call is ignored.<p>
     * This methods follows the same conventions as {@link #add(String, Object)}
     * (e.g. parsing a {@link Collection} will cause all values within this
     * collection to be removed). See the documentation of {@link #add(String, Object)}
     * for details.
     * @param field the field
     * @param value the value to remove
     * @throws IllegalArgumentException if <code>null</code>or an emtpy string is
     * parsed as field
     */
    void remove(String field, Object value) throws IllegalArgumentException; 
    /**
     * Removes to parsed reference as value for the given field. If <code>null</code>
     * is parsed as reference, that the call is ignored.
     * @param field the field
     * @param reference the string representation of the reference. Note that
     * the value will be interpreted as a "reference" so there might apply
     * some rules about the format of the string. Regardless of the implementation
     * any valid URI and URL need to be accepted as a valid reference value
     * @throws NullPointerException if <code>null</code> is parsed as field
     * @throws IllegalArgumentException if an empty string is parsed as field
     */
    void removeReference(String field,String reference) throws NullPointerException, IllegalArgumentException;
    /**
     * Removes a natural language text in given languages form a field
     * @param field the field
     * @param text the natural language text
     * @param language the language(s) of the natural language text
     *             (If <code>null</code> is parsed as language, than also labels
     *             without language tag might be removed)
     * @throws IllegalArgumentException if <code>null</code>or an emtpy string is
     * parsed as field
     */
    void removeNaturalText(String field,String text,String...languages) throws IllegalArgumentException;
    /**
     * Removes all values of the field
     * @param field the field
     * @throws IllegalArgumentException if <code>null</code>or an emtpy string is
     * parsed as field
     */
    void removeAll(String field) throws IllegalArgumentException;
    /**
     * Removes all natural language texts for the given languages or all natural
     * language labels of no language or an empty array is parsed as language.
     * To remove values with no language, parse <code>null</code> as entry of the
     * languages array.
     * @param field the field.
     * @param languages the language(s) of the natural language text. If
     *             <code>null</code> or an empty array is parsed, than all
     *             natural language label are removed. To remove only labels with
     *             no language, <code>null</code> needs to be parsed as entry of
     *             this array. 
     * @throws IllegalArgumentException if <code>null</code>or an emtpy string is
     * parsed as field
     */
    void removeAllNaturalText(String field,String...languages) throws IllegalArgumentException;
    /**
     * Getter for all the present fields
     * @return the fields
     */
    Iterator<String> getFieldNames();


}
