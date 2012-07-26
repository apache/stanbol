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
 * Thrown when the index value type is not compatible with the converter
 * 
 * @author Rupert Westenthaler
 */
public class UnsupportedIndexTypeException extends RuntimeException {

    /**
     * default serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs the exception to be thrown when a an IndexType is not supported by the current configuration
     * of the {@link IndexValueFactory}
     * 
     * @param indexType
     *            the unsupported <code>IndexType</code>
     */
    public UnsupportedIndexTypeException(IndexDataType indexType) {
        super(String.format("No Converter for IndexType %s registered!", indexType));
    }

    /**
     * Constructs the exception to be thrown if a converter does not support the {@link IndexDataType} of the
     * parsed {@link IndexValue}.
     * 
     * @param converter
     *            the converter (implement the {@link TypeConverter#toString()} method!)
     * @param type
     *            the unsupported {@link IndexDataType}
     */
    public UnsupportedIndexTypeException(TypeConverter<?> converter, IndexDataType type) {
        super(String.format("%s does not support the IndexType %s!", converter, type));
    }
}
