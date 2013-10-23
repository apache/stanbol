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

package org.apache.stanbol.rules.base.api;

/**
 * 
 * This interface provides methods that allow to adapt Stanbol adaptable rule objects to specific classes
 * provided as input.
 * 
 * 
 * @author anuzzolese
 * 
 */

public interface RuleAdapter {

    /**
     * It asks to the adapter if it is able to adapt the adaptable object passed as first argument to the type
     * passed as second argument.
     * 
     * @param <T>
     * @param adaptable
     *            {@link Adaptable}
     * @param type
     *            {@link Class<T>}
     * @return <code>true</code> if the adapter can work in transforming the adaptable object to the specified
     *         type, false otherwise.
     */
    <T> boolean canAdaptTo(Adaptable adaptable, Class<T> type);

    /**
     * It returns the object adapted by the adapter.
     * 
     * @param <T>
     * @param adaptable
     *            {@link Adaptable}
     * @param type
     *            {@link Class<T>}
     * @return <T> the adapted object
     * @throws RuleAtomCallExeption
     * @throws UnavailableRuleObjectException
     * @throws UnsupportedTypeForExportException
     */
    <T> T adaptTo(Adaptable adaptable, Class<T> type) throws RuleAtomCallExeption,
                                                     UnavailableRuleObjectException,
                                                     UnsupportedTypeForExportException;

    /**
     * It return the type that the concrete adapter implementation is able to manage.
     * 
     * @param <T>
     * @return the type that the concrete adapter implementation is able to manage {@link Class<T>}
     */
    <T> Class<T> getExportClass();

}
