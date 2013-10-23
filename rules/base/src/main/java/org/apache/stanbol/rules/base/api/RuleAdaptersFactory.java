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
 * The rule adapter factory allows to add, get, register and remove rule adapters ({@link RuleAdapter}).
 * 
 * @author anuzzolese
 * 
 */
public interface RuleAdaptersFactory {

    /**
     * It gets the list of available registered adapters.
     * 
     * @return the list of {@link RuleAdapter} objects available
     */
    List<RuleAdapter> listRuleAdapters();

    /**
     * It gets the registered rule adapter able to adapt object that are instances of the class
     * <code>type</code>
     * 
     * @param type
     *            the class that the adpter accepts
     * @return the right rule adpater able to adapt instances of the class <code>type</code>
     * @throws UnavailableRuleObjectException
     */
    RuleAdapter getRuleAdapter(Class<?> type) throws UnavailableRuleObjectException;

    /**
     * It adds a new {@link RuleAdapter} instance.
     * 
     * @param ruleAdapter
     *            the {@link RuleAdapter} instance
     * @throws UnavailableRuleObjectException
     */
    void addRuleAdapter(RuleAdapter ruleAdapter) throws UnavailableRuleObjectException;

    /**
     * It removes a new {@link RuleAdapter} instance from the list of registered adapters.
     * 
     * @param ruleAdapter
     *            the {@link RuleAdapter} instance to be removed
     * @throws UnavailableRuleObjectException
     */
    void removeRuleAdapter(RuleAdapter ruleAdapter) throws UnavailableRuleObjectException;

}
