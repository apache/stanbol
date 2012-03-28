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

import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;

/**
 * Abstract class for an {@link AdaptableAtom}.<br/>
 * This class should be extended by any implementation o an {@link AdaptableAtom} as it override the
 * <code>setRuleAdapter</code> method providing a general implementation.
 * 
 * @author anuzzolese
 * 
 */
public abstract class AbstractAdaptableAtom implements AdaptableAtom {

    protected RuleAdapter adapter;

    @Override
    public void setRuleAdapter(RuleAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public abstract <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                                  UnavailableRuleObjectException,
                                                  UnsupportedTypeForExportException;

}
