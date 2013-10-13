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
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.atoms.NumericFunctionAtom;

/**
 * It adapts any MultiplicationAtom to a multiplication operator in SPARQL, i.e., the * operator.
 * 
 * @author anuzzolese
 * 
 */
public class MultiplicationAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption,
                                         UnavailableRuleObjectException,
                                         UnsupportedTypeForExportException {

        org.apache.stanbol.rules.manager.atoms.MultiplicationAtom tmp = (org.apache.stanbol.rules.manager.atoms.MultiplicationAtom) ruleAtom;

        NumericFunctionAtom argument1 = tmp.getNumericFunctionAtom1();
        NumericFunctionAtom argument2 = tmp.getNumericFunctionAtom2();

        SPARQLObject sparqlArgument1 = adapter.adaptTo(argument1, SPARQLObject.class);
        SPARQLObject sparqlArgument2 = adapter.adaptTo(argument2, SPARQLObject.class);

        String sparqlFunction1 = sparqlArgument1.getObject();
        String sparqlFunction2 = sparqlArgument2.getObject();

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(sparqlFunction1);
        sb.append(" * ");
        sb.append(sparqlFunction2);
        sb.append(")");

        return (T) new SPARQLComparison(sb.toString());

    }

}
