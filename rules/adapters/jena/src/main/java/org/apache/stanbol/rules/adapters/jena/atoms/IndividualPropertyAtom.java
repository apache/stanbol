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
import org.apache.stanbol.rules.adapters.jena.VariableClauseEntry;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * 
 * It adapts a IndividualPropertyAtom to triple pattern of Jena.
 * 
 * @author anuzzolese
 * 
 */
public class IndividualPropertyAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.IndividualPropertyAtom tmp = (org.apache.stanbol.rules.manager.atoms.IndividualPropertyAtom) ruleAtom;

        IObjectAtom argument1 = tmp.getArgument1();
        IObjectAtom argument2 = tmp.getArgument2();
        IObjectAtom objectProperty = tmp.getObjectProperty();

        System.out.println(argument1);
        ClauseEntry argument2ClauseEntry = adapter.adaptTo(argument2, Rule.class);
        ClauseEntry argument1ClauseEntry = adapter.adaptTo(argument1, Rule.class);
        ClauseEntry objectPropertyClauseEntry = adapter.adaptTo(objectProperty, Rule.class);

        Node subjectNode;
        Node predicateNode;
        Node objectNode;

        System.out.println(argument1ClauseEntry.getClass());
        if (argument1ClauseEntry instanceof NodeClauseEntry) {
            subjectNode = ((NodeClauseEntry) argument1ClauseEntry).getNode();
        } else if (argument1ClauseEntry instanceof VariableClauseEntry) {
            subjectNode = ((VariableClauseEntry) argument1ClauseEntry).getNode();
            System.out.println("Here");
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (objectPropertyClauseEntry instanceof NodeClauseEntry) {
            predicateNode = ((NodeClauseEntry) objectPropertyClauseEntry).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (argument2ClauseEntry instanceof NodeClauseEntry) {
            objectNode = ((NodeClauseEntry) argument2ClauseEntry).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        return (T) new TriplePattern(subjectNode, predicateNode, objectNode);

    }

}
