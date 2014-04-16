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
package org.apache.stanbol.rules.adapters.sparql.atoms;

import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.sparql.SPARQLComparison;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.Symbols;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.ExpressionAtom;

/**
 * It adapts any DifferentAtom to a not equality test (i.e., !=) of SAPRQL.
 * 
 * @author anuzzolese
 * 
 */
public class DifferentAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.DifferentAtom tmp = (org.apache.stanbol.rules.manager.atoms.DifferentAtom) ruleAtom;
        ExpressionAtom expressionAtom1 = tmp.getStringFunctionAtom1();
        ExpressionAtom expressionAtom2 = tmp.getStringFunctionAtom2();

        SPARQLObject sparqlArgument1 = adapter.adaptTo(expressionAtom1, SPARQLObject.class);
        SPARQLObject sparqlArgument2 = adapter.adaptTo(expressionAtom2, SPARQLObject.class);

        String argument1 = sparqlArgument1.getObject();
        String argument2 = sparqlArgument2.getObject();

        argument1 = argument1.replace(Symbols.variablesPrefix, "");
        argument2 = argument2.replace(Symbols.variablesPrefix, "");

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(argument1);
        sb.append("!=");
        sb.append(argument2);
        sb.append(")");

        return (T) new SPARQLComparison(sb.toString());

    }

}
