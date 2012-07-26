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

/**
 * Indicates, that the requested type is not supported. <p>
 * The definition of the model requires some types to be supported.
 * Implementation may support additional types. Components that use a specific
 * implementation may therefore use types that are not required to be supported.
 * However such components should also be able to deal with this kind of
 * exceptions.
 * 
 * @author Rupert Westenthaler
 *
 */
public class UnsupportedTypeException extends IllegalArgumentException {

    /**
     * uses the default serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    public UnsupportedTypeException(Class<?> type,String dataType) {
        this(type,dataType,null);
    }

    public UnsupportedTypeException(Class<?> type,String dataType, Throwable cause) {
        super(String.format("Values of Type \"%s\" are not supported for data type \"%s\"",
                type,dataType),cause);
    }

}
