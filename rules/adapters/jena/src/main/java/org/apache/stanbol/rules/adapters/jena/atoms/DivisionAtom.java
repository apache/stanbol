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

package org.apache.stanbol.rules.adapters.jena.atoms;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.jena.HigherOrderClauseEntry;
import org.apache.stanbol.rules.adapters.jena.NodeClauseEntry;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.NumericFunctionAtom;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * 
 * It adapts any DivisionAtom to a quotient functor in Jena.
 * 
 * @author anuzzolese
 * 
 */
public class DivisionAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        String div_result = "div_result" + System.currentTimeMillis();

        Node arg1Node = null;
        Node arg2Node = null;
        Node arg3Node = Node_RuleVariable.createVariable(div_result);

        org.apache.stanbol.rules.manager.atoms.DivisionAtom tmp = (org.apache.stanbol.rules.manager.atoms.DivisionAtom) ruleAtom;

        NumericFunctionAtom numericFunctionAtom1 = tmp.getNumericFunctionAtom1();
        NumericFunctionAtom numericFunctionAtom2 = tmp.getNumericFunctionAtom2();

        ClauseEntry clauseEntry1 = adapter.adaptTo(numericFunctionAtom1, Rule.class);
        ClauseEntry clauseEntry2 = adapter.adaptTo(numericFunctionAtom2, Rule.class);

        List<ClauseEntry> clauseEntries = new ArrayList<ClauseEntry>();

        if (clauseEntry1 instanceof HigherOrderClauseEntry) {
            arg1Node = ((HigherOrderClauseEntry) clauseEntry1).getBindableNode();

            clauseEntries.addAll(((HigherOrderClauseEntry) clauseEntry1).getClauseEntries());
        } else if (clauseEntry1 instanceof NodeClauseEntry) {
            arg1Node = ((NodeClauseEntry) clauseEntry1).getNode();
        } else {
            throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
        }

        if (clauseEntry2 instanceof HigherOrderClauseEntry) {
            arg2Node = ((HigherOrderClauseEntry) clauseEntry2).getBindableNode();

            clauseEntries.addAll(((HigherOrderClauseEntry) clauseEntry2).getClauseEntries());
        } else if (clauseEntry2 instanceof NodeClauseEntry) {
            arg2Node = ((NodeClauseEntry) clauseEntry2).getNode();
        } else {
            throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
        }

        java.util.List<Node> nodes = new ArrayList<Node>();

        nodes.add(arg1Node);
        nodes.add(arg2Node);
        nodes.add(arg3Node);

        ClauseEntry clauseEntry = new Functor("quotient", nodes, BuiltinRegistry.theRegistry);

        clauseEntries.add(clauseEntry);

        return (T) new HigherOrderClauseEntry(arg3Node, clauseEntries);

    }

}
