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

import java.lang.reflect.Type;

/**
 * This exception is thrown when no adapter is available to do a required java-object to {@link IndexValue} or
 * {@link IndexValue} to java-object adapter is registered to the used {@link IndexValueFactory}.
 * 
 * @author Rupert Westenthaler
 */
public class NoConverterException extends RuntimeException {

    /**
     * default serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create an instance of <code>NoAdapterException</code> indicating that no adapter is available for the
     * type.
     * 
     * @param type
     *            the type for which no adapter is available
     */
    public NoConverterException(Type type) {
        super("No adapter available for type " + type);
    }
}
