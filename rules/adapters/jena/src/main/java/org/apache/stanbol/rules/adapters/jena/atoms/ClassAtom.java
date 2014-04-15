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
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * 
 * It adapts a ClassAtom to triple pattern of Jena.
 * 
 * @author anuzzolese
 * 
 */
public class ClassAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.ClassAtom tmp = (org.apache.stanbol.rules.manager.atoms.ClassAtom) ruleAtom;

        IObjectAtom argument1 = tmp.getArgument1();
        IObjectAtom classResource = tmp.getClassResource();

        ClauseEntry argumentClauseEntry = adapter.adaptTo(argument1, Rule.class);
        ClauseEntry classClauseEntry = adapter.adaptTo(classResource, Rule.class);

        Node argumnetNode;
        Node classNode;

        if (argumentClauseEntry instanceof NodeClauseEntry) {
            argumnetNode = ((NodeClauseEntry) argumentClauseEntry).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        if (classClauseEntry instanceof NodeClauseEntry) {
            classNode = ((NodeClauseEntry) classClauseEntry).getNode();
        } else {
            throw new RuleAtomCallExeption(getClass());
        }

        return (T) new TriplePattern(argumnetNode, Node_RuleVariable.createURI(RDF.type.getURI()), classNode);

    }

}
