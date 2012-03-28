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

import java.util.Collection;
import java.util.Iterator;

import org.apache.stanbol.rules.base.api.Rule;

public class RuleList implements Collection<Rule> {

    private Rule[] rules;

    public RuleList() {

    }

    public boolean add(Rule semionRule) {
        if (rules == null) {
            rules = new Rule[1];
            rules[0] = semionRule;
        } else {
            Rule[] semionRulesCopy = new Rule[rules.length + 1];
            System.arraycopy(rules, 0, semionRulesCopy, 0, rules.length);
            semionRulesCopy[semionRulesCopy.length - 1] = semionRule;
            rules = semionRulesCopy;
        }
        return true;
    }

    public boolean addToHead(Rule semionRule) {
        if (rules == null) {
            rules = new Rule[1];
            rules[0] = semionRule;
        } else {
            Rule[] semionRulesCopy = new Rule[rules.length + 1];
            System.arraycopy(rules, 0, semionRulesCopy, 1, rules.length);
            semionRulesCopy[0] = semionRule;
            rules = semionRulesCopy;
        }
        return true;
    }

    public boolean addAll(Collection<? extends Rule> c) {

        Rule[] collectionOfSemionRules = new Rule[c.size()];
        collectionOfSemionRules = c.toArray(collectionOfSemionRules);

        if (rules == null) {
            rules = collectionOfSemionRules;
        } else {
            Rule[] semionRulesCopy = new Rule[rules.length + collectionOfSemionRules.length];
            System.arraycopy(rules, 0, semionRulesCopy, 0, rules.length);
            System.arraycopy(collectionOfSemionRules, 0, semionRulesCopy, rules.length,
                collectionOfSemionRules.length);
            rules = semionRulesCopy;
        }

        return true;
    }

    /**
     * To clear the collection
     */
    public void clear() {
        this.rules = null;
    }

    public boolean contains(Object o) {
        for (Rule semionRule : rules) {
            if (semionRule.equals(o)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {

        for (Object o : c) {
            for (Rule semionRule : rules) {
                if (!semionRule.equals(o)) {
                    return false;
                } else {
                    break;
                }
            }
        }
        return true;
    }

    public boolean isEmpty() {
        if (rules == null || (rules.length == 1 && rules[0] == null)) {
            return true;
        } else {
            return false;
        }
    }

    public Iterator<Rule> iterator() {
        return new RuleIterator(this);
    }

    public boolean remove(Object o) {
        boolean removed = false;
        for (int i = 0; i < rules.length && !removed; i++) {
            Rule semionRule = rules[i];
            if (semionRule.equals(o)) {
                Rule[] semionRulesCopy = new Rule[rules.length - 1];
                System.arraycopy(rules, 0, semionRulesCopy, 0, i);
                System.arraycopy(rules, i + 1, semionRulesCopy, 0, semionRulesCopy.length - i);
                rules = semionRulesCopy;
                removed = true;
            }
        }
        return removed;
    }

    public boolean removeAll(Collection<?> c) {
        if (contains(c)) {
            for (Object o : c) {
                boolean removed = false;
                for (int i = 0; i < rules.length && !removed; i++) {
                    Rule semionRule = rules[i];
                    if (semionRule.equals(o)) {
                        Rule[] semionRulesCopy = new Rule[rules.length - 1];
                        System.arraycopy(rules, 0, semionRulesCopy, 0, i);
                        System.arraycopy(rules, i + 1, semionRulesCopy, 0, semionRulesCopy.length - i);
                        rules = semionRulesCopy;
                        removed = true;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean retainAll(Collection<?> c) {
        Rule[] semionRulesCopy = null;
        Rule[] semionRulesTMP = null;
        for (Object o : c) {
            if (o instanceof Rule) {
                if (contains(o)) {
                    if (semionRulesCopy == null) {
                        semionRulesCopy = new Rule[1];
                        semionRulesCopy[0] = (Rule) o;
                    } else {
                        semionRulesTMP = new Rule[semionRulesCopy.length + 1];
                        System.arraycopy(semionRulesCopy, 0, semionRulesTMP, 0, semionRulesCopy.length);
                        semionRulesTMP[semionRulesTMP.length - 1] = (Rule) o;
                        semionRulesCopy = semionRulesTMP;
                    }
                }
            }
        }
        rules = semionRulesCopy;
        return true;
    }

    public int size() {
        if (rules == null) {
            return 0;
        } else {
            return rules.length;
        }

    }

    public Object[] toArray() {
        return rules;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        return (T[]) rules;
    }

}
