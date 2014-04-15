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
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * 
 * It adapts a DatavaluedPropertyAtom to triple pattern of Jena.
 * 
 * @author anuzzolese
 * 
 */
public class DatavaluedPropertyAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.DatavaluedPropertyAtom tmp = (org.apache.stanbol.rules.manager.atoms.DatavaluedPropertyAtom) ruleAtom;

        IObjectAtom argument1 = tmp.getArgument1();

        IObjectAtom datatypeProperty = tmp.getDatatypeProperty();

        RuleAtom argument2 = tmp.getArgument2();

        ClauseEntry argument2ClauseEntry = adapter.adaptTo(argument2, Rule.class);
        ClauseEntry argument1ClauseEntry = adapter.adaptTo(argument1, Rule.class);
        ClauseEntry datatypePropertyClauseEntry = adapter.adaptTo(datatypeProperty, Rule.class);

        Node subjectNode = null;
        Node predicateNode = null;
        Node objectNode = null;

        if (argument1ClauseEntry instanceof NodeClauseEntry) {
            subjectNode = ((NodeClauseEntry) argument1ClauseEntry).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (datatypePropertyClauseEntry instanceof NodeClauseEntry) {
            predicateNode = ((NodeClauseEntry) datatypePropertyClauseEntry).getNode();
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
