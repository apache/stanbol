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

import org.apache.stanbol.rules.base.api.RuleAtom;

public class AtomIterator implements Iterator<RuleAtom> {

    private int currentIndex;
    private int listSize;
    private RuleAtom[] kReSRuleAtoms;

    public AtomIterator(AtomList atomList) {
        this.listSize = atomList.size();
        this.kReSRuleAtoms = new RuleAtom[listSize];
        this.kReSRuleAtoms = atomList.toArray(this.kReSRuleAtoms);
        this.currentIndex = 0;
    }

    public boolean hasNext() {
        if (currentIndex < listSize) {
            return true;
        } else {
            return false;
        }
    }

    public RuleAtom next() {
        RuleAtom atom = kReSRuleAtoms[currentIndex];
        currentIndex++;
        return atom;
    }

    public void remove() {

    }

}
