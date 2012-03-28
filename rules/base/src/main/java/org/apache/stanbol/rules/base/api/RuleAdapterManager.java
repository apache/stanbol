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

import java.util.List;

/**
 * The {@link RuleAdapterManager} allows to manage rule adapters.<br/>
 * A rule adapter is able to adapt a {@link Recipe} to an external representation, e.g., Jena rules, SPARQL,
 * Clerezza, etc...
 * 
 * @author anuzzolese
 * 
 */
public interface RuleAdapterManager {

    /**
     * It adapts the {@link Adaptable} object to the class provided as second parameter.
     * 
     * @param <AdaptedTo>
     * @param adaptable
     *            {The object that we want to adapt, e.g., a Recipe}
     * @param adaptedToType
     *            {The object that we want in output}
     * @return
     * @throws UnavailableRuleObjectException
     */
    <AdaptedTo> RuleAdapter getAdapter(Adaptable adaptable, Class<AdaptedTo> adaptedToType) throws UnavailableRuleObjectException;

    /**
     * It returns the list of available rule adapters.
     * 
     * @return the list of available adapters
     */
    List<RuleAdapter> listRuleAdapters();

}
