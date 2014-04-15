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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * It adapts a StrAtom to the Jena strConcat functor.<br/>
 * In this case the strConcat returns the concatenation of the string representation of the literal with the
 * empty string.
 * 
 * @author anuzzolese
 * 
 */
public class StrAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {
        org.apache.stanbol.rules.manager.atoms.StrAtom tmp = (org.apache.stanbol.rules.manager.atoms.StrAtom) ruleAtom;

        IObjectAtom iObjectAtom = tmp.getUriResource();

        ClauseEntry iObjectClauseEntry = adapter.adaptTo(iObjectAtom, Rule.class);

        if (iObjectClauseEntry instanceof NodeClauseEntry) {
            Node node = ((NodeClauseEntry) iObjectClauseEntry).getNode();

            Node emptyString = Node_RuleVariable.createLiteral("");

            Node bindind = Node_RuleVariable.createVariable("str_reuslt" + System.currentTimeMillis());

            List<Node> args = new ArrayList<Node>();
            args.add(node);
            args.add(emptyString);
            args.add(bindind);

            Functor functor = new Functor("strConcat", args);
            
            List<ClauseEntry> clauseEntries = new ArrayList<ClauseEntry>();
            clauseEntries.add(functor);
            return (T) new HigherOrderClauseEntry(node, clauseEntries);

        } else {
            throw new RuleAtomCallExeption(getClass());
        }
    }

}
