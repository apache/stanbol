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
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;
import org.apache.stanbol.rules.manager.atoms.StringFunctionAtom;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * 
 * It adapts a LetAtom to the makeSkolem functor of Jena.
 * 
 * @author anuzzolese
 * 
 */
public class LetAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.LetAtom tmp = (org.apache.stanbol.rules.manager.atoms.LetAtom) ruleAtom;

        StringFunctionAtom parameterFunctionAtom = tmp.getParameterFunctionAtom();
        IObjectAtom variableIObjectAtom = tmp.getVariable();

        ClauseEntry parameterClauseEntry = adapter.adaptTo(parameterFunctionAtom, Rule.class);
        ClauseEntry variableClauseEntry = adapter.adaptTo(variableIObjectAtom, Rule.class);

        List<ClauseEntry> clauseEntries = new ArrayList<ClauseEntry>();

        Node parameterNode;
        Node variableNode;

        if (parameterClauseEntry instanceof HigherOrderClauseEntry) {
            parameterNode = ((HigherOrderClauseEntry) parameterClauseEntry).getBindableNode();

            clauseEntries.addAll(((HigherOrderClauseEntry) parameterClauseEntry).getClauseEntries());
        } else if (parameterClauseEntry instanceof NodeClauseEntry) {
            parameterNode = ((NodeClauseEntry) parameterClauseEntry).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (variableClauseEntry instanceof NodeClauseEntry) {
            variableNode = ((NodeClauseEntry) variableClauseEntry).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        java.util.List<Node> nodes = new ArrayList<Node>();

        nodes.add(variableNode);
        nodes.add(parameterNode);

        ClauseEntry clauseEntry = new Functor("makeSkolem", nodes, BuiltinRegistry.theRegistry);

        clauseEntries.add(clauseEntry);

        return (T) new HigherOrderClauseEntry(variableNode, clauseEntries);

    }

}
