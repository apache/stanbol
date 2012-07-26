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

/**
 * The Converter Interface used by the {@link IndexValueFactory}.
 * 
 * @author Rupert Westenthaler
 * 
 * @param <T>
 *            the generic type of the java-object that can be converted to {@link IndexValue}s.
 */
public interface TypeConverter<T> {
    /**
     * Converts the parsed java instance to an index value
     * 
     * @param value
     *            the java instance
     * @return the index value representing the parsed java instance
     */
    IndexValue createIndexValue(T value);

    /**
     * Creates an java instance representing the parsed <code>IndexValue</code>
     * 
     * @param value
     *            the index value
     * @return the java instance representing the parsed index value
     * @throws if
     *             the <code>IndexType</code> of the parsed value is not compatible with this converter.
     */
    T createObject(IndexValue value) throws UnsupportedIndexTypeException, UnsupportedValueException;

    /**
     * Creates an java instance representing the parsed value as returned by the index.
     * 
     * @param type
     *            the index data type of the value. MUST NOT be <code>null</code>
     * @param value
     *            the value within the index. If <code>null</code> this method returns <code>null</code>.
     * @param lang
     *            the language
     * @return the java instance representing the parsed index value
     * @throws UnsupportedValueException
     *             if the value can not be processed by the Converter
     * @throws NullPointerException
     *             of the parsed {@link IndexDataType} is <code>null</code>
     */
    T createObject(IndexDataType type, Object value, String lang) throws UnsupportedIndexTypeException,
                                                                 UnsupportedValueException,
                                                                 NullPointerException;

    /**
     * Getter for the java type
     * 
     * @return the java class of the instances created by this converter
     */
    Class<T> getJavaType();

    /**
     * Getter for the index type
     * 
     * @return the index type of index values created by this converter
     */
    IndexDataType getIndexType();
}
