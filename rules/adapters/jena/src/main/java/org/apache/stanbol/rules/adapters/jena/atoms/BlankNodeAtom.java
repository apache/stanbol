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

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.jena.NodeClauseEntry;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * 
 * It adapts a BlankNodeAtom to triple pattern of Jena.
 * 
 * @author anuzzolese
 * 
 */
public class BlankNodeAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.BlankNodeAtom tmp = (org.apache.stanbol.rules.manager.atoms.BlankNodeAtom) ruleAtom;

        IObjectAtom argument1 = tmp.getArgument1();
        IObjectAtom argument2 = tmp.getArgument2();

        ClauseEntry argument1CE = adapter.adaptTo(argument1, Rule.class);
        ClauseEntry argument2CE = adapter.adaptTo(argument2, Rule.class);

        Node arg1Node;
        Node arg2Node;

        if (argument1CE instanceof NodeClauseEntry) {
            arg1Node = ((NodeClauseEntry) argument1CE).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (argument2CE instanceof NodeClauseEntry) {
            arg2Node = ((NodeClauseEntry) argument2CE).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        Node blank = Node_RuleVariable.createAnon();

        return (T) new TriplePattern(arg2Node, arg1Node, blank);

    }

}
