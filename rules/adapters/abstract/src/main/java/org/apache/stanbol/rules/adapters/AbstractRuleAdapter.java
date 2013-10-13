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

package org.apache.stanbol.rules.adapters;

import org.apache.stanbol.rules.base.api.Adaptable;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;

/**
 * Thi abstract class implements the method <code>adaptTo</code>
 * 
 * and introduced new methods that should be implemented by concrete adapters, i.e.,:
 * <ul>
 * <li><code>adaptRecipeTo</code></li>
 * <li><code>adaptRuleTo</code></li>
 * <li><code>adaptRuleAtomTo</code></li>
 * </ul>
 * 
 * @author anuzzolese
 * 
 */
public abstract class AbstractRuleAdapter implements RuleAdapter {

    public <T> T adaptTo(Adaptable adaptable, Class<T> type) throws RuleAtomCallExeption,
                                                            UnavailableRuleObjectException,
                                                            UnsupportedTypeForExportException {
        if (adaptable instanceof Recipe) {
            return adaptRecipeTo((Recipe) adaptable, type);
        } else if (adaptable instanceof Rule) {
            return adaptRuleTo((Rule) adaptable, type);
        } else if (adaptable instanceof RuleAtom) {
            return adaptRuleAtomTo((RuleAtom) adaptable, type);
        } else {
            throw new UnavailableRuleObjectException("The adaptable class " + adaptable.getClass()
                                                     + " is not supported by the adapter " + this.getClass());
        }
    }

    /**
     * It allows to adapt a {@link Recipe} object passed as first argument to an instance of the class passed
     * as second argument.
     * 
     * @param <T>
     * @param recipe
     *            {@link Recipe}
     * @param type
     *            {@link Class}
     * @return the <code>recipe</code> adapted to {@link Class} <code>type</code>
     * @throws RuleAtomCallExeption
     * @throws UnsupportedTypeForExportException
     * @throws UnavailableRuleObjectException
     */
    protected abstract <T> T adaptRecipeTo(Recipe recipe, Class<T> type) throws RuleAtomCallExeption,
                                                                        UnsupportedTypeForExportException,
                                                                        UnavailableRuleObjectException;

    /**
     * 
     * It allows to adapt a {@link Rule} object passed as first argument to an instance of the class passed as
     * second argument.
     * 
     * @param <T>
     * @param rule
     *            {@link Rule}
     * @param type
     *            {@link Class}
     * @return the <code>rule</code> adapted to the {@link Class} <code>type</code>
     * @throws RuleAtomCallExeption
     * @throws UnsupportedTypeForExportException
     * @throws UnavailableRuleObjectException
     */
    protected abstract <T> T adaptRuleTo(Rule rule, Class<T> type) throws RuleAtomCallExeption,
                                                                  UnsupportedTypeForExportException,
                                                                  UnavailableRuleObjectException;

    /**
     * 
     * It allows to adapt a {@link RuleAtom} object passed as first argument to an instance of the class
     * passed as second argument.
     * 
     * @param <T>
     * @param ruleAtom
     *            {@link RuleAtom}
     * @param type
     *            {@link Class}
     * @return the <code>ruleAtom</code> adapted to the {@link Class} <code>type</code>
     * @throws RuleAtomCallExeption
     * @throws UnsupportedTypeForExportException
     * @throws UnavailableRuleObjectException
     */
    protected abstract <T> T adaptRuleAtomTo(RuleAtom ruleAtom, Class<T> type) throws RuleAtomCallExeption,
                                                                              UnsupportedTypeForExportException,
                                                                              UnavailableRuleObjectException;
}
