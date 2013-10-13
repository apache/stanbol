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
package org.apache.stanbol.rules.manager.atoms;

import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.util.AtomList;

public class UnionAtom implements RuleAtom {

    private AtomList atomList1;
    private AtomList atomList2;

    public UnionAtom(AtomList atomList1, AtomList atomList2) {
        this.atomList1 = atomList1;
        this.atomList2 = atomList2;
    }

    public AtomList getAtomList1() {
        return atomList1;
    }

    public AtomList getAtomList2() {
        return atomList2;
    }

    @Override
    public String toString() {
        String scope1 = "";

        for (RuleAtom ruleAtom : atomList1) {
            if (!scope1.isEmpty()) {
                scope1 += " . ";
            }
            scope1 += ruleAtom.toString();
        }

        String scope2 = "";

        for (RuleAtom ruleAtom : atomList2) {
            if (!scope2.isEmpty()) {
                scope2 += " . ";
            }
            scope2 += ruleAtom.toString();
        }

        return "union(" + scope1 + ", " + scope2 + ")";
    }

    @Override
    public String prettyPrint() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("the union of the set of conjunctive atoms {");

        boolean first = true;
        for (RuleAtom ruleAtom : atomList1) {
            if (!first) {
                stringBuilder.append(" AND ");
            } else {
                first = false;
            }

            stringBuilder.append(ruleAtom.toString());
        }

        stringBuilder.append("} with the set of conjunctive atoms {");

        first = true;
        for (RuleAtom ruleAtom : atomList2) {
            if (!first) {
                stringBuilder.append(" AND ");
            } else {
                first = false;
            }

            stringBuilder.append(ruleAtom.toString());
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }

}
