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
package org.apache.stanbol.rules.base.api.util;

import java.util.Iterator;

import org.apache.stanbol.rules.base.api.Rule;

public class RuleIterator implements Iterator<Rule> {

    private int currentIndex;
    private int listSize;
    private Rule[] semionRules;

    public RuleIterator(RuleList semionRuleList) {
        this.listSize = semionRuleList.size();
        this.semionRules = new Rule[listSize];
        this.semionRules = semionRuleList.toArray(this.semionRules);
        this.currentIndex = 0;

    }

    public boolean hasNext() {
        if (currentIndex < (listSize)) {
            return true;
        } else {
            return false;
        }
    }

    public Rule next() {
        Rule semionRule = semionRules[currentIndex];
        currentIndex++;
        return semionRule;
    }

    public void remove() {

    }

}
