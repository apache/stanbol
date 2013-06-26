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
 * Thrown when a parsed object value can not be converted by the converter
 * 
 * @author Rupert Westenthaler
 */
public class UnsupportedValueException extends RuntimeException {

    /**
     * default serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs the exception to be thrown if a converter does not support the the parsed value
     * {@link IndexValue}.
     * 
     * @param converter
     *            the converter (implement the {@link TypeConverter#toString()} method!)
     * @param type
     *            the IndexDataType
     * @param value
     *            the value
     */
    public UnsupportedValueException(TypeConverter<?> converter, IndexDataType type, Object value) {
        this(converter, type, value, null);
    }

    /**
     * Constructs the exception to be thrown if a converter does not support the the parsed value
     * {@link IndexValue}.
     * 
     * @param converter
     *            the converter (implement the {@link TypeConverter#toString()} method!)
     * @param type
     *            the IndexDataType
     * @param value
     *            the value
     * @param cause
     *            the cause
     */
    public UnsupportedValueException(TypeConverter<?> converter,
                                     IndexDataType type,
                                     Object value,
                                     Throwable cause) {
        super(
                String.format(
                    "%s does not support the parsed value %s! Value is not compatible with the parsed IndexDataType %s",
                    converter, value, type), cause);
    }
}
