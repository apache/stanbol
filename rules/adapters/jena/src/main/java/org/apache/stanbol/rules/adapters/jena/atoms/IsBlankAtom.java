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

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.jena.NodeClauseEntry;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.IObjectAtom;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Functor;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * 
 * It adapts a IsBlankAtom to the isBlankNode functor of Jena.
 * 
 * @author anuzzolese
 * 
 */
public class IsBlankAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.IsBlankAtom tmp = (org.apache.stanbol.rules.manager.atoms.IsBlankAtom) ruleAtom;

        IObjectAtom uriResource = tmp.getUriResource();

        ClauseEntry argumentClauseEntry = adapter.adaptTo(uriResource, Rule.class);

        Node argNode;

        if (argumentClauseEntry instanceof NodeClauseEntry) {
            argNode = ((NodeClauseEntry) argumentClauseEntry).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        java.util.List<Node> nodes = new ArrayList<Node>();

        nodes.add(argNode);

        return (T) new Functor("isBlankNode", nodes, BuiltinRegistry.theRegistry);

    }

}
