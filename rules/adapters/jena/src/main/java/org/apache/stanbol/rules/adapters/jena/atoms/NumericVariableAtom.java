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

import java.net.URI;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.jena.JenaAdapter;
import org.apache.stanbol.rules.adapters.jena.NodeClauseEntry;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.Symbols;

import com.hp.hpl.jena.reasoner.rulesys.Node_RuleVariable;

/**
 * 
 * It adapts a NumericVariableAtom to a variable node in Jena.
 * 
 * @author anuzzolese
 * 
 */
public class NumericVariableAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption {

        org.apache.stanbol.rules.manager.atoms.NumericVariableAtom tmp = (org.apache.stanbol.rules.manager.atoms.NumericVariableAtom) ruleAtom;

        URI uri = tmp.getURI();

        String variable = uri.toString();
        variable = variable.replace(Symbols.variablesPrefix, "");

        if (variable.startsWith("?")) {
            variable = variable.substring(1);
        }
        
        Integer index = ((JenaAdapter)adapter).variableMap.get(variable);
        if(index == null){
            index = ((JenaAdapter)adapter).variableMap.size();
            ((JenaAdapter)adapter).variableMap.put(variable, index);
        }
        
        Node_RuleVariable ruleVariable = new Node_RuleVariable("?" + variable, index.intValue());

        return (T) new NodeClauseEntry(ruleVariable);
    }
}
