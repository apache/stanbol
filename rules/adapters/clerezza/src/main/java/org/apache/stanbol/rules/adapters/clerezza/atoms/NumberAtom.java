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
package org.apache.stanbol.rules.adapters.clerezza.atoms;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.sparql.query.Expression;
import org.apache.clerezza.rdf.core.sparql.query.LiteralExpression;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaSparqlObject;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.Symbols;

/**
 * It adapts any NumberAtom to a number literal in Clerezza.
 * 
 * @author anuzzolese
 * 
 */
public class NumberAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption {

        org.apache.stanbol.rules.manager.atoms.NumberAtom tmp = (org.apache.stanbol.rules.manager.atoms.NumberAtom) ruleAtom;

        String number = tmp.getNumber();

        Expression exp = null;

        if (number.startsWith("\"") && number.endsWith("\"")) {
            number = number.substring(1, number.length() - 1);
        }

        if (number.startsWith(Symbols.variablesPrefix)) {
            exp = new Variable(number.replace(Symbols.variablesPrefix, ""));
        } else {
            exp = new LiteralExpression(LiteralFactory.getInstance().createTypedLiteral(number));
        }

        return (T) new ClerezzaSparqlObject(exp);
    }

}
