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

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.sparql.query.LiteralExpression;
import org.apache.stanbol.rules.adapters.AbstractAdaptableAtom;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaSparqlObject;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.manager.atoms.ExpressionAtom;
import org.apache.stanbol.rules.manager.atoms.NumberAtom;
import org.apache.stanbol.rules.manager.atoms.StringAtom;

/**
 * It adapts any TypedLiteralAtom to a typed literal in Clerezza.
 * 
 * @author anuzzolese
 * 
 */
public class TypedLiteralAtom extends AbstractAdaptableAtom {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T adapt(RuleAtom ruleAtom) throws RuleAtomCallExeption {

        org.apache.stanbol.rules.manager.atoms.TypedLiteralAtom tmp = (org.apache.stanbol.rules.manager.atoms.TypedLiteralAtom) ruleAtom;

        ExpressionAtom expressionAtom = tmp.getValue();

        Literal literal = null;

        if (expressionAtom instanceof StringAtom) {
            String value = expressionAtom.toString();

            literal = LiteralFactory.getInstance().createTypedLiteral(value);
        } else if (expressionAtom instanceof NumberAtom) {
            Number number = ((NumberAtom) expressionAtom).getNumberValue();
            literal = LiteralFactory.getInstance().createTypedLiteral(number);
        } else {
            throw new org.apache.stanbol.rules.base.api.RuleAtomCallExeption(getClass());
        }

        LiteralExpression literalExpression = new LiteralExpression(literal);

        return (T) new ClerezzaSparqlObject(literalExpression);

    }

}
